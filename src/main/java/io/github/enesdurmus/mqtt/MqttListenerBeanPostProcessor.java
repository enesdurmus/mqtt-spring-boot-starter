package io.github.enesdurmus.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bean post-processor that discovers methods annotated with {@link MqttListener}
 * and registers them as listener endpoints.
 */
class MqttListenerBeanPostProcessor implements BeanPostProcessor, SmartInitializingSingleton,
        ApplicationContextAware, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(MqttListenerBeanPostProcessor.class);

    private final MqttListenerRegistry registry;
    private final MessageConverter converter;
    private final AtomicInteger listenerIdCounter = new AtomicInteger(0);

    private ApplicationContext applicationContext;
    private ConfigurableBeanFactory beanFactory;
    private BeanExpressionResolver resolver;
    private BeanExpressionContext expressionContext;

    MqttListenerBeanPostProcessor(MqttListenerRegistry registry, MessageConverter converter) {
        this.registry = registry;
        this.converter = converter;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // Use target class to find annotations on proxied beans (e.g., @Transactional)
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        ReflectionUtils.doWithMethods(targetClass, method -> {
            MqttListener annotation = AnnotatedElementUtils.findMergedAnnotation(method, MqttListener.class);
            if (annotation != null) {
                registerEndpoint(beanName, method, annotation);
            }
        }, method -> !method.isBridge() && !method.isSynthetic());

        return bean;
    }

    private void registerEndpoint(String beanName, Method method, MqttListener annotation) {
        List<String> resolvedTopics = resolveTopics(annotation.topics());

        if (resolvedTopics.isEmpty()) {
            throw new IllegalStateException("No topics specified for @MqttListener on method: " + method);
        }

        String id = resolveId(annotation.id(), beanName, method);

        MqttListenerEndpoint endpoint = new MqttListenerEndpoint(
                id,
                beanName,
                method,
                resolvedTopics,
                annotation.qos(),
                annotation.errorHandler(),
                converter
        );

        registry.register(endpoint);
        log.debug("Registered MQTT listener [{}] for topics {} on method {}", id, resolvedTopics, method.getName());
    }

    private List<String> resolveTopics(String[] topics) {
        List<String> resolved = new ArrayList<>();
        for (String topic : topics) {
            String resolvedTopic = resolveExpression(topic);
            if (StringUtils.hasText(resolvedTopic)) {
                // Handle comma-separated topics in a single string
                if (resolvedTopic.contains(",")) {
                    Arrays.stream(resolvedTopic.split(","))
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .forEach(resolved::add);
                } else {
                    resolved.add(resolvedTopic.trim());
                }
            }
        }
        return resolved;
    }

    private String resolveExpression(String value) {
        if (beanFactory == null || !value.contains("${") && !value.contains("#{")) {
            return value;
        }

        // Resolve property placeholders like ${mqtt.topic}
        String resolved = beanFactory.resolveEmbeddedValue(value);

        // Resolve SpEL expressions like #{...}
        if (resolved != null && resolved.contains("#{") && resolver != null) {
            Object evaluated = resolver.evaluate(resolved, expressionContext);
            resolved = evaluated != null ? evaluated.toString() : null;
        }

        return resolved;
    }

    private String resolveId(String annotationId, String beanName, Method method) {
        if (StringUtils.hasText(annotationId)) {
            return resolveExpression(annotationId);
        }
        return beanName + "#" + method.getName() + "-" + listenerIdCounter.getAndIncrement();
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (MqttListenerEndpoint endpoint : registry.getAllEndpoints()) {
            try {
                Object beanProxy = applicationContext.getBean(endpoint.getBeanName());
                endpoint.setBeanProxy(beanProxy);

                // Resolve error handler if specified
                String errorHandlerBeanName = endpoint.getErrorHandlerBeanName();
                if (StringUtils.hasText(errorHandlerBeanName)) {
                    MqttListenerErrorHandler errorHandler = applicationContext.getBean(
                            errorHandlerBeanName, MqttListenerErrorHandler.class);
                    endpoint.setErrorHandler(errorHandler);
                }
            } catch (BeansException e) {
                log.error("Failed to resolve bean or error handler for listener [{}]: {}",
                        endpoint.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof ConfigurableBeanFactory cbf) {
            this.beanFactory = cbf;
            this.resolver = cbf.getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext(cbf, null);
        }
    }
}
