# Migration Plan: Official MCP Java SDK

**Objective:** Migrate from custom MCP protocol implementation to official MCP Java SDK
**Estimated Time:** 4-8 hours
**Risk Level:** LOW (tools unchanged, incremental migration possible)
**Status:** Ready to begin

---

## Phase 0: Preparation & Backup

### 0.1 Create Backup Branch
```bash
cd /Users/krunal/Development/maven-sdlc-mcp-server
git checkout -b backup-custom-implementation
git add .
git commit -m "Backup: Custom MCP implementation before SDK migration"
git checkout -b feature/migrate-to-official-sdk
```

### 0.2 Current State Snapshot
```bash
# Verify current build works
mvn clean package -DskipTests

# Test current server
java -jar target/maven-sdlc-mcp-server-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

**Files to be replaced:**
- `src/main/java/com/example/mcp/protocol/McpServer.java` (252 lines)
- `src/main/java/com/example/mcp/protocol/Transport.java` (20 lines)
- `src/main/java/com/example/mcp/protocol/StdioTransport.java` (60 lines)

**Files to be kept:**
- All 10 tools in `src/main/java/com/example/mcp/tools/` ✅
- `src/main/java/com/example/mcp/MavenSdlcMcpServer.java` (minor updates only)

---

## Phase 1: Add Official SDK Dependency

**Duration:** 15 minutes
**Risk:** None (additive change)

### 1.1 Update pom.xml

Add the official MCP SDK dependency:

```xml
<!-- Add after existing dependencies, before test dependencies -->

<!-- Official MCP Java SDK -->
<dependency>
    <groupId>io.github.modelcontextprotocol</groupId>
    <artifactId>mcp</artifactId>
    <version>0.17.0</version>
</dependency>

<!-- SDK uses Project Reactor -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
    <version>3.6.0</version>
</dependency>
```

### 1.2 Verify Dependencies Download

```bash
mvn dependency:resolve
mvn dependency:tree | grep mcp
```

**Expected Output:**
```
[INFO] +- io.github.modelcontextprotocol:mcp:jar:0.17.0:compile
```

### 1.3 Build to Verify

```bash
mvn clean compile
```

**Checkpoint:** Build should succeed with no errors.

---

## Phase 2: Create Tool Adapter

**Duration:** 1-2 hours
**Risk:** LOW (new code, doesn't affect existing)

### 2.1 Create ToolAdapter Class

Create: `src/main/java/com/example/mcp/adapter/ToolAdapter.java`

```java
package com.example.mcp.adapter;

import com.example.mcp.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.modelcontextprotocol.server.tool.ServerTool;
import io.github.modelcontextprotocol.server.tool.ToolDefinition;
import io.github.modelcontextprotocol.server.tool.ToolRequest;
import io.github.modelcontextprotocol.server.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Adapter that bridges the legacy Tool interface to the official MCP SDK's ServerTool interface.
 *
 * <p>This allows existing tool implementations to work with the official SDK without modification.
 *
 * @author Maven SDLC Team
 * @version 2.0.0
 */
public class ToolAdapter implements ServerTool {

    private static final Logger logger = LoggerFactory.getLogger(ToolAdapter.class);
    private final Tool legacyTool;
    private final ObjectMapper objectMapper;
    private final ToolDefinition definition;

