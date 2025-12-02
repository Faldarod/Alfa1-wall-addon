package nl.alfaone.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import nl.alfaone.domain.mcp.McpServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for MCP (Model Context Protocol) integration.
 * Follows the same pattern as ChatClientConfig for consistency.
 *
 * <p>This configuration uses @ConditionalOnProperty to enable/disable
 * all MCP functionality through a single application.yaml property:
 * <pre>
 * mcp:
 *   enabled: true  # or false to disable
 * </pre>
 *
 * <p>When disabled (or property not set), no MCP beans are created,
 * allowing the application to function normally with only static @Tools.
 */
@Configuration
@Slf4j
public class McpConfiguration {

    /**
     * Creates a list of McpServerConfig beans when MCP integration is enabled.
     * This bean is used by McpToolDiscoveryService and McpToolExecutionService.
     *
     * @param properties The MCP server properties from application.yaml
     * @return List of domain McpServerConfig objects
     */
    @Bean
    @ConditionalOnProperty(name = "mcp.enabled", havingValue = "true")
    public List<McpServerConfig> mcpServerConfigs(McpServerProperties properties) {
        List<McpServerConfig> configs = properties.toMcpServerConfigs();

        log.info("╔═══════════════════════════════════════════════════════════╗");
        log.info("║         MCP Integration ENABLED                           ║");
        log.info("╚═══════════════════════════════════════════════════════════╝");
        log.info("Configured MCP servers: {}", configs.size());

        configs.forEach(config ->
                log.info("  - {} at {} (enabled: {}, timeout: {}s)",
                        config.getName(),
                        config.getBaseUrl(),
                        config.isEnabled(),
                        config.getTimeout().getSeconds()));

        return configs;
    }

    /**
     * Creates an empty list of McpServerConfig beans when MCP is disabled.
     * This ensures dependent beans can still be created but with no servers.
     *
     * @return Empty list of McpServerConfig
     */
    @Bean
    @ConditionalOnProperty(name = "mcp.enabled", havingValue = "false", matchIfMissing = true)
    public List<McpServerConfig> emptyMcpServerConfigs() {
        log.info("╔═══════════════════════════════════════════════════════════╗");
        log.info("║         MCP Integration DISABLED                          ║");
        log.info("╚═══════════════════════════════════════════════════════════╝");
        log.info("MCP tool discovery and execution will not be available.");
        log.info("Only static @Tool methods will be available to agents.");

        return List.of();
    }
}
