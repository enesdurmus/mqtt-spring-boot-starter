package io.github.enesdurmus.mqtt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing MQTT listener endpoints.
 * Thread-safe for concurrent access during bean initialization.
 */
class MqttListenerRegistry {

    private final Map<String, MqttListenerEndpoint> endpointsById = new ConcurrentHashMap<>();

    /**
     * Register a listener endpoint.
     *
     * @param endpoint the endpoint to register
     */
    void register(MqttListenerEndpoint endpoint) {
        endpointsById.put(endpoint.getId(), endpoint);
    }

    /**
     * Get all registered endpoints.
     *
     * @return unmodifiable list of all endpoints
     */
    public List<MqttListenerEndpoint> getAllEndpoints() {
        return new ArrayList<>(endpointsById.values());
    }

    /**
     * Get an endpoint by its ID.
     *
     * @param id the endpoint ID
     * @return the endpoint, or empty if not found
     */
    public Optional<MqttListenerEndpoint> getEndpointById(String id) {
        return Optional.ofNullable(endpointsById.get(id));
    }

    /**
     * Get the number of registered endpoints.
     *
     * @return the endpoint count
     */
    public int size() {
        return endpointsById.size();
    }
}
