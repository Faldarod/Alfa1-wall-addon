package nl.alfaone.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class HomeAssistantClient {

    private final WebClient webClient;

    public HomeAssistantClient(@Value("${homeassistant.base-url}") String baseUrl,
                               @Value("${homeassistant.access-token}") String accessToken) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    public Mono<String> callService(String domain, String service, Object data) {
        return webClient.post()
                .uri("/api/services/{domain}/{service}", domain, service)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Get all states from Home Assistant
     * @return Flux of all entity states
     */
    public Flux<JsonNode> getAllStates() {
        return webClient.get()
                .uri("/api/states")
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .doOnError(e -> log.error("Error fetching states from Home Assistant", e));
    }

    /**
     * Get state of a specific entity
     * @param entityId The entity ID (e.g., "device_tracker.phone_123")
     * @return The entity state
     */
    public Mono<JsonNode> getState(String entityId) {
        return webClient.get()
                .uri("/api/states/{entity_id}", entityId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("Error fetching state for entity {}", entityId, e));
    }

    /**
     * Get all device_tracker entities from Home Assistant
     * @return Flux of device_tracker entity states
     */
    public Flux<JsonNode> getDeviceTrackers() {
        return getAllStates()
                .filter(state -> state.has("entity_id") &&
                        state.get("entity_id").asText().startsWith("device_tracker."))
                .doOnNext(state -> log.debug("Found device_tracker: {}", state.get("entity_id").asText()));
    }

    /**
     * Check if a specific device_tracker is home
     * @param entityId The device_tracker entity ID
     * @return true if the device is home, false otherwise
     */
    public Mono<Boolean> isDeviceHome(String entityId) {
        return getState(entityId)
                .map(state -> {
                    if (state.has("state")) {
                        String stateValue = state.get("state").asText().toLowerCase();
                        return "home".equals(stateValue);
                    }
                    return false;
                })
                .defaultIfEmpty(false)
                .doOnNext(isHome -> log.debug("Device {} is home: {}", entityId, isHome));
    }
}
