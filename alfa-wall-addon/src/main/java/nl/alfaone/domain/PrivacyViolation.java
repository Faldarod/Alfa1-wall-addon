package nl.alfaone.domain;

import java.util.List;

public record PrivacyViolation(
        boolean hasViolation,
        List<String> violationTypes,
        String sanitizedQuery
) {
    public static PrivacyViolation noViolation(String originalQuery) {
        return new PrivacyViolation(false, List.of(), originalQuery);
    }

    public static PrivacyViolation withViolations(List<String> violationTypes, String sanitizedQuery) {
        return new PrivacyViolation(true, violationTypes, sanitizedQuery);
    }
}
