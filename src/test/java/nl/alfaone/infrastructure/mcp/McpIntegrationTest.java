package nl.alfaone.infrastructure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import nl.alfaone.application.mcp.McpToolDiscoveryService;
import nl.alfaone.application.mcp.McpToolExecutionService;
import nl.alfaone.domain.mcp.McpServerConfig;
import nl.alfaone.domain.mcp.McpToolDefinition;
import nl.alfaone.domain.mcp.McpToolResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for MCP components using WireMock to simulate an MCP server.
 * This test verifies the full HTTP communication flow including JSON-RPC 2.0 protocol.
 */
class McpIntegrationTest {

    private WireMockServer wireMockServer;
    private McpJsonRpcClient mcpClient;
    private McpToolDiscoveryService discoveryService;
    private McpToolExecutionService executionService;
    private McpServerConfig serverConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Start WireMock server on random port
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Create real components (not mocked)
        objectMapper = new ObjectMapper();
        mcpClient = new McpJsonRpcClient(WebClient.builder(), objectMapper);

        // Configure to point to WireMock server
        serverConfig = McpServerConfig.builder()
                .name("test-server")
                .baseUrl("http://localhost:" + wireMockServer.port())
                .enabled(true)
                .timeout(Duration.ofSeconds(5))
                .build();

        List<McpServerConfig> serverConfigs = List.of(serverConfig);

