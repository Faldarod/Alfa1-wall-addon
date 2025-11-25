package nl.alfaone.domain;

/**
 * Types of employee queries supported by the system.
 * Used to determine appropriate data sources and LED visualization colors.
 */
public enum QueryType {
    /**
     * Query about current employee presence (who is here now)
     * LED Color: Green
     */
    PRESENCE,

    /**
     * Query about employee skills/expertise
     * LED Color: Purple
     */
    SKILLS,

    /**
     * Query about customer assignments
     * LED Color: Yellow
     */
    CUSTOMER,

    /**
     * Query about office schedule (who is coming today/tomorrow)
     * LED Color: Blue
     */
    SCHEDULE,

    /**
     * Query about parking assignments
     * LED Color: Orange
     */
    PARKING,

    /**
     * General/semantic search query
     * LED Color: Employee's default color
     */
    GENERAL
}
