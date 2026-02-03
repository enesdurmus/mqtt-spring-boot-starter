package io.github.enesdurmus.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqttListenerEndpointTest {

    @Test
    void invokeCallsMethodWithCorrectArguments() throws Exception {
        // given
        Method method = TestBean.class.getDeclaredMethod("handleMessage", String.class, MqttMessage.class);
        TestBean bean = spy(new TestBean());
        MessageConverter converter = mock(MessageConverter.class);
        MqttListenerEndpoint endpoint = new MqttListenerEndpoint("test-id", "testBean", method, List.of("topic1"), 1, "", converter);
        endpoint.setBeanProxy(bean);
        MqttMessage context = new MqttMessage("context".getBytes());

        // when
        endpoint.invoke("topic1", "payload", context);

        // then
        verify(bean).handleMessage("payload", context);
    }

    @Test
    void invokeHandlesNullPayloadGracefully() throws Exception {
        // given
        Method method = TestBean.class.getDeclaredMethod("handleMessage", String.class, MqttMessage.class);
        TestBean bean = spy(new TestBean());
        MessageConverter converter = mock(MessageConverter.class);
        MqttListenerEndpoint endpoint = new MqttListenerEndpoint("test-id", "testBean", method, List.of("topic1"), 1, "", converter);
        endpoint.setBeanProxy(bean);
        MqttMessage context = new MqttMessage("context".getBytes());

        // when
        endpoint.invoke("topic1", null, context);

        // then
        verify(bean).handleMessage(null, context);
    }

    @Test
    void invokeConvertsPayloadToTargetType() throws Exception {
        // given
        Method method = TestBean.class.getDeclaredMethod("handleConvertedMessage", Integer.class);
        TestBean bean = spy(new TestBean());
        MessageConverter converter = mock(MessageConverter.class);
        when(converter.read("42", Integer.class)).thenReturn(42);
        MqttListenerEndpoint endpoint = new MqttListenerEndpoint("test-id", "testBean", method, List.of("topic1"), 1, "", converter);
        endpoint.setBeanProxy(bean);
        MqttMessage message = new MqttMessage("42".getBytes());

        // when
        endpoint.invoke("topic1", "42", message);

        // then
        verify(bean).handleConvertedMessage(42);
    }

    @Test
    void invokeThrowsIllegalStateExceptionWhenConverterFails() throws Exception {
        // given
        Method method = TestBean.class.getDeclaredMethod("handleConvertedMessage", Integer.class);
        TestBean bean = new TestBean();
        MessageConverter converter = mock(MessageConverter.class);
        when(converter.read("invalid", Integer.class)).thenThrow(new RuntimeException("Conversion failed"));
        MqttListenerEndpoint endpoint = new MqttListenerEndpoint("test-id", "testBean", method, List.of("topic1"), 1, "", converter);
        endpoint.setBeanProxy(bean);
        MqttMessage message = new MqttMessage("invalid".getBytes());

        // when / then
        assertThrows(IllegalStateException.class, () -> endpoint.invoke("topic1", "invalid", message));
    }

    @Test
    void invokeInjectsTopicWhenTopicAnnotationPresent() throws Exception {
        // given
        Method method = TestBean.class.getDeclaredMethod("handleWithTopic", String.class, String.class);
        TestBean bean = spy(new TestBean());
        MessageConverter converter = mock(MessageConverter.class);
        MqttListenerEndpoint endpoint = new MqttListenerEndpoint("test-id", "testBean", method, List.of("sensor/#"), 1, "", converter);
        endpoint.setBeanProxy(bean);
        MqttMessage message = new MqttMessage("payload".getBytes());

        // when
        endpoint.invoke("sensor/temperature", "payload", message);

        // then
        verify(bean).handleWithTopic("sensor/temperature", "payload");
    }

    @Test
    void invokeInjectsHeaderWhenHeaderAnnotationPresent() throws Exception {
        // given
        Method method = TestBean.class.getDeclaredMethod("handleWithHeader", String.class, int.class);
        TestBean bean = spy(new TestBean());
        MessageConverter converter = mock(MessageConverter.class);
        MqttListenerEndpoint endpoint = new MqttListenerEndpoint("test-id", "testBean", method, List.of("topic1"), 1, "", converter);
        endpoint.setBeanProxy(bean);
        MqttMessage message = new MqttMessage("payload".getBytes());
        message.setQos(2);

        // when
        endpoint.invoke("topic1", "payload", message);

        // then
        verify(bean).handleWithHeader("payload", 2);
    }

    @Test
    void invokeHandlesByteArrayPayload() throws Exception {
        // given
        Method method = TestBean.class.getDeclaredMethod("handleBytes", byte[].class);
        TestBean bean = spy(new TestBean());
        MessageConverter converter = mock(MessageConverter.class);
        MqttListenerEndpoint endpoint = new MqttListenerEndpoint("test-id", "testBean", method, List.of("topic1"), 1, "", converter);
        endpoint.setBeanProxy(bean);
        byte[] bytes = "payload".getBytes();
        MqttMessage message = new MqttMessage(bytes);

        // when
        endpoint.invoke("topic1", "payload", message);

        // then
        verify(bean).handleBytes(bytes);
    }

    static class TestBean {
        void handleMessage(String payload, MqttMessage context) {
        }

        void handleConvertedMessage(Integer value) {
        }

        void handleWithTopic(@Topic String topic, @Payload String payload) {
        }

        void handleWithHeader(@Payload String payload, @Header("qos") int qos) {
        }

        void handleBytes(byte[] data) {
        }

        void invalidMethod() {
        }
    }
}
