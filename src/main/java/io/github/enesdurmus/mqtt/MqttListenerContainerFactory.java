package io.github.enesdurmus.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Factory for creating {@link MqttMessageListenerContainer} instances programmatically.
 *
 * <p>Use this factory when you need to create listeners dynamically at runtime,
 * for example when topics are determined by configuration or user input.
 *
 * <p>Example usage with dynamic topics:
 * <pre>
 * {@code
 * @Configuration
 * public class MqttConfig {
 *
 *     @Bean
 *     public MqttMessageListenerContainer deviceListener(
 *             MqttListenerContainerFactory factory,
 *             @Value("${device.id}") String deviceId) {
 *
 *         return factory.createContainer("devices/" + deviceId + "/events")
 *             .id("device-" + deviceId + "-listener")
 *             .qos(1)
 *             .messageHandler((topic, payload, message) -> {
 *                 log.info("Device event from {}: {}", topic, payload);
 *             })
 *             .errorHandler((topic, msg, ex) -> {
 *                 log.error("Failed to process device event", ex);
 *             })
 *             .build();
 *     }
 * }
 * }
 * </pre>
 *
 * <p>Example with multiple dynamic listeners:
 * <pre>
 * {@code
 * @Bean
 * public List<MqttMessageListenerContainer> sensorListeners(
 *         MqttListenerContainerFactory factory,
 *         SensorRepository sensorRepository) {
 *
 *     return sensorRepository.findAll().stream()
 *         .map(sensor -> factory.createContainer("sensors/" + sensor.getId() + "/data")
 *             .id("sensor-" + sensor.getId())
 *             .qos(sensor.getQos())
 *             .messageHandler((topic, payload, message) -> {
 *                 processSensorData(sensor, payload);
 *             })
 *             .build())
 *         .toList();
 * }
 * }
 * </pre>
 *
 * @see MqttMessageListenerContainer
 * @see MqttMessageHandler
 */
public class MqttListenerContainerFactory {

    private final MqttClient mqttClient;
    private final ThreadPoolTaskExecutor executor;

    public MqttListenerContainerFactory(MqttClient mqttClient, ThreadPoolTaskExecutor executor) {
        this.mqttClient = mqttClient;
        this.executor = executor;
    }

    /**
     * Create a new container builder for the specified topics.
     *
     * @param topics the topics to subscribe to (supports MQTT wildcards)
     * @return a builder for configuring the container
     */
    public MqttMessageListenerContainer.Builder createContainer(String... topics) {
        return new MqttMessageListenerContainer.Builder(mqttClient, executor)
                .topics(topics);
    }

    /**
     * Create a new container builder without pre-configured topics.
     * Topics must be set using {@link MqttMessageListenerContainer.Builder#topics(String...)}.
     *
     * @return a builder for configuring the container
     */
    public MqttMessageListenerContainer.Builder createContainer() {
        return new MqttMessageListenerContainer.Builder(mqttClient, executor);
    }
}
