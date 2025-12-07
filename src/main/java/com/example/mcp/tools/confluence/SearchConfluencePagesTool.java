package com.example.mcp.tools.confluence;

import com.example.mcp.clients.ConfluenceClient;
import com.example.mcp.config.ConfigurationManager;
import com.example.mcp.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Tool for searching Confluence pages using CQL (Confluence Query Language).
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class SearchConfluencePagesTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(SearchConfluencePagesTool.class);

    @Override
    public String getName() {
        return "confluence-search-pages";
    }

    @Override
    public String getDescription() {
        return "Search for Confluence pages using CQL (Confluence Query Language)";
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
                                        "description", "Confluence instance URL (e.g., https://your-domain.atlassian.net/wiki). Optional if configured via environment or application.properties"
                                ),
                                "email", Map.of(
                                        "type", "string",
                                        "description", "User email for authentication. Optional if configured via environment or application.properties"
                                ),
                                "apiToken", Map.of(
                                        "type", "string",
                                        "description", "Confluence API token. Optional if configured via environment or application.properties"
                                ),
                                "cql", Map.of(
                                        "type", "string",
                                        "description", "CQL query (e.g., 'space=DEV AND type=page')"
                                ),
                                "limit", Map.of(
                                        "type", "number",
                                        "description", "Maximum number of results to return (default: 25)"
                                )
                        ),
                        "required", List.of("cql")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        ConfigurationManager config = ConfigurationManager.getInstance();

        // Get configuration values with fallback to parameters
        String confluenceUrl = config.getConfluenceUrlOrDefault((String) arguments.get("confluenceUrl"));
        String email = config.getConfluenceEmailOrDefault((String) arguments.get("email"));
        String apiToken = config.getConfluenceApiTokenOrDefault((String) arguments.get("apiToken"));

        String cql = (String) arguments.get("cql");
        int limit = arguments.containsKey("limit") ?
                ((Number) arguments.get("limit")).intValue() : 25;

        logger.info("Searching Confluence pages with CQL: {}", cql);

        ConfluenceClient client = new ConfluenceClient(confluenceUrl, email, apiToken);
        String response = client.searchContent(cql, limit);

        return Map.of(
                "success", true,
                "cql", cql,
                "limit", limit,
                "response", response
        );
    }
}
