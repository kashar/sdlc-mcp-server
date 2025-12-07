package com.example.mcp.tools.jira;

import com.example.mcp.clients.JiraClient;
import com.example.mcp.config.ConfigurationManager;
import com.example.mcp.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Tool for searching JIRA issues using JQL (JIRA Query Language).
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class SearchJiraIssuesTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(SearchJiraIssuesTool.class);

    @Override
    public String getName() {
        return "jira-search-issues";
    }

    @Override
    public String getDescription() {
        return "Search for JIRA issues using JQL (JIRA Query Language)";
    }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", getName(),
                "description", getDescription(),
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "jiraUrl", Map.of(
                                        "type", "string",
                                        "description", "JIRA instance URL (e.g., https://your-domain.atlassian.net). Optional if configured via environment or application.properties"
                                ),
                                "email", Map.of(
                                        "type", "string",
                                        "description", "User email for authentication. Optional if configured via environment or application.properties"
                                ),
                                "apiToken", Map.of(
                                        "type", "string",
                                        "description", "JIRA API token. Optional if configured via environment or application.properties"
                                ),
                                "jql", Map.of(
                                        "type", "string",
                                        "description", "JQL query (e.g., 'project = PROJ AND status = Open')"
                                ),
                                "maxResults", Map.of(
                                        "type", "number",
                                        "description", "Maximum number of results to return (default: 50)"
                                )
                        ),
                        "required", List.of("jql")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        ConfigurationManager config = ConfigurationManager.getInstance();

        // Get configuration values with fallback to parameters
        String jiraUrl = config.getJiraUrlOrDefault((String) arguments.get("jiraUrl"));
        String email = config.getJiraEmailOrDefault((String) arguments.get("email"));
        String apiToken = config.getJiraApiTokenOrDefault((String) arguments.get("apiToken"));

        String jql = (String) arguments.get("jql");
        int maxResults = arguments.containsKey("maxResults") ?
                ((Number) arguments.get("maxResults")).intValue() : 50;

        logger.info("Searching JIRA issues with JQL: {}", jql);

        JiraClient client = new JiraClient(jiraUrl, email, apiToken);
        String response = client.searchIssues(jql, maxResults);

        return Map.of(
                "success", true,
                "jql", jql,
                "maxResults", maxResults,
                "response", response
        );
    }
}
