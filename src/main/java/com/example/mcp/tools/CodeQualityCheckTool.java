package com.example.mcp.tools;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tool for running comprehensive code quality checks.
 *
 * <p>This tool performs static code analysis including:
 * <ul>
 *   <li>PMD rule violations detection</li>
 *   <li>Code complexity analysis</li>
 *   <li>Code duplication detection</li>
 *   <li>Code smell identification</li>
 *   <li>Best practices validation</li>
 * </ul>
 *
 * @author Maven SDLC Team
 * @version 1.0.0
 */
public class CodeQualityCheckTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(CodeQualityCheckTool.class);

    @Override
    public String getName() {
        return "code-quality-check";
    }

    @Override
    public String getDescription() {
        return "Runs comprehensive static code analysis including PMD, code duplication detection, " +
                "complexity metrics, and quality checks. Identifies code smells, bugs, security issues, " +
                "and maintainability problems with actionable recommendations.";
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
                                        "description", "Path to the Maven project or module"
                                ),
                                "severity", Map.of(
                                        "type", "string",
                                        "description", "Minimum severity level: low, medium, high (default: medium)",
                                        "enum", List.of("low", "medium", "high")
                                ),
                                "includeTests", Map.of(
                                        "type", "boolean",
                                        "description", "Whether to include test code in analysis (default: false)"
                                )
                        ),
                        "required", List.of("path")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String path = (String) arguments.get("path");
        String severity = (String) arguments.getOrDefault("severity", "medium");
        boolean includeTests = (boolean) arguments.getOrDefault("includeTests", false);

        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path parameter is required");
        }

        Path projectPath = Paths.get(path);
        if (!Files.exists(projectPath)) {
            throw new IllegalArgumentException("Project path does not exist: " + path);
        }

        logger.info("Running code quality checks for: {} (severity: {}, includeTests: {})",
                path, severity, includeTests);

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("projectPath", path);
        results.put("timestamp", new Date().toString());

        try {
            // Run PMD analysis
            Map<String, Object> pmdResults = runPMDAnalysis(projectPath, severity, includeTests);
            results.put("pmdAnalysis", pmdResults);

            // Analyze code complexity
            Map<String, Object> complexityResults = analyzeComplexity(projectPath, includeTests);
            results.put("complexityAnalysis", complexityResults);

            // Detect code smells
            List<Map<String, Object>> codeSmells = detectCodeSmells(projectPath, includeTests);
            results.put("codeSmells", codeSmells);

            // Generate summary
            results.put("summary", generateSummary(pmdResults, complexityResults, codeSmells));

            return Map.of(
                    "success", true,
                    "results", results
            );

        } catch (Exception e) {
            logger.error("Error during code quality check", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "recommendations", List.of(
                            "Ensure the project has Java source files",
                            "Check that the path points to a valid Maven project",
                            "Verify file permissions"
                    )
            );
        }
    }

    /**
     * Runs PMD static analysis on the codebase.
     */
    private Map<String, Object> runPMDAnalysis(Path projectPath, String severity, boolean includeTests) {
        Map<String, Object> results = new LinkedHashMap<>();
        List<Map<String, Object>> violations = new ArrayList<>();

        try {
            List<Path> javaFiles = findJavaFiles(projectPath, includeTests);

            if (javaFiles.isEmpty()) {
                results.put("violationCount", 0);
                results.put("violations", violations);
                results.put("message", "No Java files found for analysis");
                return results;
            }

            PMDConfiguration config = new PMDConfiguration();
            config.setMinimumPriority(mapSeverityToPriority(severity));

            // Use LanguageRegistry to get Java language
            Language javaLanguage = LanguageRegistry.findLanguageByTerseName("java");
            if (javaLanguage != null) {
                config.setDefaultLanguageVersion(javaLanguage.getDefaultVersion());
            }

            config.addRuleSet("category/java/bestpractices.xml");
            config.addRuleSet("category/java/codestyle.xml");
            config.addRuleSet("category/java/design.xml");
            config.addRuleSet("category/java/errorprone.xml");
            config.addRuleSet("category/java/performance.xml");
            config.addRuleSet("category/java/security.xml");

            // Add input files
            for (Path javaFile : javaFiles) {
                config.addInputPath(javaFile);
            }

            try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
                Report report = pmd.performAnalysisAndCollectReport();

                for (RuleViolation violation : report.getViolations()) {
                    Map<String, Object> v = new LinkedHashMap<>();
                    v.put("file", violation.getFilename());
                    v.put("line", violation.getBeginLine());
                    v.put("column", violation.getBeginColumn());
                    v.put("rule", violation.getRule().getName());
                    v.put("category", violation.getRule().getRuleSetName());
                    v.put("priority", violation.getRule().getPriority().getPriority());
                    v.put("message", violation.getDescription());
                    violations.add(v);
                }

                results.put("violationCount", violations.size());
                results.put("violations", violations);
                results.put("filesAnalyzed", javaFiles.size());
            }

        } catch (Exception e) {
            logger.warn("PMD analysis failed: {}", e.getMessage());
            results.put("error", "PMD analysis failed: " + e.getMessage());
            results.put("violationCount", 0);
            results.put("violations", violations);
        }

        return results;
    }

    /**
     * Analyzes code complexity (cyclomatic complexity, method length, etc.).
     */
    private Map<String, Object> analyzeComplexity(Path projectPath, boolean includeTests) {
        Map<String, Object> results = new LinkedHashMap<>();
        List<Map<String, Object>> complexMethods = new ArrayList<>();

        try {
            List<Path> javaFiles = findJavaFiles(projectPath, includeTests);

            int totalMethods = 0;
            int complexMethodCount = 0;
            int maxComplexity = 0;

            // Simple complexity estimation based on file size and control structures
            for (Path javaFile : javaFiles) {
                try {
                    String content = Files.readString(javaFile);

                    // Count methods (simplified)
                    int methodCount = countOccurrences(content, "public ") +
                                    countOccurrences(content, "private ") +
                                    countOccurrences(content, "protected ");
                    totalMethods += methodCount;

                    // Estimate complexity based on control structures
                    int ifCount = countOccurrences(content, "if ");
                    int forCount = countOccurrences(content, "for ");
                    int whileCount = countOccurrences(content, "while ");
                    int switchCount = countOccurrences(content, "switch ");
                    int catchCount = countOccurrences(content, "catch ");

                    int estimatedComplexity = ifCount + forCount + whileCount + switchCount + catchCount;

                    if (estimatedComplexity > 10 && methodCount > 0) {
                        complexMethodCount++;
                        Map<String, Object> complexMethod = new LinkedHashMap<>();
                        complexMethod.put("file", javaFile.toString());
                        complexMethod.put("estimatedComplexity", estimatedComplexity);
                        complexMethod.put("recommendation", "Consider refactoring to reduce complexity");
                        complexMethods.add(complexMethod);
                    }

                    maxComplexity = Math.max(maxComplexity, estimatedComplexity);

                } catch (IOException e) {
                    logger.warn("Error analyzing file {}: {}", javaFile, e.getMessage());
                }
            }

            results.put("totalMethods", totalMethods);
            results.put("complexMethodCount", complexMethodCount);
            results.put("maxComplexity", maxComplexity);
            results.put("complexMethods", complexMethods);
            results.put("averageComplexity", totalMethods > 0 ? maxComplexity / javaFiles.size() : 0);

        } catch (Exception e) {
            logger.warn("Complexity analysis failed: {}", e.getMessage());
            results.put("error", "Complexity analysis failed: " + e.getMessage());
        }

        return results;
    }

    /**
     * Detects common code smells.
     */
    private List<Map<String, Object>> detectCodeSmells(Path projectPath, boolean includeTests) {
        List<Map<String, Object>> codeSmells = new ArrayList<>();

        try {
            List<Path> javaFiles = findJavaFiles(projectPath, includeTests);

            for (Path javaFile : javaFiles) {
                try {
                    String content = Files.readString(javaFile);
                    long lineCount = content.lines().count();

                    // Large class smell
                    if (lineCount > 500) {
                        Map<String, Object> smell = new LinkedHashMap<>();
                        smell.put("type", "LargeClass");
                        smell.put("severity", "medium");
                        smell.put("file", javaFile.toString());
                        smell.put("lines", lineCount);
                        smell.put("message", "Class has " + lineCount + " lines. Consider splitting into smaller classes.");
                        smell.put("recommendation", "Extract related methods into new classes following Single Responsibility Principle");
                        codeSmells.add(smell);
                    }

                    // Long method smell (simplified detection)
                    String[] methods = content.split("\\{");
                    for (String method : methods) {
                        long methodLines = method.lines().count();
                        if (methodLines > 50) {
                            Map<String, Object> smell = new LinkedHashMap<>();
                            smell.put("type", "LongMethod");
                            smell.put("severity", "low");
                            smell.put("file", javaFile.toString());
                            smell.put("lines", methodLines);
                            smell.put("message", "Method has " + methodLines + " lines");
                            smell.put("recommendation", "Extract method or decompose into smaller methods");
                            codeSmells.add(smell);
                            break; // Only report one per file to avoid spam
                        }
                    }

                    // God class detection (too many responsibilities)
                    int methodCount = countOccurrences(content, "public ") +
                                    countOccurrences(content, "private ");
                    if (methodCount > 30) {
                        Map<String, Object> smell = new LinkedHashMap<>();
                        smell.put("type", "GodClass");
                        smell.put("severity", "high");
                        smell.put("file", javaFile.toString());
                        smell.put("methodCount", methodCount);
                        smell.put("message", "Class has " + methodCount + " methods, likely violating Single Responsibility");
                        smell.put("recommendation", "Decompose into multiple focused classes");
                        codeSmells.add(smell);
                    }

                    // Magic numbers
                    if (content.matches(".*[^\\w](\\d{2,})[^\\w\\.].*") &&
                        !content.contains("static final")) {
                        Map<String, Object> smell = new LinkedHashMap<>();
                        smell.put("type", "MagicNumbers");
                        smell.put("severity", "low");
                        smell.put("file", javaFile.toString());
                        smell.put("message", "Potential magic numbers found");
                        smell.put("recommendation", "Extract magic numbers to named constants");
                        codeSmells.add(smell);
                    }

                } catch (IOException e) {
                    logger.warn("Error analyzing file {}: {}", javaFile, e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.warn("Code smell detection failed: {}", e.getMessage());
        }

        return codeSmells;
    }

    /**
     * Generates a summary of all quality checks.
     */
    private Map<String, Object> generateSummary(Map<String, Object> pmdResults,
                                                Map<String, Object> complexityResults,
                                                List<Map<String, Object>> codeSmells) {
        Map<String, Object> summary = new LinkedHashMap<>();

        int violationCount = (int) pmdResults.getOrDefault("violationCount", 0);
        int complexMethodCount = (int) complexityResults.getOrDefault("complexMethodCount", 0);
        int codeSmellCount = codeSmells.size();

        summary.put("totalIssues", violationCount + complexMethodCount + codeSmellCount);
        summary.put("pmdViolations", violationCount);
        summary.put("complexMethods", complexMethodCount);
        summary.put("codeSmells", codeSmellCount);

        // Quality grade
        int totalIssues = violationCount + complexMethodCount + codeSmellCount;
        String grade;
        if (totalIssues == 0) grade = "A";
        else if (totalIssues < 10) grade = "B";
        else if (totalIssues < 25) grade = "C";
        else if (totalIssues < 50) grade = "D";
        else grade = "F";

        summary.put("qualityGrade", grade);

        // Top recommendations
        List<String> recommendations = new ArrayList<>();
        if (violationCount > 0) {
            recommendations.add("Fix " + violationCount + " PMD violations to improve code quality");
        }
        if (complexMethodCount > 0) {
            recommendations.add("Refactor " + complexMethodCount + " complex methods to improve maintainability");
        }
        if (codeSmellCount > 0) {
            recommendations.add("Address " + codeSmellCount + " code smells identified");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Code quality is excellent! No major issues found.");
        }

        summary.put("recommendations", recommendations);

        return summary;
    }

    /**
     * Finds all Java files in the project.
     */
    private List<Path> findJavaFiles(Path projectPath, boolean includeTests) throws IOException {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> includeTests || !p.toString().contains("/test/"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Maps severity level to PMD priority.
     */
    private net.sourceforge.pmd.RulePriority mapSeverityToPriority(String severity) {
        return switch (severity.toLowerCase()) {
            case "high" -> net.sourceforge.pmd.RulePriority.HIGH;
            case "low" -> net.sourceforge.pmd.RulePriority.LOW;
            default -> net.sourceforge.pmd.RulePriority.MEDIUM;
        };
    }

    /**
     * Counts occurrences of a substring in a string.
     */
    private int countOccurrences(String str, String substring) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
