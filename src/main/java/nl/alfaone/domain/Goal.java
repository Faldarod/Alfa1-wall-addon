package nl.alfaone.domain;

/**
 * Goals that the AlfaWall agent system can achieve.
 *
 * These goals are used by Embabel's GOAP (Goal-Oriented Action Planning) planner
 * to determine which actions to execute and in what sequence.
 */
public enum Goal {
    /**
     * Answer a natural language query about employees
     * This is the main goal triggered by user queries through Home Assistant
     */
    ANSWER_EMPLOYEE_QUERY("Answer a natural language query about employees, their presence, skills, customers, parking, or schedules"),

    /**
     * Visualize employee presence on the LED wall
     * This goal is achieved after collecting employee data and determining visualization needs
     */
    VISUALIZE_PRESENCE("Visualize employees on the LED wall with appropriate colors based on query context");

    private final String description;

    Goal(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
