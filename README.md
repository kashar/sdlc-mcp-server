# SDLC Tools MCP Server

A comprehensive Model Context Protocol (MCP) server for software development lifecycle management, providing integrated tools for Maven project analysis, JIRA issue tracking, and Confluence documentation.

## Overview

This MCP server provides AI assistants like Claude Code with specialized tools to manage the complete software development lifecycle, integrating Maven build automation, JIRA project management, and Confluence knowledge management through a unified interface.

### Key Features

#### Maven Integration
- **Maven Project Analysis** - Deep analysis of multi-module project structure
- **Dependency Management** - Analyze and understand dependency trees
- **Code Quality Checks** - Static analysis and quality metrics
- **Safe Command Execution** - Run Maven commands with proper safeguards
- **Automated Documentation Generation** - Generate JavaDoc analysis, README, API docs, and changelogs
- **Implementation Guidance** - Suggest approaches based on codebase patterns

#### JIRA Integration
- **Issue Search** - Search JIRA issues using JQL (JIRA Query Language)
- **Issue Details** - Get comprehensive information about specific issues
- **Issue Creation** - Create new issues with custom fields
- **Issue Updates** - Update existing issues programmatically

#### Confluence Integration
- **Page Search** - Search Confluence pages using CQL (Confluence Query Language)
- **Page Content** - Retrieve and read page content
- **Page Creation** - Create new documentation pages
- **Knowledge Management** - Organize and manage documentation

#### Advanced Features
- **MCP Resources** - Cache and reuse expensive analysis results
- **MCP Prompts** - Pre-defined SDLC workflow prompts for guided development
- **Multi-Tool Integration** - Seamlessly combine Maven, JIRA, and Confluence operations

### SDLC Personas Integration

Works seamlessly with SDLC personas defined in `.github/mcp/personas/`:
- **Analyst** - Requirements analysis and codebase understanding
- **Architect** - Solution design and technical decisions
- **Developer** - Clean code implementation
- **Tester** - Comprehensive testing strategy
- **Reviewer** - Code quality review
- **Documentor** - Technical documentation

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Claude Code CLI (for MCP client)

### Build

```bash
cd sdlc-tools-mcp-server
mvn clean package
```

This creates an executable JAR: `target/sdlc-tools-mcp-server-2.0.0-SNAPSHOT-jar-with-dependencies.jar`

### Run

```bash
java -jar target/sdlc-tools-mcp-server-*-jar-with-dependencies.jar
```

The server communicates via standard input/output using JSON-RPC 2.0 protocol.

## Integration with Claude Code

### Configure MCP Server

Add to your Claude Code MCP settings (`~/.config/claude-code/mcp-settings.json`):

```json
{
  "mcpServers": {
    "sdlc-tools": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/sdlc-tools-mcp-server-jar-with-dependencies.jar"
      ]
    }
  }
}
```

### Using with Claude Code

1. **Start Claude Code** and the MCP server will connect automatically

2. **Use MCP tools** in your prompts:
   ```
   # Maven tools
   @mcp sdlc-tools analyze-maven-project /path/to/your/maven/project

   # JIRA tools
   @mcp sdlc-tools jira-search-issues --jql "project = MYPROJ AND status = Open"

   # Confluence tools
   @mcp sdlc-tools confluence-search-pages --cql "space = DEV AND type = page"
   ```

3. **Combine with personas** for structured development:
   ```
   @mcp sdlc-tools analyze-maven-project /path/to/project

   Now act as the Analyst persona from .github/mcp/personas/01-analyst.md
   and create a detailed analysis report for implementing feature X.
   ```

## Available Tools

### analyze-maven-project

Analyzes Maven project structure, modules, dependencies, and configuration.

**Parameters:**
- `path` (required): Absolute path to Maven project root

**Example:**
```json
{
  "name": "analyze-maven-project",
  "arguments": {
    "path": "/Users/dev/my-maven-project"
  }
}
```

**Returns:**
- Project metadata (groupId, artifactId, version)
- Module structure and hierarchy
- Dependencies and plugins
- Source structure

### analyze-dependencies