    /**
     * Creates a new adapter for a legacy tool.
     *
     * @param legacyTool the legacy tool to adapt
     */
    public ToolAdapter(Tool legacyTool) {
        this.legacyTool = legacyTool;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.definition = createDefinition();
    }

    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }

    @Override
    public Mono<ToolResult> execute(ToolRequest request) {
        return Mono.fromCallable(() -> {
            try {
                logger.info("Executing legacy tool: {}", legacyTool.getName());

                // Convert SDK arguments to legacy format
                Map<String, Object> arguments = convertArguments(request.getArguments());

                // Execute legacy tool
                Object result = legacyTool.execute(arguments);

                // Convert result to SDK format
                return createSuccessResult(result);

            } catch (Exception e) {
                logger.error("Tool execution failed: {}", legacyTool.getName(), e);
                return ToolResult.error("Tool execution failed: " + e.getMessage());
            }
        });
    }

    /**
     * Creates a ToolDefinition from the legacy tool's schema.
     */
    private ToolDefinition createDefinition() {
        Map<String, Object> schema = legacyTool.getSchema();

        ToolDefinition.Builder builder = ToolDefinition.builder()
                .name(legacyTool.getName())
                .description(legacyTool.getDescription());

        // Convert legacy schema to SDK format
        if (schema.containsKey("inputSchema")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputSchema = (Map<String, Object>) schema.get("inputSchema");
            JsonNode schemaNode = objectMapper.valueToTree(inputSchema);
            builder.inputSchema(schemaNode);
        }

        return builder.build();
    }

    /**
     * Converts SDK arguments to legacy Map format.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertArguments(JsonNode arguments) {
        return objectMapper.convertValue(arguments, Map.class);
    }

    /**
     * Creates a success ToolResult from legacy tool output.
     */
    private ToolResult createSuccessResult(Object result) {
        // Convert result to JSON
        JsonNode resultNode = objectMapper.valueToTree(result);

        // SDK expects content array with text entries
        return ToolResult.success(resultNode);
    }
}
```

### 2.2 Create Directory

```bash
mkdir -p src/main/java/com/example/mcp/adapter
```

### 2.3 Test Compilation

```bash
mvn clean compile
```

**Checkpoint:** ToolAdapter should compile without errors.

---

## Phase 3: Create SDK-Based Server

**Duration:** 1-2 hours
**Risk:** LOW (parallel implementation)

### 3.1 Create SdkMcpServer Class

Create: `src/main/java/com/example/mcp/SdkMcpServer.java`

```java
package com.example.mcp;

import com.example.mcp.adapter.ToolAdapter;
import com.example.mcp.tools.*;
import io.github.modelcontextprotocol.server.McpServer;
import io.github.modelcontextprotocol.server.ServerInfo;
import io.github.modelcontextprotocol.server.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven SDLC MCP Server using the official MCP Java SDK.
 *
 * <p>This server uses the official Model Context Protocol SDK from
 * https://github.com/modelcontextprotocol/java-sdk for improved
 * protocol compliance, features, and maintainability.
 *
 * <h2>Improvements over Custom Implementation:</h2>
 * <ul>
 *   <li>Full MCP protocol support (tools, resources, prompts, sampling)</li>
 *   <li>Reactive programming model for better performance</li>
 *   <li>Multiple transport support (STDIO, HTTP, SSE)</li>
 *   <li>Community-maintained and actively developed</li>
 *   <li>Production-tested with comprehensive error handling</li>
 * </ul>
 *
 * @author Maven SDLC Team
 * @version 2.0.0
 */
public class SdkMcpServer {

    private static final Logger logger = LoggerFactory.getLogger(SdkMcpServer.class);

    private static final String SERVER_NAME = "maven-sdlc-mcp-server";
    private static final String SERVER_VERSION = "2.0.0";

    public static void main(String[] args) {
        logger.info("Starting {} v{} (Official SDK)", SERVER_NAME, SERVER_VERSION);

        try {
            // Create server with official SDK
            McpServer server = createServer();

            // Start with STDIO transport
            logger.info("Server ready, waiting for requests via STDIO...");
            server.start(StdioTransport.create());

        } catch (Exception e) {
            logger.error("Fatal error starting server", e);
            System.exit(1);
        }
    }

    /**
     * Creates and configures the MCP server with all tools.
     */
    private static McpServer createServer() {
        logger.info("Configuring MCP server...");

        // Build server with official SDK
        McpServer.Builder builder = McpServer.builder()
                .serverInfo(ServerInfo.builder()
                        .name(SERVER_NAME)
                        .version(SERVER_VERSION)
                        .description("Maven SDLC MCP Server for analyzing, documenting, " +
                                "and implementing features/bug fixes in Maven repositories")
                        .build());

        // Register all tools using adapter
        registerTools(builder);

        logger.info("Server configuration complete");
        return builder.build();
    }

