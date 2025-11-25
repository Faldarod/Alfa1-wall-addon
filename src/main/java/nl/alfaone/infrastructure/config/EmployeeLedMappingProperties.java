package nl.alfaone.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "employee-led")
@Data
public class EmployeeLedMappingProperties {

    /**
     * Map of employee names to their corresponding WLED segment entity IDs
     * Key: Employee name (case-insensitive matching will be used)
     * Value: Home Assistant entity ID for the WLED segment
     */
    private Map<String, LedSegmentConfig> mappings = new HashMap<>();

    @Data
    public static class LedSegmentConfig {
        private String entityId;
        private String color;  // Optional: hex color code (e.g., "#FF5733")
        private Integer brightness;  // Optional: 0-255
    }
}
