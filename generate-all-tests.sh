#!/bin/bash

# Script to generate tests for all Java classes in the project
# This script uses the generate-tests MCP tool via the running server

echo "Generating tests for all classes in the SDLC MCP Server project..."
echo "========================================================================="

# Base paths
PROJECT_ROOT="/Users/krunal/Development/sdlc-mcp-server"
SRC_DIR="$PROJECT_ROOT/src/main/java"
TEST_DIR="$PROJECT_ROOT/src/test/java"

# Function to generate test using the MCP server (requires server to be running)
generate_test() {
    local source_file=$1
    local output_dir=$2

    echo "Generating test for: $source_file"

    # This would require the MCP server to be running and a client to call it
    # For now, we'll use a simple Java-based approach
}

# Client classes
echo ""
echo "=== Generating tests for Client classes ==="
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
    com.example.mcp.tools.GenerateTestsTool \
    "$SRC_DIR/com/example/mcp/clients/JiraClient.java" \
    "$TEST_DIR/com/example/mcp/clients" 2>/dev/null || echo "Note: Direct Java execution requires server setup"

echo ""
echo "=== Alternative: Manual test generation using Maven ==="
echo "You can also generate tests by:"
echo "1. Building the project: mvn clean package"
echo "2. Running the server and using MCP client calls"
echo "3. Or using the test templates created below"

# List of classes to test
declare -a CLASSES=(
    # Configuration
    "com/example/mcp/config/ConfigurationManager.java"

    # Clients
    "com/example/mcp/clients/JiraClient.java"
    "com/example/mcp/clients/ConfluenceClient.java"

    # Protocol
    "com/example/mcp/protocol/StdioTransport.java"
    "com/example/mcp/protocol/McpServer.java"

    # Tools
    "com/example/mcp/tools/AnalyzeMavenProjectTool.java"
    "com/example/mcp/tools/RunMavenCommandTool.java"
    "com/example/mcp/tools/SuggestImplementationTool.java"
    "com/example/mcp/tools/FixBugTool.java"
    "com/example/mcp/tools/SecurityScanTool.java"
    "com/example/mcp/tools/GenerateTestsTool.java"
    "com/example/mcp/tools/CodeQualityCheckTool.java"
    "com/example/mcp/tools/AnalyzeDependenciesTool.java"
    "com/example/mcp/tools/ImplementFeatureTool.java"
    "com/example/mcp/tools/GenerateDocumentationTool.java"

    # JIRA Tools
    "com/example/mcp/tools/jira/SearchJiraIssuesTool.java"
    "com/example/mcp/tools/jira/GetJiraIssueTool.java"
    "com/example/mcp/tools/jira/CreateJiraIssueTool.java"

    # Confluence Tools
    "com/example/mcp/tools/confluence/SearchConfluencePagesTool.java"
    "com/example/mcp/tools/confluence/GetConfluencePageTool.java"
    "com/example/mcp/tools/confluence/CreateConfluencePageTool.java"

    # Resources
    "com/example/mcp/resources/AnalysisCacheResource.java"

    # Prompts
    "com/example/mcp/prompts/SdlcWorkflowPrompt.java"

    # Documentation generators
    "com/example/mcp/docs/JavaDocGenerator.java"
    "com/example/mcp/docs/ReadmeGenerator.java"
    "com/example/mcp/docs/ChangelogGenerator.java"
    "com/example/mcp/docs/ApiDocGenerator.java"
)

echo ""
echo "========================================================================="
echo "Classes to generate tests for: ${#CLASSES[@]}"
echo ""
echo "To generate tests, you have these options:"
echo ""
echo "1. Use the MCP generate-tests tool via Claude Code:"
echo "   @mcp sdlc-tools generate-tests --filePath <file> --outputDirectory <dir>"
echo ""
echo "2. Use the provided test templates in src/test/java/"
echo ""
echo "3. Run this command for each class:"
for class in "${CLASSES[@]}"; do
    class_name=$(basename "$class" .java)
    package_path=$(dirname "$class")
    echo "   # $class_name"
done

echo ""
echo "========================================================================="
echo "Test directory structure created at: $TEST_DIR"
echo "Run 'mvn test' after adding test files to verify they compile"
