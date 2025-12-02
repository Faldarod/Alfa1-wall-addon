package nl.alfaone.domain;

import java.util.List;

/**
 * Result of an easter egg command execution.
 * Contains the command type and any relevant employee data.
 */
public record EasterEggResult(
    EasterEggCommand command,
    List<Employee> employees,
    String message
) {
    public static EasterEggResult of(EasterEggCommand command, List<Employee> employees, String message) {
        return new EasterEggResult(command, employees, message);
    }

    public static EasterEggResult of(EasterEggCommand command, String message) {
        return new EasterEggResult(command, List.of(), message);
    }
}
