package nl.alfaone.application.mcp;

import nl.alfaone.domain.mcp.McpServerConfig;
import nl.alfaone.domain.mcp.McpToolDefinition;
import nl.alfaone.infrastructure.mcp.McpJsonRpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolDiscoveryServiceTest {

    @Mock
    private McpJsonRpcClient mcpClient;

    private McpToolDiscoveryService discoveryService;
    private List<McpServerConfig> serverConfigs;

    @BeforeEach
    void setUp() {
        McpServerConfig config1 = McpServerConfig.builder()
                .name("server1")
                .baseUrl("http://localhost:9000")
                .enabled(true)
                .timeout(Duration.ofSeconds(30))
                .build();

        McpServerConfig config2 = McpServerConfig.builder()
                .name("server2")
                .baseUrl("http://localhost:9001")
                .enabled(false) // Disabled server
                .timeout(Duration.ofSeconds(30))
                .build();

        serverConfigs = List.of(config1, config2);
    }

    @Test
    void testDiscoverAllTools_Success() {
        // Given: Two tools from server1
        McpToolDefinition tool1 = McpToolDefinition.builder()
                .serverName("server1")
                .toolName("search")
                .description("Search employees")
                .inputSchema(Map.of("type", "object"))
                .build();

        McpToolDefinition tool2 = McpToolDefinition.builder()
                .serverName("server1")
                .toolName("suggest")
                .description("Suggest employees")
                .inputSchema(Map.of("type", "object"))
                .build();

        when(mcpClient.discoverTools(any(McpServerConfig.class)))
                .thenReturn(Mono.just(List.of(tool1, tool2)));

        discoveryService = new McpToolDiscoveryService(mcpClient, serverConfigs);

        // When: Discovery runs (@PostConstruct)
        discoveryService.discoverAllTools();

        // Then: Should register both tools
        assertEquals(2, discoveryService.getToolCount());
        assertTrue(discoveryService.hasTools());

        Optional<McpToolDefinition> foundTool1 = discoveryService.getTool("server1:search");
        assertTrue(foundTool1.isPresent());
        assertEquals("search", foundTool1.get().getToolName());

        Optional<McpToolDefinition> foundTool2 = discoveryService.getTool("server1:suggest");
        assertTrue(foundTool2.isPresent());
        assertEquals("suggest", foundTool2.get().getToolName());

        // Verify disabled server was not queried
        verify(mcpClient, times(1)).discoverTools(any(McpServerConfig.class));
    }

    @Test
    void testDiscoverAllTools_NoServersConfigured() {
        // Given: Empty server list
        discoveryService = new McpToolDiscoveryService(mcpClient, List.of());

        // When: Discovery runs
        discoveryService.discoverAllTools();

        // Then: Should have no tools
        assertEquals(0, discoveryService.getToolCount());
        assertFalse(discoveryService.hasTools());
        verify(mcpClient, never()).discoverTools(any());
    }

    @Test
    void testDiscoverAllTools_ServerFailure() {
        // Given: Server returns error
        when(mcpClient.discoverTools(any(McpServerConfig.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")));

        discoveryService = new McpToolDiscoveryService(mcpClient, serverConfigs);

        // When: Discovery runs
        discoveryService.discoverAllTools();

        // Then: Should handle gracefully with no tools registered
        assertEquals(0, discoveryService.getToolCount());
        assertFalse(discoveryService.hasTools());
    }

    @Test
    void testDiscoverAllTools_EmptyToolList() {
        // Given: Server returns empty tool list
        when(mcpClient.discoverTools(any(McpServerConfig.class)))
                .thenReturn(Mono.just(List.of()));

        discoveryService = new McpToolDiscoveryService(mcpClient, serverConfigs);

        // When: Discovery runs
        discoveryService.discoverAllTools();

        // Then: Should have no tools
        assertEquals(0, discoveryService.getToolCount());
        assertFalse(discoveryService.hasTools());
    }

    @Test
    void testGetTool_Found() {
        // Given: Tool is registered
        McpToolDefinition tool = McpToolDefinition.builder()
                .serverName("server1")
                .toolName("search")
                .description("Search employees")
                .inputSchema(Map.of())
                .build();

        when(mcpClient.discoverTools(any(McpServerConfig.class)))
                .thenReturn(Mono.just(List.of(tool)));

        discoveryService = new McpToolDiscoveryService(mcpClient, serverConfigs);
        discoveryService.discoverAllTools();

        // When: Getting tool by qualified name
        Optional<McpToolDefinition> result = discoveryService.getTool("server1:search");

        // Then: Should find the tool
        assertTrue(result.isPresent());
        assertEquals("search", result.get().getToolName());
        assertEquals("server1", result.get().getServerName());
    }

    @Test
    void testGetTool_NotFound() {
        // Given: No tools registered
        when(mcpClient.discoverTools(any(McpServerConfig.class)))
                .thenReturn(Mono.just(List.of()));

        discoveryService = new McpToolDiscoveryService(mcpClient, serverConfigs);
        discoveryService.discoverAllTools();

        // When: Getting non-existent tool
        Optional<McpToolDefinition> result = discoveryService.getTool("server1:nonexistent");

        // Then: Should return empty
        assertFalse(result.isPresent());
    }

    @Test
    void testGetAllTools() {
        // Given: Multiple tools registered
        McpToolDefinition tool1 = McpToolDefinition.builder()
                .serverName("server1")
                .toolName("tool1")
                .description("Tool 1")
                .inputSchema(Map.of())
                .build();

        McpToolDefinition tool2 = McpToolDefinition.builder()
                .serverName("server1")
                .toolName("tool2")
                .description("Tool 2")
                .inputSchema(Map.of())
                .build();

        when(mcpClient.discoverTools(any(McpServerConfig.class)))
                .thenReturn(Mono.just(List.of(tool1, tool2)));

        discoveryService = new McpToolDiscoveryService(mcpClient, serverConfigs);
        discoveryService.discoverAllTools();

        // When: Getting all tools
        List<McpToolDefinition> allTools = discoveryService.getAllTools();

        // Then: Should return all registered tools
        assertEquals(2, allTools.size());
        assertTrue(allTools.stream().anyMatch(t -> t.getToolName().equals("tool1")));
        assertTrue(allTools.stream().anyMatch(t -> t.getToolName().equals("tool2")));
    }

    @Test
    void testGetToolsByServer() {
        // Given: Tools from multiple servers
        McpServerConfig enabledConfig = McpServerConfig.builder()
                .name("server1")
                .baseUrl("http://localhost:9000")
                .enabled(true)
                .timeout(Duration.ofSeconds(30))
                .build();

        McpServerConfig anotherConfig = McpServerConfig.builder()
                .name("server2")
                .baseUrl("http://localhost:9001")
                .enabled(true)
                .timeout(Duration.ofSeconds(30))
                .build();

        List<McpServerConfig> multiServerConfigs = List.of(enabledConfig, anotherConfig);

        McpToolDefinition tool1 = McpToolDefinition.builder()
                .serverName("server1")
                .toolName("tool1")
                .description("Tool 1")
                .inputSchema(Map.of())
                .build();

        McpToolDefinition tool2 = McpToolDefinition.builder()
                .serverName("server2")
                .toolName("tool2")
                .description("Tool 2")
                .inputSchema(Map.of())
                .build();

        when(mcpClient.discoverTools(enabledConfig))
                .thenReturn(Mono.just(List.of(tool1)));
        when(mcpClient.discoverTools(anotherConfig))
                .thenReturn(Mono.just(List.of(tool2)));

        discoveryService = new McpToolDiscoveryService(mcpClient, multiServerConfigs);
        discoveryService.discoverAllTools();

        // When: Getting tools by specific server
        List<McpToolDefinition> server1Tools = discoveryService.getToolsByServer("server1");
        List<McpToolDefinition> server2Tools = discoveryService.getToolsByServer("server2");

        // Then: Should return only tools from that server
        assertEquals(1, server1Tools.size());
        assertEquals("tool1", server1Tools.get(0).getToolName());

        assertEquals(1, server2Tools.size());
        assertEquals("tool2", server2Tools.get(0).getToolName());
    }

    @Test
    void testQualifiedNameGeneration() {
        // Given: A tool definition
        McpToolDefinition tool = McpToolDefinition.builder()
                .serverName("my-server")
                .toolName("my-tool")
                .description("Test tool")
                .inputSchema(Map.of())
                .build();

        // When: Getting qualified name
        String qualifiedName = tool.getQualifiedName();

        // Then: Should follow server:toolname format
        assertEquals("my-server:my-tool", qualifiedName);
    }
}
