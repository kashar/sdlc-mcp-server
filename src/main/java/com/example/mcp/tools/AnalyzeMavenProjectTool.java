package com.example.mcp.tools;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for analyzing Maven multi-module project structure.
 *
 * <p>This tool examines a Maven project and provides detailed information about:
 * <ul>
 *   <li>Module structure and hierarchy</li>
 *   <li>Dependencies between modules</li>
 *   <li>External dependencies</li>
 *   <li>Project configuration</li>
 * </ul>
 *
 * @author Maven SDLC Team
 * @version 1.0.0
 */
public class AnalyzeMavenProjectTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeMavenProjectTool.class);

    @Override
    public String getName() {
        return "analyze-maven-project";
    }

    @Override
    public String getDescription() {
        return "Analyzes a Maven multi-module project structure, including modules, dependencies, " +
                "and configuration. Useful for understanding project organization before making changes.";
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
                                        "description", "Absolute path to the Maven project root directory (containing pom.xml)"
                                )
                        ),
                        "required", List.of("path")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String projectPath = (String) arguments.get("path");

        if (projectPath == null || projectPath.isEmpty()) {
            throw new IllegalArgumentException("path parameter is required");
        }

        Path projectDir = Paths.get(projectPath);
        if (!Files.exists(projectDir)) {
            throw new IllegalArgumentException("Project directory does not exist: " + projectPath);
        }

        Path pomPath = projectDir.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            throw new IllegalArgumentException("No pom.xml found in: " + projectPath);
        }

        logger.info("Analyzing Maven project at: {}", projectPath);

        // Analyze the project
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("projectPath", projectPath);
        analysis.put("projectType", determineProjectType(pomPath));

        Model rootPom = readPom(pomPath.toFile());
        analysis.put("groupId", rootPom.getGroupId());
        analysis.put("artifactId", rootPom.getArtifactId());
        analysis.put("version", rootPom.getVersion());
        analysis.put("packaging", rootPom.getPackaging());

        // Analyze modules if multi-module
        List<String> modules = rootPom.getModules();
        if (modules != null && !modules.isEmpty()) {
            analysis.put("moduleCount", modules.size());
            analysis.put("modules", analyzeModules(projectDir, modules));
        } else {
            analysis.put("moduleCount", 0);
            analysis.put("modules", List.of());
        }

        // Analyze dependencies
        if (rootPom.getDependencies() != null) {
            analysis.put("dependencyCount", rootPom.getDependencies().size());
            analysis.put("dependencies", rootPom.getDependencies().stream()
                    .map(dep -> Map.of(
                            "groupId", dep.getGroupId(),
                            "artifactId", dep.getArtifactId(),
                            "version", dep.getVersion() != null ? dep.getVersion() : "inherited",
                            "scope", dep.getScope() != null ? dep.getScope() : "compile"
                    ))
                    .collect(Collectors.toList()));
        }

        // Analyze plugins
        if (rootPom.getBuild() != null && rootPom.getBuild().getPlugins() != null) {
            analysis.put("pluginCount", rootPom.getBuild().getPlugins().size());
            analysis.put("plugins", rootPom.getBuild().getPlugins().stream()
                    .map(plugin -> plugin.getArtifactId())
                    .collect(Collectors.toList()));
        }

        // Source structure
        analysis.put("sourceStructure", analyzeSourceStructure(projectDir));

        logger.info("Analysis complete for: {}", projectPath);

        return Map.of(
                "success", true,
                "analysis", analysis
        );
    }

    /**
     * Determines if this is a single or multi-module project.
     */
    private String determineProjectType(Path pomPath) throws Exception {
        Model pom = readPom(pomPath.toFile());
        if (pom.getModules() != null && !pom.getModules().isEmpty()) {
            return "multi-module";
        }
        return "single-module";
    }

    /**
     * Analyzes all modules in a multi-module project.
     */
    private List<Map<String, Object>> analyzeModules(Path projectDir, List<String> moduleNames) {
        return moduleNames.stream()
                .map(moduleName -> analyzeModule(projectDir, moduleName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Analyzes a single module.
     */
    private Map<String, Object> analyzeModule(Path projectDir, String moduleName) {
        try {
            Path modulePath = projectDir.resolve(moduleName);
            Path modulePomPath = modulePath.resolve("pom.xml");

            if (!Files.exists(modulePomPath)) {
                logger.warn("Module pom.xml not found: {}", modulePomPath);
                return null;
            }

            Model modulePom = readPom(modulePomPath.toFile());

            Map<String, Object> moduleInfo = new LinkedHashMap<>();
            moduleInfo.put("name", moduleName);
            moduleInfo.put("artifactId", modulePom.getArtifactId());
            moduleInfo.put("packaging", modulePom.getPackaging());

            // Dependencies
            if (modulePom.getDependencies() != null) {
                moduleInfo.put("dependencyCount", modulePom.getDependencies().size());

                // Internal dependencies (same groupId)
                String projectGroupId = modulePom.getGroupId() != null ?
                        modulePom.getGroupId() :
                        (modulePom.getParent() != null ? modulePom.getParent().getGroupId() : null);

                List<String> internalDeps = modulePom.getDependencies().stream()
                        .filter(dep -> dep.getGroupId().equals(projectGroupId))
                        .map(dep -> dep.getArtifactId())
                        .collect(Collectors.toList());

                moduleInfo.put("internalDependencies", internalDeps);
            }

            // Source structure
            Path srcMain = modulePath.resolve("src/main/java");
            Path srcTest = modulePath.resolve("src/test/java");
            moduleInfo.put("hasMainSources", Files.exists(srcMain));
            moduleInfo.put("hasTestSources", Files.exists(srcTest));

            return moduleInfo;

        } catch (Exception e) {
            logger.error("Error analyzing module: " + moduleName, e);
            return Map.of(
                    "name", moduleName,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Analyzes the source code structure.
     */
    private Map<String, Object> analyzeSourceStructure(Path projectDir) {
        Map<String, Object> structure = new LinkedHashMap<>();

        Path srcMainJava = projectDir.resolve("src/main/java");
        Path srcMainResources = projectDir.resolve("src/main/resources");
        Path srcTestJava = projectDir.resolve("src/test/java");
        Path srcTestResources = projectDir.resolve("src/test/resources");

        structure.put("hasMainJava", Files.exists(srcMainJava));
        structure.put("hasMainResources", Files.exists(srcMainResources));
        structure.put("hasTestJava", Files.exists(srcTestJava));
        structure.put("hasTestResources", Files.exists(srcTestResources));

        try {
            if (Files.exists(srcMainJava)) {
                long javaFileCount = Files.walk(srcMainJava)
                        .filter(p -> p.toString().endsWith(".java"))
                        .count();
                structure.put("mainJavaFiles", javaFileCount);
            }

            if (Files.exists(srcTestJava)) {
                long testFileCount = Files.walk(srcTestJava)
                        .filter(p -> p.toString().endsWith(".java"))
                        .count();
                structure.put("testJavaFiles", testFileCount);
            }
        } catch (Exception e) {
            logger.warn("Error counting source files", e);
        }

        return structure;
    }

    /**
     * Reads a Maven POM file.
     */
    private Model readPom(File pomFile) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileReader fileReader = new FileReader(pomFile)) {
            return reader.read(fileReader);
        }
    }
}
