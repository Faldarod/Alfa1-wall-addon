package nl.alfaone.application.mcp;

import lombok.extern.slf4j.Slf4j;
import nl.alfaone.domain.mcp.McpServerConfig;
import nl.alfaone.domain.mcp.McpToolDefinition;
import nl.alfaone.domain.mcp.McpToolInvocation;
import nl.alfaone.domain.mcp.McpToolResult;
import nl.alfaone.infrastructure.mcp.McpJsonRpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service responsible for executing MCP tools.
 * This service acts as a bridge between the application layer and the infrastructure layer,
 * routing tool execution requests to the appropriate MCP server.
 *
 * <p>Execution flow:
 * 1. Look up tool definition from registry
 * 2. Find corresponding MCP server configuration
 * 3. Build tool invocation request
 * 4. Execute via McpJsonRpcClient
 * 5. Return result or error
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "mcp.enabled", havingValue = "true")
public class McpToolExecutionService {

    private final McpJsonRpcClient mcpClient;
    private final McpToolDiscoveryService discoveryService;
    private final Map<String, McpServerConfig> serverConfigsByName;

    public McpToolExecutionService(McpJsonRpcClient mcpClient,
                                   McpToolDiscoveryService discoveryService,
                                   List<McpServerConfig> serverConfigs) {
        this.mcpClient = mcpClient;
        this.discoveryService = discoveryService;
        // Create a map of server name -> config for quick lookup
        this.serverConfigsByName = serverConfigs.stream()
                .collect(Collectors.toMap(McpServerConfig::getName, config -> config));

        log.info("McpToolExecutionService initialized with {} MCP servers",
                serverConfigsByName.size());
    }

    /**
     * Executes an MCP tool by its qualified name with the provided arguments.
     *
     * @param qualifiedName The qualified tool name (e.g., "employee-search:search")
     * @param arguments The tool arguments as a map
     * @return A Mono containing the tool execution result
     */
    public Mono<McpToolResult> executeTool(String qualifiedName, Map<String, Object> arguments) {
        log.debug("Executing MCP tool: {} with arguments: {}", qualifiedName, arguments);

        // Look up tool definition
        Optional<McpToolDefinition> toolDefOpt = discoveryService.getTool(qualifiedName);

        if (toolDefOpt.isEmpty()) {
            log.warn("Tool not found in registry: {}", qualifiedName);
            return Mono.just(McpToolResult.error("Tool not found: " + qualifiedName));
        }

        McpToolDefinition toolDef = toolDefOpt.get();

        // Find corresponding server configuration
        McpServerConfig serverConfig = serverConfigsByName.get(toolDef.getServerName());

        if (serverConfig == null) {
            log.error("Server configuration not found for tool: {} (server: {})",
                    qualifiedName, toolDef.getServerName());
            return Mono.just(McpToolResult.error(
                    "Server configuration not found: " + toolDef.getServerName()));
        }

        if (!serverConfig.isEnabled()) {
            log.warn("Attempted to execute tool on disabled server: {}", serverConfig.getName());
            return Mono.just(McpToolResult.error(
                    "Server is disabled: " + serverConfig.getName()));
        }

        // Build invocation request
        McpToolInvocation invocation = McpToolInvocation.builder()
                .toolName(toolDef.getToolName())
                .arguments(arguments != null ? arguments : Map.of())
                .build();

        // Execute via JSON-RPC client
        return mcpClient.invokeTool(serverConfig, invocation)
                .doOnSuccess(result -> {
                    if (result.isSuccess()) {
                        log.info("Successfully executed MCP tool: {}", qualifiedName);
                    } else {
                        log.warn("MCP tool execution failed: {} - {}",
                                qualifiedName, result.getErrorMessage());
                    }
                })
                .doOnError(error -> log.error("Error executing MCP tool: {} - {}",
                        qualifiedName, error.getMessage()));
    }

    /**
     * Executes an MCP tool synchronously (blocking).
     * This is a convenience method for use cases where reactive programming is not needed.
     *
     * @param qualifiedName The qualified tool name
     * @param arguments The tool arguments
     * @return The tool execution result
     */
    public McpToolResult executeToolSync(String qualifiedName, Map<String, Object> arguments) {
        return executeTool(qualifiedName, arguments).block();
    }

    /**
     * Checks if a tool with the given qualified name is available for execution.
     *
     * @param qualifiedName The qualified tool name
     * @return true if the tool exists and its server is enabled
     */
    public boolean isToolAvailable(String qualifiedName) {
        Optional<McpToolDefinition> toolDef = discoveryService.getTool(qualifiedName);

        if (toolDef.isEmpty()) {
            return false;
        }

        McpServerConfig serverConfig = serverConfigsByName.get(toolDef.get().getServerName());
        return serverConfig != null && serverConfig.isEnabled();
    }

    /**
     * Gets a list of all available (enabled) tool qualified names.
     *
     * @return List of qualified tool names that can be executed
     */
    public List<String> getAvailableToolNames() {
        return discoveryService.getAllTools().stream()
                .filter(tool -> {
                    McpServerConfig config = serverConfigsByName.get(tool.getServerName());
                    return config != null && config.isEnabled();
                })
                .map(McpToolDefinition::getQualifiedName)
                .toList();
    }
}