Deep dependency analysis including transitive dependencies and conflicts.

**Parameters:**
- `path` (required): Path to Maven project or module
- `module` (optional): Specific module to analyze

**Example:**
```json
{
  "name": "analyze-dependencies",
  "arguments": {
    "path": "/Users/dev/my-maven-project",
    "module": "core-module"
  }
}
```

### run-maven-command

Safely executes allowed Maven commands.

**Parameters:**
- `path` (required): Path to Maven project
- `command` (required): Maven command (e.g., "clean test")
- `module` (optional): Specific module to build

**Allowed commands:**
- `clean`, `compile`, `test`, `package`, `verify`, `install`
- `dependency:tree`, `dependency:analyze`
- `jacoco:report`, `pmd:pmd`, `pmd:cpd`

**Example:**
```json
{
  "name": "run-maven-command",
  "arguments": {
    "path": "/Users/dev/my-maven-project",
    "command": "clean test",
    "module": "core-module"
  }
}
```

### code-quality-check

Runs static analysis and code quality checks.

**Parameters:**
- `path` (required): Path to Maven project or module

**Example:**
```json
{
  "name": "code-quality-check",
  "arguments": {
    "path": "/Users/dev/my-maven-project"
  }
}
```

### generate-documentation

Generates comprehensive technical documentation from code analysis and Git history.

**Parameters:**
- `path` (required): Path to Maven project or module
- `type` (required): Documentation type - `javadoc-analysis`, `readme`, `api-docs`, `changelog`, or `all`
- `packageFilter` (optional): Package filter for API docs (e.g., `com.example.api`)
- `maxCommits` (optional): Maximum commits for changelog (default: 100)
- `outputFile` (optional): Whether to write output to a file (default: false)

**Documentation Types:**

1. **javadoc-analysis** - Analyzes JavaDoc coverage and generates suggestions for missing documentation
2. **readme** - Generates a comprehensive README.md based on Maven project analysis
3. **api-docs** - Creates API documentation in Markdown format for all public classes and methods
4. **changelog** - Generates CHANGELOG.md from Git commit history following Keep a Changelog format
5. **all** - Generates all documentation types

**Example - JavaDoc Analysis:**
```json
{
  "name": "generate-documentation",
  "arguments": {
    "path": "/Users/dev/my-maven-project",
    "type": "javadoc-analysis"
  }
}
```

**Example - Generate README:**
```json
{
  "name": "generate-documentation",
  "arguments": {
    "path": "/Users/dev/my-maven-project",
    "type": "readme",
    "outputFile": true
  }
}
```

**Example - Generate API Docs:**
```json
{
  "name": "generate-documentation",
  "arguments": {
    "path": "/Users/dev/my-maven-project",
    "type": "api-docs",
    "packageFilter": "com.example.api",
    "outputFile": true
  }
}
```

**Example - Generate Changelog:**
```json
{
  "name": "generate-documentation",
  "arguments": {
    "path": "/Users/dev/my-maven-project",
    "type": "changelog",
    "maxCommits": 50,
    "outputFile": true
  }
}
```

**Example - Generate All:**
```json
{
  "name": "generate-documentation",
  "arguments": {
    "path": "/Users/dev/my-maven-project",
    "type": "all",
    "outputFile": true
  }
}
```

### suggest-implementation

Suggests implementation approaches based on codebase analysis.

**Parameters:**
- `path` (required): Path to Maven project
- `feature` (required): Feature or bug fix description
- `analysisReport` (optional): Path to analysis report

**Example:**
```json
{
  "name": "suggest-implementation",
  "arguments": {
    "path": "/Users/dev/my-maven-project",
    "feature": "Add user authentication with JWT tokens"
  }
}
```

## SDLC Workflow Example

Here's how to use the MCP server with SDLC personas for a complete feature implementation:

### Step 1: Analysis
```
@mcp maven-sdlc analyze-maven-project /path/to/project

Act as the Analyst persona (.github/mcp/personas/01-analyst.md)
and analyze the impact of adding user authentication to the project.
```

