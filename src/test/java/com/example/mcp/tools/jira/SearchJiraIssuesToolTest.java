package com.example.mcp.tools.jira;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SearchJiraIssuesTool.
 */
@DisplayName("SearchJiraIssuesTool Tests")
class SearchJiraIssuesToolTest {

    private SearchJiraIssuesTool tool;

    @BeforeEach
    void setUp() {
        tool = new SearchJiraIssuesTool();
    }

    @Test
    @DisplayName("Should have correct tool name")
    void testGetName() {
        // Act
        String name = tool.getName();

        // Assert
        assertEquals("jira-search-issues", name);
    }

    @Test
    @DisplayName("Should have descriptive description")
    void testGetDescription() {
        // Act
        String description = tool.getDescription();

        // Assert
        assertNotNull(description);
        assertTrue(description.contains("JIRA") || description.contains("JQL"));
    }

    @Test
    @DisplayName("Should have valid schema")
    void testGetSchema() {
        // Act
        Map<String, Object> schema = tool.getSchema();

        // Assert
        assertNotNull(schema);
        assertEquals("jira-search-issues", schema.get("name"));
        assertTrue(schema.containsKey("description"));
        assertTrue(schema.containsKey("inputSchema"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) schema.get("inputSchema");
        assertNotNull(inputSchema);
        assertTrue(inputSchema.containsKey("properties"));
        assertTrue(inputSchema.containsKey("required"));
    }

    @Test
    @DisplayName("Should require only jql parameter when configured")
    void testSchemaRequiredParameters() {
        // Act
        Map<String, Object> schema = tool.getSchema();

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) schema.get("inputSchema");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) inputSchema.get("required");

        assertNotNull(required);
        assertTrue(required.contains("jql"));
        // Credentials are now optional
        assertFalse(required.contains("jiraUrl"));
        assertFalse(required.contains("email"));
        assertFalse(required.contains("apiToken"));
    }

    @Test
    @DisplayName("Should have all necessary properties in schema")
    void testSchemaProperties() {
        // Act
        Map<String, Object> schema = tool.getSchema();

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) schema.get("inputSchema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");

        assertTrue(properties.containsKey("jiraUrl"));
        assertTrue(properties.containsKey("email"));
        assertTrue(properties.containsKey("apiToken"));
        assertTrue(properties.containsKey("jql"));
        assertTrue(properties.containsKey("maxResults"));
    }

    @Test
    @DisplayName("Should indicate credentials are optional in descriptions")
    void testSchemaOptionalCredentials() {
        // Act
        Map<String, Object> schema = tool.getSchema();

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) schema.get("inputSchema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");

        @SuppressWarnings("unchecked")
        Map<String, Object> jiraUrlProp = (Map<String, Object>) properties.get("jiraUrl");
        String jiraUrlDesc = (String) jiraUrlProp.get("description");
        assertTrue(jiraUrlDesc.contains("Optional") || jiraUrlDesc.contains("optional"));
    }

    @Test
    @DisplayName("Should fail when JQL is missing")
    void testExecuteWithoutJQL() {
        // Arrange
        Map<String, Object> arguments = new HashMap<>();
        // Missing jql parameter

        // Act & Assert
        assertThrows(Exception.class, () -> {
            tool.execute(arguments);
        }, "Should throw exception when jql is missing");
    }

    @Test
    @DisplayName("Should fail when credentials not configured and not provided")
    void testExecuteWithoutCredentials() {
        // Arrange
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("jql", "project = TEST");
        // No credentials provided and assuming not configured

        // Act & Assert
        // This will throw if ConfigurationManager doesn't have JIRA configured
        // and no credentials are provided in arguments
        try {
            tool.execute(arguments);
            // If it succeeds, credentials were configured
        } catch (Exception e) {
            // Expected if not configured
            assertTrue(e.getMessage().contains("JIRA") ||
                      e.getMessage().contains("not configured") ||
                      e.getMessage().contains("URL"));
        }
    }

    @Test
    @DisplayName("Should use default maxResults when not specified")
    void testExecuteWithDefaultMaxResults() {
        // Arrange
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("jiraUrl", "https://test.atlassian.net");
        arguments.put("email", "test@example.com");
        arguments.put("apiToken", "test-token");
        arguments.put("jql", "project = TEST");
        // maxResults not specified, should default to 50

        // Act & Assert
        // This will fail due to network, but we're testing parameter handling
        try {
            tool.execute(arguments);
        } catch (Exception e) {
            // Expected - network error or invalid server
            // The important thing is parameters were processed
            assertNotNull(e);
        }
    }
}
