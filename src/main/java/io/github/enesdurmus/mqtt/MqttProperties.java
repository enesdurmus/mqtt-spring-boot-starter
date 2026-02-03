package io.github.enesdurmus.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MQTT connection and listener settings.
 *
 * <p>Example configuration in application.yml:
 * <pre>
 * mqtt:
 *   url: tcp://localhost:1883
 *   client-id: my-app
 *   username: user
 *   password: secret
 *   clean-session: true
 *   keep-alive-interval: 60
 *   connection-timeout: 30
 *   concurrency: 5
 *   queue-capacity: 100
 * </pre>
 */
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    /**
     * MQTT broker URL (required). Example: tcp://localhost:1883 or ssl://broker.example.com:8883
     */
    private String url;

    /**
     * Client identifier. Must be unique per connection to the broker.
     */
    private String clientId = "spring-mqtt-client";

    /**
     * Username for broker authentication (optional).
     */
    private String username;

    /**
     * Password for broker authentication (optional).
     */
    private String password;

    /**
     * Whether to start with a clean session.
     * If true, the broker will not store any subscription info or undelivered messages.
     */
    private boolean cleanSession = true;

    /**
     * Keep-alive interval in seconds. The client will send ping requests at this interval.
     */
    private int keepAliveInterval = 60;

    /**
     * Connection timeout in seconds.
     */
    private int connectionTimeout = 30;

    /**
     * Number of concurrent threads for processing messages.
     */
    private int concurrency = 3;

    /**
     * Queue capacity for pending messages when all threads are busy.
     */
    private int queueCapacity = 100;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}
