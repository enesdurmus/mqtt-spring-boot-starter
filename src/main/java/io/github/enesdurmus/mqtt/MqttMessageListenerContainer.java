package io.github.enesdurmus.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A container for a programmatically registered MQTT message listener.
 * Manages the lifecycle of a single listener subscription.
 *
 * <p>Create instances using {@link MqttListenerContainerFactory}:
 * <pre>
 * {@code
 * @Bean
 * public MqttMessageListenerContainer temperatureListener(MqttListenerContainerFactory factory) {
 *     return factory.createContainer("sensor/+/temperature")
 *         .id("temperature-listener")
 *         .qos(1)
 *         .messageHandler((topic, payload, message) -> {
 *             System.out.println("Temperature from " + topic + ": " + payload);
 *         })
 *         .build();
 * }
 * }
 * </pre>
 *
 * @see MqttListenerContainerFactory
 * @see MqttMessageHandler
 */
public class MqttMessageListenerContainer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageListenerContainer.class);

    private final String id;
    private final List<String> topics;
    private final int qos;
    private final MqttMessageHandler messageHandler;
    private final MqttListenerErrorHandler errorHandler;
    private final MqttClient mqttClient;
    private final ThreadPoolTaskExecutor executor;
    private final boolean autoStartup;

    private volatile boolean running = false;

    private MqttMessageListenerContainer(Builder builder) {
        this.id = builder.id != null ? builder.id : "mqtt-container-" + UUID.randomUUID().toString().substring(0, 8);
        this.topics = builder.topics;
        this.qos = builder.qos;
        this.messageHandler = Objects.requireNonNull(builder.messageHandler, "messageHandler is required");
        this.errorHandler = builder.errorHandler;
        this.mqttClient = builder.mqttClient;
        this.executor = builder.executor;
        this.autoStartup = builder.autoStartup;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        log.info("Starting MQTT listener container [{}] for topics {}", id, topics);

        for (String topic : topics) {
            try {
                mqttClient.subscribe(topic, qos, createMessageListener(topic));
                log.debug("Container [{}] subscribed to topic [{}] with QoS {}", id, topic, qos);
            } catch (MqttException e) {
                throw new IllegalStateException("Failed to subscribe container [" + id + "] to topic [" + topic + "]", e);
            }
        }

        running = true;
        log.info("MQTT listener container [{}] started", id);
    }

    private IMqttMessageListener createMessageListener(String subscribedTopic) {
        return (topic, message) -> {
            String payload = new String(message.getPayload());

            executor.execute(() -> {
                try {
                    messageHandler.handleMessage(topic, payload, message);
                } catch (Exception e) {
                    handleException(topic, message, e);
                }
            });
        };
    }

    private void handleException(String topic, MqttMessage message, Exception e) {
        if (errorHandler != null) {
            try {
                errorHandler.handleError(topic, message, e);
            } catch (Exception handlerException) {
                log.error("Error handler threw exception for container [{}], topic [{}]",
                        id, topic, handlerException);
            }
        } else {
            log.error("Error processing message in container [{}] from topic [{}]: {}",
                    id, topic, e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        log.info("Stopping MQTT listener container [{}]", id);

        for (String topic : topics) {
            try {
                mqttClient.unsubscribe(topic);
                log.debug("Container [{}] unsubscribed from topic [{}]", id, topic);
            } catch (MqttException e) {
                log.warn("Failed to unsubscribe container [{}] from topic [{}]: {}", id, topic, e.getMessage());
            }
        }

        running = false;
        log.info("MQTT listener container [{}] stopped", id);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return autoStartup;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 50;
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

    /**
     * Builder for creating {@link MqttMessageListenerContainer} instances.
     */
    public static class Builder {
        private String id;
        private List<String> topics;
        private int qos = 0;
        private MqttMessageHandler messageHandler;
        private MqttListenerErrorHandler errorHandler;
        private MqttClient mqttClient;
        private ThreadPoolTaskExecutor executor;
        private boolean autoStartup = true;

        Builder(MqttClient mqttClient, ThreadPoolTaskExecutor executor) {
            this.mqttClient = mqttClient;
            this.executor = executor;
        }

        /**
         * Set the unique identifier for this container.
         * If not set, a random UUID-based ID will be generated.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set the topics to subscribe to.
         * Supports MQTT wildcards: '+' for single level, '#' for multi-level.
         */
        public Builder topics(String... topics) {
            this.topics = Arrays.asList(topics);
            return this;
        }

        /**
         * Set the topics to subscribe to.
         */
        public Builder topics(List<String> topics) {
            this.topics = topics;
            return this;
        }

        /**
         * Set the Quality of Service level.
         * <ul>
         *   <li>0 - At most once</li>
         *   <li>1 - At least once</li>
         *   <li>2 - Exactly once</li>
         * </ul>
         */
        public Builder qos(int qos) {
            if (qos < 0 || qos > 2) {
                throw new IllegalArgumentException("QoS must be 0, 1, or 2");
            }
            this.qos = qos;
            return this;
        }

        /**
         * Set the message handler for processing incoming messages.
         * This is required.
         */
        public Builder messageHandler(MqttMessageHandler handler) {
            this.messageHandler = handler;
            return this;
        }

        /**
         * Set a custom error handler for this container.
         */
        public Builder errorHandler(MqttListenerErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * Set whether this container should start automatically.
         * Default is true.
         */
        public Builder autoStartup(boolean autoStartup) {
            this.autoStartup = autoStartup;
            return this;
        }

        /**
         * Build the container instance.
         *
         * @throws NullPointerException if messageHandler is not set
         * @throws IllegalArgumentException if topics is empty
         */
        public MqttMessageListenerContainer build() {
            if (topics == null || topics.isEmpty()) {
                throw new IllegalArgumentException("At least one topic is required");
            }
            return new MqttMessageListenerContainer(this);
        }
    }
}
