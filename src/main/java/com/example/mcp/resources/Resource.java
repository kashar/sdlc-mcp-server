package com.example.mcp.resources;

import java.util.Map;

/**
 * Interface for MCP resources.
 *
 * <p>Resources provide a way to expose data and content that can be read by MCP clients.
 * Unlike tools which perform actions, resources are read-only data sources that can be
 * cached and referenced by URI.
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Caching analysis results for reuse</li>
 *   <li>Providing access to project files and configurations</li>
 *   <li>Exposing documentation and metadata</li>
 *   <li>Sharing large datasets without re-computation</li>
 * </ul>
 *
 * @author Maven SDLC Team
 * @version 2.0.0
 */
public interface Resource {

    /**
     * Gets the resource URI.
     *
     * <p>URIs should follow a consistent pattern, e.g.:
     * <ul>
     *   <li>cache://analysis/{projectPath}</li>
     *   <li>file://project/{relativePath}</li>
     *   <li>config://maven/{setting}</li>
     * </ul>
     *
     * @return the resource URI
     */
    String getUri();

    /**
     * Gets the resource name.
     *
     * @return the resource name
     */
    String getName();

    /**
     * Gets the resource description.
     *
     * @return the resource description
     */
    String getDescription();

    /**
     * Gets the MIME type of the resource content.
     *
     * @return the MIME type (e.g., "application/json", "text/plain")
     */
    String getMimeType();

    /**
     * Reads the resource content.
     *
     * @param uriParams parameters extracted from the URI
     * @return the resource content
     * @throws Exception if reading fails
     */
    Object read(Map<String, String> uriParams) throws Exception;
}
