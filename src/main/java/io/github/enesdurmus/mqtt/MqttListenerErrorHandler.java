package io.github.enesdurmus.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Error handler for MQTT listener processing failures.
 * Implement this interface to provide custom error handling logic.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @Bean
 * public MqttListenerErrorHandler myErrorHandler() {
 *     return (topic, message, exception) -> {
 *         log.error("Failed to process message from {}: {}", topic, exception.getMessage());
 *         // Send to dead letter topic, metrics, alerting, etc.
 *     };
 * }
 * }
 * </pre>
 */
@FunctionalInterface
public interface MqttListenerErrorHandler {

    /**
     * Handle an error that occurred during message processing.
     *
     * @param topic     the topic the message was received from
     * @param message   the original MQTT message
     * @param exception the exception that occurred
     */
    void handleError(String topic, MqttMessage message, Exception exception);
}
