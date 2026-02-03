package io.github.enesdurmus.mqtt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject the topic name into a listener method parameter.
 * The annotated parameter must be of type {@code String}.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @MqttListener(topics = "sensor/#")
 * public void handleMessage(@Topic String topic, @Payload SensorData data) {
 *     System.out.println("Received from: " + topic);
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Topic {
}
