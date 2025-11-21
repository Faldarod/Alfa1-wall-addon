package nl.alfaone.application.agents;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.common.OperationContext;
import lombok.extern.slf4j.Slf4j;
import nl.alfaone.domain.PrivacyViolation;
import nl.alfaone.domain.QueryInput;
import nl.alfaone.domain.SanitizedQuery;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Agent(description = "Privacy and compliance agent that checks queries for PII and GDPR violations. " +
                     "Uses regex patterns for 8 PII types (email, phone, credit cards, SSN/BSN, addresses, IP, postcodes) " +
                     "and LLM-based analysis for contextual GDPR concerns. Provides @Condition methods to gate sensitive actions.")
@Slf4j
public class PrivacyOfficerAgent {

    private ChatClient chatClient;

    public PrivacyOfficerAgent() {
        // Default constructor - ChatClient will be optionally injected via setter
        log.info("PrivacyOfficerAgent initialized with regex-based privacy detection");
    }

    /**
     * Optional setter for ChatClient - only injected if ChatModel bean is available
     * This allows LLM-based GDPR analysis when configured
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            log.info("ChatClient configured - LLM-based GDPR analysis enabled");
        } else {
            log.debug("ChatClient not available - LLM-based GDPR analysis will be disabled");
        }
    }

    // Regex patterns for common PII
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(?:\\+\\d{1,3}[-.\\s]?)?(?:\\(?\\d{1,4}\\)?[-.\\s]?)?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}\\b"
    );

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b(?:\\d{4}[-.\\s]?){3}\\d{4}\\b"
    );

    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b\\d{3}[-.]?\\d{2}[-.]?\\d{4}\\b"
    );

    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"
    );

    // Common address indicators
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "\\b\\d{1,5}\\s+[A-Z][a-z]+\\s+(Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Dutch-specific patterns
    private static final Pattern DUTCH_POSTCODE_PATTERN = Pattern.compile(
            "\\b\\d{4}\\s?[A-Z]{2}\\b"
    );

    private static final Pattern BSN_PATTERN = Pattern.compile(
            "\\b\\d{8,9}\\b"  // Dutch social security number (BSN)
    );

    /**
     * Check if the query contains any PII that could violate privacy regulations
     *
     * @param query The user's query
     * @return PrivacyViolation object with detection results
     */
    public PrivacyViolation checkPrivacy(String query) {
        // Defensive: Handle null or empty query
        if (query == null || query.trim().isEmpty()) {
            log.warn("Received null or empty query for privacy check");
            return PrivacyViolation.noViolation("");
        }

        log.debug("Checking query for PII: {}", query);

        List<String> violations = new ArrayList<>();
        String sanitizedQuery = query;

        // Check for email addresses
        if (EMAIL_PATTERN.matcher(query).find()) {
            violations.add("EMAIL");
            sanitizedQuery = EMAIL_PATTERN.matcher(sanitizedQuery).replaceAll("[EMAIL_REDACTED]");
            log.warn("PII detected: Email address in query");
        }

        // Check for phone numbers
        if (PHONE_PATTERN.matcher(query).find()) {
            violations.add("PHONE");
            sanitizedQuery = PHONE_PATTERN.matcher(sanitizedQuery).replaceAll("[PHONE_REDACTED]");
            log.warn("PII detected: Phone number in query");
        }

        // Check for credit card numbers
        if (CREDIT_CARD_PATTERN.matcher(query).find()) {
            violations.add("CREDIT_CARD");
            sanitizedQuery = CREDIT_CARD_PATTERN.matcher(sanitizedQuery).replaceAll("[CC_REDACTED]");
            log.warn("PII detected: Credit card number in query");
        }

        // Check for SSN/BSN
        if (SSN_PATTERN.matcher(query).find() || BSN_PATTERN.matcher(query).find()) {
            violations.add("SSN/BSN");
            sanitizedQuery = SSN_PATTERN.matcher(sanitizedQuery).replaceAll("[SSN_REDACTED]");
            sanitizedQuery = BSN_PATTERN.matcher(sanitizedQuery).replaceAll("[BSN_REDACTED]");
            log.warn("PII detected: SSN/BSN in query");
        }

        // Check for IP addresses
        if (IP_ADDRESS_PATTERN.matcher(query).find()) {
            violations.add("IP_ADDRESS");
            sanitizedQuery = IP_ADDRESS_PATTERN.matcher(sanitizedQuery).replaceAll("[IP_REDACTED]");
            log.warn("PII detected: IP address in query");
        }

        // Check for physical addresses
        if (ADDRESS_PATTERN.matcher(query).find()) {
            violations.add("PHYSICAL_ADDRESS");
            sanitizedQuery = ADDRESS_PATTERN.matcher(sanitizedQuery).replaceAll("[ADDRESS_REDACTED]");
            log.warn("PII detected: Physical address in query");
        }

        // Check for Dutch postcodes
        if (DUTCH_POSTCODE_PATTERN.matcher(query).find()) {
            violations.add("POSTCODE");
            sanitizedQuery = DUTCH_POSTCODE_PATTERN.matcher(sanitizedQuery).replaceAll("[POSTCODE_REDACTED]");
            log.warn("PII detected: Dutch postcode in query");
        }

        if (!violations.isEmpty()) {
            log.error("Privacy violation detected! Types: {}", violations);
            return PrivacyViolation.withViolations(violations, sanitizedQuery);
        }

        log.debug("No privacy violations detected by regex patterns");
        return PrivacyViolation.noViolation(query);
    }

