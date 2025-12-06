package com.example.mcp;

import com.example.mcp.protocol.McpServer;
import com.example.mcp.protocol.StdioTransport;
import com.example.mcp.tools.*;
import com.example.mcp.tools.jira.*;
import com.example.mcp.tools.confluence.*;
import com.example.mcp.resources.*;
import com.example.mcp.prompts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Main entry point for the SDLC Tools MCP Server.
 *
 * <p>This server implements the Model Context Protocol (MCP) and provides
 * tools for software development lifecycle management including Maven project
 * analysis, JIRA issue tracking, and Confluence documentation.
 *
 * <h2>Provided Tools:</h2>
 * <ul>
 *   <li><b>Maven Tools:</b></li>
 *   <ul>
 *     <li>analyze-maven-project - Analyzes Maven project structure and dependencies</li>
 *     <li>analyze-dependencies - Deep analysis of module dependencies and conflicts</li>
 *     <li>run-maven-command - Executes Maven commands safely</li>
 *     <li>code-quality-check - Comprehensive static analysis</li>
 *     <li>security-scan - Security vulnerability scanning</li>
 *     <li>generate-documentation - Generates technical documentation</li>
 *     <li>suggest-implementation - Provides implementation suggestions</li>
 *     <li>implement-feature - Autonomous feature implementation</li>
 *     <li>fix-bug - Automated bug detection and fixing</li>
 *     <li>generate-tests - Comprehensive JUnit test generation</li>
 *   </ul>
 *   <li><b>JIRA Tools:</b></li>
 *   <ul>
 *     <li>jira-search-issues - Search for JIRA issues using JQL</li>
 *     <li>jira-get-issue - Get detailed information about a JIRA issue</li>
 *     <li>jira-create-issue - Create a new JIRA issue</li>
 *   </ul>
 *   <li><b>Confluence Tools:</b></li>
 *   <ul>
 *     <li>confluence-search-pages - Search for Confluence pages using CQL</li>
 *     <li>confluence-get-page - Get detailed information about a Confluence page</li>
 *     <li>confluence-create-page - Create a new Confluence page</li>
 *   </ul>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * java -jar sdlc-tools-mcp-server.jar
 * </pre>
 *
 * <p>The server communicates via standard input/output using JSON-RPC 2.0 protocol.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 * @since 1.0.0
 */
public class SdlcToolsMcpServer {

    private static final Logger logger = LoggerFactory.getLogger(SdlcToolsMcpServer.class);

    /**
     * Server metadata
     */
    private static final String SERVER_NAME = "sdlc-tools-mcp-server";
    private static final String SERVER_VERSION = "2.0.0";

    public static void main(String[] args) {
        logger.info("Starting {} v{}", SERVER_NAME, SERVER_VERSION);

        try {
            // Create server instance
            McpServer server = new McpServer(
                    SERVER_NAME,
                    SERVER_VERSION,
                    createServerInfo()
            );

            // Register tools, resources, and prompts
            registerTools(server);
            registerResources(server);
            registerPrompts(server);

            // Create stdio transport
            StdioTransport transport = new StdioTransport();

            // Start server
            logger.info("Server ready, waiting for requests...");
            server.start(transport);

        } catch (Exception e) {
            logger.error("Fatal error starting server", e);
            System.exit(1);
        }
    }

    /**
     * Registers all available tools with the server.
     *
     * @param server the MCP server instance
     */
    private static void registerTools(McpServer server) {
        logger.info("Registering tools...");

        // Maven project analysis tools
        server.registerTool(new AnalyzeMavenProjectTool());
        server.registerTool(new AnalyzeDependenciesTool());

        // Maven execution tools
        server.registerTool(new RunMavenCommandTool());

        // Code quality and security tools
        server.registerTool(new CodeQualityCheckTool());
        server.registerTool(new SecurityScanTool());

        // Documentation tools
        server.registerTool(new GenerateDocumentationTool());

        // Implementation assistance tools
        server.registerTool(new SuggestImplementationTool());
        server.registerTool(new ImplementFeatureTool());

        // Bug fixing and testing tools
        server.registerTool(new FixBugTool());
        server.registerTool(new GenerateTestsTool());

        // JIRA integration tools
        server.registerTool(new SearchJiraIssuesTool());
        server.registerTool(new GetJiraIssueTool());
        server.registerTool(new CreateJiraIssueTool());

        // Confluence integration tools
        server.registerTool(new SearchConfluencePagesTool());
        server.registerTool(new GetConfluencePageTool());
        server.registerTool(new CreateConfluencePageTool());

        logger.info("Registered {} tools", server.getToolCount());
    }

    /**
     * Registers all available resources with the server.
     *
     * @param server the MCP server instance
     */
    private static void registerResources(McpServer server) {
        logger.info("Registering resources...");

        // Cache resources
        server.registerResource(new AnalysisCacheResource());

        logger.info("Registered {} resources", server.getResourceCount());
    }

    /**
     * Registers all available prompts with the server.
     *
     * @param server the MCP server instance
     */
    private static void registerPrompts(McpServer server) {
        logger.info("Registering prompts...");

        // SDLC workflow prompts
        server.registerPrompt(new SdlcWorkflowPrompt());

        logger.info("Registered {} prompts", server.getPromptCount());
    }

    /**
     * Creates server information metadata.
     *
     * @return map of server information
     */
    private static Map<String, Object> createServerInfo() {
        return Map.of(
                "description", "SDLC Tools MCP Server for Maven analysis, JIRA issue tracking, and Confluence documentation",
                "capabilities", Map.of(
                        "tools", true,
                        "resources", true,
                        "prompts", true
                ),
                "integrations", Map.of(
                        "maven", "Maven project analysis and build automation",
                        "jira", "JIRA issue tracking and management",
                        "confluence", "Confluence documentation management"
                ),
                "sdlcPersonas", Map.of(
                        "analyst", ".github/mcp/personas/01-analyst.md",
                        "architect", ".github/mcp/personas/02-architect.md",
                        "developer", ".github/mcp/personas/03-developer.md",
                        "tester", ".github/mcp/personas/04-tester.md",
                        "reviewer", ".github/mcp/personas/05-reviewer.md",
                        "documentor", ".github/mcp/personas/06-documentor.md"
                )
        );
    }
}
