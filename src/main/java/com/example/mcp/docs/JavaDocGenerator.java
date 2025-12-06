package com.example.mcp.docs;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generates JavaDoc documentation for Java source files.
 *
 * <p>This generator analyzes Java source code and creates comprehensive
 * JavaDoc comments for classes, methods, and fields that are missing documentation.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class JavaDocGenerator {

    private static final Logger logger = LoggerFactory.getLogger(JavaDocGenerator.class);

    /**
     * Analyzes a Java project and generates JavaDoc documentation.
     *
     * @param projectPath path to the project root
     * @return documentation analysis result
     * @throws IOException if file reading fails
     */
    public JavaDocAnalysis analyzeProject(String projectPath) throws IOException {
        Path basePath = Paths.get(projectPath);
        JavaDocAnalysis analysis = new JavaDocAnalysis();

        // Find all Java source files
        try (Stream<Path> paths = Files.walk(basePath)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> path.toString().contains("/src/main/java/"))
                 .forEach(path -> {
                     try {
                         analyzeFile(path, analysis);
                     } catch (IOException e) {
                         logger.error("Failed to analyze file: {}", path, e);
                     }
                 });
        }

        return analysis;
    }

    /**
     * Analyzes a single Java file for documentation coverage.
     */
    private void analyzeFile(Path filePath, JavaDocAnalysis analysis) throws IOException {
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = parser.parse(filePath);

        if (!result.isSuccessful()) {
            logger.warn("Failed to parse: {}", filePath);
            return;
        }

        CompilationUnit cu = result.getResult().orElse(null);
        if (cu == null) return;

        String relativePath = filePath.toString();
        analysis.totalFiles++;

        // Analyze classes
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            analysis.totalClasses++;
            if (!cls.getJavadoc().isPresent()) {
                analysis.undocumentedClasses++;
                analysis.missingDocs.add(new MissingDoc(
                    relativePath,
                    cls.getNameAsString(),
                    "class",
                    generateClassJavaDoc(cls)
                ));
            }
        });

        // Analyze methods
        cu.findAll(MethodDeclaration.class).forEach(method -> {
            analysis.totalMethods++;
            if (!method.getJavadoc().isPresent() && method.isPublic()) {
                analysis.undocumentedMethods++;
                analysis.missingDocs.add(new MissingDoc(
                    relativePath,
                    method.getSignature().toString(),
                    "method",
                    generateMethodJavaDoc(method)
                ));
            }
        });

        // Analyze fields
        cu.findAll(FieldDeclaration.class).forEach(field -> {
            analysis.totalFields++;
            if (!field.getJavadoc().isPresent() && field.isPublic()) {
                analysis.undocumentedFields++;
            }
        });
    }

    /**
     * Generates JavaDoc comment for a class.
     */
    private String generateClassJavaDoc(ClassOrInterfaceDeclaration cls) {
        StringBuilder doc = new StringBuilder();
        doc.append("/**\n");
        doc.append(" * ").append(cls.getNameAsString());

        if (cls.isInterface()) {
            doc.append(" interface.\n");
        } else {
            doc.append(" class.\n");
        }

        doc.append(" *\n");
        doc.append(" * <p>TODO: Add detailed description\n");
        doc.append(" *\n");
        doc.append(" * @author SDLC Tools Team\n");
        doc.append(" * @version 2.0.0\n");
        doc.append(" */");

        return doc.toString();
    }

    /**
     * Generates JavaDoc comment for a method.
     */
    private String generateMethodJavaDoc(MethodDeclaration method) {
        StringBuilder doc = new StringBuilder();
        doc.append("/**\n");
        doc.append(" * ").append(method.getNameAsString()).append(".\n");
        doc.append(" *\n");
        doc.append(" * <p>TODO: Add detailed description\n");
        doc.append(" *\n");

        // Add parameter documentation
        method.getParameters().forEach(param -> {
            doc.append(" * @param ").append(param.getNameAsString())
               .append(" TODO: describe parameter\n");
        });

        // Add return documentation
        if (!method.getType().isVoidType()) {
            doc.append(" * @return TODO: describe return value\n");
        }

        // Add throws documentation
        method.getThrownExceptions().forEach(ex -> {
            doc.append(" * @throws ").append(ex.asString())
               .append(" TODO: describe exception\n");
        });

        doc.append(" */");

        return doc.toString();
    }

    /**
     * Result of JavaDoc analysis.
     */
    public static class JavaDocAnalysis {
        public int totalFiles = 0;
        public int totalClasses = 0;
        public int totalMethods = 0;
        public int totalFields = 0;
        public int undocumentedClasses = 0;
        public int undocumentedMethods = 0;
        public int undocumentedFields = 0;
        public List<MissingDoc> missingDocs = new ArrayList<>();

        public double getCoveragePercentage() {
            int total = totalClasses + totalMethods + totalFields;
            int documented = total - (undocumentedClasses + undocumentedMethods + undocumentedFields);
            return total > 0 ? (documented * 100.0 / total) : 100.0;
        }
    }

    /**
     * Represents a missing documentation item.
     */
    public static class MissingDoc {
        public final String filePath;
        public final String elementName;
        public final String elementType;
        public final String suggestedDoc;

        public MissingDoc(String filePath, String elementName, String elementType, String suggestedDoc) {
            this.filePath = filePath;
            this.elementName = elementName;
            this.elementType = elementType;
            this.suggestedDoc = suggestedDoc;
        }
    }
}
