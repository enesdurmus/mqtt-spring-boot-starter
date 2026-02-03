package io.github.enesdurmus.mqtt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as an MQTT message listener.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @MqttListener(topics = "sensor/temperature", qos = 1)
 * public void handleTemperature(TemperatureReading reading) {
 *     // process the reading
 * }
 *
 * // With property placeholder
 * @MqttListener(topics = "${mqtt.topics.sensor}")
 * public void handleSensor(SensorData data) {
 *     // process sensor data
 * }
 *
 * // With topic injection
 * @MqttListener(topics = "sensor/#")
 * public void handleWildcard(@Topic String topic, @Payload SensorData data) {
 *     System.out.println("Received from: " + topic);
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MqttListener {

    /**
     * The topics to subscribe to. Supports property placeholders (e.g., "${mqtt.topic}").
     * Wildcards are supported: '+' for single level, '#' for multi-level.
     */
    String[] topics();

    /**
     * Quality of Service level for the subscription.
     * <ul>
     *   <li>0 - At most once (fire and forget)</li>
     *   <li>1 - At least once (acknowledged delivery)</li>
     *   <li>2 - Exactly once (assured delivery)</li>
     * </ul>
     */
    int qos() default 0;

    /**
     * Unique identifier for this listener. Used for logging and management.
     * If not specified, a default ID will be generated.
     */
    String id() default "";

    /**
     * Bean name of a custom {@link MqttListenerErrorHandler} to use for this listener.
     * If not specified, the default error handler will be used.
     */
    String errorHandler() default "";
}
