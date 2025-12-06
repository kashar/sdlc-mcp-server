package com.example.mcp.protocol;

/**
 * Transport layer interface for MCP communication.
 *
 * <p>Implementations of this interface handle the actual communication
 * mechanism (stdio, HTTP, WebSocket, etc.).
 *
 * @author Maven SDLC Team
 * @version 1.0.0
 */
public interface Transport {

    /**
     * Reads a line of input from the transport.
     *
     * @return the line read, or null if end of stream
     * @throws Exception if an error occurs reading
     */
    String readLine() throws Exception;

    /**
     * Writes a line of output to the transport.
     *
     * @param line the line to write
     * @throws Exception if an error occurs writing
     */
    void writeLine(String line) throws Exception;

    /**
     * Closes the transport.
     *
     * @throws Exception if an error occurs closing
     */
    default void close() throws Exception {
        // Default implementation does nothing
    }
}
