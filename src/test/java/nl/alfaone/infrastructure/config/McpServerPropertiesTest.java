package nl.alfaone.infrastructure.config;

import nl.alfaone.domain.mcp.McpServerConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class McpServerPropertiesTest {

    @Test
    void testDefaultValues() {
        // Given: Empty properties
        McpServerProperties properties = new McpServerProperties();

        // Then: Should have sensible defaults
        assertFalse(properties.isEnabled()); // Disabled by default
        assertNotNull(properties.getServers());
        assertTrue(properties.getServers().isEmpty());
    }

    @Test
    void testServerConfigDefaults() {
        // Given: Server config with minimal settings
        McpServerProperties.ServerConfig serverConfig = new McpServerProperties.ServerConfig();
        serverConfig.setName("test-server");
        serverConfig.setBaseUrl("http://localhost:9000");

        // Then: Should have sensible defaults
        assertTrue(serverConfig.isEnabled()); // Enabled by default
        assertEquals(30, serverConfig.getTimeoutSeconds()); // 30 second default
        assertNull(serverConfig.getAuth());
    }

    @Test
    void testAuthConfigDefaults() {
        // Given: Auth config with token
        McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
        authConfig.setToken("test-token");

        // Then: Should have default header
        assertEquals("Authorization", authConfig.getHeader());
        assertEquals("test-token", authConfig.getToken());
    }

    @Test
    void testToMcpServerConfigs_SingleServer() {
        // Given: Properties with one server
        McpServerProperties properties = new McpServerProperties();
        properties.setEnabled(true);

        McpServerProperties.ServerConfig serverConfig = new McpServerProperties.ServerConfig();
        serverConfig.setName("test-server");
        serverConfig.setBaseUrl("http://localhost:9000");
        serverConfig.setEnabled(true);
        serverConfig.setTimeoutSeconds(45);

        McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
        authConfig.setToken("secret-token");
        authConfig.setHeader("X-API-Key");
        serverConfig.setAuth(authConfig);

        properties.setServers(List.of(serverConfig));

        // When: Converting to domain configs
        List<McpServerConfig> configs = properties.toMcpServerConfigs();

        // Then: Should map correctly
        assertEquals(1, configs.size());

        McpServerConfig config = configs.get(0);
        assertEquals("test-server", config.getName());
        assertEquals("http://localhost:9000", config.getBaseUrl());
        assertTrue(config.isEnabled());
        assertEquals(Duration.ofSeconds(45), config.getTimeout());
        assertEquals("secret-token", config.getAuthToken());
    }

    @Test
    void testToMcpServerConfigs_MultipleServers() {
        // Given: Properties with multiple servers
        McpServerProperties properties = new McpServerProperties();
        properties.setEnabled(true);

        McpServerProperties.ServerConfig server1 = new McpServerProperties.ServerConfig();
        server1.setName("server1");
        server1.setBaseUrl("http://localhost:9000");
        server1.setEnabled(true);
        server1.setTimeoutSeconds(30);

        McpServerProperties.ServerConfig server2 = new McpServerProperties.ServerConfig();
        server2.setName("server2");
        server2.setBaseUrl("http://localhost:9001");
        server2.setEnabled(false);
        server2.setTimeoutSeconds(60);

        properties.setServers(List.of(server1, server2));

        // When: Converting to domain configs
        List<McpServerConfig> configs = properties.toMcpServerConfigs();

        // Then: Should map both servers
        assertEquals(2, configs.size());

        McpServerConfig config1 = configs.get(0);
        assertEquals("server1", config1.getName());
        assertTrue(config1.isEnabled());
        assertEquals(Duration.ofSeconds(30), config1.getTimeout());

        McpServerConfig config2 = configs.get(1);
        assertEquals("server2", config2.getName());
        assertFalse(config2.isEnabled());
        assertEquals(Duration.ofSeconds(60), config2.getTimeout());
    }

    @Test
    void testToMcpServerConfigs_NoAuth() {
        // Given: Server without auth
        McpServerProperties properties = new McpServerProperties();
        properties.setEnabled(true);

        McpServerProperties.ServerConfig serverConfig = new McpServerProperties.ServerConfig();
        serverConfig.setName("public-server");
        serverConfig.setBaseUrl("http://localhost:9000");
        // No auth config set

        properties.setServers(List.of(serverConfig));

        // When: Converting to domain configs
        List<McpServerConfig> configs = properties.toMcpServerConfigs();

        // Then: Auth token should be null
        assertEquals(1, configs.size());
        assertNull(configs.get(0).getAuthToken());
    }

    @Test
    void testToMcpServerConfigs_EmptyList() {
        // Given: Properties with no servers
        McpServerProperties properties = new McpServerProperties();
        properties.setEnabled(true);

        // When: Converting to domain configs
        List<McpServerConfig> configs = properties.toMcpServerConfigs();

        // Then: Should return empty list
        assertNotNull(configs);
        assertTrue(configs.isEmpty());
    }

    @Test
    void testServerConfigSetters() {
        // Given: Server config
        McpServerProperties.ServerConfig config = new McpServerProperties.ServerConfig();

        // When: Setting all properties
        config.setName("my-server");
        config.setBaseUrl("https://api.example.com");
        config.setEnabled(false);
        config.setTimeoutSeconds(120);

        McpServerProperties.AuthConfig auth = new McpServerProperties.AuthConfig();
        auth.setToken("bearer-token");
        auth.setHeader("Custom-Auth");
        config.setAuth(auth);

        // Then: All properties should be set
        assertEquals("my-server", config.getName());
        assertEquals("https://api.example.com", config.getBaseUrl());
        assertFalse(config.isEnabled());
        assertEquals(120, config.getTimeoutSeconds());
        assertNotNull(config.getAuth());
        assertEquals("bearer-token", config.getAuth().getToken());
        assertEquals("Custom-Auth", config.getAuth().getHeader());
    }

    @Test
    void testGlobalEnableFlag() {
        // Given: Properties with global enable flag
        McpServerProperties properties = new McpServerProperties();

        // When: Setting enabled to true
        properties.setEnabled(true);

        // Then: Should be enabled
        assertTrue(properties.isEnabled());

        // When: Setting enabled to false
        properties.setEnabled(false);

        // Then: Should be disabled
        assertFalse(properties.isEnabled());
    }

    @Test
    void testTimeoutConversion() {
        // Given: Server with various timeout values
        McpServerProperties properties = new McpServerProperties();

        McpServerProperties.ServerConfig shortTimeout = new McpServerProperties.ServerConfig();
        shortTimeout.setName("short");
        shortTimeout.setBaseUrl("http://localhost:9000");
        shortTimeout.setTimeoutSeconds(5);

        McpServerProperties.ServerConfig longTimeout = new McpServerProperties.ServerConfig();
        longTimeout.setName("long");
        longTimeout.setBaseUrl("http://localhost:9001");
        longTimeout.setTimeoutSeconds(300);

        properties.setServers(List.of(shortTimeout, longTimeout));

        // When: Converting to domain configs
        List<McpServerConfig> configs = properties.toMcpServerConfigs();

        // Then: Timeouts should be converted to Duration
        assertEquals(Duration.ofSeconds(5), configs.get(0).getTimeout());
        assertEquals(Duration.ofSeconds(300), configs.get(1).getTimeout());
    }

    @Test
    void testAuthTokenMapping() {
        // Given: Server with auth token
        McpServerProperties properties = new McpServerProperties();

        McpServerProperties.ServerConfig serverConfig = new McpServerProperties.ServerConfig();
        serverConfig.setName("secure-server");
        serverConfig.setBaseUrl("http://localhost:9000");

        McpServerProperties.AuthConfig auth = new McpServerProperties.AuthConfig();
        auth.setToken("my-secret-token");
        serverConfig.setAuth(auth);

        properties.setServers(List.of(serverConfig));

        // When: Converting to domain configs
        List<McpServerConfig> configs = properties.toMcpServerConfigs();

        // Then: Auth token should be mapped
        assertEquals("my-secret-token", configs.get(0).getAuthToken());
    }

    @Test
    void testDisabledServerMapping() {
        // Given: Disabled server
        McpServerProperties properties = new McpServerProperties();

        McpServerProperties.ServerConfig serverConfig = new McpServerProperties.ServerConfig();
        serverConfig.setName("disabled-server");
        serverConfig.setBaseUrl("http://localhost:9000");
        serverConfig.setEnabled(false);

        properties.setServers(List.of(serverConfig));

        // When: Converting to domain configs
        List<McpServerConfig> configs = properties.toMcpServerConfigs();

        // Then: Server should be marked as disabled
        assertFalse(configs.get(0).isEnabled());
    }
}