### Step 2: Architecture Design
```
Based on the analysis above, act as the Architect persona
(.github/mcp/personas/02-architect.md) and create a design document
for implementing JWT-based authentication.
```

### Step 3: Implementation
```
@mcp maven-sdlc suggest-implementation --feature "JWT authentication"

Act as the Developer persona (.github/mcp/personas/03-developer.md)
and implement the authentication service following the design.
```

### Step 4: Testing
```
@mcp maven-sdlc run-maven-command --command "test"

Act as the Tester persona (.github/mcp/personas/04-tester.md)
and create comprehensive tests for the authentication feature.
```

### Step 5: Code Review
```
Act as the Reviewer persona (.github/mcp/personas/05-reviewer.md)
and review the authentication implementation for quality, security,
and best practices.
```

### Step 6: Documentation
```
@mcp maven-sdlc generate-documentation --type "api-guide"

Act as the Documentor persona (.github/mcp/personas/06-documentor.md)
and create user documentation for the authentication API.
```

## JIRA Tools

### jira-search-issues

Search for JIRA issues using JQL (JIRA Query Language).

**Parameters:**
- `jiraUrl` (required): JIRA instance URL (e.g., "https://your-domain.atlassian.net")
- `email` (required): User email for authentication
- `apiToken` (required): JIRA API token
- `jql` (required): JQL query string
- `maxResults` (optional): Maximum number of results (default: 50)

**Example:**
```json
{
  "name": "jira-search-issues",
  "arguments": {
    "jiraUrl": "https://your-domain.atlassian.net",
    "email": "user@example.com",
    "apiToken": "your-api-token",
    "jql": "project = PROJ AND status = Open",
    "maxResults": 25
  }
}
```

### jira-get-issue

Get detailed information about a specific JIRA issue.

**Parameters:**
- `jiraUrl` (required): JIRA instance URL
- `email` (required): User email for authentication
- `apiToken` (required): JIRA API token
- `issueKey` (required): Issue key (e.g., "PROJ-123")

**Example:**
```json
{
  "name": "jira-get-issue",
  "arguments": {
    "jiraUrl": "https://your-domain.atlassian.net",
    "email": "user@example.com",
    "apiToken": "your-api-token",
    "issueKey": "PROJ-123"
  }
}
```

### jira-create-issue

Create a new JIRA issue in a specified project.

**Parameters:**
- `jiraUrl` (required): JIRA instance URL
- `email` (required): User email for authentication
- `apiToken` (required): JIRA API token
- `projectKey` (required): Project key (e.g., "PROJ")
- `issueType` (required): Issue type (e.g., "Bug", "Task", "Story")
- `summary` (required): Issue summary/title
- `description` (optional): Detailed description

**Example:**
```json
{
  "name": "jira-create-issue",
  "arguments": {
    "jiraUrl": "https://your-domain.atlassian.net",
    "email": "user@example.com",
    "apiToken": "your-api-token",
    "projectKey": "PROJ",
    "issueType": "Bug",
    "summary": "Fix login page error",
    "description": "Users are unable to login when using special characters"
  }
}
```

## Confluence Tools

### confluence-search-pages

Search for Confluence pages using CQL (Confluence Query Language).

**Parameters:**
- `confluenceUrl` (required): Confluence instance URL (e.g., "https://your-domain.atlassian.net/wiki")
- `email` (required): User email for authentication
- `apiToken` (required): Confluence API token
- `cql` (required): CQL query string
- `limit` (optional): Maximum number of results (default: 25)

**Example:**
```json
{
  "name": "confluence-search-pages",
  "arguments": {
    "confluenceUrl": "https://your-domain.atlassian.net/wiki",
    "email": "user@example.com",
    "apiToken": "your-api-token",
    "cql": "space = DEV AND type = page",
    "limit": 10
  }
}
```

### confluence-get-page

Get detailed information about a specific Confluence page.

**Parameters:**
- `confluenceUrl` (required): Confluence instance URL
- `email` (required): User email for authentication
- `apiToken` (required): Confluence API token
- `pageId` (required): Page ID
- `expand` (optional): Fields to expand (default: "body.storage,version")

