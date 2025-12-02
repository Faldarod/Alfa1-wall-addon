package nl.alfaone.domain;

/**
 * Easter egg commands for special LED effects.
 * Fun, interactive commands that trigger visual effects on the LED wall.
 */
public enum EasterEggCommand {
    /**
     * Disco mode - cycle through rainbow colors rapidly
     */
    DISCO_MODE,

    /**
     * Random person - pick and highlight a random employee
     */
    RANDOM_PERSON,

    /**
     * Rainbow mode - display rainbow colors across all segments
     */
    RAINBOW,

    /**
     * Pulse effect - breathing/pulsing effect on all segments
     */
    PULSE,

    /**
     * Wave effect - color wave across segments
     */
    WAVE,

    /**
     * Party mode - rapid color changes and effects
     */
    PARTY_MODE,

    /**
     * All segments on - turn on all LEDs with white color
     */
    ALL_ON,

    /**
     * All segments off - turn off all LEDs
     */
    ALL_OFF,

    /**
     * Unknown easter egg command
     */
    UNKNOWN
}
