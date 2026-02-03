package io.github.enesdurmus.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Represents a single MQTT listener endpoint, encapsulating the target method
 * and its configuration.
 */
class MqttListenerEndpoint {

    private final String id;
    private final String beanName;
    private final Method method;
    private final List<String> topics;
    private final int qos;
    private final String errorHandlerBeanName;
    private final MessageConverter converter;
    private final Class<?>[] parameterTypes;
    private final Annotation[][] parameterAnnotations;

    private Object beanProxy;
    private MqttListenerErrorHandler errorHandler;

    public MqttListenerEndpoint(String id,
                                String beanName,
                                Method method,
                                List<String> topics,
                                int qos,
                                String errorHandlerBeanName,
                                MessageConverter converter) {
        this.id = id;
        this.beanName = beanName;
        this.method = method;
        this.topics = topics;
        this.qos = qos;
        this.errorHandlerBeanName = errorHandlerBeanName;
        this.converter = converter;
        this.parameterTypes = method.getParameterTypes();
        this.parameterAnnotations = method.getParameterAnnotations();
    }

    /**
     * Invoke the listener method with the given message.
     *
     * @param topic   the topic the message was received from
     * @param payload the message payload as string
     * @param message the original MQTT message
     */
    public void invoke(String topic, String payload, MqttMessage message) {
        try {
            Object[] args = resolveArguments(topic, payload, message);
            method.invoke(getBeanProxy(), args);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke method: " + method, e);
        }
    }

    private Object[] resolveArguments(String topic, String payload, MqttMessage message) throws Exception {
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = resolveArgument(i, topic, payload, message);
        }

        return args;
    }

    private Object resolveArgument(int index, String topic, String payload, MqttMessage message) throws Exception {
        Class<?> type = parameterTypes[index];
        Annotation[] annotations = parameterAnnotations[index];

        // Check for @Topic annotation
        if (hasAnnotation(annotations, Topic.class)) {
            if (type != String.class) {
                throw new IllegalStateException("@Topic parameter must be of type String");
            }
            return topic;
        }

        // Check for @Header annotation
        Header headerAnnotation = getAnnotation(annotations, Header.class);
        if (headerAnnotation != null) {
            return resolveHeader(headerAnnotation.value(), type, message);
        }

        // Check for @Payload annotation or treat as payload by default
        if (hasAnnotation(annotations, Payload.class) || isPayloadCandidate(type, annotations)) {
            return resolvePayload(type, payload, message);
        }

        // MqttMessage injection (for backward compatibility)
        if (type == MqttMessage.class) {
            return message;
        }

        // Default: treat as payload
        return resolvePayload(type, payload, message);
    }

    private boolean isPayloadCandidate(Class<?> type, Annotation[] annotations) {
        // If no special annotations, and not a special type, it's a payload candidate
        return !hasAnnotation(annotations, Topic.class) &&
               !hasAnnotation(annotations, Header.class) &&
               type != MqttMessage.class;
    }

    private Object resolvePayload(Class<?> type, String payload, MqttMessage message) throws Exception {
        if (type == String.class) {
            return payload;
        }
        if (type == byte[].class) {
            return message.getPayload();
        }
        return converter.read(payload, type);
    }

    private Object resolveHeader(String headerName, Class<?> type, MqttMessage message) {
        return switch (headerName.toLowerCase()) {
            case "qos" -> convertToType(message.getQos(), type);
            case "retained" -> convertToType(message.isRetained(), type);
            case "duplicate" -> convertToType(message.isDuplicate(), type);
            case "messageid", "message_id", "id" -> convertToType(message.getId(), type);
            default -> throw new IllegalStateException("Unknown header: " + headerName +
                    ". Supported headers: qos, retained, duplicate, messageId");
        };
    }

    private Object convertToType(Object value, Class<?> targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            if (value instanceof Boolean b) {
                return b ? 1 : 0;
            }
            return ((Number) value).intValue();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Number n) {
                return n.intValue() != 0;
            }
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        return value;
    }

    private boolean hasAnnotation(Annotation[] annotations, Class<? extends Annotation> annotationType) {
        return getAnnotation(annotations, annotationType) != null;
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> annotationType) {
        for (Annotation annotation : annotations) {
            if (annotationType.isInstance(annotation)) {
                return (A) annotation;
            }
        }
        return null;
    }

    public void setBeanProxy(Object beanProxy) {
        this.beanProxy = beanProxy;
    }

    public void setErrorHandler(MqttListenerErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public String getId() {
        return id;
    }

    public List<String> getTopics() {
        return topics;
    }

    public int getQos() {
        return qos;
    }

    public String getBeanName() {
        return beanName;
    }

    public Object getBeanProxy() {
        return beanProxy;
    }

    public Method getMethod() {
        return method;
    }

    public String getErrorHandlerBeanName() {
        return errorHandlerBeanName;
    }

    public MqttListenerErrorHandler getErrorHandler() {
        return errorHandler;
    }
}
