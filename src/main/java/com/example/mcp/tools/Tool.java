package com.example.mcp.tools;

import java.util.Map;

/**
 * Interface for MCP tools.
 *
 * <p>Each tool provides a specific capability to the MCP server, such as
 * analyzing Maven projects, running commands, or generating documentation.
 *
 * @author Maven SDLC Team
 * @version 1.0.0
 */
public interface Tool {

    /**
     * Gets the tool name.
     *
     * <p>Tool names should be kebab-case and descriptive, e.g., "analyze-maven-project".
     *
     * @return the tool name
     */
    String getName();

    /**
     * Gets the tool description.
     *
     * <p>This description is shown to AI assistants to help them understand
     * when to use this tool.
     *
     * @return the tool description
     */
    String getDescription();

    /**
     * Gets the tool schema.
     *
     * <p>The schema defines the tool's parameters and return type in a format
     * that can be sent to the MCP client.
     *
     * @return the tool schema as a map
     */
    Map<String, Object> getSchema();

    /**
     * Executes the tool with the given arguments.
     *
     * @param arguments the tool arguments
     * @return the tool result
     * @throws Exception if the tool execution fails
     */
    Object execute(Map<String, Object> arguments) throws Exception;
}
