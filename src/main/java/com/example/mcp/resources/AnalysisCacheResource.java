package com.example.mcp.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource for caching and retrieving Maven project analysis results.
 *
 * <p>This resource allows MCP clients to cache expensive analysis operations
 * and retrieve them later without re-running the analysis.
 *
 * <p>URI Pattern: {@code cache://analysis/{projectPath}}
 *
 * @author Maven SDLC Team
 * @version 2.0.0
 */
public class AnalysisCacheResource implements Resource {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisCacheResource.class);

    // In-memory cache (in production, could use Redis, etc.)
    private static final ConcurrentHashMap<String, CachedAnalysis> cache = new ConcurrentHashMap<>();

    @Override
    public String getUri() {
        return "cache://analysis/{projectPath}";
    }

    @Override
    public String getName() {
        return "analysis-cache";
    }

    @Override
    public String getDescription() {
        return "Cached Maven project analysis results to avoid re-running expensive operations";
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public Object read(Map<String, String> uriParams) throws Exception {
        String projectPath = uriParams.get("projectPath");

        if (projectPath == null || projectPath.isEmpty()) {
            throw new IllegalArgumentException("projectPath parameter is required");
        }

        // Normalize path
        String cacheKey = normalizePath(projectPath);

        logger.info("Reading analysis cache for: {}", cacheKey);

        CachedAnalysis cached = cache.get(cacheKey);

        if (cached == null) {
            return Map.of(
                    "cached", false,
                    "message", "No cached analysis found for: " + projectPath,
                    "hint", "Run analyze-maven-project tool first"
            );
        }

        // Check if cache is stale (older than 1 hour)
        long ageMinutes = (System.currentTimeMillis() - cached.timestamp) / 1000 / 60;
        boolean isStale = ageMinutes > 60;

        return Map.of(
                "cached", true,
                "projectPath", projectPath,
                "cachedAt", new java.util.Date(cached.timestamp).toString(),
                "ageMinutes", ageMinutes,
                "isStale", isStale,
                "data", cached.data
        );
    }

    /**
     * Stores analysis results in the cache.
     *
     * @param projectPath the project path
     * @param analysisData the analysis data to cache
     */
    public static void store(String projectPath, Object analysisData) {
        String cacheKey = normalizePath(projectPath);
        cache.put(cacheKey, new CachedAnalysis(System.currentTimeMillis(), analysisData));
        logger.info("Cached analysis for: {}", cacheKey);
    }

    /**
     * Clears the cache for a specific project.
     *
     * @param projectPath the project path
     */
    public static void invalidate(String projectPath) {
        String cacheKey = normalizePath(projectPath);
        cache.remove(cacheKey);
        logger.info("Invalidated cache for: {}", cacheKey);
    }

    /**
     * Clears all cached analyses.
     */
    public static void clearAll() {
        cache.clear();
        logger.info("Cleared all analysis cache");
    }

    /**
     * Normalizes a file path for use as a cache key.
     */
    private static String normalizePath(String path) {
        try {
            return Paths.get(path).toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            return path;
        }
    }

    /**
     * Internal class to store cached data with timestamp.
     */
    private record CachedAnalysis(long timestamp, Object data) {}
}
