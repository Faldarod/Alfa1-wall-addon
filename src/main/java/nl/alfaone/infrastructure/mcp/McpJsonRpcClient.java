package nl.alfaone.infrastructure.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.alfaone.domain.mcp.McpServerConfig;
import nl.alfaone.domain.mcp.McpToolDefinition;
import nl.alfaone.domain.mcp.McpToolInvocation;
import nl.alfaone.domain.mcp.McpToolResult;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JSON-RPC 2.0 client for communicating with MCP (Model Context Protocol) servers.
 * Follows the same WebClient pattern as HomeAssistantClient for consistency.
 *
 * <p>This client implements the JSON-RPC 2.0 specification:
 * - Request: {"jsonrpc":"2.0","id":1,"method":"tools/list"}
 * - Response: {"jsonrpc":"2.0","id":1,"result":{"tools":[...]}}
 * - Error: {"jsonrpc":"2.0","id":1,"error":{"code":-32600,"message":"Invalid Request"}}
 */
@Component
@Slf4j
public class McpJsonRpcClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestIdCounter = new AtomicLong(1);

    public McpJsonRpcClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Discovers all tools available on the MCP server by calling the "tools/list" RPC method.
     *
     * @param config The MCP server configuration
     * @return A Mono containing the list of discovered tool definitions
     */
    public Mono<List<McpToolDefinition>> discoverTools(McpServerConfig config) {
        log.debug("Discovering tools from MCP server: {}", config.getName());

        WebClient webClient = createWebClient(config);
        Map<String, Object> request = createJsonRpcRequest("tools/list", Collections.emptyMap());

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(config.getTimeout())
                .map(response -> parseToolsListResponse(response, config.getName()))
                .doOnSuccess(tools -> log.info("Discovered {} tools from MCP server '{}'",
                        tools.size(), config.getName()))
                .doOnError(e -> log.warn("Failed to discover tools from MCP server '{}': {}",
                        config.getName(), e.getMessage()))
                .onErrorReturn(Collections.emptyList());
    }

    /**
     * Invokes a specific tool on the MCP server by calling the "tools/call" RPC method.
     *
     * @param config The MCP server configuration
     * @param invocation The tool invocation request containing tool name and arguments
     * @return A Mono containing the tool execution result
     */
    public Mono<McpToolResult> invokeTool(McpServerConfig config, McpToolInvocation invocation) {
        log.debug("Invoking tool '{}' on MCP server '{}'", invocation.getToolName(), config.getName());

        WebClient webClient = createWebClient(config);
        Map<String, Object> params = Map.of(
                "name", invocation.getToolName(),
                "arguments", invocation.getArguments()
        );
        Map<String, Object> request = createJsonRpcRequest("tools/call", params);

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(config.getTimeout())
                .map(this::parseToolCallResponse)
                .doOnSuccess(result -> {
                    if (result.isSuccess()) {
                        log.debug("Tool '{}' executed successfully on MCP server '{}'",
                                invocation.getToolName(), config.getName());
                    } else {
                        log.warn("Tool '{}' execution failed on MCP server '{}': {}",
                                invocation.getToolName(), config.getName(), result.getErrorMessage());
                    }
                })
                .doOnError(e -> log.error("Error invoking tool '{}' on MCP server '{}': {}",
                        invocation.getToolName(), config.getName(), e.getMessage()))
                .onErrorReturn(McpToolResult.error("Network error: " + config.getName()));
    }

    /**
     * Creates a WebClient instance configured for the specified MCP server.
     */
    private WebClient createWebClient(McpServerConfig config) {
        WebClient.Builder builder = webClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Content-Type", "application/json");

        // Add authentication header if auth token is provided
        if (config.getAuthToken() != null && !config.getAuthToken().isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + config.getAuthToken());
        }

        return builder.build();
    }

    /**
     * Creates a JSON-RPC 2.0 request object.
     */
    private Map<String, Object> createJsonRpcRequest(String method, Map<String, Object> params) {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", requestIdCounter.getAndIncrement());
        request.put("method", method);
        if (params != null && !params.isEmpty()) {
            request.put("params", params);
        }
        return request;
    }

    /**
     * Parses the "tools/list" JSON-RPC response into a list of McpToolDefinition objects.
     */
    private List<McpToolDefinition> parseToolsListResponse(JsonNode response, String serverName) {
        if (response.has("error")) {
            log.error("JSON-RPC error from server '{}': {}", serverName, response.get("error"));
            return Collections.emptyList();
        }

        if (!response.has("result") || !response.get("result").has("tools")) {
            log.warn("Invalid tools/list response from server '{}': missing result.tools", serverName);
            return Collections.emptyList();
        }

        JsonNode toolsArray = response.get("result").get("tools");
        List<McpToolDefinition> tools = new ArrayList<>();

        for (JsonNode toolNode : toolsArray) {
            try {
                String toolName = toolNode.get("name").asText();
                String description = toolNode.has("description")
                        ? toolNode.get("description").asText()
                        : "No description available";

                Map<String, Object> inputSchema = toolNode.has("inputSchema")
                        ? objectMapper.convertValue(toolNode.get("inputSchema"), Map.class)
                        : Collections.emptyMap();

                McpToolDefinition tool = McpToolDefinition.builder()
                        .serverName(serverName)
                        .toolName(toolName)
                        .description(description)
                        .inputSchema(inputSchema)
                        .build();

                tools.add(tool);
                log.debug("Parsed tool: {}", tool.getQualifiedName());
            } catch (Exception e) {
                log.warn("Failed to parse tool from server '{}': {}", serverName, e.getMessage());
            }
        }

        return tools;
    }

    /**
     * Parses the "tools/call" JSON-RPC response into a McpToolResult object.
     */
    private McpToolResult parseToolCallResponse(JsonNode response) {
        if (response.has("error")) {
            JsonNode error = response.get("error");
            String errorMessage = error.has("message")
                    ? error.get("message").asText()
                    : "Unknown error";
            return McpToolResult.error(errorMessage);
        }

        if (!response.has("result")) {
            return McpToolResult.error("Invalid response: missing result");
        }

        Object result = objectMapper.convertValue(response.get("result"), Object.class);
        return McpToolResult.success(result);
    }
}
