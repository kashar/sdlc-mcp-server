package com.example.mcp.protocol;

import com.example.mcp.tools.Tool;
import com.example.mcp.resources.Resource;
import com.example.mcp.prompts.Prompt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core MCP (Model Context Protocol) server implementation.
 *
 * <p>This class handles JSON-RPC 2.0 communication and dispatches tool
 * invocations to registered tools.
 *
 * <h2>Protocol:</h2>
 * The server implements a subset of the Model Context Protocol:
 * <ul>
 *   <li>initialize - Initialize the server</li>
 *   <li>tools/list - List available tools</li>
 *   <li>tools/call - Invoke a specific tool</li>
 *   <li>resources/list - List available resources</li>
 *   <li>resources/read - Read a specific resource</li>
 *   <li>prompts/list - List available prompts</li>
 *   <li>prompts/get - Get a specific prompt with arguments</li>
 * </ul>
 *
 * <h2>Message Format:</h2>
 * All messages follow JSON-RPC 2.0 format:
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "tools/call",
 *   "params": {
 *     "name": "analyze-maven-project",
 *     "arguments": { "path": "/path/to/project" }
 *   }
 * }
 * }</pre>
 *
 * @author Maven SDLC Team
 * @version 2.0.0
 */
public class McpServer {

    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);

    private final String serverName;
    private final String serverVersion;
    private final Map<String, Object> serverInfo;
    private final Map<String, Tool> tools;
    private final Map<String, Resource> resources;
    private final Map<String, Prompt> prompts;
    private final ObjectMapper objectMapper;

    private boolean initialized = false;

    /**
     * Creates a new MCP server instance.
     *
     * @param serverName the server name
     * @param serverVersion the server version
     * @param serverInfo additional server information
     */
    public McpServer(String serverName, String serverVersion, Map<String, Object> serverInfo) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.serverInfo = serverInfo;
        this.tools = new HashMap<>();
        this.resources = new HashMap<>();
        this.prompts = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Registers a tool with the server.
     *
     * @param tool the tool to register
     */
    public void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
        logger.info("Registered tool: {}", tool.getName());
    }

    /**
     * Registers a resource with the server.
     *
     * @param resource the resource to register
     */
    public void registerResource(Resource resource) {
        resources.put(resource.getName(), resource);
        logger.info("Registered resource: {} (URI: {})", resource.getName(), resource.getUri());
    }

    /**
     * Registers a prompt with the server.
     *
     * @param prompt the prompt to register
     */
    public void registerPrompt(Prompt prompt) {
        prompts.put(prompt.getName(), prompt);
        logger.info("Registered prompt: {}", prompt.getName());
    }

    /**
     * Gets the number of registered tools.
     *
     * @return the tool count
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * Gets the number of registered resources.
     *
     * @return the resource count
     */
    public int getResourceCount() {
        return resources.size();
    }

    /**
     * Gets the number of registered prompts.
     *
     * @return the prompt count
     */
    public int getPromptCount() {
        return prompts.size();
    }

    /**
     * Starts the server with the given transport.
     *
     * @param transport the transport layer (e.g., stdio)
     * @throws Exception if server fails to start
     */
    public void start(Transport transport) throws Exception {
        logger.info("Starting server...");

        while (true) {
            try {
                // Read request
                String requestLine = transport.readLine();
                if (requestLine == null) {
                    logger.info("Transport closed, shutting down");
                    break;
                }

                logger.debug("Received request: {}", requestLine);

                // Parse and handle request
                JsonNode request = objectMapper.readTree(requestLine);
                JsonNode response = handleRequest(request);

                // Send response
                String responseLine = objectMapper.writeValueAsString(response);
                transport.writeLine(responseLine);
                logger.debug("Sent response: {}", responseLine);

            } catch (Exception e) {
                logger.error("Error processing request", e);
                // Send error response
                ObjectNode errorResponse = createErrorResponse(null, -32603, "Internal error: " + e.getMessage());
                transport.writeLine(objectMapper.writeValueAsString(errorResponse));
            }
        }
    }

    /**
     * Handles an incoming JSON-RPC request.
     *
     * @param request the request JSON
     * @return the response JSON
     */
    private JsonNode handleRequest(JsonNode request) {
        String method = request.get("method").asText();
        JsonNode id = request.get("id");
        JsonNode params = request.get("params");

        logger.info("Handling method: {}", method);

        try {
            return switch (method) {
                case "initialize" -> handleInitialize(id, params);
                case "tools/list" -> handleToolsList(id);
                case "tools/call" -> handleToolsCall(id, params);
                case "resources/list" -> handleResourcesList(id);
                case "resources/read" -> handleResourcesRead(id, params);
                case "prompts/list" -> handlePromptsList(id);
                case "prompts/get" -> handlePromptsGet(id, params);
                default -> createErrorResponse(id, -32601, "Method not found: " + method);
            };
        } catch (Exception e) {
            logger.error("Error handling method: " + method, e);
            return createErrorResponse(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Handles the initialize method.
     */
    private JsonNode handleInitialize(JsonNode id, JsonNode params) {
        logger.info("Initializing server");
        initialized = true;

        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "0.1.0");
        result.put("serverName", serverName);
        result.put("serverVersion", serverVersion);
        result.set("serverInfo", objectMapper.valueToTree(serverInfo));

        return createSuccessResponse(id, result);
    }

    /**
     * Handles the tools/list method.
     */
    private JsonNode handleToolsList(JsonNode id) {
        if (!initialized) {
            return createErrorResponse(id, -32002, "Server not initialized");
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("tools", objectMapper.valueToTree(
                tools.values().stream()
                        .map(Tool::getSchema)
                        .toList()
        ));

        return createSuccessResponse(id, result);
    }

    /**
     * Handles the tools/call method.
     */
    private JsonNode handleToolsCall(JsonNode id, JsonNode params) {
        if (!initialized) {
            return createErrorResponse(id, -32002, "Server not initialized");
        }

        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        Tool tool = tools.get(toolName);
        if (tool == null) {
            return createErrorResponse(id, -32602, "Tool not found: " + toolName);
        }

        try {
            logger.info("Executing tool: {} with arguments: {}", toolName, arguments);

            // Convert arguments to Map
            @SuppressWarnings("unchecked")
            Map<String, Object> argsMap = objectMapper.convertValue(arguments, Map.class);

            // Execute tool
            Object toolResult = tool.execute(argsMap);

            // Create response
            ObjectNode result = objectMapper.createObjectNode();
            result.set("content", objectMapper.valueToTree(toolResult));

            return createSuccessResponse(id, result);

        } catch (Exception e) {
            logger.error("Tool execution failed: " + toolName, e);
            return createErrorResponse(id, -32000, "Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Handles the resources/list method.
     */
    private JsonNode handleResourcesList(JsonNode id) {
        if (!initialized) {
            return createErrorResponse(id, -32002, "Server not initialized");
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("resources", objectMapper.valueToTree(
                resources.values().stream()
                        .map(resource -> Map.of(
                                "uri", resource.getUri(),
                                "name", resource.getName(),
                                "description", resource.getDescription(),
                                "mimeType", resource.getMimeType()
                        ))
                        .toList()
        ));

        return createSuccessResponse(id, result);
    }

    /**
     * Handles the resources/read method.
     */
    private JsonNode handleResourcesRead(JsonNode id, JsonNode params) {
        if (!initialized) {
            return createErrorResponse(id, -32002, "Server not initialized");
        }

        String uri = params.get("uri").asText();

        // Find matching resource and extract URI parameters
        Resource matchingResource = null;
        Map<String, String> uriParams = new HashMap<>();

        for (Resource resource : resources.values()) {
            Map<String, String> extractedParams = extractUriParams(resource.getUri(), uri);
            if (extractedParams != null) {
                matchingResource = resource;
                uriParams = extractedParams;
                break;
            }
        }

        if (matchingResource == null) {
            return createErrorResponse(id, -32602, "Resource not found for URI: " + uri);
        }

        try {
            logger.info("Reading resource: {} with URI: {}", matchingResource.getName(), uri);

            Object resourceData = matchingResource.read(uriParams);

            ObjectNode result = objectMapper.createObjectNode();
            result.set("contents", objectMapper.valueToTree(List.of(Map.of(
                    "uri", uri,
                    "mimeType", matchingResource.getMimeType(),
                    "text", objectMapper.writeValueAsString(resourceData)
            ))));

            return createSuccessResponse(id, result);

        } catch (Exception e) {
            logger.error("Resource read failed: " + uri, e);
            return createErrorResponse(id, -32000, "Resource read failed: " + e.getMessage());
        }
    }

    /**
     * Handles the prompts/list method.
     */
    private JsonNode handlePromptsList(JsonNode id) {
        if (!initialized) {
            return createErrorResponse(id, -32002, "Server not initialized");
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("prompts", objectMapper.valueToTree(
                prompts.values().stream()
                        .map(prompt -> Map.of(
                                "name", prompt.getName(),
                                "description", prompt.getDescription(),
                                "arguments", prompt.getArguments()
                        ))
                        .toList()
        ));

        return createSuccessResponse(id, result);
    }

    /**
     * Handles the prompts/get method.
     */
    private JsonNode handlePromptsGet(JsonNode id, JsonNode params) {
        if (!initialized) {
            return createErrorResponse(id, -32002, "Server not initialized");
        }

        String promptName = params.get("name").asText();
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.createObjectNode();

        Prompt prompt = prompts.get(promptName);
        if (prompt == null) {
            return createErrorResponse(id, -32602, "Prompt not found: " + promptName);
        }

        try {
            logger.info("Getting prompt: {} with arguments: {}", promptName, arguments);

            @SuppressWarnings("unchecked")
            Map<String, Object> argsMap = objectMapper.convertValue(arguments, Map.class);

            String promptContent = prompt.getPrompt(argsMap);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("description", prompt.getDescription());
            result.set("messages", objectMapper.valueToTree(List.of(Map.of(
                    "role", "user",
                    "content", Map.of(
                            "type", "text",
                            "text", promptContent
                    )
            ))));

            return createSuccessResponse(id, result);

        } catch (Exception e) {
            logger.error("Prompt get failed: " + promptName, e);
            return createErrorResponse(id, -32000, "Prompt get failed: " + e.getMessage());
        }
    }

    /**
     * Extracts parameters from a URI based on a URI pattern.
     *
     * @param pattern the URI pattern (e.g., "cache://analysis/{projectPath}")
     * @param uri the actual URI (e.g., "cache://analysis/path/to/project")
     * @return map of extracted parameters, or null if URI doesn't match pattern
     */
    private Map<String, String> extractUriParams(String pattern, String uri) {
        // Convert URI pattern to regex
        String regex = pattern.replaceAll("\\{([^}]+)\\}", "(?<$1>.+)");
        regex = regex.replace("://", "://");

        Pattern compiledPattern = Pattern.compile(regex);
        Matcher matcher = compiledPattern.matcher(uri);

        if (!matcher.matches()) {
            return null;
        }

        Map<String, String> params = new HashMap<>();

        // Extract named groups
        Pattern paramPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher paramMatcher = paramPattern.matcher(pattern);

        while (paramMatcher.find()) {
            String paramName = paramMatcher.group(1);
            try {
                String paramValue = matcher.group(paramName);
                params.put(paramName, paramValue);
            } catch (IllegalArgumentException e) {
                // Parameter not found in match
            }
        }

        return params;
    }

    /**
     * Creates a success response.
     */
    private ObjectNode createSuccessResponse(JsonNode id, ObjectNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    /**
     * Creates an error response.
     */
    private ObjectNode createErrorResponse(JsonNode id, int code, String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("error", error);
        return response;
    }
}
