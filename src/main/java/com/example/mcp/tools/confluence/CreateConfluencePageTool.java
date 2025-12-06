package com.example.mcp.tools.confluence;

import com.example.mcp.clients.ConfluenceClient;
import com.example.mcp.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Tool for creating a new Confluence page.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class CreateConfluencePageTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(CreateConfluencePageTool.class);

    @Override
    public String getName() {
        return "confluence-create-page";
    }

    @Override
    public String getDescription() {
        return "Create a new Confluence page in a specified space";
    }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", getName(),
                "description", getDescription(),
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "confluenceUrl", Map.of(
                                        "type", "string",
                                        "description", "Confluence instance URL (e.g., https://your-domain.atlassian.net/wiki)"
                                ),
                                "email", Map.of(
                                        "type", "string",
                                        "description", "User email for authentication"
                                ),
                                "apiToken", Map.of(
                                        "type", "string",
                                        "description", "Confluence API token"
                                ),
                                "spaceKey", Map.of(
                                        "type", "string",
                                        "description", "Space key (e.g., 'DEV')"
                                ),
                                "title", Map.of(
                                        "type", "string",
                                        "description", "Page title"
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "Page content in Confluence storage format (HTML)"
                                ),
                                "parentId", Map.of(
                                        "type", "string",
                                        "description", "Parent page ID (optional)"
                                )
                        ),
                        "required", List.of("confluenceUrl", "email", "apiToken", "spaceKey", "title", "content")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String confluenceUrl = (String) arguments.get("confluenceUrl");
        String email = (String) arguments.get("email");
        String apiToken = (String) arguments.get("apiToken");
        String spaceKey = (String) arguments.get("spaceKey");
        String title = (String) arguments.get("title");
        String content = (String) arguments.get("content");
        String parentId = (String) arguments.getOrDefault("parentId", null);

        logger.info("Creating Confluence page in space: {}", spaceKey);

        ConfluenceClient client = new ConfluenceClient(confluenceUrl, email, apiToken);
        String response = client.createPage(spaceKey, title, content, parentId);

        return Map.of(
                "success", true,
                "spaceKey", spaceKey,
                "title", title,
                "response", response
        );
    }
}
