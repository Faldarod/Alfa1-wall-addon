package nl.alfaone.wledsimulatorspring.config;

import nl.alfaone.wledsimulatorspring.websocket.WledWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for WLED simulator
 * Configures /ws endpoint for real-time state updates
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WledWebSocketHandler wledWebSocketHandler;

    public WebSocketConfig(WledWebSocketHandler wledWebSocketHandler) {
        this.wledWebSocketHandler = wledWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register /ws endpoint (matches real WLED devices)
        registry.addHandler(wledWebSocketHandler, "/ws")
                .setAllowedOrigins("*");  // Allow all origins for development
    }
}
