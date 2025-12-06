package com.example.mcp.tools;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tool for automated bug detection and fixing in Maven projects.
 *
 * <p>This tool analyzes bug descriptions, stack traces, and code to:
 * <ul>
 *   <li>Locate potential bug locations</li>
 *   <li>Identify common bug patterns</li>
 *   <li>Suggest or apply fixes</li>
 *   <li>Generate regression tests</li>
 *   <li>Verify fixes don't break existing functionality</li>
 * </ul>
 *
 * @author Maven SDLC Team
 * @version 1.0.0
 */
public class FixBugTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(FixBugTool.class);
    private final JavaParser javaParser = new JavaParser();

    @Override
    public String getName() {
        return "fix-bug";
    }

    @Override
    public String getDescription() {
        return "Analyzes bug descriptions and stack traces to locate, identify, and suggest fixes for bugs. " +
                "Can detect common bug patterns like NullPointerException, ArrayIndexOutOfBounds, " +
                "resource leaks, and logic errors. Generates regression tests to prevent recurrence.";
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
                                "bugDescription", Map.of(
                                        "type", "string",
                                        "description", "Description of the bug"
                                ),
                                "stackTrace", Map.of(
                                        "type", "string",
                                        "description", "Optional: stack trace if available"
                                ),
                                "affectedFile", Map.of(
                                        "type", "string",
                                        "description", "Optional: specific file where bug occurs"
                                ),
                                "generateTest", Map.of(
                                        "type", "boolean",
                                        "description", "Generate regression test (default: true)"
                                )
                        ),
                        "required", List.of("path", "bugDescription")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) throws Exception {
        String path = (String) arguments.get("path");
        String bugDescription = (String) arguments.get("bugDescription");
        String stackTrace = (String) arguments.getOrDefault("stackTrace", "");
        String affectedFile = (String) arguments.getOrDefault("affectedFile", "");
        boolean generateTest = (boolean) arguments.getOrDefault("generateTest", true);

        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path parameter is required");
        }
        if (bugDescription == null || bugDescription.isEmpty()) {
            throw new IllegalArgumentException("bugDescription parameter is required");
        }

        Path projectPath = Paths.get(path);
        if (!Files.exists(projectPath)) {
            throw new IllegalArgumentException("Project path does not exist: " + path);
        }

        logger.info("Analyzing bug in {}: {}", path, bugDescription);

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("projectPath", path);
        results.put("bugDescription", bugDescription);
        results.put("timestamp", new Date().toString());

        try {
            // Parse stack trace to find affected files
            List<Map<String, Object>> stackFrames = parseStackTrace(stackTrace);
            results.put("stackFrames", stackFrames);

            // Identify bug type
            String bugType = identifyBugType(bugDescription, stackTrace);
            results.put("bugType", bugType);

            // Locate potential bug locations
            List<Map<String, Object>> potentialLocations = locateBugLocations(
                    projectPath, bugDescription, stackFrames, affectedFile);
            results.put("potentialLocations", potentialLocations);

            // Analyze common bug patterns
            List<Map<String, Object>> bugPatterns = analyzeBugPatterns(
                    projectPath, bugType, potentialLocations);
            results.put("detectedPatterns", bugPatterns);

            // Generate fix suggestions
            List<Map<String, Object>> fixSuggestions = generateFixSuggestions(
                    bugType, bugDescription, potentialLocations, bugPatterns);
            results.put("fixSuggestions", fixSuggestions);

            // Generate regression test if requested
            if (generateTest) {
                Map<String, Object> regressionTest = generateRegressionTest(
                        bugDescription, bugType, potentialLocations);
                results.put("regressionTest", regressionTest);
            }

            // Summary
            results.put("summary", Map.of(
                    "bugType", bugType,
                    "locationsFound", potentialLocations.size(),
                    "patternsDetected", bugPatterns.size(),
                    "fixSuggestionsCount", fixSuggestions.size(),
                    "confidence", calculateConfidence(potentialLocations, bugPatterns)
            ));

            return Map.of(
                    "success", true,
                    "results", results
            );

        } catch (Exception e) {
            logger.error("Error during bug analysis", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "recommendations", List.of(
                            "Provide more specific bug description",
                            "Include stack trace if available",
                            "Specify affected file to narrow search"
                    )
            );
        }
    }

    /**
     * Parses stack trace to extract file and line information.
     */
    private List<Map<String, Object>> parseStackTrace(String stackTrace) {
        List<Map<String, Object>> frames = new ArrayList<>();

        if (stackTrace == null || stackTrace.isEmpty()) {
            return frames;
        }

        // Pattern: at com.example.Class.method(Class.java:123)
        Pattern pattern = Pattern.compile("at\\s+([\\w.]+)\\(([\\w.]+):(\\d+)\\)");
        Matcher matcher = pattern.matcher(stackTrace);

        while (matcher.find()) {
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("fullClass", matcher.group(1));
            frame.put("file", matcher.group(2));
            frame.put("line", Integer.parseInt(matcher.group(3)));
            frames.add(frame);
        }

        return frames;
    }

    /**
     * Identifies the type of bug from description and stack trace.
     */
    private String identifyBugType(String description, String stackTrace) {
        String combined = (description + " " + stackTrace).toLowerCase();

        if (combined.contains("nullpointerexception") || combined.contains("null pointer")) {
            return "NullPointerException";
        }
        if (combined.contains("arrayindexoutofbounds") || combined.contains("index out of bounds")) {
            return "ArrayIndexOutOfBoundsException";
        }
        if (combined.contains("classcastexception")) {
            return "ClassCastException";
        }
        if (combined.contains("concurrentmodificationexception")) {
            return "ConcurrentModificationException";
        }
        if (combined.contains("stackoverflow")) {
            return "StackOverflowError";
        }
        if (combined.contains("memory") || combined.contains("outofmemory")) {
            return "OutOfMemoryError";
        }
        if (combined.contains("deadlock")) {
            return "Deadlock";
        }
        if (combined.contains("resource leak") || combined.contains("not closed")) {
            return "ResourceLeak";
        }
        if (combined.contains("wrong") || combined.contains("incorrect") || combined.contains("unexpected")) {
            return "LogicError";
        }

        return "Unknown";
    }

    /**
     * Locates potential bug locations in the codebase.
     */
    private List<Map<String, Object>> locateBugLocations(Path projectPath,
                                                         String description,
                                                         List<Map<String, Object>> stackFrames,
                                                         String affectedFile) throws IOException {
        List<Map<String, Object>> locations = new ArrayList<>();

        // If stack trace provided, use it
        if (!stackFrames.isEmpty()) {
            for (Map<String, Object> frame : stackFrames) {
                String fileName = (String) frame.get("file");
                List<Path> matchingFiles = findFilesByName(projectPath, fileName);

                for (Path file : matchingFiles) {
                    Map<String, Object> location = new LinkedHashMap<>();
                    location.put("file", file.toString());
                    location.put("line", frame.get("line"));
                    location.put("confidence", "high");
                    location.put("source", "stackTrace");
                    locations.add(location);
                }
            }
        }

        // If affected file specified
        if (affectedFile != null && !affectedFile.isEmpty()) {
            Path file = projectPath.resolve(affectedFile);
            if (Files.exists(file)) {
                Map<String, Object> location = new LinkedHashMap<>();
                location.put("file", file.toString());
                location.put("confidence", "high");
                location.put("source", "specified");
                locations.add(location);
            }
        }

        // Search by keywords in description
        if (locations.isEmpty()) {
            locations.addAll(searchByKeywords(projectPath, description));
        }

        return locations.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * Searches for files containing keywords from bug description.
     */
    private List<Map<String, Object>> searchByKeywords(Path projectPath, String description) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();

        // Extract potential class/method names from description
        Pattern wordPattern = Pattern.compile("\\b[A-Z][a-zA-Z0-9]*\\b");
        Matcher matcher = wordPattern.matcher(description);

        Set<String> keywords = new HashSet<>();
        while (matcher.find()) {
            keywords.add(matcher.group());
        }

        if (keywords.isEmpty()) {
            return results;
        }

        List<Path> javaFiles = findJavaFiles(projectPath);

        for (Path javaFile : javaFiles) {
            String fileName = javaFile.getFileName().toString();

            for (String keyword : keywords) {
                if (fileName.contains(keyword)) {
                    Map<String, Object> location = new LinkedHashMap<>();
                    location.put("file", javaFile.toString());
                    location.put("confidence", "medium");
                    location.put("source", "keyword: " + keyword);
                    results.add(location);
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Analyzes code for common bug patterns.
     */
    private List<Map<String, Object>> analyzeBugPatterns(Path projectPath,
                                                         String bugType,
                                                         List<Map<String, Object>> locations) {
        List<Map<String, Object>> patterns = new ArrayList<>();

        for (Map<String, Object> location : locations) {
            String filePath = (String) location.get("file");

            try {
                Path file = Paths.get(filePath);
                if (!Files.exists(file)) continue;

                CompilationUnit cu = javaParser.parse(file).getResult().orElse(null);
                if (cu == null) continue;

                String content = Files.readString(file);

                // Check for null pointer patterns
                if (bugType.equals("NullPointerException")) {
                    if (content.contains(".get(") && !content.contains("!= null")) {
                        patterns.add(createPattern(filePath, "Missing null check before .get()", "high"));
                    }
                    if (content.matches(".*\\.\\w+\\(.*\\).*") && !content.contains("@NonNull")) {
                        patterns.add(createPattern(filePath, "Method calls without null safety", "medium"));
                    }
                }

                // Check for resource leak patterns
                if (bugType.equals("ResourceLeak")) {
                    if (content.contains("new FileInputStream") && !content.contains("try-with-resources")) {
                        patterns.add(createPattern(filePath, "File stream without try-with-resources", "high"));
                    }
                }

                // Check for concurrency issues
                if (bugType.equals("ConcurrentModificationException")) {
                    if (content.contains("iterator()") && content.contains(".remove(")) {
                        patterns.add(createPattern(filePath, "Collection modified during iteration", "high"));
                    }
                }

            } catch (Exception e) {
                logger.debug("Error analyzing patterns in {}: {}", filePath, e.getMessage());
            }
        }

        return patterns;
    }

    /**
     * Generates fix suggestions based on bug analysis.
     */
    private List<Map<String, Object>> generateFixSuggestions(String bugType,
                                                             String description,
                                                             List<Map<String, Object>> locations,
                                                             List<Map<String, Object>> patterns) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        switch (bugType) {
            case "NullPointerException":
                suggestions.add(createSuggestion(
                        "Add null checks",
                        "Add null safety checks before dereferencing objects",
                        "if (object != null) { object.method(); }",
                        "high"
                ));
                suggestions.add(createSuggestion(
                        "Use Optional",
                        "Use Java Optional to handle null values safely",
                        "Optional.ofNullable(value).map(v -> v.method()).orElse(default)",
                        "medium"
                ));
                break;

            case "ArrayIndexOutOfBoundsException":
                suggestions.add(createSuggestion(
                        "Add bounds checking",
                        "Validate array index before access",
                        "if (index >= 0 && index < array.length) { array[index] }",
                        "high"
                ));
                break;

            case "ResourceLeak":
                suggestions.add(createSuggestion(
                        "Use try-with-resources",
                        "Ensure resources are properly closed",
                        "try (FileInputStream fis = new FileInputStream(file)) { ... }",
                        "high"
                ));
                break;

            case "ConcurrentModificationException":
                suggestions.add(createSuggestion(
                        "Use Iterator.remove()",
                        "Use iterator's remove method instead of collection's",
                        "Iterator<T> it = list.iterator(); while(it.hasNext()) { if(condition) it.remove(); }",
                        "high"
                ));
                break;

            case "LogicError":
                suggestions.add(createSuggestion(
                        "Review business logic",
                        "Verify the algorithm and edge cases",
                        "Add logging and validation to identify the logic flaw",
                        "medium"
                ));
                break;

            default:
                suggestions.add(createSuggestion(
                        "Add defensive programming",
                        "Add validation and error handling",
                        "Validate inputs and handle edge cases",
                        "medium"
                ));
        }

        // Add pattern-specific suggestions
        for (Map<String, Object> pattern : patterns) {
            suggestions.add(createSuggestion(
                    "Fix pattern: " + pattern.get("description"),
                    "Address the detected code smell",
                    "See location: " + pattern.get("file"),
                    (String) pattern.get("severity")
            ));
        }

        return suggestions;
    }

    /**
     * Generates a regression test for the bug.
     */
    private Map<String, Object> generateRegressionTest(String description,
                                                       String bugType,
                                                       List<Map<String, Object>> locations) {
        StringBuilder testCode = new StringBuilder();

        testCode.append("@Test\n");
        testCode.append("void testBugFix_").append(bugType.replaceAll("[^A-Za-z0-9]", "")).append("() {\n");
        testCode.append("    // Regression test for: ").append(description).append("\n");
        testCode.append("    // Bug type: ").append(bugType).append("\n\n");

        if (bugType.equals("NullPointerException")) {
            testCode.append("    // Test with null input\n");
            testCode.append("    assertDoesNotThrow(() -> methodUnderTest(null));\n");
        } else if (bugType.equals("ArrayIndexOutOfBoundsException")) {
            testCode.append("    // Test with boundary conditions\n");
            testCode.append("    assertDoesNotThrow(() -> methodUnderTest(0));\n");
            testCode.append("    assertDoesNotThrow(() -> methodUnderTest(array.length - 1));\n");
        } else {
            testCode.append("    // TODO: Add specific test for bug scenario\n");
            testCode.append("    assertNotNull(result);\n");
        }

        testCode.append("}\n");

        return Map.of(
                "testMethod", testCode.toString(),
                "description", "Regression test to prevent bug recurrence",
                "recommendation", "Add this test to the relevant test class"
        );
    }

    /**
     * Calculates confidence in bug analysis.
     */
    private String calculateConfidence(List<Map<String, Object>> locations,
                                      List<Map<String, Object>> patterns) {
        if (locations.isEmpty()) return "low";
        if (patterns.size() >= 2) return "high";
        if (locations.size() >= 1 && !patterns.isEmpty()) return "medium";
        return "low";
    }

    private Map<String, Object> createPattern(String file, String description, String severity) {
        Map<String, Object> pattern = new LinkedHashMap<>();
        pattern.put("file", file);
        pattern.put("description", description);
        pattern.put("severity", severity);
        return pattern;
    }

    private Map<String, Object> createSuggestion(String title, String description, String example, String priority) {
        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("title", title);
        suggestion.put("description", description);
        suggestion.put("example", example);
        suggestion.put("priority", priority);
        return suggestion;
    }

    private List<Path> findJavaFiles(Path projectPath) throws IOException {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .collect(Collectors.toList());
        }
    }

    private List<Path> findFilesByName(Path projectPath, String fileName) throws IOException {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .filter(p -> !p.toString().contains("/target/"))
                    .collect(Collectors.toList());
        }
    }
}
