package com.example.mcp.tools.jira;

import com.example.mcp.clients.JiraClient;
import com.example.mcp.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Tool for getting detailed information about a specific JIRA issue.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class GetJiraIssueTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(GetJiraIssueTool.class);

    @Override
    public String getName() {
        return "jira-get-issue";
    }

    @Override
    public String getDescription() {
        return "Get detailed information about a specific JIRA issue by its key";
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
                                        "description", "JIRA instance URL (e.g., https://your-domain.atlassian.net)"
                                ),
                                "email", Map.of(
                                        "type", "string",
                                        "description", "User email for authentication"
                                ),
                                "apiToken", Map.of(
                                        "type", "string",
                                        "description", "JIRA API token"
                                ),
                                "issueKey", Map.of(
                                        "type", "string",
                                        "description", "JIRA issue key (e.g., 'PROJ-123')"
                                )
                        ),
                        "required", List.of("jiraUrl", "email", "apiToken", "issueKey")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String jiraUrl = (String) arguments.get("jiraUrl");
        String email = (String) arguments.get("email");
        String apiToken = (String) arguments.get("apiToken");
        String issueKey = (String) arguments.get("issueKey");

        logger.info("Getting JIRA issue: {}", issueKey);

        JiraClient client = new JiraClient(jiraUrl, email, apiToken);
        String response = client.getIssue(issueKey);

        return Map.of(
                "success", true,
                "issueKey", issueKey,
                "response", response
        );
    }
}
