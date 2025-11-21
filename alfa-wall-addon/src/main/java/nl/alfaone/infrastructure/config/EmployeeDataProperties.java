package nl.alfaone.infrastructure.config;

import lombok.Data;
import nl.alfaone.domain.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "employee-data")
@Data
public class EmployeeDataProperties {
    private List<EmployeeConfig> employees = new ArrayList<>();

    @Data
    public static class EmployeeConfig {
        private String id;
        private String name;
        private String email;
        private String background;
        private List<String> deviceTrackers = new ArrayList<>();
        private LedConfig ledSegment;
        private List<String> skills = new ArrayList<>();
        private List<CustomerAssignmentConfig> customers = new ArrayList<>();
        private ParkingConfig parking;
        private OfficeScheduleConfig schedule;
    }

    @Data
    public static class LedConfig {
        private String entityId;
        private String color;
        private Integer brightness;
    }

    @Data
    public static class CustomerAssignmentConfig {
        private String customerName;
        private String role;
        private Integer percentage;
    }

    @Data
    public static class ParkingConfig {
        private String spotNumber;
        private List<String> recurringDays = new ArrayList<>();
    }

    @Data
    public static class OfficeScheduleConfig {
        private boolean monday = true;
        private boolean tuesday = true;
        private boolean wednesday = true;
        private boolean thursday = true;
        private boolean friday = true;
        private boolean saturday = false;
        private boolean sunday = false;
    }
}
