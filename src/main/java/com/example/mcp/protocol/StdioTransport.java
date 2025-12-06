package com.example.mcp.protocol;

import java.io.*;

/**
 * Standard input/output transport implementation.
 *
 * <p>This transport reads from System.in and writes to System.out,
 * which is the standard way MCP servers communicate with clients.
 *
 * @author Maven SDLC Team
 * @version 1.0.0
 */
public class StdioTransport implements Transport {

    private final BufferedReader reader;
    private final BufferedWriter writer;

    /**
     * Creates a new stdio transport.
     */
    public StdioTransport() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new BufferedWriter(new OutputStreamWriter(System.out));
    }

    @Override
    public String readLine() throws Exception {
        return reader.readLine();
    }

    @Override
    public void writeLine(String line) throws Exception {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() throws Exception {
        reader.close();
        writer.close();
    }
}
