package nl.alfaone.domain.mcp;

import lombok.Builder;
import lombok.Value;
import java.util.Map;

/**
 * Represents a tool definition discovered from an MCP server.
 * This is a domain value object that encapsulates all metadata about a tool,
 * including its qualified name (server:toolname), description, and input schema.
 */
@Value
@Builder
public class McpToolDefinition {
    /**
     * The name of the MCP server that provides this tool
     */
    String serverName;

    /**
     * The tool's name as defined in the MCP server
     */
    String toolName;

    /**
     * Human-readable description of what the tool does
     */
    String description;

    /**
     * JSON Schema definition of the tool's input parameters
     * This follows the JSON Schema spec for parameter validation
     */
    Map<String, Object> inputSchema;

    /**
     * Returns the qualified name of the tool in the format "server:toolname".
     * This format prevents conflicts with static @Tool methods.
     *
     * @return The qualified tool name (e.g., "employee-search:search")
     */
    public String getQualifiedName() {
        return serverName + ":" + toolName;
    }
}