        discoveryService = new McpToolDiscoveryService(mcpClient, serverConfigs);
        executionService = new McpToolExecutionService(mcpClient, discoveryService, serverConfigs);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    void testFullMcpFlow_DiscoveryAndExecution() {
        // Given: WireMock stub for tools/list
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "result": {
                                "tools": [
                                  {
                                    "name": "search_employees",
                                    "description": "Search for employees by criteria",
                                    "inputSchema": {
                                      "type": "object",
                                      "properties": {
                                        "query": {"type": "string"},
                                        "limit": {"type": "number"}
                                      },
                                      "required": ["query"]
                                    }
                                  }
                                ]
                              }
                            }
                            """)));

        // Given: WireMock stub for tools/call
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/call"))
                .withRequestBody(containing("search_employees"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 2,
                              "result": {
                                "employees": [
                                  {"name": "John Doe", "skills": ["Java", "Spring Boot"]},
                                  {"name": "Jane Smith", "skills": ["Java", "Kubernetes"]}
                                ]
                              }
                            }
                            """)));

        // When: Discover tools
        discoveryService.discoverAllTools();

        // Then: Tool should be registered
        assertEquals(1, discoveryService.getToolCount());
        assertTrue(discoveryService.getTool("test-server:search_employees").isPresent());

        McpToolDefinition tool = discoveryService.getTool("test-server:search_employees").get();
        assertEquals("search_employees", tool.getToolName());
        assertEquals("Search for employees by criteria", tool.getDescription());
        assertNotNull(tool.getInputSchema());

        // When: Execute tool
        Map<String, Object> arguments = Map.of("query", "Java developers", "limit", 10);
        McpToolResult result = executionService.executeToolSync("test-server:search_employees", arguments);

        // Then: Should get successful result
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Tool execution should succeed");
        assertNotNull(result.getResult());
        assertNull(result.getErrorMessage());

        // Verify the result contains employee data
        Map<String, Object> resultData = (Map<String, Object>) result.getResult();
        assertTrue(resultData.containsKey("employees"));

        // Verify WireMock received correct requests
        verify(exactly(1), postRequestedFor(urlEqualTo("/"))
                .withRequestBody(containing("tools/list")));

        verify(exactly(1), postRequestedFor(urlEqualTo("/"))
                .withRequestBody(containing("tools/call"))
                .withRequestBody(containing("search_employees"))
                .withRequestBody(containing("Java developers")));
    }

    @Test
    void testMcpServerError_GracefulHandling() {
        // Given: WireMock returns error
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "error": {
                                "code": -32600,
                                "message": "Invalid Request"
                              }
                            }
                            """)));

        // When: Discover tools
        discoveryService.discoverAllTools();

        // Then: Should handle gracefully with no tools registered
        assertEquals(0, discoveryService.getToolCount());
        assertFalse(discoveryService.hasTools());
    }

    @Test
    void testMcpToolExecution_WithError() {
        // Given: Tool is discovered successfully
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "result": {
                                "tools": [
                                  {
                                    "name": "failing_tool",
                                    "description": "A tool that fails"
                                  }
                                ]
                              }
                            }
                            """)));

        // Given: Tool execution returns error
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/call"))
                .withRequestBody(containing("failing_tool"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 2,
                              "error": {
                                "code": -32602,
                                "message": "Invalid params: missing required field 'query'"
                              }
                            }
                            """)));

        discoveryService.discoverAllTools();

        // When: Execute tool
        McpToolResult result = executionService.executeToolSync("test-server:failing_tool", Map.of());

        // Then: Should get error result
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Invalid params"));
    }

    @Test
    void testMcpServerTimeout() {
        // Given: WireMock with delay longer than timeout
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(6000) // 6 seconds delay
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // When: Discover tools (should timeout)
        discoveryService.discoverAllTools();

        // Then: Should handle timeout gracefully
        assertEquals(0, discoveryService.getToolCount());
    }

    @Test
    void testJsonRpcProtocol_RequestFormat() {
        // Given: Capture request
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "result": {"tools": []}
                            }
                            """)));

        // When: Call discover
        List<McpToolDefinition> tools = mcpClient.discoverTools(serverConfig).block();

        // Then: Verify JSON-RPC 2.0 request format
        verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                .withRequestBody(matchingJsonPath("$.method", equalTo("tools/list")))
                .withRequestBody(matchingJsonPath("$.id")));
    }

    @Test
    void testMultipleToolDiscovery() {
        // Given: Server returns multiple tools
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "result": {
                                "tools": [
                                  {
                                    "name": "tool1",
                                    "description": "First tool"
                                  },
                                  {
                                    "name": "tool2",
                                    "description": "Second tool"
                                  },
                                  {
                                    "name": "tool3",
                                    "description": "Third tool"
                                  }
                                ]
                              }
                            }
                            """)));

        // When: Discover tools
        discoveryService.discoverAllTools();

        // Then: All tools should be registered
        assertEquals(3, discoveryService.getToolCount());
        assertTrue(discoveryService.getTool("test-server:tool1").isPresent());
        assertTrue(discoveryService.getTool("test-server:tool2").isPresent());
        assertTrue(discoveryService.getTool("test-server:tool3").isPresent());

        List<McpToolDefinition> allTools = discoveryService.getAllTools();
        assertEquals(3, allTools.size());
    }

    @Test
    void testAuthenticationHeader() {
        // Given: Server config with auth token
        McpServerConfig configWithAuth = McpServerConfig.builder()
                .name("secure-server")
                .baseUrl("http://localhost:" + wireMockServer.port())
                .enabled(true)
                .timeout(Duration.ofSeconds(5))
                .authToken("secret-bearer-token-123")
                .build();

        stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "result": {"tools": []}
                            }
                            """)));

        // When: Make request with auth
        mcpClient.discoverTools(configWithAuth).block();

        // Then: Should include authorization header
        verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("Authorization", equalTo("Bearer secret-bearer-token-123")));
    }

    @Test
    void testReactiveExecution() {
        // Given: Tool is discovered
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "result": {
                                "tools": [{"name": "async_tool", "description": "Async tool"}]
                              }
                            }
                            """)));

        stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("tools/call"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "id": 2,
                              "result": {"status": "success"}
                            }
                            """)));

        discoveryService.discoverAllTools();

        // When: Execute tool reactively (using Mono)
        Mono<McpToolResult> resultMono = executionService.executeTool("test-server:async_tool", Map.of());

        // Then: Should complete successfully
        McpToolResult result = resultMono.block();
        assertNotNull(result);
        assertTrue(result.isSuccess());
    }
}
