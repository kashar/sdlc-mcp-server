package com.example.mcp.tools;

import com.example.mcp.docs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for generating comprehensive technical documentation from code.
 *
 * <p>This tool provides automated documentation generation including:
 * <ul>
 *   <li>JavaDoc analysis and suggestions</li>
 *   <li>README generation from project analysis</li>
 *   <li>API documentation in Markdown format</li>
 *   <li>Changelog generation from Git history</li>
 * </ul>
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class GenerateDocumentationTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(GenerateDocumentationTool.class);

    @Override
    public String getName() {
        return "generate-documentation";
    }

    @Override
    public String getDescription() {
        return "Generates comprehensive technical documentation including JavaDoc analysis, README, " +
                "API docs, and changelogs based on code and Git history analysis. " +
                "Useful for the Documentor persona.";
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
                                "type", Map.of(
                                        "type", "string",
                                        "description", "Documentation type: javadoc-analysis, readme, api-docs, or changelog",
                                        "enum", List.of("javadoc-analysis", "readme", "api-docs", "changelog", "all")
                                ),
                                "packageFilter", Map.of(
                                        "type", "string",
                                        "description", "Package filter for API docs (optional, e.g., 'com.example.api')"
                                ),
                                "maxCommits", Map.of(
                                        "type", "number",
                                        "description", "Maximum commits for changelog (default: 100)"
                                ),
                                "outputFile", Map.of(
                                        "type", "boolean",
                                        "description", "Whether to write output to a file (default: false)"
                                )
                        ),
                        "required", List.of("path", "type")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String path = (String) arguments.get("path");
        String type = (String) arguments.get("type");
        String packageFilter = (String) arguments.get("packageFilter");
        int maxCommits = arguments.containsKey("maxCommits") ?
                ((Number) arguments.get("maxCommits")).intValue() : 100;
        boolean outputFile = arguments.containsKey("outputFile") &&
                (Boolean) arguments.get("outputFile");

        logger.info("Generating {} documentation for: {}", type, path);

        // Validate path
        File projectDir = new File(path);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid project path: " + path);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("path", path);
        result.put("type", type);

        switch (type) {
            case "javadoc-analysis":
                result.putAll(generateJavaDocAnalysis(path, outputFile));
                break;

            case "readme":
                result.putAll(generateReadme(path, outputFile));
                break;

            case "api-docs":
                result.putAll(generateApiDocs(path, packageFilter, outputFile));
                break;

            case "changelog":
                result.putAll(generateChangelog(path, maxCommits, outputFile));
                break;

            case "all":
                result.putAll(generateAllDocs(path, packageFilter, maxCommits, outputFile));
                break;

            default:
                throw new IllegalArgumentException("Unknown documentation type: " + type);
        }

        return result;
    }

    /**
     * Generates JavaDoc analysis.
     */
    private Map<String, Object> generateJavaDocAnalysis(String projectPath, boolean outputFile) throws Exception {
        JavaDocGenerator generator = new JavaDocGenerator();
        JavaDocGenerator.JavaDocAnalysis analysis = generator.analyzeProject(projectPath);

        Map<String, Object> result = new HashMap<>();
        result.put("totalFiles", analysis.totalFiles);
        result.put("totalClasses", analysis.totalClasses);
        result.put("totalMethods", analysis.totalMethods);
        result.put("totalFields", analysis.totalFields);
        result.put("undocumentedClasses", analysis.undocumentedClasses);
        result.put("undocumentedMethods", analysis.undocumentedMethods);
        result.put("undocumentedFields", analysis.undocumentedFields);
        result.put("coveragePercentage", String.format("%.2f%%", analysis.getCoveragePercentage()));

        // Add sample missing docs
        List<Map<String, String>> sampleMissingDocs = new ArrayList<>();
        int count = 0;
        for (JavaDocGenerator.MissingDoc doc : analysis.missingDocs) {
            if (count++ >= 10) break; // Limit to 10 examples
            sampleMissingDocs.add(Map.of(
                "file", doc.filePath,
                "element", doc.elementName,
                "type", doc.elementType,
                "suggestedDoc", doc.suggestedDoc
            ));
        }
        result.put("sampleMissingDocs", sampleMissingDocs);

        result.put("message", String.format(
            "JavaDoc coverage: %.2f%% (%d/%d items documented)",
            analysis.getCoveragePercentage(),
            (analysis.totalClasses + analysis.totalMethods + analysis.totalFields) -
                (analysis.undocumentedClasses + analysis.undocumentedMethods + analysis.undocumentedFields),
            analysis.totalClasses + analysis.totalMethods + analysis.totalFields
        ));

        return result;
    }

    /**
     * Generates README.
     */
    private Map<String, Object> generateReadme(String projectPath, boolean outputFile) throws Exception {
        ReadmeGenerator generator = new ReadmeGenerator();
        String readme = generator.generateReadme(projectPath);

        if (outputFile) {
            Path outputPath = Paths.get(projectPath, "README.md");
            Files.writeString(outputPath, readme);
            return Map.of(
                "content", readme,
                "outputFile", outputPath.toString(),
                "message", "README.md generated successfully at: " + outputPath
            );
        }

        return Map.of(
            "content", readme,
            "message", "README generated successfully (not saved to file)"
        );
    }

    /**
     * Generates API documentation.
     */
    private Map<String, Object> generateApiDocs(String projectPath, String packageFilter, boolean outputFile) throws Exception {
        ApiDocGenerator generator = new ApiDocGenerator();
        String apiDocs = generator.generateApiDocs(projectPath, packageFilter);

        if (outputFile) {
            Path outputPath = Paths.get(projectPath, "API.md");
            Files.writeString(outputPath, apiDocs);
            return Map.of(
                "content", apiDocs,
                "outputFile", outputPath.toString(),
                "packageFilter", packageFilter != null ? packageFilter : "all",
                "message", "API.md generated successfully at: " + outputPath
            );
        }

        return Map.of(
            "content", apiDocs,
            "packageFilter", packageFilter != null ? packageFilter : "all",
            "message", "API documentation generated successfully (not saved to file)"
        );
    }

    /**
     * Generates changelog.
     */
    private Map<String, Object> generateChangelog(String projectPath, int maxCommits, boolean outputFile) throws Exception {
        ChangelogGenerator generator = new ChangelogGenerator();
        String changelog = generator.generateChangelog(projectPath, maxCommits);

        if (outputFile) {
            Path outputPath = Paths.get(projectPath, "CHANGELOG.md");
            Files.writeString(outputPath, changelog);
            return Map.of(
                "content", changelog,
                "outputFile", outputPath.toString(),
                "maxCommits", maxCommits,
                "message", "CHANGELOG.md generated successfully at: " + outputPath
            );
        }

        return Map.of(
            "content", changelog,
            "maxCommits", maxCommits,
            "message", "Changelog generated successfully (not saved to file)"
        );
    }

    /**
     * Generates all documentation types.
     */
    private Map<String, Object> generateAllDocs(String projectPath, String packageFilter,
                                                 int maxCommits, boolean outputFile) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try {
            result.put("javadocAnalysis", generateJavaDocAnalysis(projectPath, false));
        } catch (Exception e) {
            logger.warn("Failed to generate JavaDoc analysis", e);
            result.put("javadocAnalysis", Map.of("error", e.getMessage()));
        }

        try {
            result.put("readme", generateReadme(projectPath, outputFile));
        } catch (Exception e) {
            logger.warn("Failed to generate README", e);
            result.put("readme", Map.of("error", e.getMessage()));
        }

        try {
            result.put("apiDocs", generateApiDocs(projectPath, packageFilter, outputFile));
        } catch (Exception e) {
            logger.warn("Failed to generate API docs", e);
            result.put("apiDocs", Map.of("error", e.getMessage()));
        }

        try {
            result.put("changelog", generateChangelog(projectPath, maxCommits, outputFile));
        } catch (Exception e) {
            logger.warn("Failed to generate changelog", e);
            result.put("changelog", Map.of("error", e.getMessage()));
        }

        result.put("message", "All documentation generated successfully");
        return result;
    }
}
