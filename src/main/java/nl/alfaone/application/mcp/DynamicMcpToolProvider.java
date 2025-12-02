package nl.alfaone.application.mcp;

import lombok.extern.slf4j.Slf4j;
import nl.alfaone.domain.mcp.McpToolDefinition;
import nl.alfaone.domain.mcp.McpToolResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides dynamic @Tool wrappers for discovered MCP tools.
 * This component creates Spring AI @Tool-annotated methods at runtime
 * that delegate to the MCP tool execution service.
 *
 * <p>This bridge pattern allows MCP tools to be exposed through the same
 * @Tool interface as static tools, making them available to LLM-driven
 * tool selection via Embabel's PromptRunner.
 *
 * <p>Future integration pattern (when switching from keyword routing to LLM tool selection):
 * <pre>
 * PromptRunner.withToolObject(this)
 *     .withToolObjects(dynamicMcpToolProvider.generateToolWrappers())
 *     .execute(prompt);
 * </pre>
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "mcp.enabled", havingValue = "true")
public class DynamicMcpToolProvider {

    private final McpToolExecutionService executionService;
    private final McpToolDiscoveryService discoveryService;

    public DynamicMcpToolProvider(McpToolExecutionService executionService,
                                  McpToolDiscoveryService discoveryService) {
        this.executionService = executionService;
        this.discoveryService = discoveryService;
        log.info("DynamicMcpToolProvider initialized. MCP tools will be available as @Tool methods.");
    }

    /**
     * Generates @Tool wrapper objects for all discovered MCP tools.
     * Each wrapper exposes a @Tool-annotated method that the LLM can invoke.
     *
     * @return List of tool wrapper objects for LLM integration
     */
    public List<Object> generateToolWrappers() {
        List<McpToolDefinition> allTools = discoveryService.getAllTools();

        log.debug("Generating {} @Tool wrappers for MCP tools", allTools.size());

        return allTools.stream()
                .map(toolDef -> new McpToolWrapper(toolDef, executionService))
                .collect(Collectors.toList());
    }

    /**
     * Inner class that wraps an MCP tool as a Spring AI @Tool.
     * Each instance of this class represents one MCP tool and exposes
     * a @Tool-annotated method for LLM invocation.
     *
     * <p>The @Tool annotation makes this method discoverable by Spring AI,
     * allowing the LLM to understand and invoke MCP tools through the
     * standard tool calling mechanism.
     */
    public static class McpToolWrapper {
        private final McpToolDefinition toolDefinition;
        private final McpToolExecutionService executionService;

        public McpToolWrapper(McpToolDefinition toolDefinition,
                              McpToolExecutionService executionService) {
            this.toolDefinition = toolDefinition;
            this.executionService = executionService;
        }

        /**
         * Spring AI @Tool method that executes the MCP tool.
         * This method is automatically discovered by Spring AI and can be
         * called by the LLM through Embabel's tool selection mechanism.
         *
         * <p>The method signature follows Spring AI conventions:
         * - Returns Object for maximum flexibility
         * - Takes Map<String, Object> for JSON-compatible arguments
         * - Blocks on reactive Mono for synchronous @Tool contract
         *
         * @param arguments The tool arguments provided by the LLM
         * @return The tool execution result
         * @throws RuntimeException if tool execution fails
         */
        @Tool(description = "MCP tool")
        public Object executeMcpTool(Map<String, Object> arguments) {
            log.info("Executing MCP tool via @Tool wrapper: {}", toolDefinition.getQualifiedName());
            log.debug("Tool arguments: {}", arguments);

            // Execute the tool synchronously (blocking on reactive Mono)
            // This is acceptable because @Tool methods are expected to be synchronous
            McpToolResult result = executionService
                    .executeTool(toolDefinition.getQualifiedName(), arguments)
                    .block(); // Synchronous execution for @Tool contract

            if (result == null) {
                throw new RuntimeException("MCP tool returned null result: " +
                        toolDefinition.getQualifiedName());
            }

            if (!result.isSuccess()) {
                throw new RuntimeException("MCP tool execution failed: " +
                        result.getErrorMessage());
            }

            return result.getResult();
        }

        /**
         * Gets the tool name for logging and debugging.
         *
         * @return The qualified tool name
         */
        public String getToolName() {
            return toolDefinition.getQualifiedName();
        }

        /**
         * Gets the tool description for logging and debugging.
         *
         * @return The tool description
         */
        public String getToolDescription() {
            return toolDefinition.getDescription();
        }

        /**
         * Gets the underlying tool definition.
         *
         * @return The MCP tool definition
         */
        public McpToolDefinition getToolDefinition() {
            return toolDefinition;
        }
    }

    /**
     * Gets the count of available MCP tool wrappers.
     *
     * @return The number of available MCP tools
     */
    public int getToolWrapperCount() {
        return discoveryService.getToolCount();
    }

    /**
     * Checks if any MCP tools are available.
     *
     * @return true if at least one MCP tool is available
     */
    public boolean hasToolWrappers() {
        return discoveryService.hasTools();
    }

    /**
     * Gets a summary of all available MCP tools for logging/debugging.
     *
     * @return String describing all available tools
     */
    public String getToolSummary() {
        List<McpToolDefinition> tools = discoveryService.getAllTools();
        if (tools.isEmpty()) {
            return "No MCP tools available";
        }

        return tools.stream()
                .map(tool -> String.format("%s: %s",
                        tool.getQualifiedName(),
                        tool.getDescription()))
                .collect(Collectors.joining("\n"));
    }
}