**Example:**
```json
{
  "name": "confluence-get-page",
  "arguments": {
    "confluenceUrl": "https://your-domain.atlassian.net/wiki",
    "email": "user@example.com",
    "apiToken": "your-api-token",
    "pageId": "123456789"
  }
}
```

### confluence-create-page

Create a new Confluence page in a specified space.

**Parameters:**
- `confluenceUrl` (required): Confluence instance URL
- `email` (required): User email for authentication
- `apiToken` (required): Confluence API token
- `spaceKey` (required): Space key (e.g., "DEV")
- `title` (required): Page title
- `content` (required): Page content in Confluence storage format (HTML)
- `parentId` (optional): Parent page ID

**Example:**
```json
{
  "name": "confluence-create-page",
  "arguments": {
    "confluenceUrl": "https://your-domain.atlassian.net/wiki",
    "email": "user@example.com",
    "apiToken": "your-api-token",
    "spaceKey": "DEV",
    "title": "API Documentation",
    "content": "<h1>API Documentation</h1><p>This page contains API documentation...</p>"
  }
}
```

## Available Resources

### analysis-cache

Caches Maven project analysis results to avoid re-running expensive operations.

**URI Pattern:** `cache://analysis/{projectPath}`

**Example:**
```json
{
  "uri": "cache://analysis//Users/dev/my-maven-project"
}
```

**Returns:**
- Cached analysis data if available
- Cache age and staleness information
- Hint to run analyze-maven-project if no cache exists

## Available Prompts

### sdlc-full-workflow

Complete SDLC workflow from analysis through documentation for implementing a feature or fixing a bug.

**Parameters:**
- `projectPath` (required): Path to the Maven project
- `task` (required): Feature to implement or bug to fix
- `type` (optional): Task type - `feature` or `bugfix` (default: feature)

**Example:**
```json
{
  "name": "sdlc-full-workflow",
  "arguments": {
    "projectPath": "/Users/dev/my-maven-project",
    "task": "Add user authentication with JWT",
    "type": "feature"
  }
}
```

**Returns:** A structured multi-phase workflow covering:
1. Analysis (Analyst Persona)
2. Architecture (Architect Persona)
3. Implementation (Developer Persona)
4. Testing (Tester Persona)
5. Review (Reviewer Persona)
6. Documentation (Documentor Persona)

## Development

### Project Structure

```
sdlc-tools-mcp-server/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/example/mcp/
    │   ├── SdlcToolsMcpServer.java     # Main entry point
    │   ├── protocol/                   # MCP protocol implementation
    │   │   ├── McpServer.java
    │   │   ├── Transport.java
    │   │   └── StdioTransport.java
    │   ├── clients/                    # HTTP clients for integrations
    │   │   ├── JiraClient.java
    │   │   └── ConfluenceClient.java
    │   ├── tools/                      # Maven tools
    │   │   ├── Tool.java
    │   │   ├── AnalyzeMavenProjectTool.java
    │   │   ├── AnalyzeDependenciesTool.java
    │   │   ├── RunMavenCommandTool.java
    │   │   ├── CodeQualityCheckTool.java
    │   │   ├── GenerateDocumentationTool.java
    │   │   ├── SuggestImplementationTool.java
    │   │   ├── jira/                   # JIRA integration tools
    │   │   │   ├── SearchJiraIssuesTool.java
    │   │   │   ├── GetJiraIssueTool.java
    │   │   │   └── CreateJiraIssueTool.java
    │   │   └── confluence/             # Confluence integration tools
    │   │       ├── SearchConfluencePagesTool.java
    │   │       ├── GetConfluencePageTool.java
    │   │       └── CreateConfluencePageTool.java
    │   ├── resources/                  # MCP resources
    │   │   ├── Resource.java
    │   │   └── AnalysisCacheResource.java
    │   └── prompts/                    # MCP prompts
    │       ├── Prompt.java
    │       └── SdlcWorkflowPrompt.java
    └── test/java/                      # Unit tests
```

### Running Tests

```bash
mvn test
```

### Code Coverage

