package io.github.enesdurmus.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Container that manages MQTT listener lifecycle, subscribing to topics
 * and handling message dispatch.
 */
class MqttListenerContainer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MqttListenerContainer.class);

    private final MqttClient mqttClient;
    private final MqttListenerRegistry registry;
    private final ThreadPoolTaskExecutor executor;
    private final MqttListenerErrorHandler defaultErrorHandler;

    private volatile boolean running = false;
    private final Set<String> subscribedTopics = new HashSet<>();

    MqttListenerContainer(MqttClient mqttClient,
                          MqttListenerRegistry registry,
                          ThreadPoolTaskExecutor executor,
                          MqttListenerErrorHandler defaultErrorHandler) {
        this.mqttClient = mqttClient;
        this.registry = registry;
        this.executor = executor;
        this.defaultErrorHandler = defaultErrorHandler;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        log.info("Starting MQTT listener container");

        List<MqttListenerEndpoint> endpoints = registry.getAllEndpoints();
        for (MqttListenerEndpoint endpoint : endpoints) {
            try {
                subscribeEndpoint(endpoint);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to subscribe listener [" + endpoint.getId() + "]", e);
            }
        }

        running = true;
        log.info("MQTT listener container started with {} endpoints", endpoints.size());
    }

    private void subscribeEndpoint(MqttListenerEndpoint endpoint) throws MqttException {
        for (String topic : endpoint.getTopics()) {
            MqttMessageListener messageListener = new MqttMessageListener(endpoint, executor, defaultErrorHandler);
            mqttClient.subscribe(topic, endpoint.getQos(), messageListener);
            subscribedTopics.add(topic);
            log.debug("Subscribed listener [{}] to topic [{}] with QoS {}", endpoint.getId(), topic, endpoint.getQos());
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        log.info("Stopping MQTT listener container");

        for (String topic : subscribedTopics) {
            try {
                mqttClient.unsubscribe(topic);
                log.debug("Unsubscribed from topic [{}]", topic);
            } catch (MqttException e) {
                log.warn("Failed to unsubscribe from topic [{}]: {}", topic, e.getMessage());
            }
        }

        subscribedTopics.clear();
        running = false;
        log.info("MQTT listener container stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Start after most beans, stop before most beans
        return Integer.MAX_VALUE - 100;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
