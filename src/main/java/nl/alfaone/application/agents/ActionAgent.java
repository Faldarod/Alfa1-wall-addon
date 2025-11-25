package nl.alfaone.application.agents;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Export;
import nl.alfaone.domain.AgentContext;
import nl.alfaone.domain.Employee;
import nl.alfaone.domain.EmployeeSearchResult;
import nl.alfaone.domain.Presence;
import nl.alfaone.domain.QueryType;
import nl.alfaone.domain.VisualizationResult;
import nl.alfaone.infrastructure.HomeAssistantClient;
import nl.alfaone.infrastructure.config.EmployeeLedMappingProperties;
import nl.alfaone.infrastructure.config.EmployeeLedMappingProperties.LedSegmentConfig;
import nl.alfaone.infrastructure.repository.EmployeeRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.embabel.agent.api.annotation.Agent;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Agent(description = "Controls WLED LED wall to visualize employee presence and information. " +
                     "Turns matched employee segments ON with context-appropriate colors (green=presence, blue=schedule, purple=skills, yellow=customers, orange=parking), " +
                     "and unmatched segments OFF. Achieves the VISUALIZE_PRESENCE goal.")
@Slf4j
public class ActionAgent {

    private final HomeAssistantClient homeAssistantClient;
    private final EmployeeLedMappingProperties ledMappingProperties;
    private final EmployeeRepository employeeRepository;

