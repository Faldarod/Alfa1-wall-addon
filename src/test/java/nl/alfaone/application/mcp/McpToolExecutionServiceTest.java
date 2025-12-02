package nl.alfaone.application.mcp;

import nl.alfaone.domain.mcp.McpServerConfig;
import nl.alfaone.domain.mcp.McpToolDefinition;
import nl.alfaone.domain.mcp.McpToolInvocation;
import nl.alfaone.domain.mcp.McpToolResult;
import nl.alfaone.infrastructure.mcp.McpJsonRpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolExecutionServiceTest {

    @Mock
    private McpJsonRpcClient mcpClient;

    @Mock
    private McpToolDiscoveryService discoveryService;

    private McpToolExecutionService executionService;
    private McpServerConfig testServerConfig;
    private McpToolDefinition testToolDefinition;

    @BeforeEach
    void setUp() {
        testServerConfig = McpServerConfig.builder()
                .name("test-server")
                .baseUrl("http://localhost:9000")
                .enabled(true)
                .timeout(Duration.ofSeconds(30))
                .build();

        testToolDefinition = McpToolDefinition.builder()
                .serverName("test-server")
                .toolName("search")
                .description("Search employees")
                .inputSchema(Map.of("type", "object"))
                .build();

        List<McpServerConfig> serverConfigs = List.of(testServerConfig);
        executionService = new McpToolExecutionService(mcpClient, discoveryService, serverConfigs);
    }

    @Test
    void testExecuteTool_Success() {
        // Given: Tool exists and execution succeeds
        when(discoveryService.getTool("test-server:search"))
                .thenReturn(Optional.of(testToolDefinition));

        McpToolResult successResult = McpToolResult.success(
                Map.of("employees", List.of("John Doe"))
        );

        when(mcpClient.invokeTool(any(McpServerConfig.class), any(McpToolInvocation.class)))
                .thenReturn(Mono.just(successResult));

        Map<String, Object> arguments = Map.of("query", "Java");

        // When: Executing tool
        Mono<McpToolResult> result = executionService.executeTool("test-server:search", arguments);

        // Then: Should return success result
        StepVerifier.create(result)
                .assertNext(toolResult -> {
                    assertTrue(toolResult.isSuccess());
                    assertNotNull(toolResult.getResult());
                })
                .verifyComplete();

        // Verify invocation was called with correct parameters
        ArgumentCaptor<McpToolInvocation> invocationCaptor = ArgumentCaptor.forClass(McpToolInvocation.class);
        verify(mcpClient).invokeTool(eq(testServerConfig), invocationCaptor.capture());

        McpToolInvocation capturedInvocation = invocationCaptor.getValue();
        assertEquals("search", capturedInvocation.getToolName());
        assertEquals("Java", capturedInvocation.getArguments().get("query"));
    }

    @Test
    void testExecuteTool_ToolNotFound() {
        // Given: Tool does not exist in registry
        when(discoveryService.getTool("nonexistent:tool"))
                .thenReturn(Optional.empty());

        // When: Executing non-existent tool
        Mono<McpToolResult> result = executionService.executeTool("nonexistent:tool", Map.of());

        // Then: Should return error result
        StepVerifier.create(result)
                .assertNext(toolResult -> {
                    assertFalse(toolResult.isSuccess());
                    assertTrue(toolResult.getErrorMessage().contains("Tool not found"));
                })
                .verifyComplete();

        // Verify client was never called
        verify(mcpClient, never()).invokeTool(any(), any());
    }

    @Test
    void testExecuteTool_ServerDisabled() {
        // Given: Server is disabled
        McpServerConfig disabledConfig = McpServerConfig.builder()
                .name("disabled-server")
                .baseUrl("http://localhost:9001")
                .enabled(false) // Disabled
                .timeout(Duration.ofSeconds(30))
                .build();

        McpToolDefinition disabledServerTool = McpToolDefinition.builder()
                .serverName("disabled-server")
                .toolName("search")
                .description("Test tool")
                .inputSchema(Map.of())
                .build();

        executionService = new McpToolExecutionService(
                mcpClient,
                discoveryService,
                List.of(disabledConfig)
        );

        when(discoveryService.getTool("disabled-server:search"))
                .thenReturn(Optional.of(disabledServerTool));

        // When: Executing tool on disabled server
        Mono<McpToolResult> result = executionService.executeTool("disabled-server:search", Map.of());

        // Then: Should return error result
        StepVerifier.create(result)
                .assertNext(toolResult -> {
                    assertFalse(toolResult.isSuccess());
                    assertTrue(toolResult.getErrorMessage().contains("Server is disabled"));
                })
                .verifyComplete();

        // Verify client was never called
        verify(mcpClient, never()).invokeTool(any(), any());
    }

    @Test
    void testExecuteTool_ExecutionError() {
        // Given: Tool exists but execution fails
        when(discoveryService.getTool("test-server:search"))
                .thenReturn(Optional.of(testToolDefinition));

        McpToolResult errorResult = McpToolResult.error("Invalid parameters");

        when(mcpClient.invokeTool(any(McpServerConfig.class), any(McpToolInvocation.class)))
                .thenReturn(Mono.just(errorResult));

        // When: Executing tool
        Mono<McpToolResult> result = executionService.executeTool("test-server:search", Map.of());

        // Then: Should return error result
        StepVerifier.create(result)
                .assertNext(toolResult -> {
                    assertFalse(toolResult.isSuccess());
                    assertEquals("Invalid parameters", toolResult.getErrorMessage());
                })
                .verifyComplete();
    }

    @Test
    void testExecuteTool_NetworkError() {
        // Given: Tool exists but network error occurs
        when(discoveryService.getTool("test-server:search"))
                .thenReturn(Optional.of(testToolDefinition));

        when(mcpClient.invokeTool(any(McpServerConfig.class), any(McpToolInvocation.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection timeout")));

        // When: Executing tool
        Mono<McpToolResult> result = executionService.executeTool("test-server:search", Map.of());

        // Then: Should propagate error
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testExecuteToolSync_Success() {
        // Given: Tool exists and execution succeeds
        when(discoveryService.getTool("test-server:search"))
                .thenReturn(Optional.of(testToolDefinition));

        McpToolResult successResult = McpToolResult.success(Map.of("result", "test"));

        when(mcpClient.invokeTool(any(McpServerConfig.class), any(McpToolInvocation.class)))
                .thenReturn(Mono.just(successResult));

        // When: Executing tool synchronously
        McpToolResult result = executionService.executeToolSync("test-server:search", Map.of());

        // Then: Should return success result
        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    void testExecuteToolSync_WithNullArguments() {
        // Given: Tool exists
        when(discoveryService.getTool("test-server:search"))
                .thenReturn(Optional.of(testToolDefinition));

        McpToolResult successResult = McpToolResult.success(Map.of());

        when(mcpClient.invokeTool(any(McpServerConfig.class), any(McpToolInvocation.class)))
                .thenReturn(Mono.just(successResult));

        // When: Executing with null arguments
        McpToolResult result = executionService.executeToolSync("test-server:search", null);

        // Then: Should handle gracefully with empty map
        assertNotNull(result);
        assertTrue(result.isSuccess());

        ArgumentCaptor<McpToolInvocation> captor = ArgumentCaptor.forClass(McpToolInvocation.class);
        verify(mcpClient).invokeTool(any(), captor.capture());
        assertNotNull(captor.getValue().getArguments());
        assertTrue(captor.getValue().getArguments().isEmpty());
    }

    @Test
    void testIsToolAvailable_Available() {
        // Given: Tool exists and server is enabled
        when(discoveryService.getTool("test-server:search"))
                .thenReturn(Optional.of(testToolDefinition));

        // When: Checking availability
        boolean available = executionService.isToolAvailable("test-server:search");

        // Then: Should be available
        assertTrue(available);
    }

    @Test
    void testIsToolAvailable_NotFound() {
        // Given: Tool does not exist
        when(discoveryService.getTool("nonexistent:tool"))
                .thenReturn(Optional.empty());

        // When: Checking availability
        boolean available = executionService.isToolAvailable("nonexistent:tool");

        // Then: Should not be available
        assertFalse(available);
    }

    @Test
    void testIsToolAvailable_ServerDisabled() {
        // Given: Tool exists but server is disabled
        McpServerConfig disabledConfig = McpServerConfig.builder()
                .name("disabled-server")
                .baseUrl("http://localhost:9001")
                .enabled(false)
                .timeout(Duration.ofSeconds(30))
                .build();

        McpToolDefinition disabledServerTool = McpToolDefinition.builder()
                .serverName("disabled-server")
                .toolName("search")
                .description("Test")
                .inputSchema(Map.of())
                .build();

        executionService = new McpToolExecutionService(
                mcpClient,
                discoveryService,
                List.of(disabledConfig)
        );

        when(discoveryService.getTool("disabled-server:search"))
                .thenReturn(Optional.of(disabledServerTool));

        // When: Checking availability
        boolean available = executionService.isToolAvailable("disabled-server:search");

        // Then: Should not be available
        assertFalse(available);
    }

    @Test
    void testGetAvailableToolNames() {
        // Given: Multiple tools with different availability
        McpServerConfig enabledConfig = McpServerConfig.builder()
                .name("enabled-server")
                .baseUrl("http://localhost:9000")
                .enabled(true)
                .timeout(Duration.ofSeconds(30))
                .build();

        McpServerConfig disabledConfig = McpServerConfig.builder()
                .name("disabled-server")
                .baseUrl("http://localhost:9001")
                .enabled(false)
                .timeout(Duration.ofSeconds(30))
                .build();

        McpToolDefinition enabledTool = McpToolDefinition.builder()
                .serverName("enabled-server")
                .toolName("tool1")
                .description("Enabled tool")
                .inputSchema(Map.of())
                .build();

        McpToolDefinition disabledTool = McpToolDefinition.builder()
                .serverName("disabled-server")
                .toolName("tool2")
                .description("Disabled tool")
                .inputSchema(Map.of())
                .build();

        executionService = new McpToolExecutionService(
                mcpClient,
                discoveryService,
                List.of(enabledConfig, disabledConfig)
        );

        when(discoveryService.getAllTools())
                .thenReturn(List.of(enabledTool, disabledTool));

        // When: Getting available tool names
        List<String> availableNames = executionService.getAvailableToolNames();

        // Then: Should only return enabled tools
        assertEquals(1, availableNames.size());
        assertTrue(availableNames.contains("enabled-server:tool1"));
        assertFalse(availableNames.contains("disabled-server:tool2"));
    }
}
