package io.github.enesdurmus.mqtt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MqttListenerBeanPostProcessorTest {

    @Mock
    private MqttListenerRegistry mqttListenerRegistry;

    @Mock
    private MessageConverter messageConverter;

    private MqttListenerBeanPostProcessor sut;

    @BeforeEach
    void setUp() {
        sut = new MqttListenerBeanPostProcessor(mqttListenerRegistry, messageConverter);
    }

    @Test
    void postProcessAfterInitializationRegistersEndpointWhenMqttListenerAnnotationIsPresent() {
        // given
        Object bean = new Object() {
            @MqttListener(topics = {"topic1"}, qos = 1)
            public void annotatedMethod() {
            }
        };
        String beanName = "testBean";

        // when
        sut.postProcessAfterInitialization(bean, beanName);

        // then
        verify(mqttListenerRegistry).register(argThat(endpoint ->
                Objects.equals(endpoint.getBeanName(), beanName) &&
                endpoint.getMethod().getName().equals("annotatedMethod") &&
                endpoint.getTopics().contains("topic1") &&
                endpoint.getQos() == 1
        ));
    }

    @Test
    void postProcessAfterInitializationRegistersEndpointWithCustomId() {
        // given
        Object bean = new Object() {
            @MqttListener(topics = {"topic1"}, id = "myCustomId")
            public void annotatedMethod() {
            }
        };
        String beanName = "testBean";

        // when
        sut.postProcessAfterInitialization(bean, beanName);

        // then
        verify(mqttListenerRegistry).register(argThat(endpoint ->
                Objects.equals(endpoint.getId(), "myCustomId")
        ));
    }

    @Test
    void postProcessAfterInitializationRegistersEndpointWithErrorHandler() {
        // given
        Object bean = new Object() {
            @MqttListener(topics = {"topic1"}, errorHandler = "myErrorHandler")
            public void annotatedMethod() {
            }
        };
        String beanName = "testBean";

        // when
        sut.postProcessAfterInitialization(bean, beanName);

        // then
        verify(mqttListenerRegistry).register(argThat(endpoint ->
                Objects.equals(endpoint.getErrorHandlerBeanName(), "myErrorHandler")
        ));
    }

    @Test
    void postProcessAfterInitializationDoesNotRegisterEndpointWhenNoMqttListenerAnnotationIsPresent() {
        // given
        Object bean = new Object() {
            public void nonAnnotatedMethod() {
            }
        };
        String beanName = "testBean";

        // when
        sut.postProcessAfterInitialization(bean, beanName);

        // then
        verifyNoInteractions(mqttListenerRegistry);
    }

    @Test
    void postProcessAfterInitializationHandlesBeanWithNoDeclaredMethods() {
        // given
        Object bean = new Object();
        String beanName = "testBean";

        // when
        sut.postProcessAfterInitialization(bean, beanName);

        // then
        verifyNoInteractions(mqttListenerRegistry);
    }

    @Test
    void postProcessAfterInitializationHandlesMultipleTopics() {
        // given
        Object bean = new Object() {
            @MqttListener(topics = {"topic1", "topic2", "topic3"})
            public void multiTopicMethod() {
            }
        };
        String beanName = "testBean";

        // when
        sut.postProcessAfterInitialization(bean, beanName);

        // then
        verify(mqttListenerRegistry).register(argThat(endpoint ->
                endpoint.getTopics().size() == 3 &&
                endpoint.getTopics().contains("topic1") &&
                endpoint.getTopics().contains("topic2") &&
                endpoint.getTopics().contains("topic3")
        ));
    }
}
