package nl.alfaone.infrastructure.repository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.alfaone.domain.*;
import nl.alfaone.infrastructure.HomeAssistantClient;
import nl.alfaone.infrastructure.config.EmployeeDataProperties;
import nl.alfaone.infrastructure.config.EmployeeLedMappingProperties;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class EmployeeRepository {

    private final EmployeeDataProperties employeeDataProperties;
    private final HomeAssistantClient homeAssistantClient;
    private final List<Employee> employees = new ArrayList<>();

    public EmployeeRepository(EmployeeDataProperties employeeDataProperties,
                             HomeAssistantClient homeAssistantClient) {
        this.employeeDataProperties = employeeDataProperties;
        this.homeAssistantClient = homeAssistantClient;
    }

    @PostConstruct
    public void loadEmployees() {
        log.info("Loading employees from configuration...");

        for (EmployeeDataProperties.EmployeeConfig config : employeeDataProperties.getEmployees()) {
            Employee employee = new Employee();
            employee.setId(config.getId());
            employee.setName(config.getName());
            employee.setEmail(config.getEmail());
            employee.setBackground(config.getBackground());
            employee.setDeviceTrackers(config.getDeviceTrackers());
            employee.setSkills(config.getSkills());

            // Map LED segment
            if (config.getLedSegment() != null) {
                EmployeeLedMappingProperties.LedSegmentConfig ledConfig =
                    new EmployeeLedMappingProperties.LedSegmentConfig();
                ledConfig.setEntityId(config.getLedSegment().getEntityId());
                ledConfig.setColor(config.getLedSegment().getColor());
                ledConfig.setBrightness(config.getLedSegment().getBrightness());
                employee.setLedSegment(ledConfig);
            }

            // Map customer assignments
            if (config.getCustomers() != null) {
                List<CustomerAssignment> assignments = config.getCustomers().stream()
                    .map(c -> {
                        CustomerAssignment assignment = new CustomerAssignment();
                        assignment.setCustomerName(c.getCustomerName());
                        assignment.setRole(c.getRole());
                        assignment.setPercentage(c.getPercentage());
                        return assignment;
                    })
                    .collect(Collectors.toList());
                employee.setCustomerAssignments(assignments);
            }

            // Map parking
            if (config.getParking() != null) {
                ParkingAssignment parking = new ParkingAssignment();
                parking.setSpotNumber(config.getParking().getSpotNumber());

                List<DayOfWeek> recurringDays = config.getParking().getRecurringDays().stream()
                    .map(DayOfWeek::valueOf)
                    .collect(Collectors.toList());
                parking.setRecurringDays(recurringDays);
                employee.setParkingAssignment(parking);
            }

            // Map office schedule
            if (config.getSchedule() != null) {
                OfficeSchedule schedule = new OfficeSchedule();
                schedule.getWeeklySchedule().put(DayOfWeek.MONDAY, config.getSchedule().isMonday());
                schedule.getWeeklySchedule().put(DayOfWeek.TUESDAY, config.getSchedule().isTuesday());
                schedule.getWeeklySchedule().put(DayOfWeek.WEDNESDAY, config.getSchedule().isWednesday());
                schedule.getWeeklySchedule().put(DayOfWeek.THURSDAY, config.getSchedule().isThursday());
                schedule.getWeeklySchedule().put(DayOfWeek.FRIDAY, config.getSchedule().isFriday());
                schedule.getWeeklySchedule().put(DayOfWeek.SATURDAY, config.getSchedule().isSaturday());
                schedule.getWeeklySchedule().put(DayOfWeek.SUNDAY, config.getSchedule().isSunday());
                employee.setOfficeSchedule(schedule);
            }

            employees.add(employee);
            log.info("Loaded employee: {} with {} skills", employee.getName(), employee.getSkills().size());
        }

        log.info("Loaded {} employees total", employees.size());
    }

    public List<Employee> findAll() {
        return new ArrayList<>(employees);
    }

    public Employee findById(String id) {
        return employees.stream()
                .filter(emp -> emp.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Simple text-based search (in-memory semantic search simulation)
     * Checks if employee's searchable text contains the query keywords
     * Now supports partial name matching (e.g., "John" matches "John Doe")
     */
    public List<Employee> semanticSearch(String query) {
        log.info("Performing semantic search for: '{}'", query);

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String lowerQuery = query.toLowerCase();

        // Extract potential name keywords from query (remove common question words)
        String cleanedQuery = lowerQuery
                .replaceAll("\\bis\\b", "")
                .replaceAll("\\bare\\b", "")
                .replaceAll("\\bpresent\\b", "")
                .replaceAll("\\bhere\\b", "")
                .replaceAll("\\?", "")
                .trim();

        // Split into words for partial matching
        String[] queryWords = cleanedQuery.split("\\s+");

        return employees.stream()
                .filter(emp -> {
                    String searchableText = emp.toSearchableText().toLowerCase();

                    // Check if the full query matches (original behavior)
                    if (searchableText.contains(lowerQuery)) {
                        return true;
                    }

                    // Check if any significant word from query matches employee name or text
                    // This allows "John" to match "John Doe"
                    for (String word : queryWords) {
                        if (word.length() >= 3 && searchableText.contains(word)) {
                            return true;
                        }
                    }

                    return false;
                })
                .collect(Collectors.toList());
    }

    public List<Employee> findBySkill(String skill) {
        log.info("Finding employees with skill: {}", skill);
        return employees.stream()
                .filter(emp -> emp.hasSkill(skill))
                .collect(Collectors.toList());
    }

    public List<Employee> findByCustomer(String customerName) {
        log.info("Finding employees for customer: {}", customerName);
        return employees.stream()
                .filter(emp -> emp.worksForCustomer(customerName))
                .collect(Collectors.toList());
    }

    public List<Employee> findScheduledFor(LocalDate date) {
        log.info("Finding employees scheduled for: {}", date);
        return employees.stream()
                .filter(emp -> emp.isScheduledFor(date))
                .collect(Collectors.toList());
    }

    public List<Employee> findWithParkingOn(LocalDate date) {
        log.info("Finding employees with parking on: {}", date);
        return employees.stream()
                .filter(emp -> emp.hasParkingOn(date))
                .collect(Collectors.toList());
    }

    /**
     * Enrich employees with current presence from Home Assistant
     */
    public List<Employee> enrichWithCurrentPresence(List<Employee> employeeList) {
        for (Employee employee : employeeList) {
            boolean isPresent = employee.getDeviceTrackers().stream()
                    .anyMatch(tracker -> {
                        try {
                            return Boolean.TRUE.equals(homeAssistantClient.isDeviceHome(tracker).block());
                        } catch (Exception e) {
                            log.warn("Could not check presence for tracker: {}", tracker, e);
                            return false;
                        }
                    });
            employee.setCurrentlyPresent(isPresent);
        }
        return employeeList;
    }
}
