package com.rafa.jdmatch.mcp;

import com.rafa.jdmatch.claude.ClaudeClient;
import io.modelcontextprotocol.server.McpSyncServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: boots the full app (web server + DB) with the MCP server starter active
 * and confirms the MCP server auto-configures and our tool bean is registered. The
 * tool's own behaviour is covered by {@link AnalysisToolsTest}; this guards the wiring
 * that adding the Spring AI MCP starter introduces. Claude is mocked, so no API key or
 * network is needed.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "anthropic.api-key=test-key")
@Testcontainers
class McpServerSmokeTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    private ClaudeClient claudeClient;

    @Autowired
    private ApplicationContext context;

    @Test
    void mcpServerAutoConfiguresWithTheAnalysisTool() {
        assertThat(context.getBeansOfType(McpSyncServer.class)).isNotEmpty();
        assertThat(context.getBeansOfType(AnalysisTools.class)).isNotEmpty();
    }
}
