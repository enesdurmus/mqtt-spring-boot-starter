# MQTT Spring Boot Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.enesdurmus/mqtt-spring-boot-starter.svg)](https://search.maven.org/artifact/io.github.enesdurmus/mqtt-spring-boot-starter)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Spring Boot starter for MQTT integration with annotation-driven listeners, similar to `@SqsListener` or `@KafkaListener`.

## Features

- **`@MqttListener`** - Annotation-based topic subscription
- **Programmatic Listeners** - Create listeners dynamically with `MqttListenerContainerFactory`
- **`@Topic`** - Inject the actual topic name (useful for wildcards)
- **`@Payload`** - Explicit payload binding
- **`@Header`** - Access message metadata (QoS, retained, messageId)
- **`MqttTemplate`** - Easy message publishing
- **SpEL Support** - Use `${property}` placeholders in topics
- **Error Handling** - Configurable error handlers per listener
- **Auto JSON** - Automatic Jackson serialization/deserialization

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.enesdurmus</groupId>
    <artifactId>mqtt-spring-boot-starter</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.enesdurmus:mqtt-spring-boot-starter:1.1.0'
```

## Configuration

```yaml
mqtt:
  url: tcp://localhost:1883
  client-id: my-app
  username: user          # optional
  password: secret        # optional
  clean-session: true
  keep-alive-interval: 60
  connection-timeout: 30
  concurrency: 5
  queue-capacity: 100
```

| Property | Description | Default |
|----------|-------------|---------|
| `mqtt.url` | Broker URL (required) | - |
| `mqtt.client-id` | Client identifier | `spring-mqtt-client` |
| `mqtt.username` | Authentication username | - |
| `mqtt.password` | Authentication password | - |
| `mqtt.clean-session` | Start with clean session | `true` |
| `mqtt.keep-alive-interval` | Keep-alive in seconds | `60` |
| `mqtt.connection-timeout` | Connection timeout in seconds | `30` |
| `mqtt.concurrency` | Thread pool size | `3` |
| `mqtt.queue-capacity` | Message queue capacity | `100` |

## Usage

### Annotation-Based Listeners

#### Basic Listener

```java
@Component
public class SensorListener {

    @MqttListener(topics = "sensor/temperature", qos = 1)
    public void handleTemperature(TemperatureReading reading) {
        System.out.println("Temperature: " + reading.value());
    }
}
```

#### Topic Injection (Wildcards)

```java
@MqttListener(topics = "devices/#")
public void handleDevice(@Topic String topic, @Payload DeviceEvent event) {
    System.out.println("Event from " + topic + ": " + event);
}
```

#### Header Injection

```java
@MqttListener(topics = "events/+")
public void handleEvent(@Payload String payload,
                        @Header("qos") int qos,
                        @Header("retained") boolean retained) {
    System.out.println("QoS: " + qos + ", Retained: " + retained);
}
```

#### Raw Bytes

```java
@MqttListener(topics = "binary/data")
public void handleBinary(byte[] data) {
    System.out.println("Received " + data.length + " bytes");
}
```

#### Property Placeholders

```java
@MqttListener(topics = "${mqtt.topics.sensor}")
public void handleSensor(SensorData data) {
    // topic resolved from application.yml
}
```

### Programmatic Listeners

Use `MqttListenerContainerFactory` when you need dynamic topic subscription at runtime.

#### Basic Programmatic Listener

```java
@Configuration
public class MqttConfig {

    @Bean
    public MqttMessageListenerContainer temperatureListener(MqttListenerContainerFactory factory) {
        return factory.createContainer("sensor/+/temperature")
            .id("temperature-listener")
            .qos(1)
            .messageHandler((topic, payload, message) -> {
                System.out.println("Temperature from " + topic + ": " + payload);
            })
            .build();
    }
}
```

#### Dynamic Topics from Configuration

```java
@Bean
public MqttMessageListenerContainer deviceListener(
        MqttListenerContainerFactory factory,
        @Value("${device.id}") String deviceId) {

    return factory.createContainer("devices/" + deviceId + "/events")
        .id("device-" + deviceId)
        .qos(1)
        .messageHandler((topic, payload, message) -> {
            log.info("Device {} event: {}", deviceId, payload);
        })
        .errorHandler((topic, msg, ex) -> {
            log.error("Failed to process event from {}", topic, ex);
        })
        .build();
}
```

#### Multiple Dynamic Listeners

```java
@Bean
public List<MqttMessageListenerContainer> sensorListeners(
        MqttListenerContainerFactory factory,
        SensorRepository sensorRepository) {

    return sensorRepository.findAll().stream()
        .map(sensor -> factory.createContainer("sensors/" + sensor.getId() + "/data")
            .id("sensor-" + sensor.getId())
            .qos(sensor.getRequiredQos())
            .messageHandler((topic, payload, message) -> {
                processSensorData(sensor, payload);
            })
            .build())
        .toList();
}
```

#### Manual Lifecycle Control

```java
@Bean
public MqttMessageListenerContainer manualListener(MqttListenerContainerFactory factory) {
    return factory.createContainer("manual/topic")
        .autoStartup(false)  // Don't start automatically
        .messageHandler((topic, payload, message) -> {
            // handle message
        })
        .build();
}

// Later, start/stop manually:
@Autowired
private MqttMessageListenerContainer manualListener;

public void startListening() {
    manualListener.start();
}

public void stopListening() {
    manualListener.stop();
}
```

### Error Handling

#### Global Error Handler

```java
@Bean
public MqttListenerErrorHandler mqttListenerErrorHandler() {
    return (topic, message, exception) -> {
        log.error("Failed processing message from {}", topic, exception);
        // send to dead letter topic, metrics, alerting, etc.
    };
}
```

#### Per-Listener Error Handler

```java
@Bean
public MqttListenerErrorHandler criticalErrorHandler() {
    return (topic, message, exception) -> {
        alertingService.sendAlert("Critical MQTT error", exception);
    };
}

@MqttListener(topics = "critical/#", errorHandler = "criticalErrorHandler")
public void handleCritical(CriticalEvent event) {
    // ...
}
```

### Publishing Messages

```java
@Service
public class NotificationService {

    private final MqttTemplate mqttTemplate;

    public NotificationService(MqttTemplate mqttTemplate) {
        this.mqttTemplate = mqttTemplate;
    }

    public void notify(String message) throws MqttException {
        mqttTemplate.publish("notifications", message);
    }

    public void notifyWithQos(String message) throws MqttException {
        mqttTemplate.publish("notifications", message, 2, false);
    }
}
```

## Supported Parameter Types

| Type | Description |
|------|-------------|
| `String` | Payload as string |
| `byte[]` | Raw payload bytes |
| `MqttMessage` | Full Paho message object |
| `@Payload T` | Deserialized JSON payload |
| `@Topic String` | The topic name |
| `@Header("qos") int` | QoS level (0, 1, 2) |
| `@Header("retained") boolean` | Retained flag |
| `@Header("duplicate") boolean` | Duplicate flag |
| `@Header("messageId") int` | Message ID |

## Requirements

- Java 17+
- Spring Boot 3.x

## License

Apache License 2.0
