package nl.alfaone.infrastructure.config;

import lombok.Data;
import nl.alfaone.domain.mcp.McpServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration properties for MCP (Model Context Protocol) servers.
 * Follows the same pattern as EmployeeDataProperties for consistency.
 *
 * <p>This class binds YAML configuration under the "mcp" prefix:
 * <pre>
 * mcp:
 *   enabled: true
 *   servers:
 *     - name: employee-search
 *       base-url: http://localhost:9000
 *       enabled: true
 *       timeout-seconds: 30
 *       auth:
 *         token: ${MCP_EMPLOYEE_SEARCH_TOKEN:}
 *         header: Authorization
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "mcp")
@Data
public class McpServerProperties {

    /**
     * Global enable/disable flag for MCP integration.
     * When false, all MCP beans are disabled via @ConditionalOnProperty.
     */
    private boolean enabled = false;

    /**
     * List of MCP server configurations.
     */
    private List<ServerConfig> servers = new ArrayList<>();

    /**
     * Converts the YAML configuration into domain McpServerConfig objects.
     * This method maps infrastructure configuration to domain models.
     *
     * @return List of domain McpServerConfig objects
     */
    public List<McpServerConfig> toMcpServerConfigs() {
        return servers.stream()
                .map(s -> McpServerConfig.builder()
                        .name(s.getName())
                        .baseUrl(s.getBaseUrl())
                        .enabled(s.isEnabled())
                        .timeout(Duration.ofSeconds(s.getTimeoutSeconds()))
                        .authToken(s.getAuth() != null ? s.getAuth().getToken() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Configuration for a single MCP server.
     * Nested static class following Spring Boot configuration properties pattern.
     */
    @Data
    public static class ServerConfig {
        /**
         * Unique name identifier for this MCP server.
         * Used in qualified tool names (e.g., "employee-search:search").
         */
        private String name;

        /**
         * Base URL of the MCP server (e.g., "http://localhost:9000").
         */
        private String baseUrl;

        /**
         * Whether this specific server is enabled.
         * Allows disabling individual servers without removing config.
         */
        private boolean enabled = true;

        /**
         * HTTP request timeout in seconds.
         * Default: 30 seconds.
         */
        private int timeoutSeconds = 30;

        /**
         * Optional authentication configuration.
         */
        private AuthConfig auth;
    }

    /**
     * Authentication configuration for MCP server.
     */
    @Data
    public static class AuthConfig {
        /**
         * Authentication token (e.g., Bearer token).
         * Can be sourced from environment variables.
         */
        private String token;

        /**
         * HTTP header name for authentication.
         * Default: "Authorization"
         */
        private String header = "Authorization";
    }
}
