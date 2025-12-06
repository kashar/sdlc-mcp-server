package com.example.mcp.tools.jira;

import com.example.mcp.clients.JiraClient;
import com.example.mcp.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Tool for creating a new JIRA issue.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class CreateJiraIssueTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(CreateJiraIssueTool.class);

    @Override
    public String getName() {
        return "jira-create-issue";
    }

    @Override
    public String getDescription() {
        return "Create a new JIRA issue in a specified project";
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
                                "projectKey", Map.of(
                                        "type", "string",
                                        "description", "Project key (e.g., 'PROJ')"
                                ),
                                "issueType", Map.of(
                                        "type", "string",
                                        "description", "Issue type (e.g., 'Bug', 'Task', 'Story')"
                                ),
                                "summary", Map.of(
                                        "type", "string",
                                        "description", "Issue summary/title"
                                ),
                                "description", Map.of(
                                        "type", "string",
                                        "description", "Detailed description of the issue"
                                )
                        ),
                        "required", List.of("jiraUrl", "email", "apiToken", "projectKey", "issueType", "summary")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String jiraUrl = (String) arguments.get("jiraUrl");
        String email = (String) arguments.get("email");
        String apiToken = (String) arguments.get("apiToken");
        String projectKey = (String) arguments.get("projectKey");
        String issueType = (String) arguments.get("issueType");
        String summary = (String) arguments.get("summary");
        String description = (String) arguments.getOrDefault("description", "");

        logger.info("Creating JIRA issue in project: {}", projectKey);

        JiraClient client = new JiraClient(jiraUrl, email, apiToken);
        String response = client.createIssue(projectKey, issueType, summary, description);

        return Map.of(
                "success", true,
                "projectKey", projectKey,
                "issueType", issueType,
                "summary", summary,
                "response", response
        );
    }
}
