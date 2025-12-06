package com.example.mcp.docs;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Generates CHANGELOG.md files from Git commit history.
 *
 * <p>This generator analyzes Git history and creates a structured changelog
 * following the Keep a Changelog format.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class ChangelogGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ChangelogGenerator.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Generates a changelog from Git history.
     *
     * @param projectPath path to the Git repository
     * @param maxCommits maximum number of commits to include
     * @return generated changelog content
     * @throws Exception if generation fails
     */
    public String generateChangelog(String projectPath, int maxCommits) throws Exception {
        Path basePath = Paths.get(projectPath);
        File gitDir = basePath.resolve(".git").toFile();

        if (!gitDir.exists()) {
            throw new IllegalArgumentException("No Git repository found at: " + projectPath);
        }

        Repository repository = new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .build();

        try (Git git = new Git(repository)) {
            List<RevCommit> commits = new ArrayList<>();

            // Get commits
            Iterable<RevCommit> log = git.log().setMaxCount(maxCommits).call();
            for (RevCommit commit : log) {
                commits.add(commit);
            }

            // Group commits by type
            Map<String, List<CommitInfo>> groupedCommits = groupCommitsByType(commits);

            // Build changelog
            return buildChangelog(groupedCommits);

        } finally {
            repository.close();
        }
    }

    /**
     * Groups commits by conventional commit type.
     */
    private Map<String, List<CommitInfo>> groupCommitsByType(List<RevCommit> commits) {
        Map<String, List<CommitInfo>> grouped = new LinkedHashMap<>();
        grouped.put("Features", new ArrayList<>());
        grouped.put("Bug Fixes", new ArrayList<>());
        grouped.put("Performance", new ArrayList<>());
        grouped.put("Documentation", new ArrayList<>());
        grouped.put("Refactoring", new ArrayList<>());
        grouped.put("Testing", new ArrayList<>());
        grouped.put("Build", new ArrayList<>());
        grouped.put("Other", new ArrayList<>());

        for (RevCommit commit : commits) {
            String message = commit.getShortMessage();
            String fullMessage = commit.getFullMessage();
            Date date = new Date(commit.getCommitTime() * 1000L);

            CommitInfo info = new CommitInfo(
                commit.getName().substring(0, 7),
                message,
                fullMessage,
                commit.getAuthorIdent().getName(),
                date
            );

            // Categorize based on conventional commits
            String category = categorizeCommit(message);
            grouped.get(category).add(info);
        }

        // Remove empty categories
        grouped.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        return grouped;
    }

    /**
     * Categorizes a commit based on its message.
     */
    private String categorizeCommit(String message) {
        String lower = message.toLowerCase();

        if (lower.startsWith("feat:") || lower.startsWith("feature:") || lower.contains("add ")) {
            return "Features";
        } else if (lower.startsWith("fix:") || lower.contains("bug") || lower.contains("issue")) {
            return "Bug Fixes";
        } else if (lower.startsWith("perf:") || lower.contains("performance") || lower.contains("optimize")) {
            return "Performance";
        } else if (lower.startsWith("docs:") || lower.contains("documentation") || lower.contains("readme")) {
            return "Documentation";
        } else if (lower.startsWith("refactor:") || lower.contains("refactor")) {
            return "Refactoring";
        } else if (lower.startsWith("test:") || lower.contains("test")) {
            return "Testing";
        } else if (lower.startsWith("build:") || lower.startsWith("chore:") || lower.contains("dependency")) {
            return "Build";
        } else {
            return "Other";
        }
    }

    /**
     * Builds the changelog content.
     */
    private String buildChangelog(Map<String, List<CommitInfo>> groupedCommits) {
        StringBuilder changelog = new StringBuilder();

        // Header
        changelog.append("# Changelog\n\n");
        changelog.append("All notable changes to this project will be documented in this file.\n\n");
        changelog.append("The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),\n");
        changelog.append("and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).\n\n");

        // Unreleased section
        changelog.append("## [Unreleased]\n\n");

        // Add commits by category
        for (Map.Entry<String, List<CommitInfo>> entry : groupedCommits.entrySet()) {
            String category = entry.getKey();
            List<CommitInfo> commits = entry.getValue();

            if (!commits.isEmpty()) {
                changelog.append("### ").append(category).append("\n\n");

                for (CommitInfo commit : commits) {
                    changelog.append("- ").append(cleanMessage(commit.message));
                    changelog.append(" ([").append(commit.hash).append("])");
                    changelog.append("\n");
                }

                changelog.append("\n");
            }
        }

        // Template for future releases
        changelog.append("## [Version] - YYYY-MM-DD\n\n");
        changelog.append("### Added\n");
        changelog.append("- New features\n\n");
        changelog.append("### Changed\n");
        changelog.append("- Changes in existing functionality\n\n");
        changelog.append("### Deprecated\n");
        changelog.append("- Soon-to-be removed features\n\n");
        changelog.append("### Removed\n");
        changelog.append("- Removed features\n\n");
        changelog.append("### Fixed\n");
        changelog.append("- Bug fixes\n\n");
        changelog.append("### Security\n");
        changelog.append("- Security fixes\n\n");

        return changelog.toString();
    }

    /**
     * Cleans commit message by removing prefixes.
     */
    private String cleanMessage(String message) {
        // Remove conventional commit prefixes
        message = message.replaceFirst("^(feat|fix|docs|style|refactor|perf|test|build|ci|chore)(\\([^)]+\\))?:\\s*", "");

        // Capitalize first letter
        if (!message.isEmpty()) {
            message = message.substring(0, 1).toUpperCase() + message.substring(1);
        }

        return message;
    }

    /**
     * Commit information.
     */
    private static class CommitInfo {
        final String hash;
        final String message;
        final String fullMessage;
        final String author;
        final Date date;

        CommitInfo(String hash, String message, String fullMessage, String author, Date date) {
            this.hash = hash;
            this.message = message;
            this.fullMessage = fullMessage;
            this.author = author;
            this.date = date;
        }
    }
}
