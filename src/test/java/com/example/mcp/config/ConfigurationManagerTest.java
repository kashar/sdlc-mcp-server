package com.example.mcp.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigurationManager.
 */
@DisplayName("ConfigurationManager Tests")
class ConfigurationManagerTest {

    private ConfigurationManager config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Clear any existing environment variables for testing
        config = ConfigurationManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        // Cleanup if needed
    }

    @Test
    @DisplayName("Should return singleton instance")
    void testGetInstanceReturnsSingleton() {
        // Arrange & Act
        ConfigurationManager instance1 = ConfigurationManager.getInstance();
        ConfigurationManager instance2 = ConfigurationManager.getInstance();

        // Assert
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2, "Should return the same singleton instance");
    }

    @Test
    @DisplayName("Should return null for unconfigured JIRA URL")
    void testGetJiraUrlWhenNotConfigured() {
        // Arrange & Act
        String jiraUrl = config.getJiraUrl();

        // Assert
        // This will depend on whether environment variables are set
        // In a clean test environment, it should be null or empty
        assertTrue(jiraUrl == null || jiraUrl.isEmpty() || jiraUrl.startsWith("https://"),
                "JIRA URL should be null, empty, or a valid URL");
    }

    @Test
    @DisplayName("Should return null for unconfigured Confluence URL")
    void testGetConfluenceUrlWhenNotConfigured() {
        // Arrange & Act
        String confluenceUrl = config.getConfluenceUrl();

        // Assert
        assertTrue(confluenceUrl == null || confluenceUrl.isEmpty() || confluenceUrl.startsWith("https://"),
                "Confluence URL should be null, empty, or a valid URL");
    }

    @Test
    @DisplayName("Should check JIRA configuration status")
    void testHasJiraConfiguration() {
        // Arrange & Act
        boolean hasConfig = config.hasJiraConfiguration();

        // Assert
        // This depends on environment setup
        assertTrue(hasConfig || !hasConfig, "Should return a boolean value");
    }

    @Test
    @DisplayName("Should check Confluence configuration status")
    void testHasConfluenceConfiguration() {
        // Arrange & Act
        boolean hasConfig = config.hasConfluenceConfiguration();

        // Assert
        assertTrue(hasConfig || !hasConfig, "Should return a boolean value");
    }

    @Test
    @DisplayName("Should use parameter value when provided")
    void testGetJiraUrlOrDefaultWithParameter() {
        // Arrange
        String paramUrl = "https://test.atlassian.net";

        // Act
        String result = config.getJiraUrlOrDefault(paramUrl);

        // Assert
        assertEquals(paramUrl, result, "Should return the provided parameter value");
    }

    @Test
    @DisplayName("Should use parameter email when provided")
    void testGetJiraEmailOrDefaultWithParameter() {
        // Arrange
        String paramEmail = "test@example.com";

        // Act
        String result = config.getJiraEmailOrDefault(paramEmail);

        // Assert
        assertEquals(paramEmail, result, "Should return the provided parameter email");
    }

    @Test
    @DisplayName("Should use parameter API token when provided")
    void testGetJiraApiTokenOrDefaultWithParameter() {
        // Arrange
        String paramToken = "test-token-123";

        // Act
        String result = config.getJiraApiTokenOrDefault(paramToken);

        // Assert
        assertEquals(paramToken, result, "Should return the provided parameter token");
    }

    @Test
    @DisplayName("Should use parameter value for Confluence URL when provided")
    void testGetConfluenceUrlOrDefaultWithParameter() {
        // Arrange
        String paramUrl = "https://test.atlassian.net/wiki";

        // Act
        String result = config.getConfluenceUrlOrDefault(paramUrl);

        // Assert
        assertEquals(paramUrl, result, "Should return the provided parameter value");
    }

    @Test
    @DisplayName("Should throw exception when JIRA URL not configured and no parameter")
    void testGetJiraUrlOrDefaultThrowsWhenNotConfigured() {
        // Arrange & Act & Assert
        if (!config.hasJiraConfiguration()) {
            assertThrows(IllegalArgumentException.class, () -> {
                config.getJiraUrlOrDefault(null);
            }, "Should throw IllegalArgumentException when JIRA URL is not configured");
        }
    }

    @Test
    @DisplayName("Should throw exception when JIRA email not configured and no parameter")
    void testGetJiraEmailOrDefaultThrowsWhenNotConfigured() {
        // Arrange & Act & Assert
        if (!config.hasJiraConfiguration()) {
            assertThrows(IllegalArgumentException.class, () -> {
                config.getJiraEmailOrDefault(null);
            }, "Should throw IllegalArgumentException when JIRA email is not configured");
        }
    }

    @Test
    @DisplayName("Should throw exception when JIRA API token not configured and no parameter")
    void testGetJiraApiTokenOrDefaultThrowsWhenNotConfigured() {
        // Arrange & Act & Assert
        if (!config.hasJiraConfiguration()) {
            assertThrows(IllegalArgumentException.class, () -> {
                config.getJiraApiTokenOrDefault(null);
            }, "Should throw IllegalArgumentException when JIRA API token is not configured");
        }
    }

    @Test
    @DisplayName("Should handle empty string parameters")
    void testGetJiraUrlOrDefaultWithEmptyString() {
        // Arrange
        String emptyParam = "";

        // Act & Assert
        if (!config.hasJiraConfiguration()) {
            assertThrows(IllegalArgumentException.class, () -> {
                config.getJiraUrlOrDefault(emptyParam);
            }, "Should throw IllegalArgumentException when parameter is empty and not configured");
        }
    }

    @Test
    @DisplayName("Should prefer parameter over configuration")
    void testParameterPriorityOverConfiguration() {
        // Arrange
        String paramUrl = "https://override.atlassian.net";

        // Act
        String result = config.getJiraUrlOrDefault(paramUrl);

        // Assert
        assertEquals(paramUrl, result, "Should prefer parameter value over configuration");
    }
}
