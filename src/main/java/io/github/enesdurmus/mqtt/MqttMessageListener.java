package io.github.enesdurmus.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Internal message listener that receives MQTT messages and delegates
 * to the appropriate endpoint for processing.
 */
class MqttMessageListener implements IMqttMessageListener {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageListener.class);

    private final MqttListenerEndpoint endpoint;
    private final ThreadPoolTaskExecutor executor;
    private final MqttListenerErrorHandler defaultErrorHandler;

    MqttMessageListener(MqttListenerEndpoint endpoint,
                        ThreadPoolTaskExecutor executor,
                        MqttListenerErrorHandler defaultErrorHandler) {
        this.endpoint = endpoint;
        this.executor = executor;
        this.defaultErrorHandler = defaultErrorHandler;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());

        executor.execute(() -> {
            try {
                endpoint.invoke(topic, payload, message);
            } catch (Exception e) {
                handleException(topic, message, e);
            }
        });
    }

    private void handleException(String topic, MqttMessage message, Exception e) {
        MqttListenerErrorHandler handler = endpoint.getErrorHandler();
        if (handler == null) {
            handler = defaultErrorHandler;
        }

        if (handler != null) {
            try {
                handler.handleError(topic, message, e);
            } catch (Exception handlerException) {
                log.error("Error handler threw exception for message from topic [{}], listener [{}]",
                        topic, endpoint.getId(), handlerException);
            }
        } else {
            // Default behavior: log the error
            log.error("Failed to process message from topic [{}], listener [{}]: {}",
                    topic, endpoint.getId(), e.getMessage(), e);
        }
    }

    public MqttListenerEndpoint getEndpoint() {
        return endpoint;
    }
}