    /**
     * GOAP Action: Sanitize and validate query for privacy compliance
     * This is the first action in the GOAP chain (OODA "Observe" step).
     * Takes QueryInput and produces SanitizedQuery for downstream agents.
     *
     * @param queryInput The initial query input
     * @param context Operation context
     * @return SanitizedQuery with privacy check results
     */
    @Action(cost = 1)
    public SanitizedQuery sanitizeQuery(QueryInput queryInput, OperationContext context) {
        String query = queryInput.query();
        log.info("Sanitizing query for privacy compliance: '{}'", query);

        // Perform privacy check
        PrivacyViolation violation = checkPrivacy(query);

        // Determine if violation is critical (should block processing)
        boolean isCritical = shouldBlockRequest(violation);

        // Get sanitized query (with PII redacted if violations exist)
        String sanitized = violation.hasViolation() ? violation.sanitizedQuery() : query;

        if (isCritical) {
            log.error("Critical privacy violation detected - query will be blocked");
        } else if (violation.hasViolation()) {
            log.warn("Non-critical privacy violation detected - proceeding with sanitized query");
        } else {
            log.info("Query passed privacy check");
        }

        return new SanitizedQuery(query, sanitized, isCritical);
    }

    /**
     * Use LLM to analyze if the query might request GDPR-protected data
     * This catches contextual privacy concerns that regex patterns might miss
     *
     * @param query The user's query
     * @param context Operation context
     * @return GDPRAnalysisResult with concerns and explanation
     */
    @Action
    public GDPRAnalysisResult analyzeGDPRConcerns(String query, OperationContext context) {
        // Defensive: Handle null or empty query
        if (query == null || query.trim().isEmpty()) {
            log.warn("Received null or empty query for GDPR analysis");
            return new GDPRAnalysisResult(false, List.of(), "Empty query");
        }

        // If ChatClient is not available, skip LLM analysis
        if (chatClient == null) {
            log.debug("Skipping LLM-based GDPR analysis (ChatClient not configured)");
            return new GDPRAnalysisResult(false, List.of(), "LLM analysis not available");
        }

        log.info("Analyzing query for GDPR concerns using LLM: '{}'", query);

        String promptText = """
                Analyze the following query for potential GDPR privacy concerns.

                Consider if the query requests:
                - Personal identifiable information (names, addresses, birthdates)
                - Location data (where someone lives, works, or has been)
                - Health information
                - Financial data
                - Biometric data
                - Religious or political beliefs
                - Sensitive personal characteristics
                - Data that could identify individuals when combined

                Query: "{query}"

                Respond in JSON format:
                {{
                  "hasConcerns": true/false,
                  "concernTypes": ["type1", "type2"],
                  "severity": "LOW/MEDIUM/HIGH",
                  "explanation": "Brief explanation of the concern"
                }}

                If the query is a general employee search (like "who knows Java") with no personal data request, respond with hasConcerns: false.
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptText);
        Prompt prompt = promptTemplate.create(Map.of("query", query));

        try {
            String response = chatClient.prompt(prompt).call().content();
            log.debug("LLM GDPR analysis response: {}", response);

            // Parse JSON response (simplified - in production use proper JSON parsing)
            boolean hasConcerns = response.contains("\"hasConcerns\": true") ||
                                 response.contains("\"hasConcerns\":true");

            List<String> concerns = new ArrayList<>();
            if (hasConcerns) {
                if (response.contains("location") || response.contains("LOCATION")) {
                    concerns.add("LOCATION_DATA");
                }
                if (response.contains("health") || response.contains("HEALTH")) {
                    concerns.add("HEALTH_DATA");
                }
                if (response.contains("financial") || response.contains("FINANCIAL")) {
                    concerns.add("FINANCIAL_DATA");
                }
                if (response.contains("personal") || response.contains("PERSONAL")) {
                    concerns.add("PERSONAL_DATA");
                }
                if (concerns.isEmpty()) {
                    concerns.add("GDPR_SENSITIVE_DATA");
                }
            }

            String explanation = extractExplanation(response);

            log.info("GDPR Analysis: concerns={}, types={}", hasConcerns, concerns);
            return new GDPRAnalysisResult(hasConcerns, concerns, explanation);

        } catch (Exception e) {
            log.error("Error during LLM GDPR analysis", e);
            // Fail-safe: assume no concerns if LLM fails
            return new GDPRAnalysisResult(false, List.of(), "LLM analysis failed: " + e.getMessage());
        }
    }

    /**
     * Extract explanation from JSON response (simplified parsing)
     */
    private String extractExplanation(String response) {
        try {
            int start = response.indexOf("\"explanation\":");
            if (start == -1) return "No explanation provided";

            start = response.indexOf("\"", start + 14) + 1;
            int end = response.indexOf("\"", start);

            if (end > start) {
                return response.substring(start, end);
            }
        } catch (Exception e) {
            log.warn("Could not extract explanation from LLM response", e);
        }
        return "Unable to parse explanation";
    }

    /**
     * Result of GDPR analysis by LLM
     */
    public record GDPRAnalysisResult(
            boolean hasConcerns,
            List<String> concernTypes,
            String explanation
    ) {}

    /**
     * Check if PII should block the request
     * Some PII types might be more critical than others
     *
     * @param violation The privacy violation result
     * @return true if the request should be blocked
     */
    public boolean shouldBlockRequest(PrivacyViolation violation) {
        if (!violation.hasViolation()) {
            return false;
        }

        // Block requests with critical PII
        List<String> criticalPii = List.of("CREDIT_CARD", "SSN/BSN");
        for (String criticalType : criticalPii) {
            if (violation.violationTypes().contains(criticalType)) {
                log.error("Request blocked due to critical PII: {}", criticalType);
                return true;
            }
        }

        // For other PII types, log a warning but allow the request with sanitized data
        log.warn("Non-critical PII detected but request will proceed with sanitized data");
        return false;
    }

    // ==================== CONDITION METHODS FOR GOAP ====================

    /**
     * Condition: Check if query contains any PII violations
     * Used as precondition to gate actions that should not execute with PII data
     */
    @Condition
    public boolean hasPrivacyViolation(String query) {
        PrivacyViolation violation = checkPrivacy(query);
        return violation.hasViolation();
    }

    /**
     * Condition: Check if query contains critical PII that should block execution
     * Critical PII includes: CREDIT_CARD, SSN/BSN
     */
    @Condition
    public boolean hasCriticalPrivacyViolation(String query) {
        PrivacyViolation violation = checkPrivacy(query);
        return shouldBlockRequest(violation);
    }

    /**
     * Condition: Check if query is safe to process (no privacy violations)
     * This is the inverse of hasPrivacyViolation for convenience
     */
    @Condition
    public boolean isSafeQuery(String query) {
        return !hasPrivacyViolation(query);
    }
}
