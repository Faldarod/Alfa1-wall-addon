package nl.alfaone.domain.mcp;

import lombok.Builder;
import lombok.Value;
import java.time.Duration;

/**
 * Represents the configuration for an MCP server connection.
 * This is a domain value object that encapsulates all necessary information
 * to establish communication with an MCP server.
 */
@Value
@Builder
public class McpServerConfig {
    /**
     * The unique name identifier for this MCP server
     */
    String name;

    /**
     * The base URL of the MCP server (e.g., "http://localhost:9000")
     */
    String baseUrl;

    /**
     * Whether this server is enabled and should be used
     */
    boolean enabled;

    /**
     * The timeout duration for HTTP requests to this server
     */
    Duration timeout;

    /**
     * Optional authentication token for API requests
     */
    String authToken;
}
