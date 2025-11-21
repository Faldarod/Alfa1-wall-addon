package nl.alfaone.domain;

import java.util.List;

/**
 * Final result of the employee query processing flow.
 * This object is produced by ActionAgent.visualizeAndRespond() which achieves the goal.
 * Represents the OODA "Act" step - the final output after visualization.
 */
public record VisualizationResult(
    String message,
    List<Employee> visualizedEmployees
) {
    /**
     * Check if visualization was successful
     */
    public boolean isSuccessful() {
        return message != null && !message.trim().isEmpty();
    }

    /**
     * Get count of visualized employees
     */
    public int getVisualizedCount() {
        return visualizedEmployees != null ? visualizedEmployees.size() : 0;
    }
}
