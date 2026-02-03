package io.github.enesdurmus.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Functional interface for handling MQTT messages programmatically.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * MqttMessageHandler handler = (topic, payload, message) -> {
 *     System.out.println("Received from " + topic + ": " + payload);
 * };
 * }
 * </pre>
 *
 * @see MqttListenerContainerFactory
 */
@FunctionalInterface
public interface MqttMessageHandler {

    /**
     * Handle an incoming MQTT message.
     *
     * @param topic   the topic the message was received from
     * @param payload the message payload as a string
     * @param message the raw MQTT message with headers (qos, retained, etc.)
     */
    void handleMessage(String topic, String payload, MqttMessage message);
}
