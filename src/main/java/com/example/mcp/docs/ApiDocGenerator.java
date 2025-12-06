package com.example.mcp.docs;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Generates API documentation in Markdown format.
 *
 * <p>This generator analyzes Java source code and creates comprehensive
 * API documentation with method signatures, parameters, and return types.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class ApiDocGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ApiDocGenerator.class);

    /**
     * Generates API documentation for a Maven project.
     *
     * @param projectPath path to the project root
     * @param packageFilter package to filter (null for all packages)
     * @return generated API documentation
     * @throws IOException if file reading fails
     */
    public String generateApiDocs(String projectPath, String packageFilter) throws IOException {
        Path basePath = Paths.get(projectPath);
        List<ApiClass> apiClasses = new ArrayList<>();

        // Find all Java source files
        try (Stream<Path> paths = Files.walk(basePath)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> path.toString().contains("/src/main/java/"))
                 .forEach(path -> {
                     try {
                         ApiClass apiClass = parseClass(path, packageFilter);
                         if (apiClass != null) {
                             apiClasses.add(apiClass);
                         }
                     } catch (IOException e) {
                         logger.error("Failed to parse file: {}", path, e);
                     }
                 });
        }

        // Sort by package and class name
        apiClasses.sort(Comparator.comparing((ApiClass c) -> c.packageName)
                                  .thenComparing(c -> c.className));

        return buildApiDocumentation(apiClasses);
    }

    /**
     * Parses a Java class file.
     */
    private ApiClass parseClass(Path filePath, String packageFilter) throws IOException {
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = parser.parse(filePath);

        if (!result.isSuccessful()) {
            return null;
        }

        CompilationUnit cu = result.getResult().orElse(null);
        if (cu == null) return null;

        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        // Filter by package if specified
        if (packageFilter != null && !packageName.startsWith(packageFilter)) {
            return null;
        }

        // Parse classes
        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        if (classes.isEmpty()) return null;

        ClassOrInterfaceDeclaration cls = classes.get(0);
        if (!cls.isPublic()) return null; // Only document public classes

        ApiClass apiClass = new ApiClass();
        apiClass.packageName = packageName;
        apiClass.className = cls.getNameAsString();
        apiClass.isInterface = cls.isInterface();
        apiClass.javadoc = cls.getJavadoc()
                .map(jd -> jd.getDescription().toText())
                .orElse("");

        // Parse public methods
        cls.getMethods().stream()
           .filter(m -> m.isPublic())
           .forEach(method -> {
               ApiMethod apiMethod = parseMethod(method);
               apiClass.methods.add(apiMethod);
           });

        return apiClass;
    }

    /**
     * Parses a method declaration.
     */
    private ApiMethod parseMethod(MethodDeclaration method) {
        ApiMethod apiMethod = new ApiMethod();
        apiMethod.name = method.getNameAsString();
        apiMethod.returnType = method.getTypeAsString();
        apiMethod.javadoc = method.getJavadoc()
                .map(jd -> jd.getDescription().toText())
                .orElse("");

        // Parse parameters
        for (Parameter param : method.getParameters()) {
            ApiParameter apiParam = new ApiParameter();
            apiParam.name = param.getNameAsString();
            apiParam.type = param.getTypeAsString();
            apiMethod.parameters.add(apiParam);
        }

        // Parse exceptions
        method.getThrownExceptions().forEach(ex -> {
            apiMethod.exceptions.add(ex.asString());
        });

        return apiMethod;
    }

    /**
     * Builds the API documentation in Markdown format.
     */
    private String buildApiDocumentation(List<ApiClass> apiClasses) {
        StringBuilder doc = new StringBuilder();

        // Header
        doc.append("# API Documentation\n\n");
        doc.append("This document provides detailed API documentation for all public classes and methods.\n\n");

        // Table of Contents
        doc.append("## Table of Contents\n\n");
        String currentPackage = "";
        for (ApiClass cls : apiClasses) {
            if (!cls.packageName.equals(currentPackage)) {
                currentPackage = cls.packageName;
                doc.append("### ").append(currentPackage).append("\n\n");
            }
            doc.append("- [").append(cls.className).append("](#")
               .append(cls.className.toLowerCase()).append(")\n");
        }
        doc.append("\n");

        // Class documentation
        currentPackage = "";
        for (ApiClass cls : apiClasses) {
            if (!cls.packageName.equals(currentPackage)) {
                currentPackage = cls.packageName;
                doc.append("## Package: `").append(currentPackage).append("`\n\n");
            }

            // Class header
            doc.append("### ").append(cls.className).append("\n\n");
            doc.append("**Type:** ").append(cls.isInterface ? "Interface" : "Class").append("\n\n");

            if (!cls.javadoc.isEmpty()) {
                doc.append(cls.javadoc).append("\n\n");
            }

            // Methods
            if (!cls.methods.isEmpty()) {
                doc.append("#### Methods\n\n");

                for (ApiMethod method : cls.methods) {
                    doc.append("##### `").append(method.name).append("`\n\n");

                    // Signature
                    doc.append("```java\n");
                    doc.append(method.returnType).append(" ").append(method.name).append("(");

                    for (int i = 0; i < method.parameters.size(); i++) {
                        ApiParameter param = method.parameters.get(i);
                        if (i > 0) doc.append(", ");
                        doc.append(param.type).append(" ").append(param.name);
                    }

                    doc.append(")");

                    if (!method.exceptions.isEmpty()) {
                        doc.append(" throws ");
                        doc.append(String.join(", ", method.exceptions));
                    }

                    doc.append("\n```\n\n");

                    // Description
                    if (!method.javadoc.isEmpty()) {
                        doc.append(method.javadoc).append("\n\n");
                    }

                    // Parameters
                    if (!method.parameters.isEmpty()) {
                        doc.append("**Parameters:**\n\n");
                        for (ApiParameter param : method.parameters) {
                            doc.append("- `").append(param.name).append("` (")
                               .append(param.type).append("): TODO: describe parameter\n");
                        }
                        doc.append("\n");
                    }

                    // Return value
                    if (!"void".equals(method.returnType)) {
                        doc.append("**Returns:** `").append(method.returnType)
                           .append("` - TODO: describe return value\n\n");
                    }

                    // Exceptions
                    if (!method.exceptions.isEmpty()) {
                        doc.append("**Throws:**\n\n");
                        for (String exception : method.exceptions) {
                            doc.append("- `").append(exception).append("`: TODO: describe exception\n");
                        }
                        doc.append("\n");
                    }
                }
            }

            doc.append("---\n\n");
        }

        return doc.toString();
    }

    /**
     * Represents a documented class.
     */
    private static class ApiClass {
        String packageName;
        String className;
        boolean isInterface;
        String javadoc;
        List<ApiMethod> methods = new ArrayList<>();
    }

    /**
     * Represents a documented method.
     */
    private static class ApiMethod {
        String name;
        String returnType;
        String javadoc;
        List<ApiParameter> parameters = new ArrayList<>();
        List<String> exceptions = new ArrayList<>();
    }

    /**
     * Represents a method parameter.
     */
    private static class ApiParameter {
        String name;
        String type;
    }
}
