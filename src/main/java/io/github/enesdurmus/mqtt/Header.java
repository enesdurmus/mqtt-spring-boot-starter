package io.github.enesdurmus.mqtt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject MQTT message headers into a listener method parameter.
 *
 * <p>Supported header names:
 * <ul>
 *   <li>{@code qos} - Quality of Service level (int)</li>
 *   <li>{@code retained} - Whether the message was retained (boolean)</li>
 *   <li>{@code duplicate} - Whether the message is a duplicate (boolean)</li>
 *   <li>{@code messageId} - The message ID (int)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @MqttListener(topics = "sensor/temperature")
 * public void handleMessage(@Payload String data,
 *                           @Header("qos") int qos,
 *                           @Header("retained") boolean retained) {
 *     // process with header info
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Header {
    /**
     * The name of the header to inject.
     * Supported values: "qos", "retained", "duplicate", "messageId"
     */
    String value();
}
