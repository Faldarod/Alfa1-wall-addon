package nl.alfaone.domain.mcp;

import lombok.Builder;
import lombok.Value;

/**
 * Represents the result of an MCP tool invocation.
 * This is a domain value object that encapsulates either a successful result
 * or an error message, following the Result pattern for error handling.
 */
@Value
@Builder
public class McpToolResult {
    /**
     * Indicates whether the tool execution was successful
     */
    boolean success;

    /**
     * The result data from the tool execution (null if error occurred)
     */
    Object result;

    /**
     * The error message if execution failed (null if successful)
     */
    String errorMessage;

    /**
     * Factory method to create a successful result.
     *
     * @param result The result data from the tool execution
     * @return A McpToolResult representing success
     */
    public static McpToolResult success(Object result) {
        return McpToolResult.builder()
            .success(true)
            .result(result)
            .build();
    }

    /**
     * Factory method to create an error result.
     *
     * @param message The error message describing what went wrong
     * @return A McpToolResult representing failure
     */
    public static McpToolResult error(String message) {
        return McpToolResult.builder()
            .success(false)
            .errorMessage(message)
            .build();
    }
}
