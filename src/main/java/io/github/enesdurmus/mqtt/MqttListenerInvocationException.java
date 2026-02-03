package io.github.enesdurmus.mqtt;

/**
 * Exception thrown when a listener method invocation fails.
 * This exception properly unwraps the underlying cause for better error messages.
 */
public class MqttListenerInvocationException extends RuntimeException {

    public MqttListenerInvocationException(String message) {
        super(message);
    }

    public MqttListenerInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
