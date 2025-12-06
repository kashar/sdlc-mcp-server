package com.example.mcp.tools;

import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tool for safely executing Maven commands.
 *
 * <p>Provides a controlled way to run Maven goals and phases with
 * proper error handling and output capture.
 *
 * @author Maven SDLC Team
 * @version 1.0.0
 */
public class RunMavenCommandTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(RunMavenCommandTool.class);

    // Allowed commands for security
    private static final List<String> ALLOWED_COMMANDS = List.of(
            "clean", "compile", "test", "package", "verify", "install",
            "dependency:tree", "dependency:analyze", "versions:display-dependency-updates",
            "jacoco:report", "pmd:pmd", "pmd:cpd"
    );

    @Override
    public String getName() {
        return "run-maven-command";
    }

    @Override
    public String getDescription() {
        return "Executes Maven commands safely. Supports common Maven goals like clean, compile, " +
                "test, package, and analysis commands. Use for building, testing, and analyzing projects.";
    }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", getName(),
                "description", getDescription(),
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "Path to the Maven project"
                                ),
                                "command", Map.of(
                                        "type", "string",
                                        "description", "Maven command to execute (e.g., 'clean test')",
                                        "enum", ALLOWED_COMMANDS
                                ),
                                "module", Map.of(
                                        "type", "string",
                                        "description", "Optional: specific module to build"
                                )
                        ),
                        "required", List.of("path", "command")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String path = (String) arguments.get("path");
        String command = (String) arguments.get("command");
        String module = (String) arguments.getOrDefault("module", null);

        // Security: validate command
        boolean isAllowed = ALLOWED_COMMANDS.stream()
                .anyMatch(allowed -> command.startsWith(allowed));

        if (!isAllowed) {
            throw new SecurityException("Command not allowed: " + command +
                    ". Allowed commands: " + ALLOWED_COMMANDS);
        }

        logger.info("Executing Maven command: {} in {}", command, path);

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(path, "pom.xml"));
        request.setGoals(Arrays.asList(command.split("\\s+")));

        if (module != null) {
            request.setProjects(List.of(module));
            request.setAlsoMake(true);
        }

        Invoker invoker = new DefaultInvoker();
        InvocationResult result = invoker.execute(request);

        if (result.getExitCode() == 0) {
            return Map.of(
                    "success", true,
                    "exitCode", 0,
                    "message", "Command executed successfully"
            );
        } else {
            return Map.of(
                    "success", false,
                    "exitCode", result.getExitCode(),
                    "message", "Command failed with exit code: " + result.getExitCode(),
                    "exception", result.getExecutionException() != null ?
                            result.getExecutionException().getMessage() : null
            );
        }
    }
}
