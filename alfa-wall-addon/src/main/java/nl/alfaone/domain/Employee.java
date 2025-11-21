package nl.alfaone.domain;

import lombok.Data;
import nl.alfaone.infrastructure.config.EmployeeLedMappingProperties.LedSegmentConfig;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Employee {
    private String id;
    private String name;
    private String email;
    private String background;  // Rich description for semantic search

    // Presence tracking
    private List<String> deviceTrackers = new ArrayList<>();
    private boolean currentlyPresent;

    // LED visualization
    private LedSegmentConfig ledSegment;

    // Skills and expertise
    private List<String> skills = new ArrayList<>();

    // Customer assignments
    private List<CustomerAssignment> customerAssignments = new ArrayList<>();

    // Parking allocation
    private ParkingAssignment parkingAssignment;

    // Office schedule
    private OfficeSchedule officeSchedule;

    /**
     * Build searchable text for semantic/vector search
     */
    public String toSearchableText() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(". ");

        if (background != null && !background.isEmpty()) {
            sb.append(background).append(". ");
        }

        if (!skills.isEmpty()) {
            sb.append("Skills: ").append(String.join(", ", skills)).append(". ");
        }

        if (!customerAssignments.isEmpty()) {
            sb.append("Works for: ");
            for (CustomerAssignment assignment : customerAssignments) {
                sb.append(assignment.getCustomerName())
                  .append(" as ")
                  .append(assignment.getRole())
                  .append(", ");
            }
        }

        return sb.toString();
    }

    /**
     * Check if employee has a specific skill (case-insensitive)
     */
    public boolean hasSkill(String skill) {
        return skills.stream()
                .anyMatch(s -> s.equalsIgnoreCase(skill));
    }

    /**
     * Check if employee works for a specific customer (case-insensitive)
     */
    public boolean worksForCustomer(String customerName) {
        return customerAssignments.stream()
                .anyMatch(assignment ->
                    assignment.getCustomerName().equalsIgnoreCase(customerName));
    }

    /**
     * Check if employee is scheduled for a specific date
     */
    public boolean isScheduledFor(LocalDate date) {
        if (officeSchedule == null) {
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return officeSchedule.getWeeklySchedule().getOrDefault(dayOfWeek, false);
    }

    /**
     * Check if employee has parking on a specific date
     */
    public boolean hasParkingOn(LocalDate date) {
        if (parkingAssignment == null) {
            return false;
        }

        // Check specific dates
        if (parkingAssignment.getSpecificDates().contains(date)) {
            return true;
        }

        // Check recurring days
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return parkingAssignment.getRecurringDays().contains(dayOfWeek);
    }
}
