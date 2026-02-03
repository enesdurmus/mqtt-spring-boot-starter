package io.github.enesdurmus.mqtt;

import java.util.ArrayList;
import java.util.Collections;
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
    private final List<MqttListenerEndpoint> endpoints = Collections.synchronizedList(new ArrayList<>());

    /**
     * Register a listener endpoint.
     *
     * @param endpoint the endpoint to register
     */
    void register(MqttListenerEndpoint endpoint) {
        endpointsById.put(endpoint.getId(), endpoint);
        endpoints.add(endpoint);
    }

    /**
     * Get all registered endpoints.
     *
     * @return unmodifiable list of all endpoints
     */
    public List<MqttListenerEndpoint> getAllEndpoints() {
        return Collections.unmodifiableList(new ArrayList<>(endpoints));
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
        return endpoints.size();
    }
}
