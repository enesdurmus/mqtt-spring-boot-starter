package io.github.enesdurmus.mqtt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to explicitly mark a parameter as the message payload.
 * When used, the parameter will receive the deserialized message content.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @MqttListener(topics = "sensor/temperature")
 * public void handleMessage(@Payload TemperatureReading reading) {
 *     // process the reading
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Payload {
}
