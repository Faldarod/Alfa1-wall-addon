package nl.alfaone.application.agents;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.common.OperationContext;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import nl.alfaone.domain.EasterEggCommand;
import nl.alfaone.domain.Employee;
import nl.alfaone.domain.EmployeeSearchResult;
import nl.alfaone.domain.QueryType;
import nl.alfaone.domain.SanitizedQuery;
import nl.alfaone.infrastructure.repository.EmployeeRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Agent(description = "Collects employees from various data sources based on query context. " +
                     "Can search by: current presence (device trackers), skills, customers, schedule, parking, or semantic search. " +
                     "Provides @Tool methods for LLM-driven selection and @Condition methods for query classification.")
@Slf4j
public class EmployeeCollectorAgent {

    private final EmployeeRepository employeeRepository;

    public EmployeeCollectorAgent(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Main GOAP action: Collect employees matching a query.
     * Takes SanitizedQuery as input (type dependency - GOAP will provide this!)
     * Returns EmployeeSearchResult (consumed by ActionAgent)
     *
     * This is the OODA "Orient" step - understanding who matches and why.
     */
    @Action(cost = 2)
    public EmployeeSearchResult collectEmployees(
            SanitizedQuery sanitizedQuery,
            OperationContext context) {

        String query = sanitizedQuery.getQueryForProcessing();

        // Defensive: Handle blocked or empty queries
        if (query == null || query.trim().isEmpty() || sanitizedQuery.shouldBlock()) {
            log.warn("Query blocked or empty, returning empty result");
            return new EmployeeSearchResult(
                    sanitizedQuery.originalQuery(),
                    List.of(),
                    QueryType.GENERAL
            );
        }

        log.info("Collecting employees for query: '{}'", query);

        // Determine query type using @Condition methods
        QueryType type = determineQueryType(query);
        log.info("Query type determined: {}", type);

        // Route to appropriate data source based on query type
        List<Employee> employees = switch (type) {
            case PRESENCE -> {
                log.info("Routing to getCurrentPresence()");
                yield getCurrentPresence();
            }
            case SKILLS -> {
                log.info("Routing to skill search");
                yield getEmployeesBySkillFromQuery(query);
            }
            case CUSTOMER -> {
                log.info("Routing to customer search");
                yield getEmployeesByCustomerFromQuery(query);
            }
            case SCHEDULE -> {
                log.info("Routing to schedule search");
                String dateStr = query.toLowerCase().contains("tomorrow") ? "tomorrow" : "today";
                yield filterBySchedule(dateStr);
            }
            case PARKING -> {
                log.info("Routing to parking search");
                String dateStr = query.toLowerCase().contains("tomorrow") ? "tomorrow" : "today";
                yield filterByParking(dateStr);
            }
            case EASTER_EGG -> {
                log.info("Routing to easter egg command");
                yield executeEasterEggCommand(query);
            }
            case GENERAL -> {
                log.info("Routing to semantic search");
                yield searchEmployees(query);
            }
        };

        log.info("Found {} employees for query type {}", employees.size(), type);

        return new EmployeeSearchResult(query, employees, type);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Determine query type by evaluating all @Condition methods
     */
    private QueryType determineQueryType(String query) {
        // Check easter eggs first (before other types to avoid false matches)
        if (isEasterEggQuery(query)) return QueryType.EASTER_EGG;
        if (isPresenceQuery(query)) return QueryType.PRESENCE;
        if (isSkillQuery(query)) return QueryType.SKILLS;
        if (isCustomerQuery(query)) return QueryType.CUSTOMER;
        if (isScheduleQuery(query)) return QueryType.SCHEDULE;
        if (isParkingQuery(query)) return QueryType.PARKING;
        return QueryType.GENERAL;
    }

    /**
     * Extract skill from query and search by skill
     */
    private List<Employee> getEmployeesBySkillFromQuery(String query) {
        // Extract skill from query (improved logic)
        List<String> commonSkills = List.of("java", "python", "react", "typescript", "node",
            "docker", "kubernetes", "aws", "azure", "sql", "spring", "kafka");

        for (String skill : commonSkills) {
            if (query.toLowerCase().contains(skill.toLowerCase())) {
                return filterBySkill(skill);
            }
        }

        // Fallback to semantic search if no skill keyword found
        return searchEmployees(query);
    }

    /**
     * Extract customer name from query and search by customer
     */
    private List<Employee> getEmployeesByCustomerFromQuery(String query) {
        String customerName = extractCustomerName(query);
        if (customerName != null) {
            return filterByCustomer(customerName);
        }

        // Fallback to semantic search
        return searchEmployees(query);
    }

    // ==================== TOOLS (LLM can call these methods) ====================

    @Tool(description = "Search employee database using semantic text search. " +
          "Use for general queries about skills, background, or expertise. " +
          "Examples: 'backend experts', 'cloud specialists', 'experienced developers'")
    public List<Employee> searchEmployees(String searchQuery) {
        log.info("Tool called: searchEmployees('{}')", searchQuery);
        return employeeRepository.semanticSearch(searchQuery);
    }

    @Tool(description = "Get employees who are currently in the office right now (based on device trackers). " +
          "Use for queries like: 'who is here', 'who is in the office', 'current presence', 'who is present now'")
    public List<Employee> getCurrentPresence() {
        log.info("Tool called: getCurrentPresence()");

        List<Employee> allEmployees = employeeRepository.findAll();
        return employeeRepository.enrichWithCurrentPresence(allEmployees).stream()
                .filter(Employee::isCurrentlyPresent)
                .collect(Collectors.toList());
    }

    @Tool(description = "Filter employees by exact skill name. " +
          "Use for queries about specific technologies or skills. " +
          "Skill parameter should be exact like 'Java', 'Python', 'React', 'Docker', 'Kubernetes'")
    public List<Employee> filterBySkill(String skill) {
        log.info("Tool called: filterBySkill('{}')", skill);
        return employeeRepository.findBySkill(skill);
    }

    @Tool(description = "Find employees working for a specific customer or company. " +
          "Use when query mentions customer/client names. " +
          "Examples: 'works for ACME', 'working for TechCorp', 'assigned to customer X'")
    public List<Employee> filterByCustomer(String customerName) {
        log.info("Tool called: filterByCustomer('{}')", customerName);
        return employeeRepository.findByCustomer(customerName);
    }

    @Tool(description = "Find employees scheduled to be in the office on a specific date. " +
          "Use for queries about future presence. " +
          "Date parameter: 'today', 'tomorrow', or ISO date like '2024-01-15'. " +
          "Examples: 'who is coming today', 'scheduled for tomorrow'")
    public List<Employee> filterBySchedule(String dateString) {
        log.info("Tool called: filterBySchedule('{}')", dateString);

        LocalDate date = parseDate(dateString);
        return employeeRepository.findScheduledFor(date);
    }

    @Tool(description = "Find employees who have a parking spot on a specific date. " +
          "Use for queries about parking allocation. " +
          "Date parameter: 'today', 'tomorrow', or ISO date. " +
          "Examples: 'who has parking today', 'parking spot tomorrow'")
    public List<Employee> filterByParking(String dateString) {
        log.info("Tool called: filterByParking('{}')", dateString);

        LocalDate date = parseDate(dateString);
        return employeeRepository.findWithParkingOn(date);
    }

    @Tool(description = "Get all employees in the database. " +
          "Use for queries like: 'show all employees', 'list everyone', 'all people'")
    public List<Employee> getAllEmployees() {
        log.info("Tool called: getAllEmployees()");
        return employeeRepository.findAll();
    }

    @Tool(description = "Intersect multiple employee lists to find employees matching ALL criteria (AND logic). " +
          "Use for complex queries with multiple requirements. " +
          "Example: To find 'Java developers coming today', intersect filterBySkill('Java') and filterBySchedule('today')")
    public List<Employee> intersectEmployeeLists(List<List<Employee>> lists) {
        log.info("Tool called: intersectEmployeeLists() with {} lists", lists.size());

        if (lists == null || lists.isEmpty()) {
            return List.of();
        }

        if (lists.size() == 1) {
            return lists.get(0);
        }

        return lists.get(0).stream()
                .filter(emp -> lists.stream().allMatch(list -> list.contains(emp)))
                .distinct()
                .collect(Collectors.toList());
    }

    @Tool(description = "Combine multiple employee lists to find employees matching ANY criteria (OR logic). " +
          "Use when query asks for employees meeting at least one condition. " +
          "Example: 'Java or Python developers' would combine filterBySkill('Java') and filterBySkill('Python')")
    public List<Employee> combineEmployeeLists(List<List<Employee>> lists) {
        log.info("Tool called: combineEmployeeLists() with {} lists", lists.size());

        if (lists == null || lists.isEmpty()) {
            return List.of();
        }

        return lists.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Execute easter egg command and return appropriate employee list
     */
    private List<Employee> executeEasterEggCommand(String query) {
        EasterEggCommand command = parseEasterEggCommand(query);
        log.info("Executing easter egg command: {}", command);

        return switch (command) {
            case RANDOM_PERSON -> {
                // Select a random employee
                List<Employee> allEmployees = getAllEmployees();
                if (allEmployees.isEmpty()) {
                    yield List.of();
                }
                int randomIndex = (int) (Math.random() * allEmployees.size());
                yield List.of(allEmployees.get(randomIndex));
            }
            case ALL_OFF -> {
                // Return empty list to turn off all LEDs
                yield List.of();
            }
            case DISCO_MODE, RAINBOW, PULSE, WAVE, PARTY_MODE, ALL_ON -> {
                // Return all employees for full-wall effects
                yield getAllEmployees();
            }
            case UNKNOWN -> {
                log.warn("Unknown easter egg command, returning all employees");
                yield getAllEmployees();
            }
        };
    }

    /**
     * Parse which specific easter egg command was requested
     */
    private EasterEggCommand parseEasterEggCommand(String query) {
        if (query == null) return EasterEggCommand.UNKNOWN;
        String lowerQuery = query.toLowerCase();

        // Check for specific commands (order matters - most specific first)
        if (lowerQuery.contains("disco")) {
            return EasterEggCommand.DISCO_MODE;
        }
        if (lowerQuery.contains("party mode") || lowerQuery.contains("party")) {
            return EasterEggCommand.PARTY_MODE;
        }
        if (lowerQuery.contains("random") && (lowerQuery.contains("person") || lowerQuery.contains("employee"))) {
            return EasterEggCommand.RANDOM_PERSON;
        }
        if (lowerQuery.contains("rainbow")) {
            return EasterEggCommand.RAINBOW;
        }
        if (lowerQuery.contains("pulse")) {
            return EasterEggCommand.PULSE;
        }
        if (lowerQuery.contains("wave")) {
            return EasterEggCommand.WAVE;
        }
        if (lowerQuery.contains("all") && lowerQuery.contains("off")) {
            return EasterEggCommand.ALL_OFF;
        }
        if (lowerQuery.contains("all") && lowerQuery.contains("on")) {
            return EasterEggCommand.ALL_ON;
        }
        if (lowerQuery.contains("light show") || lowerQuery.contains("surprise me")) {
            // Default to party mode for "surprise" commands
            return EasterEggCommand.PARTY_MODE;
        }

        return EasterEggCommand.UNKNOWN;
    }

    /**
     * Extract customer name from query (simple keyword-based approach)
     */
    private String extractCustomerName(String query) {
        String[] patterns = {"works for ", "working for ", "customer ", "client "};

        for (String pattern : patterns) {
            int index = query.toLowerCase().indexOf(pattern);
            if (index != -1) {
                String remaining = query.substring(index + pattern.length()).trim();

                // Take quoted string
                if (remaining.startsWith("\"")) {
                    int endQuote = remaining.indexOf("\"", 1);
                    if (endQuote > 0) {
                        return remaining.substring(1, endQuote);
                    }
                }

                // Take capitalized words
                String[] words = remaining.split("\\s+");
                StringBuilder customerName = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0 && Character.isUpperCase(word.charAt(0))) {
                        if (customerName.length() > 0) customerName.append(" ");
                        customerName.append(word);
                    } else {
                        break;
                    }
                }
                if (customerName.length() > 0) {
                    return customerName.toString();
                }
            }
        }
        return null;
    }

    /**
     * Parse date string to LocalDate (supports 'today', 'tomorrow', or ISO date format)
     */
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty() || dateString.equalsIgnoreCase("today")) {
            return LocalDate.now();
        }

