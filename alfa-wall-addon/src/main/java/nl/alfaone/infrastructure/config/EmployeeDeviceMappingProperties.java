package nl.alfaone.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "employee-device")
@Data
public class EmployeeDeviceMappingProperties {

    /**
     * Map of employee names to their device tracker entity IDs
     * Key: Employee name
     * Value: List of device_tracker entity IDs associated with this employee
     */
    private Map<String, List<String>> mappings = new HashMap<>();

    /**
     * Whether to use mock data when real device trackers are not available
     */
    private boolean useMockData = true;
}