    public ActionAgent(HomeAssistantClient homeAssistantClient,
                      EmployeeLedMappingProperties ledMappingProperties,
                      EmployeeRepository employeeRepository) {
        this.homeAssistantClient = homeAssistantClient;
        this.ledMappingProperties = ledMappingProperties;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Visualize employees on LED wall with context-based colors
     * This is the final GOAP action (OODA "Act" step) that achieves the goal.
     * Consumes EmployeeSearchResult and produces VisualizationResult.
     */
    @AchievesGoal(
        description = "Visualize employees on the LED wall based on the query context (presence, skills, customers, parking, schedule)",
        examples = {
            "Who is here?",
            "Show me Java developers",
            "Who has parking today?",
            "Who works for ACME Corp?"
        }
    )
    @Export
    @Action(cost = 1)
    public VisualizationResult visualizeEmployees(EmployeeSearchResult searchResult) {
        List<Employee> employees = searchResult.employees();
        QueryType queryType = searchResult.queryType();

        log.info("Visualizing {} employees for query type: {}", employees.size(), queryType);

        if (employees == null || employees.isEmpty()) {
            log.warn("No employees to visualize");
            // Turn off all LEDs
            turnOffAllLeds().block();
            return new VisualizationResult("No matching employees found", List.of());
        }

        // Determine color based on query type
        String contextColor = determineContextColor(queryType);

        List<String> allEmployeeNames = employeeRepository.findAll().stream()
                .map(Employee::getName)
                .collect(Collectors.toList());

        List<String> matchedEmployeeNames = employees.stream()
                .map(Employee::getName)
                .collect(Collectors.toList());

        List<String> unmatchedEmployeeNames = allEmployeeNames.stream()
                .filter(name -> !matchedEmployeeNames.contains(name))
                .collect(Collectors.toList());

        log.info("Matched: {}, Unmatched: {}", matchedEmployeeNames.size(), unmatchedEmployeeNames.size());

        // Turn on LEDs for matched employees with context color
        Flux.concat(
                turnOnLedsForEmployees(matchedEmployeeNames, contextColor),
                turnOffLedsForEmployees(unmatchedEmployeeNames)
        ).then().block();

        String message = employees.size() + " employees visualized on LED wall";
        return new VisualizationResult(message, employees);
    }

    /**
     * Determine LED color based on query type
     * Maps QueryType enum to LED color codes
     */
    private String determineContextColor(QueryType queryType) {
        if (queryType == null) {
            return null; // Use employee's default color
        }

        return switch (queryType) {
            case PRESENCE -> "#00FF00";  // Green - current presence
            case SKILLS -> "#9400D3";    // Purple - skills/expertise
            case CUSTOMER -> "#FFD700";  // Yellow - customer assignments
            case SCHEDULE -> "#0000FF";  // Blue - office schedule
            case PARKING -> "#FFA500";   // Orange - parking assignments
            case GENERAL -> null;        // Use employee's default color
        };
    }

    public Mono<Void> controlWled(AgentContext context) {
        if (context.presences() == null || context.presences().isEmpty()) {
            log.warn("No presence information available in context");
            return Mono.empty();
        }

        // Get list of present employees
        List<String> presentEmployees = context.presences().stream()
                .filter(Presence::isPresent)
                .map(Presence::getEmployeeName)
                .collect(Collectors.toList());

        // Get list of absent employees
        List<String> absentEmployees = context.presences().stream()
                .filter(p -> !p.isPresent())
                .map(Presence::getEmployeeName)
                .collect(Collectors.toList());

        log.info("Controlling LEDs - Present: {}, Absent: {}", presentEmployees, absentEmployees);

        // Turn on LEDs for present employees and turn off for absent ones
        return Flux.concat(
                turnOnLedsForEmployees(presentEmployees),
                turnOffLedsForEmployees(absentEmployees)
        ).then();
    }

    private Flux<String> turnOnLedsForEmployees(List<String> employeeNames, String contextColor) {
        return Flux.fromIterable(employeeNames)
                .flatMap(name -> turnOnLedForEmployee(name, contextColor))
                .doOnError(e -> log.error("Error turning on LED", e));
    }

    private Flux<String> turnOnLedsForEmployees(List<String> employeeNames) {
        return turnOnLedsForEmployees(employeeNames, null);
    }

    private Flux<String> turnOffLedsForEmployees(List<String> employeeNames) {
        return Flux.fromIterable(employeeNames)
                .flatMap(this::turnOffLedForEmployee)
                .doOnError(e -> log.error("Error turning off LED", e));
    }

    private Mono<String> turnOnLedForEmployee(String employeeName, String contextColor) {
        LedSegmentConfig config = findLedConfigForEmployee(employeeName);

        if (config == null) {
            log.warn("No LED mapping found for employee: {}", employeeName);
            return Mono.empty();
        }

        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("entity_id", config.getEntityId());

        if (config.getBrightness() != null) {
            serviceData.put("brightness", config.getBrightness());
        }

        // Use context color if provided, otherwise use employee's configured color
        String colorToUse = contextColor != null ? contextColor : config.getColor();
        if (colorToUse != null) {
            serviceData.put("rgb_color", hexToRgb(colorToUse));
        }

        log.info("Turning on LED for {}: entity={}, color={}, brightness={}",
                employeeName, config.getEntityId(), colorToUse, config.getBrightness());

        return homeAssistantClient.callService("light", "turn_on", serviceData)
                .doOnSuccess(result -> log.info("Successfully turned on LED for {}", employeeName))
                .doOnError(e -> log.error("Failed to turn on LED for {}: {}", employeeName, e.getMessage()));
    }

    private Mono<String> turnOnLedForEmployee(String employeeName) {
        return turnOnLedForEmployee(employeeName, null);
    }

    private Mono<String> turnOffLedForEmployee(String employeeName) {
        LedSegmentConfig config = findLedConfigForEmployee(employeeName);

        if (config == null) {
            log.warn("No LED mapping found for employee: {}", employeeName);
            return Mono.empty();
        }

        Map<String, Object> serviceData = Map.of("entity_id", config.getEntityId());

        log.info("Turning off LED for {}: entity={}", employeeName, config.getEntityId());

        return homeAssistantClient.callService("light", "turn_off", serviceData)
                .doOnSuccess(result -> log.info("Successfully turned off LED for {}", employeeName))
                .doOnError(e -> log.error("Failed to turn off LED for {}: {}", employeeName, e.getMessage()));
    }

    private LedSegmentConfig findLedConfigForEmployee(String employeeName) {
        // Try exact match first
        LedSegmentConfig config = ledMappingProperties.getMappings().get(employeeName);

        // If no exact match, try case-insensitive match
        if (config == null) {
            config = ledMappingProperties.getMappings().entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(employeeName))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return config;
    }

    /**
     * Turn off all LED segments
     */
    private Mono<Void> turnOffAllLeds() {
        log.info("Turning off all LEDs");
        List<String> allEmployeeNames = employeeRepository.findAll().stream()
                .map(Employee::getName)
                .collect(Collectors.toList());

        return turnOffLedsForEmployees(allEmployeeNames).then();
    }

    /**
     * Convert hex color code to RGB array for Home Assistant
     * @param hex Hex color code (e.g., "#FF5733" or "FF5733")
     * @return Array of [R, G, B] values (0-255)
     */
    private int[] hexToRgb(String hex) {
        // Remove # if present
        hex = hex.replace("#", "");

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (Exception e) {
            log.warn("Invalid hex color: {}, using white", hex);
            return new int[]{255, 255, 255};
        }
    }
}
