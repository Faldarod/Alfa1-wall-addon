package nl.alfaone.application.mcp;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.alfaone.domain.mcp.McpServerConfig;
import nl.alfaone.domain.mcp.McpToolDefinition;
import nl.alfaone.infrastructure.mcp.McpJsonRpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for discovering MCP tools from configured servers
 * and maintaining a registry of available tools.
 *
 * <p>This service follows the same initialization pattern as EmployeeRepository:
 * - @Service annotation for Spring bean management
 * - @PostConstruct for startup initialization
 * - ConcurrentHashMap for thread-safe registry
 * - @ConditionalOnProperty to enable/disable MCP integration
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "mcp.enabled", havingValue = "true")
public class McpToolDiscoveryService {

    private final McpJsonRpcClient mcpClient;
    private final List<McpServerConfig> serverConfigs;

    // Registry: qualifiedName (e.g., "server:toolname") -> McpToolDefinition
    private final Map<String, McpToolDefinition> toolRegistry = new ConcurrentHashMap<>();

    // Static tool names to detect conflicts
    private static final Set<String> STATIC_TOOL_NAMES = Set.of(
            "searchEmployees", "getCurrentPresence", "filterBySkill",
            "filterByCustomer", "filterBySchedule", "filterByParking",
            "getAllEmployees", "intersectEmployeeLists", "combineEmployeeLists"
    );

    public McpToolDiscoveryService(McpJsonRpcClient mcpClient, List<McpServerConfig> serverConfigs) {
        this.mcpClient = mcpClient;
        this.serverConfigs = serverConfigs;
    }

    /**
     * Discovers all tools from enabled MCP servers at application startup.
     * This method runs after bean construction and populates the tool registry.
     *
     * <p>Discovery process:
     * 1. Filter to only enabled servers
     * 2. Call tools/list RPC method for each server
     * 3. Register tools in ConcurrentHashMap
     * 4. Warn about any naming conflicts with static @Tools
     * 5. Log discovery summary
     *
     * <p>Failures are handled gracefully - if a server is unreachable,
     * we log a warning and continue with other servers.
     */
    @PostConstruct
    public void discoverAllTools() {
        log.info("Starting MCP tool discovery...");

        if (serverConfigs == null || serverConfigs.isEmpty()) {
            log.info("No MCP servers configured. MCP integration is enabled but no servers defined.");
            return;
        }

        int totalServers = 0;
        int successfulServers = 0;

        for (McpServerConfig config : serverConfigs) {
            if (!config.isEnabled()) {
                log.debug("Skipping disabled MCP server: {}", config.getName());
                continue;
            }

            totalServers++;
            log.info("Discovering tools from MCP server: {} at {}", config.getName(), config.getBaseUrl());

            try {
                List<McpToolDefinition> tools = mcpClient
                        .discoverTools(config)
                        .block(); // Block during startup initialization - acceptable for @PostConstruct

                if (tools != null && !tools.isEmpty()) {
                    for (McpToolDefinition tool : tools) {
                        // Register with qualified name
                        toolRegistry.put(tool.getQualifiedName(), tool);
                        log.info("Registered MCP tool: {} - {}", tool.getQualifiedName(), tool.getDescription());

                        // Warn if tool name conflicts with static @Tools
                        if (STATIC_TOOL_NAMES.contains(tool.getToolName())) {
                            log.warn("MCP tool '{}' has same name as static @Tool. " +
                                            "LLM must use qualified name '{}' to avoid conflicts",
                                    tool.getToolName(), tool.getQualifiedName());
                        }
                    }
                    successfulServers++;
                } else {
                    log.warn("MCP server '{}' returned no tools", config.getName());
                }

            } catch (Exception e) {
                log.warn("Failed to discover tools from MCP server '{}': {}",
                        config.getName(), e.getMessage());
                // Continue with other servers - graceful degradation
            }
        }

        log.info("MCP tool discovery complete. {} tools registered from {}/{} servers",
                toolRegistry.size(), successfulServers, totalServers);

        // Log all registered tool names for debugging
        if (!toolRegistry.isEmpty()) {
            log.debug("Available MCP tools: {}", String.join(", ", toolRegistry.keySet()));
        }
    }

    /**
     * Retrieves a tool definition by its qualified name.
     *
     * @param qualifiedName The qualified name in format "server:toolname"
     * @return Optional containing the tool definition if found
     */
    public Optional<McpToolDefinition> getTool(String qualifiedName) {
        return Optional.ofNullable(toolRegistry.get(qualifiedName));
    }

    /**
     * Retrieves all registered MCP tool definitions.
     *
     * @return List of all available tool definitions
     */
    public List<McpToolDefinition> getAllTools() {
        return new ArrayList<>(toolRegistry.values());
    }

    /**
     * Checks if any MCP tools are available.
     *
     * @return true if at least one tool is registered, false otherwise
     */
    public boolean hasTools() {
        return !toolRegistry.isEmpty();
    }

    /**
     * Gets the count of registered tools.
     *
     * @return The number of available MCP tools
     */
    public int getToolCount() {
        return toolRegistry.size();
    }

    /**
     * Retrieves all tools from a specific MCP server.
     *
     * @param serverName The name of the MCP server
     * @return List of tools from that server
     */
    public List<McpToolDefinition> getToolsByServer(String serverName) {
        return toolRegistry.values().stream()
                .filter(tool -> tool.getServerName().equals(serverName))
                .toList();
    }
}