    /**
     * Registers all available tools with the server.
     */
    private static void registerTools(McpServer.Builder builder) {
        logger.info("Registering tools...");

        // Maven project analysis tools
        builder.addTool(new ToolAdapter(new AnalyzeMavenProjectTool()));
        builder.addTool(new ToolAdapter(new AnalyzeDependenciesTool()));

        // Maven execution tools
        builder.addTool(new ToolAdapter(new RunMavenCommandTool()));

        // Code quality and security tools
        builder.addTool(new ToolAdapter(new CodeQualityCheckTool()));
        builder.addTool(new ToolAdapter(new SecurityScanTool()));

        // Documentation tools
        builder.addTool(new ToolAdapter(new GenerateDocumentationTool()));

        // Implementation assistance tools
        builder.addTool(new ToolAdapter(new SuggestImplementationTool()));
        builder.addTool(new ToolAdapter(new ImplementFeatureTool()));

        // Bug fixing and testing tools
        builder.addTool(new ToolAdapter(new FixBugTool()));
        builder.addTool(new ToolAdapter(new GenerateTestsTool()));

        logger.info("Registered 10 tools successfully");
    }
}
```

### 3.2 Verify Compilation

```bash
mvn clean compile
```

**Checkpoint:** SdkMcpServer should compile successfully.

---

## Phase 4: Update Main Entry Point

**Duration:** 30 minutes
**Risk:** MEDIUM (changes entry point, but reversible)

### 4.1 Update pom.xml Main Class

Update the main class in `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
        <archive>
            <manifest>
                <!-- Change from old to new -->
                <mainClass>com.example.mcp.SdkMcpServer</mainClass>
            </manifest>
        </archive>
    </configuration>
    ...
