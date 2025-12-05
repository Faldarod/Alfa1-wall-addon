package nl.alfaone.infrastructure;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Minimal Conversation API Controller - Skeleton Implementation
 *
 * This is a bare-bones REST API endpoint that echoes received messages.
 * Developers should implement their own conversation logic here.
 *
 * Home Assistant Conversation API compatible:
 * https://developers.home-assistant.io/docs/intent_conversation_api/
 */
@RestController
@RequestMapping("/api/conversation")
@Slf4j
public class ConversationApiController {

    /**
     * Process a conversation query.
     * Currently just echoes the received message.
     *
     * TODO: Implement your conversation processing logic here
     *
     * @param request Conversation request with text query
     * @return Conversation response in Home Assistant format
     */
    @PostMapping("/process")
    public ConversationResponse process(@RequestBody ConversationRequest request) {
        // Get query text
        String queryText = request.getText();
        if (queryText == null || queryText.trim().isEmpty()) {
            queryText = request.getQuery();
        }

        log.info("Received message: {}", queryText);

        // TODO: Add your conversation logic here
        // Examples:
        // - Call an LLM API
        // - Query a database
        // - Invoke business logic
        // - Call external services

        // For now, just echo the message
        String responseMessage = "Received message: " + queryText;

        // Generate conversation ID
        String conversationId = request.getConversationId() != null ?
                request.getConversationId() :
                UUID.randomUUID().toString();

        log.info("Returning response: {} (conversationId: {})", responseMessage, conversationId);

        // Return Home Assistant compatible response
        return new ConversationResponse(
                new Response(
                        new Speech(new Plain(responseMessage)),
                        request.getLanguage() != null ? request.getLanguage() : "en",
                        "action_done"
                ),
                conversationId
        );
    }

    // ==================== REQUEST/RESPONSE DTOs ====================

    /**
     * Home Assistant conversation request format
     */
    @Data
    public static class ConversationRequest {
        private String text;           // User's query text
        private String query;          // Alternative field name
        private String conversationId; // Optional conversation ID
        private String language;       // Language code (e.g., "en")
        private String agent_id;       // Optional agent ID
        private String device_id;      // Optional device ID
    }

    /**
     * Home Assistant conversation response format
     */
    public record ConversationResponse(
            Response response,
            String conversation_id
    ) {}

    public record Response(
            Speech speech,
            String language,
            String response_type
    ) {}

    public record Speech(
            Plain plain
    ) {}

    public record Plain(
            String speech
    ) {}
}
