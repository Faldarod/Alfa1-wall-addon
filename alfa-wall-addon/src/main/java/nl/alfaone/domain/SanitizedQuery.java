package nl.alfaone.domain;

/**
 * Result of privacy checking and sanitization.
 * This object is produced by PrivacyOfficerAgent and consumed by EmployeeCollectorAgent.
 * Represents the OODA "Observe" step - understanding what we're allowed to process.
 */
public record SanitizedQuery(
    String originalQuery,
    String sanitizedQuery,
    boolean hasCriticalViolation
) {
    /**
     * Get the query to use for processing (sanitized if needed, empty if blocked)
     */
    public String getQueryForProcessing() {
        return hasCriticalViolation ? "" : sanitizedQuery;
    }

    /**
     * Check if the query should be blocked from processing
     */
    public boolean shouldBlock() {
        return hasCriticalViolation;
    }
}
