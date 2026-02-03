package io.github.enesdurmus.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttMessageListenerContainerTest {

    @Mock
    private MqttClient mqttClient;

    @Mock
    private ThreadPoolTaskExecutor executor;

    private MqttListenerContainerFactory factory;

    @BeforeEach
    void setUp() {
        factory = new MqttListenerContainerFactory(mqttClient, executor);
    }

    @Test
    void builderCreatesContainerWithCorrectConfiguration() {
        MqttMessageListenerContainer container = factory.createContainer("test/topic")
                .id("my-container")
                .qos(1)
                .messageHandler((topic, payload, message) -> {})
                .autoStartup(false)
                .build();

        assertEquals("my-container", container.getId());
        assertEquals(1, container.getTopics().size());
        assertEquals("test/topic", container.getTopics().get(0));
        assertEquals(1, container.getQos());
        assertFalse(container.isAutoStartup());
        assertFalse(container.isRunning());
    }

    @Test
    void builderSupportsMultipleTopics() {
        MqttMessageListenerContainer container = factory.createContainer("topic1", "topic2", "topic3")
                .messageHandler((topic, payload, message) -> {})
                .build();

        assertEquals(3, container.getTopics().size());
        assertTrue(container.getTopics().contains("topic1"));
        assertTrue(container.getTopics().contains("topic2"));
        assertTrue(container.getTopics().contains("topic3"));
    }

    @Test
    void builderThrowsExceptionWhenMessageHandlerNotSet() {
        MqttMessageListenerContainer.Builder builder = factory.createContainer("test/topic");

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void builderThrowsExceptionWhenTopicsEmpty() {
        MqttMessageListenerContainer.Builder builder = factory.createContainer()
                .messageHandler((topic, payload, message) -> {});

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void builderThrowsExceptionForInvalidQos() {
        assertThrows(IllegalArgumentException.class, () ->
                factory.createContainer("test/topic").qos(3));

        assertThrows(IllegalArgumentException.class, () ->
                factory.createContainer("test/topic").qos(-1));
    }

    @Test
    void startSubscribesToTopics() throws Exception {
        MqttMessageListenerContainer container = factory.createContainer("test/topic")
                .qos(2)
                .messageHandler((topic, payload, message) -> {})
                .build();

        container.start();

        verify(mqttClient).subscribe(eq("test/topic"), eq(2), any());
        assertTrue(container.isRunning());
    }

    @Test
    void stopUnsubscribesFromTopics() throws Exception {
        MqttMessageListenerContainer container = factory.createContainer("test/topic")
                .messageHandler((topic, payload, message) -> {})
                .build();

        container.start();
        assertTrue(container.isRunning());

        container.stop();

        verify(mqttClient).unsubscribe("test/topic");
        assertFalse(container.isRunning());
    }

    @Test
    void startIsIdempotent() throws Exception {
        MqttMessageListenerContainer container = factory.createContainer("test/topic")
                .messageHandler((topic, payload, message) -> {})
                .build();

        container.start();
        container.start(); // second call should be no-op

        verify(mqttClient, times(1)).subscribe(eq("test/topic"), anyInt(), any());
    }

    @Test
    void stopIsIdempotent() throws Exception {
        MqttMessageListenerContainer container = factory.createContainer("test/topic")
                .messageHandler((topic, payload, message) -> {})
                .build();

        container.stop(); // should be no-op when not running

        verify(mqttClient, never()).unsubscribe(anyString());
    }

    @Test
    void containerGeneratesIdWhenNotProvided() {
        MqttMessageListenerContainer container = factory.createContainer("test/topic")
                .messageHandler((topic, payload, message) -> {})
                .build();

        assertNotNull(container.getId());
        assertTrue(container.getId().startsWith("mqtt-container-"));
    }

    @Test
    void containerDefaultsToAutoStartupTrue() {
        MqttMessageListenerContainer container = factory.createContainer("test/topic")
                .messageHandler((topic, payload, message) -> {})
                .build();

        assertTrue(container.isAutoStartup());
    }

    @Test
    void containerDefaultsToQosZero() {
        MqttMessageListenerContainer container = factory.createContainer("test/topic")
                .messageHandler((topic, payload, message) -> {})
                .build();

        assertEquals(0, container.getQos());
    }
}