```bash
mvn clean verify jacoco:report
open target/site/jacoco/index.html
```

### Code Quality

```bash
# PMD analysis
mvn pmd:pmd

# Code duplication detection
mvn pmd:cpd

# View reports
open target/site/pmd.html
```

## Protocol Details

The server implements JSON-RPC 2.0 over stdio. Each message is a JSON object on a single line.

### Supported Methods

- `initialize` - Initialize the server
- `tools/list` - List available tools
- `tools/call` - Invoke a specific tool
- `resources/list` - List available resources (v2.0.0+)
- `resources/read` - Read a specific resource (v2.0.0+)
- `prompts/list` - List available prompts (v2.0.0+)
- `prompts/get` - Get a specific prompt with arguments (v2.0.0+)

### Initialize Request
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {}
}
```

### List Tools Request
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}
```

### Call Tool Request
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "analyze-maven-project",
    "arguments": {
      "path": "/path/to/project"
    }
  }
}
```

### List Resources Request (v2.0.0+)
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "resources/list",
  "params": {}
}
```

### Read Resource Request (v2.0.0+)
```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "resources/read",
  "params": {
    "uri": "cache://analysis//path/to/project"
  }
}
```

### List Prompts Request (v2.0.0+)
```json
{
  "jsonrpc": "2.0",
  "id": 6,
  "method": "prompts/list",
  "params": {}
}
```

### Get Prompt Request (v2.0.0+)
```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "prompts/get",
  "params": {
    "name": "sdlc-full-workflow",
    "arguments": {
      "projectPath": "/path/to/project",
      "task": "Add user authentication",
      "type": "feature"
    }
  }
}
```

## Troubleshooting

### Server won't start

**Issue:** `Error: Could not find or load main class`

**Solution:** Ensure you're using the JAR with dependencies:
```bash
java -jar maven-sdlc-mcp-server-*-jar-with-dependencies.jar
```

### Claude Code can't connect

**Issue:** MCP server not responding

**Solution:**
1. Verify the JAR path in `mcp-settings.json` is absolute
2. Check Java 17+ is installed: `java -version`
3. Test server manually: `echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | java -jar server.jar`

### Tool execution fails

**Issue:** "Tool execution failed" error

**Solution:**
1. Verify the project path exists and contains `pom.xml`
2. Check Maven is installed: `mvn -version`
3. Review server logs for detailed error messages

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Development Guidelines

- Follow Java best practices and coding standards
- Write unit tests for all new tools (target >80% coverage)
- Update documentation when adding features
- Use the SDLC personas to guide your development process!

## License

Copyright © 2025. All rights reserved.

## Support

- **Documentation**: See `.github/mcp/README.md` for SDLC personas
- **Issues**: Report issues on GitHub
- **Discussions**: Join discussions for questions and feedback

## Roadmap

### v2.0.0 (Released)
- [x] MCP Resources support for caching analysis results
- [x] MCP Prompts support for pre-defined SDLC workflows
- [x] Enhanced error handling with better error codes
- [x] Comprehensive logging throughout the server
- [x] JIRA integration (search, get, create issues)
- [x] Confluence integration (search, get, create pages)
- [x] Renamed project to SDLC Tools (removed Maven-specific naming)
- [x] Multi-platform SDLC support
- [x] **Automated documentation generation** (JavaDoc analysis, README, API docs, Changelogs)

### v2.1.0 (Planned)
- [ ] Full PMD integration for code quality checks
- [ ] Dependency conflict resolution suggestions
- [ ] Integration with SonarQube
- [ ] Additional JIRA tools (transition issues, add comments)

### v2.2.0 (Planned)
- [ ] Performance optimization for large projects
- [ ] WebSocket transport option
- [ ] Additional workflow prompts (debugging, refactoring)
- [ ] More resource types (project files, configurations)

### v3.0.0 (Future)
- [ ] Support for Gradle projects
- [ ] AI-powered code generation
- [ ] Integration with CI/CD platforms
- [ ] Multi-language support (Kotlin, Scala)

---

Built with ❤️ for better AI-assisted software development lifecycle management
