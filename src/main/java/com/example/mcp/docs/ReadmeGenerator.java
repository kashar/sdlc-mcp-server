package com.example.mcp.docs;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generates README.md files based on Maven project analysis.
 *
 * <p>This generator analyzes a Maven project and creates a comprehensive
 * README with project description, build instructions, and usage examples.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class ReadmeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ReadmeGenerator.class);

    /**
     * Generates a README for a Maven project.
     *
     * @param projectPath path to the project root
     * @return generated README content
     * @throws Exception if generation fails
     */
    public String generateReadme(String projectPath) throws Exception {
        Path basePath = Paths.get(projectPath);
        Path pomPath = basePath.resolve("pom.xml");

        if (!Files.exists(pomPath)) {
            throw new IllegalArgumentException("No pom.xml found at: " + projectPath);
        }

        // Parse POM
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try (FileReader fileReader = new FileReader(pomPath.toFile())) {
            model = reader.read(fileReader);
        }

        // Analyze project structure
        ProjectStructure structure = analyzeStructure(basePath);

        // Generate README
        return buildReadme(model, structure, basePath);
    }

    /**
     * Analyzes the project structure.
     */
    private ProjectStructure analyzeStructure(Path basePath) {
        ProjectStructure structure = new ProjectStructure();

        // Check for main source
        Path mainJava = basePath.resolve("src/main/java");
        if (Files.exists(mainJava)) {
            structure.hasMainSource = true;
            try (Stream<Path> paths = Files.walk(mainJava)) {
                structure.sourceFileCount = paths.filter(p -> p.toString().endsWith(".java")).count();
            } catch (Exception e) {
                logger.warn("Failed to count source files", e);
            }
        }

        // Check for tests
        Path testJava = basePath.resolve("src/test/java");
        if (Files.exists(testJava)) {
            structure.hasTests = true;
            try (Stream<Path> paths = Files.walk(testJava)) {
                structure.testFileCount = paths.filter(p -> p.toString().endsWith(".java")).count();
            } catch (Exception e) {
                logger.warn("Failed to count test files", e);
            }
        }

        // Check for resources
        structure.hasResources = Files.exists(basePath.resolve("src/main/resources"));

        // Check for Docker
        structure.hasDocker = Files.exists(basePath.resolve("Dockerfile"));

        // Check for CI/CD
        structure.hasGithubActions = Files.exists(basePath.resolve(".github/workflows"));
        structure.hasGitlabCI = Files.exists(basePath.resolve(".gitlab-ci.yml"));

        return structure;
    }

    /**
     * Builds the README content.
     */
    private String buildReadme(Model model, ProjectStructure structure, Path basePath) {
        StringBuilder readme = new StringBuilder();

        // Title
        String projectName = model.getName() != null ? model.getName() : model.getArtifactId();
        readme.append("# ").append(projectName).append("\n\n");

        // Description
        if (model.getDescription() != null && !model.getDescription().isEmpty()) {
            readme.append(model.getDescription()).append("\n\n");
        } else {
            readme.append("TODO: Add project description\n\n");
        }

        // Badges (optional)
        readme.append("[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]");
        readme.append("(#) ");
        readme.append("[![License](https://img.shields.io/badge/license-MIT-blue.svg)]");
        readme.append("(LICENSE)\n\n");

        // Table of Contents
        readme.append("## Table of Contents\n\n");
        readme.append("- [Overview](#overview)\n");
        readme.append("- [Features](#features)\n");
        readme.append("- [Getting Started](#getting-started)\n");
        readme.append("- [Usage](#usage)\n");
        readme.append("- [Building](#building)\n");
        if (structure.hasTests) {
            readme.append("- [Testing](#testing)\n");
        }
        if (structure.hasDocker) {
            readme.append("- [Docker](#docker)\n");
        }
        readme.append("- [Contributing](#contributing)\n");
        readme.append("- [License](#license)\n\n");

        // Overview
        readme.append("## Overview\n\n");
        readme.append("This project is built with Maven and uses Java ");
        readme.append(getJavaVersion(model)).append(".\n\n");
        readme.append("**Project Coordinates:**\n");
        readme.append("```xml\n");
        readme.append("<dependency>\n");
        readme.append("    <groupId>").append(model.getGroupId()).append("</groupId>\n");
        readme.append("    <artifactId>").append(model.getArtifactId()).append("</artifactId>\n");
        readme.append("    <version>").append(model.getVersion()).append("</version>\n");
        readme.append("</dependency>\n");
        readme.append("```\n\n");

        // Features
        readme.append("## Features\n\n");
        readme.append("- ✅ Feature 1: TODO\n");
        readme.append("- ✅ Feature 2: TODO\n");
        readme.append("- ✅ Feature 3: TODO\n\n");

        // Getting Started
        readme.append("## Getting Started\n\n");
        readme.append("### Prerequisites\n\n");
        readme.append("- Java ").append(getJavaVersion(model)).append(" or higher\n");
        readme.append("- Maven 3.6+\n\n");

        readme.append("### Installation\n\n");
        readme.append("```bash\n");
        readme.append("git clone <repository-url>\n");
        readme.append("cd ").append(basePath.getFileName()).append("\n");
        readme.append("mvn clean install\n");
        readme.append("```\n\n");

        // Usage
        readme.append("## Usage\n\n");
        readme.append("```java\n");
        readme.append("// TODO: Add usage examples\n");
        readme.append("```\n\n");

        // Building
        readme.append("## Building\n\n");
        readme.append("```bash\n");
        readme.append("# Compile\n");
        readme.append("mvn clean compile\n\n");
        readme.append("# Package\n");
        readme.append("mvn clean package\n\n");
        readme.append("# Install to local repository\n");
        readme.append("mvn clean install\n");
        readme.append("```\n\n");

        // Testing
        if (structure.hasTests) {
            readme.append("## Testing\n\n");
            readme.append("```bash\n");
            readme.append("# Run all tests\n");
            readme.append("mvn test\n\n");
            readme.append("# Run with coverage\n");
            readme.append("mvn clean verify\n\n");
            readme.append("# View coverage report\n");
            readme.append("open target/site/jacoco/index.html\n");
            readme.append("```\n\n");
        }

        // Docker
        if (structure.hasDocker) {
            readme.append("## Docker\n\n");
            readme.append("```bash\n");
            readme.append("# Build image\n");
            readme.append("docker build -t ").append(model.getArtifactId()).append(" .\n\n");
            readme.append("# Run container\n");
            readme.append("docker run -p 8080:8080 ").append(model.getArtifactId()).append("\n");
            readme.append("```\n\n");
        }

        // Contributing
        readme.append("## Contributing\n\n");
        readme.append("1. Fork the repository\n");
        readme.append("2. Create your feature branch (`git checkout -b feature/amazing-feature`)\n");
        readme.append("3. Commit your changes (`git commit -m 'Add some amazing feature'`)\n");
        readme.append("4. Push to the branch (`git push origin feature/amazing-feature`)\n");
        readme.append("5. Open a Pull Request\n\n");

        // License
        readme.append("## License\n\n");
        if (model.getLicenses() != null && !model.getLicenses().isEmpty()) {
            String license = model.getLicenses().get(0).getName();
            readme.append("This project is licensed under the ").append(license).append(" - see the LICENSE file for details.\n");
        } else {
            readme.append("TODO: Add license information\n");
        }

        return readme.toString();
    }

    /**
     * Extracts Java version from Maven model.
     */
    private String getJavaVersion(Model model) {
        if (model.getProperties() != null) {
            String version = model.getProperties().getProperty("maven.compiler.source");
            if (version != null) return version;

            version = model.getProperties().getProperty("maven.compiler.target");
            if (version != null) return version;
        }
        return "17";
    }

    /**
     * Project structure information.
     */
    private static class ProjectStructure {
        boolean hasMainSource = false;
        boolean hasTests = false;
        boolean hasResources = false;
        boolean hasDocker = false;
        boolean hasGithubActions = false;
        boolean hasGitlabCI = false;
        long sourceFileCount = 0;
        long testFileCount = 0;
    }
}