</plugin>
```

### 4.2 Build New Executable

```bash
mvn clean package -DskipTests
```

**Expected Output:**
```
[INFO] Building jar: .../maven-sdlc-mcp-server-1.0.0-SNAPSHOT-jar-with-dependencies.jar
[INFO] BUILD SUCCESS
```

### 4.3 Verify JAR Size

```bash
ls -lh target/*-jar-with-dependencies.jar
```

**Expected:** Should be slightly larger (~65-70MB) due to SDK dependencies.

**Checkpoint:** Build completes successfully with new main class.

---

## Phase 5: Testing

**Duration:** 2-3 hours
**Risk:** LOW (testing only)

### 5.1 Manual STDIO Test

Create test script: `test-sdk-server.sh`

```bash
#!/bin/bash
# Test script for SDK-based MCP server

JAR="target/maven-sdlc-mcp-server-1.0.0-SNAPSHOT-jar-with-dependencies.jar"

echo "Testing MCP Server with Official SDK"
echo "===================================="

# Test 1: Initialize
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | \
  java -jar "$JAR"

# Test 2: List Tools
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' | \
  java -jar "$JAR"

# Test 3: Call a simple tool
echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"analyze-maven-project","arguments":{"path":"'$(pwd)'"}}}' | \
  java -jar "$JAR"
```

```bash
chmod +x test-sdk-server.sh
./test-sdk-server.sh
```

### 5.2 Expected Responses

**Initialize Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "serverInfo": {
      "name": "maven-sdlc-mcp-server",
      "version": "2.0.0"
    },
    "capabilities": {
      "tools": {}
    }
  }
}
```

**Tools List Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "analyze-maven-project",
        "description": "Analyzes Maven project structure...",
        "inputSchema": {...}
      },
      // ... 9 more tools
    ]
  }
}
```

### 5.3 Comprehensive Tool Testing

Create: `test-all-tools.sh`

```bash
#!/bin/bash
# Test all 10 tools

JAR="target/maven-sdlc-mcp-server-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
PROJECT_PATH="$(pwd)"

test_tool() {
    local tool_name=$1
    local args=$2

    echo "Testing: $tool_name"
    echo "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"$tool_name\",\"arguments\":$args}}" | \
      java -jar "$JAR" 2>/dev/null | jq '.result.content[0].success'
}

# Test each tool
test_tool "analyze-maven-project" "{\"path\":\"$PROJECT_PATH\"}"
test_tool "analyze-dependencies" "{\"path\":\"$PROJECT_PATH\"}"
test_tool "code-quality-check" "{\"path\":\"$PROJECT_PATH\"}"
test_tool "security-scan" "{\"path\":\"$PROJECT_PATH\"}"

echo "All tests complete!"
```

```bash
chmod +x test-all-tools.sh
./test-all-tools.sh
```

### 5.4 Integration Test with Claude Desktop

If you have Claude Desktop configured:

1. Update `mcp-settings.json`:
```json
{
  "mcpServers": {
    "maven-sdlc": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/krunal/Development/maven-sdlc-mcp-server/target/maven-sdlc-mcp-server-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
      ]
    }
  }
}
```

2. Restart Claude Desktop

3. Test with: "Use the analyze-maven-project tool on /path/to/project"

**Checkpoint:** All tools return success responses.

---

## Phase 6: Cleanup & Documentation

**Duration:** 30 minutes
**Risk:** None (cleanup only)

### 6.1 Remove Old Protocol Implementation

After confirming everything works:

```bash
# Remove old custom protocol files
rm src/main/java/com/example/mcp/protocol/McpServer.java
rm src/main/java/com/example/mcp/protocol/Transport.java
rm src/main/java/com/example/mcp/protocol/StdioTransport.java
rmdir src/main/java/com/example/mcp/protocol
```

### 6.2 Remove Old Main Class

```bash
# Remove old main class
rm src/main/java/com/example/mcp/MavenSdlcMcpServer.java
```

### 6.3 Update README.md

Add migration notes:

```markdown
## Version 2.0.0 - Official SDK Migration

This version migrates from a custom MCP implementation to the official
MCP Java SDK (https://github.com/modelcontextprotocol/java-sdk).

### Benefits:
- Full MCP protocol compliance
- Reactive programming support
- Multiple transport options
- Community maintenance
- Future feature support (resources, prompts)

### Breaking Changes:
None - all existing tools work identically

### Dependencies Added:
- io.github.modelcontextprotocol:mcp:0.17.0
- io.projectreactor:reactor-core:3.6.0
```

### 6.4 Update IMPLEMENTATION_SUMMARY.md

Add section about SDK migration:

```markdown
## Version 2.0.0 - Migration to Official SDK

**Date:** December 2025
**Status:** ✅ Migrated Successfully

### What Changed
- Replaced custom protocol implementation (350 lines) with official SDK
- All 10 tools work identically via ToolAdapter
- Added reactive programming support
- Enabled future features (resources, prompts)

### Files Changed
- Removed: McpServer.java, Transport.java, StdioTransport.java
- Added: SdkMcpServer.java, ToolAdapter.java
- Modified: pom.xml (new dependencies)

### Benefits Gained
- Protocol compliance: 100%
- Maintenance: Community-supported
- Features: Resources, prompts ready for future
- Transports: HTTP, SSE available
```

---

## Phase 7: Final Build & Verification

**Duration:** 15 minutes
**Risk:** None (verification only)

### 7.1 Clean Build

```bash
mvn clean package
```

### 7.2 Verify Artifacts

```bash
ls -lh target/maven-sdlc-mcp-server-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

### 7.3 Final Smoke Test

```bash
# Quick test all tools respond
./test-all-tools.sh
```

### 7.4 Commit Migration

```bash
git add .
git commit -m "feat: Migrate to official MCP Java SDK v0.17.0

- Replace custom protocol implementation with official SDK
- Add ToolAdapter for backward compatibility
- All 10 tools working identically
- Gained: reactive support, multiple transports, community maintenance

BREAKING CHANGE: None (internal refactoring only)
"

git push origin feature/migrate-to-official-sdk
```

---

## Rollback Plan

If anything goes wrong:

### Quick Rollback (5 minutes)
```bash
git checkout backup-custom-implementation
mvn clean package
```

### Partial Rollback (Keep SDK, Use Both)

Create dual-mode support:

```java
public static void main(String[] args) {
    String mode = System.getProperty("mcp.mode", "sdk");

    if ("legacy".equals(mode)) {
        // Use old custom implementation
        startLegacyServer();
    } else {
        // Use official SDK
        startSdkServer();
    }
}
```

Run with legacy: `java -Dmcp.mode=legacy -jar server.jar`

---

## Success Criteria

✅ All 10 tools execute successfully
✅ Protocol compliance verified
✅ Build produces working JAR
✅ Claude Desktop integration works
✅ No breaking changes to tools
✅ Documentation updated

---

## Next Steps After Migration

### Immediate (Post-Migration)
1. ✅ Verify in production environment
2. ✅ Monitor logs for any issues
3. ✅ Update deployment documentation

### Short-Term (Week 1-2)
1. Add MCP Resources support for caching analysis results
2. Add MCP Prompts for SDLC workflows
3. Add HTTP transport for remote access

### Medium-Term (Month 1)
1. Implement async tool execution for long-running tasks
2. Add Spring Boot integration (optional)
3. Implement proper logging with Reactor Context

---

## Estimated Timeline

| Phase | Duration | Status |
|-------|----------|--------|
| 0. Preparation | 15 min | Ready |
| 1. Add Dependencies | 15 min | Ready |
| 2. Create Adapter | 1-2 hours | Ready |
| 3. Create SDK Server | 1-2 hours | Ready |
| 4. Update Entry Point | 30 min | Ready |
| 5. Testing | 2-3 hours | Ready |
| 6. Cleanup | 30 min | Ready |
| 7. Final Build | 15 min | Ready |
| **TOTAL** | **4-8 hours** | **Ready to Start** |

---

**Ready to begin migration!** ✨
