package com.example.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration manager for SDLC Tools MCP Server.
 *
 * <p>Loads configuration from multiple sources in the following priority order:
 * <ol>
 *   <li>Environment variables</li>
 *   <li>application.properties file in current directory</li>
 *   <li>application.properties file in user home directory (~/.sdlc-tools/)</li>
 *   <li>application.properties file in classpath</li>
 * </ol>
 *
 * <h2>JIRA Configuration:</h2>
 * <ul>
 *   <li><b>JIRA_URL</b> or jira.url - JIRA instance URL (e.g., https://your-domain.atlassian.net)</li>
 *   <li><b>JIRA_EMAIL</b> or jira.email - User email for authentication</li>
 *   <li><b>JIRA_API_TOKEN</b> or jira.api.token - JIRA API token</li>
 * </ul>
 *
 * <h2>Confluence Configuration:</h2>
 * <ul>
 *   <li><b>CONFLUENCE_URL</b> or confluence.url - Confluence instance URL (e.g., https://your-domain.atlassian.net/wiki)</li>
 *   <li><b>CONFLUENCE_EMAIL</b> or confluence.email - User email for authentication</li>
 *   <li><b>CONFLUENCE_API_TOKEN</b> or confluence.api.token - Confluence API token</li>
 * </ul>
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class ConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final ConfigurationManager INSTANCE = new ConfigurationManager();

    private final Properties properties;
    private boolean configLoaded = false;

    // Configuration file locations (in priority order)
    private static final String[] CONFIG_LOCATIONS = {
            "application.properties",
            System.getProperty("user.home") + "/.sdlc-tools/application.properties"
    };

    private ConfigurationManager() {
        this.properties = new Properties();
        loadConfiguration();
    }

    /**
     * Gets the singleton instance.
     *
     * @return configuration manager instance
     */
    public static ConfigurationManager getInstance() {
        return INSTANCE;
    }

    /**
     * Loads configuration from available sources.
     */
    private void loadConfiguration() {
        logger.info("Loading configuration...");

        // Try to load from file
        for (String location : CONFIG_LOCATIONS) {
            Path configPath = Paths.get(location);
            if (Files.exists(configPath)) {
                try (InputStream input = new FileInputStream(configPath.toFile())) {
                    properties.load(input);
                    configLoaded = true;
                    logger.info("Loaded configuration from: {}", configPath.toAbsolutePath());
                    break;
                } catch (IOException e) {
                    logger.warn("Failed to load configuration from {}: {}", location, e.getMessage());
                }
            }
        }

        // Try to load from classpath as fallback
        if (!configLoaded) {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
                if (input != null) {
                    properties.load(input);
                    configLoaded = true;
                    logger.info("Loaded configuration from classpath");
                }
            } catch (IOException e) {
                logger.warn("Failed to load configuration from classpath: {}", e.getMessage());
            }
        }

        if (!configLoaded) {
            logger.info("No configuration file found. Using environment variables only.");
        }

        // Log which services are configured
        logConfigurationStatus();
    }

    /**
     * Logs the configuration status for debugging.
     */
    private void logConfigurationStatus() {
        boolean jiraConfigured = hasJiraConfiguration();
        boolean confluenceConfigured = hasConfluenceConfiguration();

        logger.info("Configuration status:");
        logger.info("  JIRA: {}", jiraConfigured ? "configured" : "not configured");
        logger.info("  Confluence: {}", confluenceConfigured ? "configured" : "not configured");

        if (!jiraConfigured && !confluenceConfigured) {
            logger.warn("No external configuration found. JIRA and Confluence credentials must be provided as tool parameters.");
        }
    }

    /**
     * Gets a configuration value from environment variable or properties file.
     * Environment variables take priority over properties file.
     *
     * @param envKey environment variable key
     * @param propKey properties file key
     * @return configuration value or null if not found
     */
    private String getConfigValue(String envKey, String propKey) {
        // Check environment variable first
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Fall back to properties file
        return properties.getProperty(propKey);
    }

    // JIRA Configuration

    /**
     * Gets the configured JIRA URL.
     *
     * @return JIRA URL or null if not configured
     */
    public String getJiraUrl() {
        return getConfigValue("JIRA_URL", "jira.url");
    }

    /**
     * Gets the configured JIRA email.
     *
     * @return JIRA email or null if not configured
     */
    public String getJiraEmail() {
        return getConfigValue("JIRA_EMAIL", "jira.email");
    }

    /**
     * Gets the configured JIRA API token.
     *
     * @return JIRA API token or null if not configured
     */
    public String getJiraApiToken() {
        return getConfigValue("JIRA_API_TOKEN", "jira.api.token");
    }

    /**
     * Checks if JIRA is fully configured.
     *
     * @return true if all JIRA settings are present
     */
    public boolean hasJiraConfiguration() {
        return getJiraUrl() != null && getJiraEmail() != null && getJiraApiToken() != null;
    }

    // Confluence Configuration

    /**
     * Gets the configured Confluence URL.
     *
     * @return Confluence URL or null if not configured
     */
    public String getConfluenceUrl() {
        return getConfigValue("CONFLUENCE_URL", "confluence.url");
    }

    /**
     * Gets the configured Confluence email.
     *
     * @return Confluence email or null if not configured
     */
    public String getConfluenceEmail() {
        return getConfigValue("CONFLUENCE_EMAIL", "confluence.email");
    }

    /**
     * Gets the configured Confluence API token.
     *
     * @return Confluence API token or null if not configured
     */
    public String getConfluenceApiToken() {
        return getConfigValue("CONFLUENCE_API_TOKEN", "confluence.api.token");
    }

    /**
     * Checks if Confluence is fully configured.
     *
     * @return true if all Confluence settings are present
     */
    public boolean hasConfluenceConfiguration() {
        return getConfluenceUrl() != null && getConfluenceEmail() != null && getConfluenceApiToken() != null;
    }

    /**
     * Gets JIRA URL with fallback to parameter.
     *
     * @param paramValue parameter value (can be null)
     * @return configured value or parameter value
     * @throws IllegalArgumentException if both are null
     */
    public String getJiraUrlOrDefault(String paramValue) {
        if (paramValue != null && !paramValue.isEmpty()) {
            return paramValue;
        }
        String configured = getJiraUrl();
        if (configured == null) {
            throw new IllegalArgumentException("JIRA URL not configured. Please set JIRA_URL environment variable, add jira.url to application.properties, or provide jiraUrl parameter.");
        }
        return configured;
    }

    /**
     * Gets JIRA email with fallback to parameter.
     *
     * @param paramValue parameter value (can be null)
     * @return configured value or parameter value
     * @throws IllegalArgumentException if both are null
     */
    public String getJiraEmailOrDefault(String paramValue) {
        if (paramValue != null && !paramValue.isEmpty()) {
            return paramValue;
        }
        String configured = getJiraEmail();
        if (configured == null) {
            throw new IllegalArgumentException("JIRA email not configured. Please set JIRA_EMAIL environment variable, add jira.email to application.properties, or provide email parameter.");
        }
        return configured;
    }

    /**
     * Gets JIRA API token with fallback to parameter.
     *
     * @param paramValue parameter value (can be null)
     * @return configured value or parameter value
     * @throws IllegalArgumentException if both are null
     */
    public String getJiraApiTokenOrDefault(String paramValue) {
        if (paramValue != null && !paramValue.isEmpty()) {
            return paramValue;
        }
        String configured = getJiraApiToken();
        if (configured == null) {
            throw new IllegalArgumentException("JIRA API token not configured. Please set JIRA_API_TOKEN environment variable, add jira.api.token to application.properties, or provide apiToken parameter.");
        }
        return configured;
    }

    /**
     * Gets Confluence URL with fallback to parameter.
     *
     * @param paramValue parameter value (can be null)
     * @return configured value or parameter value
     * @throws IllegalArgumentException if both are null
     */
    public String getConfluenceUrlOrDefault(String paramValue) {
        if (paramValue != null && !paramValue.isEmpty()) {
            return paramValue;
        }
        String configured = getConfluenceUrl();
        if (configured == null) {
            throw new IllegalArgumentException("Confluence URL not configured. Please set CONFLUENCE_URL environment variable, add confluence.url to application.properties, or provide confluenceUrl parameter.");
        }
        return configured;
    }

    /**
     * Gets Confluence email with fallback to parameter.
     *
     * @param paramValue parameter value (can be null)
     * @return configured value or parameter value
     * @throws IllegalArgumentException if both are null
     */
    public String getConfluenceEmailOrDefault(String paramValue) {
        if (paramValue != null && !paramValue.isEmpty()) {
            return paramValue;
        }
        String configured = getConfluenceEmail();
        if (configured == null) {
            throw new IllegalArgumentException("Confluence email not configured. Please set CONFLUENCE_EMAIL environment variable, add confluence.email to application.properties, or provide email parameter.");
        }
        return configured;
    }

    /**
     * Gets Confluence API token with fallback to parameter.
     *
     * @param paramValue parameter value (can be null)
     * @return configured value or parameter value
     * @throws IllegalArgumentException if both are null
     */
    public String getConfluenceApiTokenOrDefault(String paramValue) {
        if (paramValue != null && !paramValue.isEmpty()) {
            return paramValue;
        }
        String configured = getConfluenceApiToken();
        if (configured == null) {
            throw new IllegalArgumentException("Confluence API token not configured. Please set CONFLUENCE_API_TOKEN environment variable, add confluence.api.token to application.properties, or provide apiToken parameter.");
        }
        return configured;
    }
}
