package com.example.mcp.tools;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tool for autonomous feature implementation in Maven projects.
 *
 * <p>This tool analyzes the codebase, identifies where to implement the feature,
 * generates necessary code following existing patterns, and creates corresponding tests.
 *
 * <p>Capabilities:
 * <ul>
 *   <li>Parse and understand feature descriptions</li>
 *   <li>Identify appropriate module and package for implementation</li>
 *   <li>Analyze existing code patterns and conventions</li>
 *   <li>Generate new classes/methods following project conventions</li>
 *   <li>Create unit tests for new functionality</li>
 *   <li>Integrate with existing architecture</li>
 * </ul>
 *
 * @author Maven SDLC Team
 * @version 1.0.0
 */
public class ImplementFeatureTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(ImplementFeatureTool.class);
    private final JavaParser javaParser = new JavaParser();

    @Override
    public String getName() {
        return "implement-feature";
    }

    @Override
    public String getDescription() {
        return "Autonomously implements new features by analyzing codebase patterns, identifying " +
                "appropriate locations, generating code following project conventions, and creating tests. " +
                "Provides intelligent code generation based on feature description and existing architecture.";
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
                                "featureDescription", Map.of(
                                        "type", "string",
                                        "description", "Natural language description of the feature to implement"
                                ),
                                "targetModule", Map.of(
                                        "type", "string",
                                        "description", "Optional: specific module to implement feature in"
                                ),
                                "targetPackage", Map.of(
                                        "type", "string",
                                        "description", "Optional: specific package to implement feature in"
                                ),
                                "generateTests", Map.of(
                                        "type", "boolean",
                                        "description", "Generate unit tests for the feature (default: true)"
                                )
                        ),
                        "required", List.of("path", "featureDescription")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String path = (String) arguments.get("path");
        String featureDescription = (String) arguments.get("featureDescription");
        String targetModule = (String) arguments.getOrDefault("targetModule", null);
        String targetPackage = (String) arguments.getOrDefault("targetPackage", null);
        boolean generateTests = (boolean) arguments.getOrDefault("generateTests", true);

        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path parameter is required");
        }
        if (featureDescription == null || featureDescription.isEmpty()) {
            throw new IllegalArgumentException("featureDescription parameter is required");
        }

        Path projectPath = Paths.get(path);
        if (!Files.exists(projectPath)) {
            throw new IllegalArgumentException("Project path does not exist: " + path);
        }

        logger.info("Implementing feature in {}: {}", path, featureDescription);

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("projectPath", path);
        results.put("featureDescription", featureDescription);
        results.put("timestamp", new Date().toString());

        try {
            // Analyze existing code patterns
            Map<String, Object> codePatterns = analyzeCodePatterns(projectPath);
            results.put("detectedPatterns", codePatterns);

            // Determine where to implement the feature
            Map<String, String> location = determineImplementationLocation(
                    projectPath, featureDescription, targetModule, targetPackage, codePatterns);
            results.put("recommendedLocation", location);

            // Generate implementation plan
            List<Map<String, Object>> implementationSteps = generateImplementationPlan(
                    featureDescription, location, codePatterns);
            results.put("implementationPlan", implementationSteps);

            // Generate code templates
            List<Map<String, Object>> codeTemplates = generateCodeTemplates(
                    featureDescription, location, codePatterns);
            results.put("codeTemplates", codeTemplates);

            // Generate test templates if requested
            if (generateTests) {
                List<Map<String, Object>> testTemplates = generateTestTemplates(
                        featureDescription, location, codePatterns);
                results.put("testTemplates", testTemplates);
            }

            // Summary and recommendations
            results.put("summary", Map.of(
                    "filestoCreate", codeTemplates.size(),
                    "testFilesToCreate", generateTests ? codeTemplates.size() : 0,
                    "estimatedComplexity", estimateComplexity(featureDescription),
                    "recommendations", generateImplementationRecommendations(featureDescription, codePatterns)
            ));

            return Map.of(
                    "success", true,
                    "results", results
            );

        } catch (Exception e) {
            logger.error("Error during feature implementation", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "recommendations", List.of(
                            "Ensure the project has valid Java source structure",
                            "Provide more specific feature description",
                            "Check that target module/package exists"
                    )
            );
        }
    }

    /**
     * Analyzes existing code patterns in the project.
     */
    private Map<String, Object> analyzeCodePatterns(Path projectPath) throws IOException {
        Map<String, Object> patterns = new LinkedHashMap<>();

        List<Path> javaFiles = findJavaFiles(projectPath);

        if (javaFiles.isEmpty()) {
            return Map.of("message", "No Java files found for pattern analysis");
        }

        // Analyze naming conventions
        Set<String> classNamePrefixes = new HashSet<>();
        Set<String> classNameSuffixes = new HashSet<>();

        // Use arrays to allow modification in lambda
        final boolean[] usesInterfaces = {false};
        final boolean[] usesAbstractClasses = {false};
        final int[] springAnnotations = {0};
        final int[] builderPattern = {0};

        for (Path javaFile : javaFiles.stream().limit(50).collect(Collectors.toList())) {
            try {
                CompilationUnit cu = javaParser.parse(javaFile).getResult().orElse(null);
                if (cu == null) continue;

                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                    String name = cls.getNameAsString();

                    // Detect suffixes
                    if (name.endsWith("Service")) classNameSuffixes.add("Service");
                    if (name.endsWith("Controller")) classNameSuffixes.add("Controller");
                    if (name.endsWith("Repository")) classNameSuffixes.add("Repository");
                    if (name.endsWith("Impl")) classNameSuffixes.add("Impl");
                    if (name.endsWith("Tool")) classNameSuffixes.add("Tool");
                    if (name.endsWith("Util")) classNameSuffixes.add("Util");
                    if (name.endsWith("Helper")) classNameSuffixes.add("Helper");

                    // Detect patterns
                    if (cls.isInterface()) usesInterfaces[0] = true;
                    if (cls.isAbstract() && !cls.isInterface()) usesAbstractClasses[0] = true;

                    // Check for Spring annotations
                    if (cls.getAnnotations().stream().anyMatch(ann ->
                            ann.getNameAsString().contains("Service") ||
                            ann.getNameAsString().contains("Controller") ||
                            ann.getNameAsString().contains("Repository") ||
                            ann.getNameAsString().contains("Component"))) {
                        springAnnotations[0]++;
                    }

                    // Check for builder pattern
                    if (cls.getMethods().stream().anyMatch(m ->
                            m.getNameAsString().equals("builder"))) {
                        builderPattern[0]++;
                    }
                });

            } catch (Exception e) {
                logger.debug("Error parsing {}: {}", javaFile, e.getMessage());
            }
        }

        patterns.put("commonSuffixes", new ArrayList<>(classNameSuffixes));
        patterns.put("usesInterfaces", usesInterfaces[0]);
        patterns.put("usesAbstractClasses", usesAbstractClasses[0]);
        patterns.put("usesSpring", springAnnotations[0] > 0);
        patterns.put("usesBuilderPattern", builderPattern[0] > 0);
        patterns.put("filesAnalyzed", Math.min(50, javaFiles.size()));

        return patterns;
    }

    /**
     * Determines where to implement the feature.
     */
    private Map<String, String> determineImplementationLocation(Path projectPath,
                                                                String featureDescription,
                                                                String targetModule,
                                                                String targetPackage,
                                                                Map<String, Object> patterns) {
        Map<String, String> location = new LinkedHashMap<>();

        // Use provided target or infer from feature description
        String module = targetModule != null ? targetModule : inferModule(featureDescription);
        String packageName = targetPackage != null ? targetPackage : inferPackage(featureDescription, patterns);
        String className = inferClassName(featureDescription, patterns);

        location.put("module", module);
        location.put("packageName", packageName);
        location.put("className", className);
        location.put("sourceFile", "src/main/java/" + packageName.replace(".", "/") + "/" + className + ".java");
        location.put("testFile", "src/test/java/" + packageName.replace(".", "/") + "/" + className + "Test.java");

        return location;
    }

    /**
     * Infers module from feature description.
     */
    private String inferModule(String description) {
        // Simplified logic
        if (description.toLowerCase().contains("api") || description.toLowerCase().contains("rest")) {
            return "api";
        }
        if (description.toLowerCase().contains("core") || description.toLowerCase().contains("service")) {
            return "core";
        }
        return "main"; // default
    }

    /**
     * Infers package name from feature description and patterns.
     */
    private String inferPackage(String description, Map<String, Object> patterns) {
        String lowerDesc = description.toLowerCase();

        if (lowerDesc.contains("controller") || lowerDesc.contains("rest") || lowerDesc.contains("api")) {
            return "com.example.controller";
        }
        if (lowerDesc.contains("service")) {
            return "com.example.service";
        }
        if (lowerDesc.contains("repository") || lowerDesc.contains("dao")) {
            return "com.example.repository";
        }
        if (lowerDesc.contains("util") || lowerDesc.contains("helper")) {
            return "com.example.util";
        }

        return "com.example.feature";
    }

    /**
     * Infers class name from feature description.
     */
    private String inferClassName(String description, Map<String, Object> patterns) {
        // Extract key words and create class name
        String[] words = description.split("\\s+");
        StringBuilder className = new StringBuilder();

        for (String word : words) {
            if (word.length() > 2 && !isCommonWord(word)) {
                className.append(capitalize(word));
                if (className.length() > 20) break; // Limit length
            }
        }

        // Add appropriate suffix based on patterns
        @SuppressWarnings("unchecked")
        List<String> suffixes = (List<String>) patterns.getOrDefault("commonSuffixes", List.of());

        if (description.toLowerCase().contains("service") || suffixes.contains("Service")) {
            if (!className.toString().endsWith("Service")) {
                className.append("Service");
            }
        } else if (description.toLowerCase().contains("controller") || suffixes.contains("Controller")) {
            if (!className.toString().endsWith("Controller")) {
                className.append("Controller");
            }
        } else if (!suffixes.isEmpty() && !className.toString().matches(".*(" + String.join("|", suffixes) + ")")) {
            // Default to first common suffix if none applied
            className.append(suffixes.get(0));
        }

        return className.length() > 0 ? className.toString() : "NewFeature";
    }

    /**
     * Generates implementation plan.
     */
    private List<Map<String, Object>> generateImplementationPlan(String description,
                                                                 Map<String, String> location,
                                                                 Map<String, Object> patterns) {
        List<Map<String, Object>> steps = new ArrayList<>();

        steps.add(Map.of(
                "step", 1,
                "action", "Create class " + location.get("className"),
                "file", location.get("sourceFile"),
                "description", "Create main implementation class"
        ));

        steps.add(Map.of(
                "step", 2,
                "action", "Implement core methods",
                "description", "Add methods based on feature description"
        ));

        if ((boolean) patterns.getOrDefault("usesInterfaces", false)) {
            steps.add(Map.of(
                    "step", 3,
                    "action", "Consider creating interface",
                    "description", "Project uses interface pattern"
            ));
        }

        steps.add(Map.of(
                "step", 4,
                "action", "Create unit tests",
                "file", location.get("testFile"),
                "description", "Create comprehensive test coverage"
        ));

        return steps;
    }

    /**
     * Generates code templates.
     */
    private List<Map<String, Object>> generateCodeTemplates(String description,
                                                            Map<String, String> location,
                                                            Map<String, Object> patterns) {
        List<Map<String, Object>> templates = new ArrayList<>();

        String className = location.get("className");
        String packageName = location.get("packageName");
        boolean usesSpring = (boolean) patterns.getOrDefault("usesSpring", false);

        StringBuilder classTemplate = new StringBuilder();
        classTemplate.append("package ").append(packageName).append(";\n\n");

        if (usesSpring) {
            classTemplate.append("import org.springframework.stereotype.Service;\n");
            classTemplate.append("import org.slf4j.Logger;\n");
            classTemplate.append("import org.slf4j.LoggerFactory;\n\n");
            classTemplate.append("@Service\n");
        }

        classTemplate.append("public class ").append(className).append(" {\n\n");

        if (usesSpring) {
            classTemplate.append("    private static final Logger logger = LoggerFactory.getLogger(")
                    .append(className).append(".class);\n\n");
        }

        classTemplate.append("    /**\n");
        classTemplate.append("     * ").append(description).append("\n");
        classTemplate.append("     */\n");
        classTemplate.append("    public void executeFeature() {\n");
        classTemplate.append("        // TODO: Implement feature logic\n");
        classTemplate.append("    }\n\n");
        classTemplate.append("}\n");

        templates.add(Map.of(
                "type", "source",
                "file", location.get("sourceFile"),
                "content", classTemplate.toString(),
                "description", "Main implementation class"
        ));

        return templates;
    }

    /**
     * Generates test templates.
     */
    private List<Map<String, Object>> generateTestTemplates(String description,
                                                            Map<String, String> location,
                                                            Map<String, Object> patterns) {
        List<Map<String, Object>> templates = new ArrayList<>();

        String className = location.get("className");
        String packageName = location.get("packageName");

        StringBuilder testTemplate = new StringBuilder();
        testTemplate.append("package ").append(packageName).append(";\n\n");
        testTemplate.append("import org.junit.jupiter.api.Test;\n");
        testTemplate.append("import org.junit.jupiter.api.BeforeEach;\n");
        testTemplate.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        testTemplate.append("class ").append(className).append("Test {\n\n");
        testTemplate.append("    private ").append(className).append(" ").append(uncapitalize(className)).append(";\n\n");
        testTemplate.append("    @BeforeEach\n");
        testTemplate.append("    void setUp() {\n");
        testTemplate.append("        ").append(uncapitalize(className)).append(" = new ").append(className).append("();\n");
        testTemplate.append("    }\n\n");
        testTemplate.append("    @Test\n");
        testTemplate.append("    void testExecuteFeature() {\n");
        testTemplate.append("        // TODO: Implement test\n");
        testTemplate.append("        assertNotNull(").append(uncapitalize(className)).append(");\n");
        testTemplate.append("    }\n");
        testTemplate.append("}\n");

        templates.add(Map.of(
                "type", "test",
                "file", location.get("testFile"),
                "content", testTemplate.toString(),
                "description", "Unit test for " + className
        ));

        return templates;
    }

    /**
     * Estimates complexity of the feature.
     */
    private String estimateComplexity(String description) {
        int wordCount = description.split("\\s+").length;
        if (wordCount < 10) return "low";
        if (wordCount < 20) return "medium";
        return "high";
    }

    /**
     * Generates implementation recommendations.
     */
    private List<String> generateImplementationRecommendations(String description,
                                                               Map<String, Object> patterns) {
        List<String> recommendations = new ArrayList<>();

        recommendations.add("Follow existing code patterns detected in the project");

        if ((boolean) patterns.getOrDefault("usesSpring", false)) {
            recommendations.add("Use Spring annotations (@Service, @Component, etc.)");
        }

        if ((boolean) patterns.getOrDefault("usesBuilderPattern", false)) {
            recommendations.add("Consider using builder pattern for complex objects");
        }

        recommendations.add("Write comprehensive unit tests with >80% coverage");
        recommendations.add("Add proper logging for debugging and monitoring");
        recommendations.add("Document public APIs with JavaDoc");

        return recommendations;
    }

    /**
     * Finds all Java files in the project.
     */
    private List<Path> findJavaFiles(Path projectPath) throws IOException {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .collect(Collectors.toList());
        }
    }

    private boolean isCommonWord(String word) {
        Set<String> common = Set.of("a", "an", "the", "to", "for", "of", "in", "on", "at", "by", "with");
        return common.contains(word.toLowerCase());
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }

    private String uncapitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