        if (dateString.equalsIgnoreCase("tomorrow")) {
            return LocalDate.now().plusDays(1);
        }

        try {
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            log.warn("Could not parse date '{}', using today", dateString);
            return LocalDate.now();
        }
    }

    // ==================== CONDITION METHODS FOR GOAP ====================

    /**
     * Condition: Check if query is about employee presence
     * Detects keywords: here, present, in the office, currently, now
     * Also detects patterns like "Is X present?" or "Is anyone here?"
     */
    @Condition
    public boolean isPresenceQuery(String query) {
        if (query == null) return false;
        String lowerQuery = query.toLowerCase();

        // Direct presence keywords
        if (lowerQuery.contains("here") ||
            lowerQuery.contains("in the office") ||
            lowerQuery.contains("currently") ||
            (lowerQuery.contains("who") && lowerQuery.contains("now"))) {
            return true;
        }

        // Pattern: "Is X present?" or "Are X present?" or "X present?"
        if (lowerQuery.contains("present")) {
            return true;
        }

        return false;
    }

    /**
     * Condition: Check if query is about employee skills
     * Detects skill-related keywords and specific technologies
     */
    @Condition
    public boolean isSkillQuery(String query) {
        if (query == null) return false;
        String lowerQuery = query.toLowerCase();

        // Check for skill-related keywords
        if (lowerQuery.contains("skill") || lowerQuery.contains("knows") ||
            lowerQuery.contains("expert") || lowerQuery.contains("experience") ||
            lowerQuery.contains("developer")) {
            return true;
        }

        // Check for specific technologies
        List<String> commonSkills = List.of("java", "python", "react", "typescript", "node",
            "docker", "kubernetes", "aws", "azure", "sql", "spring", "kafka");

        for (String skill : commonSkills) {
            if (lowerQuery.contains(skill.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Condition: Check if query is about customer assignments
     * Detects keywords: works for, customer, client, project
     */
    @Condition
    public boolean isCustomerQuery(String query) {
        if (query == null) return false;
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("works for") ||
               lowerQuery.contains("working for") ||
               lowerQuery.contains("customer") ||
               lowerQuery.contains("client") ||
               lowerQuery.contains("project");
    }

    /**
     * Condition: Check if query is about schedule/coming to office
     * Detects keywords: coming, scheduled, today, tomorrow
     */
    @Condition
    public boolean isScheduleQuery(String query) {
        if (query == null) return false;
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("coming") ||
               lowerQuery.contains("scheduled") ||
               lowerQuery.contains("will be") ||
               (lowerQuery.contains("who") && (lowerQuery.contains("today") || lowerQuery.contains("tomorrow")));
    }

    /**
     * Condition: Check if query is about parking
     * Detects keywords: parking, park, spot
     */
    @Condition
    public boolean isParkingQuery(String query) {
        if (query == null) return false;
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("parking") || lowerQuery.contains("park") || lowerQuery.contains("spot");
    }

    /**
     * Condition: Check if query is an easter egg command
     * Detects special commands: disco, random, rainbow, pulse, wave, party, all on, all off
     */
    @Condition
    public boolean isEasterEggQuery(String query) {
        if (query == null) return false;
        String lowerQuery = query.toLowerCase();

        return lowerQuery.contains("disco") ||
               lowerQuery.contains("party mode") ||
               lowerQuery.contains("party") ||
               (lowerQuery.contains("random") && (lowerQuery.contains("person") || lowerQuery.contains("employee"))) ||
               lowerQuery.contains("rainbow") ||
               lowerQuery.contains("pulse") ||
               lowerQuery.contains("wave") ||
               (lowerQuery.contains("all") && (lowerQuery.contains("on") || lowerQuery.contains("off"))) ||
               lowerQuery.contains("light show") ||
               lowerQuery.contains("surprise me");
    }
}
