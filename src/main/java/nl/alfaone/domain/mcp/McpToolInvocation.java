package nl.alfaone.domain.mcp;

import lombok.Builder;
import lombok.Value;
import java.util.Map;

/**
 * Represents a request to invoke an MCP tool with specific arguments.
 * This is a domain value object used to transport tool execution parameters
 * from the application layer to the infrastructure layer (JSON-RPC client).
 */
@Value
@Builder
public class McpToolInvocation {
    /**
     * The name of the tool to invoke (not the qualified name)
     */
    String toolName;

    /**
     * The arguments to pass to the tool, structured as a JSON-compatible map.
     * The keys correspond to parameter names defined in the tool's inputSchema.
     */
    Map<String, Object> arguments;
}
