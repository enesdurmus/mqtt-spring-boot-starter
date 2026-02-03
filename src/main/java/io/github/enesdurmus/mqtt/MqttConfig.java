package io.github.enesdurmus.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Auto-configuration for MQTT listener infrastructure.
 */
@Configuration
@ConditionalOnProperty(prefix = "mqtt", name = "url")
@EnableConfigurationProperties(MqttProperties.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new JacksonMessageConverter(objectMapper);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public MqttListenerRegistry mqttListenerRegistry() {
        return new MqttListenerRegistry();
    }

    @Bean(name = "mqttListenerExecutor")
    public ThreadPoolTaskExecutor mqttListenerExecutor(MqttProperties mqttProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(mqttProperties.getConcurrency());
        executor.setMaxPoolSize(mqttProperties.getConcurrency() * 2);
        executor.setQueueCapacity(mqttProperties.getQueueCapacity());
        executor.setThreadNamePrefix("mqtt-listener-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttListenerErrorHandler mqttListenerErrorHandler() {
        // Default error handler that just logs
        return (topic, message, exception) ->
                log.error("Error processing message from topic [{}]: {}", topic, exception.getMessage(), exception);
    }

    @Bean
    public MqttListenerContainer mqttListenerContainer(MqttClient listenerClient,
                                                       MqttListenerRegistry mqttListenerRegistry,
                                                       @Qualifier("mqttListenerExecutor") ThreadPoolTaskExecutor executor,
                                                       MqttListenerErrorHandler mqttListenerErrorHandler) {
        return new MqttListenerContainer(listenerClient, mqttListenerRegistry, executor, mqttListenerErrorHandler);
    }

    @Bean
    public MqttListenerBeanPostProcessor mqttListenerBeanPostProcessor(MqttListenerRegistry mqttListenerRegistry,
                                                                       MessageConverter messageConverter) {
        return new MqttListenerBeanPostProcessor(mqttListenerRegistry, messageConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttTemplate mqttTemplate(@Qualifier("publisherClient") MqttClient publisherClient,
                                     MessageConverter messageConverter) {
        return new DefaultMqttTemplate(publisherClient, messageConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttListenerContainerFactory mqttListenerContainerFactory(
            MqttClient listenerClient,
            @Qualifier("mqttListenerExecutor") ThreadPoolTaskExecutor executor) {
        return new MqttListenerContainerFactory(listenerClient, executor);
    }

    @Bean(destroyMethod = "close")
    public MqttClient listenerClient(MqttProperties props) throws MqttException {
        return buildClient(props, "listener-");
    }

    @Bean(name = "publisherClient", destroyMethod = "close")
    @Primary
    public MqttClient publisherClient(MqttProperties props) throws MqttException {
        return buildClient(props, "publisher-");
    }

    private MqttClient buildClient(MqttProperties mqttProperties, String prefix) throws MqttException {
        String brokerUrl = mqttProperties.getUrl();
        String clientId = prefix + mqttProperties.getClientId();

        MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(mqttProperties.isCleanSession());
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(mqttProperties.getConnectionTimeout());
        options.setKeepAliveInterval(mqttProperties.getKeepAliveInterval());

        // Only set credentials if username is provided
        if (StringUtils.hasText(mqttProperties.getUsername())) {
            options.setUserName(mqttProperties.getUsername());
            if (mqttProperties.getPassword() != null) {
                options.setPassword(mqttProperties.getPassword().toCharArray());
            }
        }

        client.connect(options);
        log.info("Connected MQTT client [{}] to broker [{}]", clientId, brokerUrl);
        return client;
    }
}
