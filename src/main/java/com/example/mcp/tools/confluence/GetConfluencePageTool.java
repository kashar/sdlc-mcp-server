package com.example.mcp.tools.confluence;

import com.example.mcp.clients.ConfluenceClient;
import com.example.mcp.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Tool for getting detailed information about a specific Confluence page.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class GetConfluencePageTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(GetConfluencePageTool.class);

    @Override
    public String getName() {
        return "confluence-get-page";
    }

    @Override
    public String getDescription() {
        return "Get detailed information about a specific Confluence page by its ID";
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
                                "pageId", Map.of(
                                        "type", "string",
                                        "description", "Page ID"
                                ),
                                "expand", Map.of(
                                        "type", "string",
                                        "description", "Fields to expand (e.g., 'body.storage,version') (optional)"
                                )
                        ),
                        "required", List.of("confluenceUrl", "email", "apiToken", "pageId")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String confluenceUrl = (String) arguments.get("confluenceUrl");
        String email = (String) arguments.get("email");
        String apiToken = (String) arguments.get("apiToken");
        String pageId = (String) arguments.get("pageId");
        String expand = (String) arguments.getOrDefault("expand", "body.storage,version");

        logger.info("Getting Confluence page: {}", pageId);

        ConfluenceClient client = new ConfluenceClient(confluenceUrl, email, apiToken);
        String response = client.getPage(pageId, expand);

        return Map.of(
                "success", true,
                "pageId", pageId,
                "response", response
        );
    }
}
