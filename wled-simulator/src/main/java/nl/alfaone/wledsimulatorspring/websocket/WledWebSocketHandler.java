package nl.alfaone.wledsimulatorspring.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.alfaone.wledsimulatorspring.service.WledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for WLED simulator
 * Manages client connections and broadcasts state updates
 */
@Component
public class WledWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WledWebSocketHandler.class);

    private final WledService wledService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public WledWebSocketHandler(WledService wledService) {
        this.wledService = wledService;
        this.objectMapper = new ObjectMapper();
        // Register this handler with WledService for broadcasts
        wledService.setWebSocketHandler(this);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("WebSocket connection established: {} (total connections: {})", sessionId, sessions.size());

        // Send initial state to newly connected client
        sendStateToClient(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        logger.info("WebSocket connection closed: {} - Status: {} (remaining connections: {})",
                    sessionId, status, sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("WebSocket transport error for session {}: {}", sessionId, exception.getMessage());
        sessions.remove(sessionId);
    }

    /**
     * Send current WLED state to a specific client
     */
    private void sendStateToClient(WebSocketSession session) {
        try {
            Map<String, Object> fullState = Map.of(
                "state", wledService.getCurrentState(),
                "info", wledService.getDeviceInfo()
            );

            String json = objectMapper.writeValueAsString(fullState);
            session.sendMessage(new TextMessage(json));
            logger.debug("Sent initial state to session: {}", session.getId());
        } catch (IOException e) {
            logger.error("Error sending state to session {}: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * Broadcast WLED state update to all connected clients
     * Called by WledService when state changes
     */
    public void broadcastStateUpdate() {
        if (sessions.isEmpty()) {
            logger.debug("No WebSocket clients connected, skipping broadcast");
            return;
        }

        try {
            Map<String, Object> fullState = Map.of(
                "state", wledService.getCurrentState(),
                "info", wledService.getDeviceInfo()
            );

            String json = objectMapper.writeValueAsString(fullState);
            TextMessage message = new TextMessage(json);

            // Broadcast to all connected clients
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    logger.error("Error broadcasting to session {}: {}", session.getId(), e.getMessage());
                }
            });

            logger.debug("Broadcasted state update to {} client(s)", sessions.size());
        } catch (Exception e) {
            logger.error("Error preparing broadcast message: {}", e.getMessage());
        }
    }

    /**
     * Get number of connected WebSocket clients
     */
    public int getConnectionCount() {
        return sessions.size();
    }
}
