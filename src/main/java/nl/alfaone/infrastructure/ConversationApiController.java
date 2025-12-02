package nl.alfaone.infrastructure;

import com.embabel.agent.api.common.OperationContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.alfaone.application.agents.ActionAgent;
import nl.alfaone.application.agents.EmployeeCollectorAgent;
import nl.alfaone.application.agents.PrivacyOfficerAgent;
import nl.alfaone.domain.EmployeeSearchResult;
import nl.alfaone.domain.QueryInput;
import nl.alfaone.domain.SanitizedQuery;
import nl.alfaone.domain.VisualizationResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Home Assistant Conversation API Controller
 * https://developers.home-assistant.io/docs/intent_conversation_api/
 */
@RestController
@RequestMapping("/api/conversation")
@Slf4j
public class ConversationApiController {

    private final PrivacyOfficerAgent privacyOfficerAgent;
    private final EmployeeCollectorAgent employeeCollectorAgent;
    private final ActionAgent actionAgent;
    private final OperationContext operationContext;

    public ConversationApiController(PrivacyOfficerAgent privacyOfficerAgent,
                                    EmployeeCollectorAgent employeeCollectorAgent,
                                    ActionAgent actionAgent,
                                    OperationContext operationContext) {
        this.privacyOfficerAgent = privacyOfficerAgent;
        this.employeeCollectorAgent = employeeCollectorAgent;
        this.actionAgent = actionAgent;
        this.operationContext = operationContext;
    }

    @PostMapping("/process")
    public ConversationResponse process(@RequestBody ConversationRequest request) {
        // Defensive: Validate request body
        if (request == null) {
            log.error("Received null request body");
            return createErrorResponse("Error: Request body cannot be null", null);
        }

        // Get query text from either 'text' or 'query' field
        String queryText = request.getText();
        if (queryText == null || queryText.trim().isEmpty()) {
            queryText = request.getQuery();
        }

        // Defensive: Validate query text
        if (queryText == null || queryText.trim().isEmpty()) {
            log.error("Received request with no query text");
            return createErrorResponse("Error: Query text cannot be empty", request.getConversationId());
        }

        log.info("Received conversation request: '{}'", queryText);

        // Execute OODA loop manually with type-based action chaining
        // TODO: Future enhancement - use full GOAP cross-agent planning when supported
        VisualizationResult result;
        try {
            log.info("Starting agent chain execution (OODA loop)");

            // 1. OBSERVE: Sanitize query for privacy
            QueryInput queryInput = new QueryInput(queryText);
            SanitizedQuery sanitizedQuery = privacyOfficerAgent.sanitizeQuery(queryInput, operationContext);
            log.info("Privacy check complete - hasCriticalViolation: {}", sanitizedQuery.hasCriticalViolation());

            // GDPR compliance: Block queries with critical PII (credit cards, SSN/BSN)
            if (sanitizedQuery.shouldBlock()) {
                log.warn("Query blocked due to critical PII violation: {}", queryText);
                return createErrorResponse(
                    "I cannot process this query as it contains sensitive personal information (credit card numbers, social security numbers, etc.). " +
                    "Please rephrase your query without including such sensitive data.",
                    request.getConversationId()
                );
            }

            // 2. ORIENT: Collect matching employees
            EmployeeSearchResult searchResult = employeeCollectorAgent.collectEmployees(sanitizedQuery, operationContext);
            log.info("Employee collection complete - found {} employees, query type: {}",
                    searchResult.getCount(), searchResult.queryType());

            // 3. DECIDE & ACT: Visualize on LED wall
            result = actionAgent.visualizeEmployees(searchResult);
            log.info("Visualization complete - {} employees displayed", result.getVisualizedCount());

        } catch (Exception e) {
            log.error("Error during agent chain execution", e);
            return createErrorResponse("Error processing query: " + e.getMessage(), request.getConversationId());
        }

        // Defensive: Handle null result
        if (result == null) {
            log.error("GOAP execution returned null result");
            return createErrorResponse("Error: No response from query processor", request.getConversationId());
        }

        // Build response text
        String responseText = result.message();

        // Defensive: Handle null response text
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("Result message is null or empty, using default message");
            responseText = "Query processed successfully";
        }

        // Generate or use existing conversation ID
        String conversationId = request.getConversationId() != null ?
                request.getConversationId() :
                UUID.randomUUID().toString();

        log.info("GOAP execution complete. Returning response: '{}' (conversationId: {})",
                responseText, conversationId);

        return new ConversationResponse(
                new Response(
                        new Speech(new Plain(responseText)),
                        request.getLanguage() != null ? request.getLanguage() : "en",
                        "action_done"
                ),
                conversationId
        );
    }

    // ==================== HELPER METHODS ====================

    private ConversationResponse createErrorResponse(String errorMessage, String conversationId) {
        String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();
        return new ConversationResponse(
                new Response(
                        new Speech(new Plain(errorMessage)),
                        "en",
                        "error"
                ),
                convId
        );
    }

    // ==================== REQUEST/RESPONSE DTOs ====================

    @Data
    public static class ConversationRequest {
        private String text;      // Standard Home Assistant field
        private String query;     // Alternative field name for flexibility
        private String conversationId;
        private String language;
        private String agent_id;
        private String device_id;
    }

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
