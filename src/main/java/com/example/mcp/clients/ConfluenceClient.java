package com.example.mcp.clients;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for Confluence REST API.
 *
 * <p>Provides methods to interact with Confluence Cloud and Server instances
 * using the REST API v1 and v2.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class ConfluenceClient {

    private static final Logger logger = LoggerFactory.getLogger(ConfluenceClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient client;
    private final Gson gson;
    private final String authHeader;

    /**
     * Creates a new Confluence client.
     *
     * @param baseUrl Confluence instance URL (e.g., "https://your-domain.atlassian.net/wiki")
     * @param email user email for authentication
     * @param apiToken API token for authentication
     */
    public ConfluenceClient(String baseUrl, String email, String apiToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.gson = new Gson();

        // Create auth header
        String credentials = email + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        // Create HTTP client with timeouts
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Searches for Confluence pages using CQL.
     *
     * @param cql CQL query string
     * @param limit maximum number of results
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String searchContent(String cql, int limit) throws IOException {
        String url = baseUrl + "/rest/api/content/search?cql=" +
                     encodeValue(cql) + "&limit=" + limit;

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .get()
                .build();

        return executeRequest(request);
    }

    /**
     * Gets details of a specific Confluence page.
     *
     * @param pageId page ID
     * @param expand fields to expand (e.g., "body.storage,version")
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String getPage(String pageId, String expand) throws IOException {
        String url = baseUrl + "/rest/api/content/" + pageId;
        if (expand != null && !expand.isEmpty()) {
            url += "?expand=" + encodeValue(expand);
        }

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .get()
                .build();

        return executeRequest(request);
    }

    /**
     * Gets page content by space and title.
     *
     * @param spaceKey space key
     * @param title page title
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String getPageByTitle(String spaceKey, String title) throws IOException {
        String cql = "space=" + spaceKey + " and title=\"" + title + "\"";
        return searchContent(cql, 1);
    }

    /**
     * Creates a new Confluence page.
     *
     * @param spaceKey space key
     * @param title page title
     * @param content page content (storage format)
     * @param parentId parent page ID (optional)
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String createPage(String spaceKey, String title, String content, String parentId) throws IOException {
        String url = baseUrl + "/rest/api/content";

        JsonObject space = new JsonObject();
        space.addProperty("key", spaceKey);

        JsonObject body = new JsonObject();
        body.addProperty("type", "page");
        body.addProperty("title", title);
        body.add("space", space);

        JsonObject storage = new JsonObject();
        storage.addProperty("value", content);
        storage.addProperty("representation", "storage");

        JsonObject bodyObj = new JsonObject();
        bodyObj.add("storage", storage);
        body.add("body", bodyObj);

        if (parentId != null && !parentId.isEmpty()) {
            JsonObject ancestor = new JsonObject();
            ancestor.addProperty("id", parentId);

            JsonObject ancestors = new JsonObject();
            ancestors.add("ancestors", gson.toJsonTree(new Object[]{ancestor}));
            body.add("ancestors", gson.toJsonTree(new Object[]{ancestor}));
        }

        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .post(requestBody)
                .build();

        return executeRequest(request);
    }

    /**
     * Updates a Confluence page.
     *
     * @param pageId page ID
     * @param title new title
     * @param content new content (storage format)
     * @param version current version number
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String updatePage(String pageId, String title, String content, int version) throws IOException {
        String url = baseUrl + "/rest/api/content/" + pageId;

        JsonObject body = new JsonObject();
        body.addProperty("type", "page");
        body.addProperty("title", title);

        JsonObject versionObj = new JsonObject();
        versionObj.addProperty("number", version + 1);
        body.add("version", versionObj);

        JsonObject storage = new JsonObject();
        storage.addProperty("value", content);
        storage.addProperty("representation", "storage");

        JsonObject bodyObj = new JsonObject();
        bodyObj.add("storage", storage);
        body.add("body", bodyObj);

        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .put(requestBody)
                .build();

        return executeRequest(request);
    }

    /**
     * Gets child pages of a specific page.
     *
     * @param pageId parent page ID
     * @param limit maximum number of results
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String getChildPages(String pageId, int limit) throws IOException {
        String url = baseUrl + "/rest/api/content/" + pageId + "/child/page?limit=" + limit;

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .get()
                .build();

        return executeRequest(request);
    }

    /**
     * Executes an HTTP request and returns the response body.
     */
    private String executeRequest(Request request) throws IOException {
        logger.debug("Executing Confluence request: {} {}", request.method(), request.url());

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                logger.error("Confluence request failed: {} - {}", response.code(), responseBody);
                throw new IOException("Confluence API request failed: " + response.code() + " - " + responseBody);
            }

            logger.debug("Confluence request successful: {}", response.code());
            return responseBody;
        }
    }

    /**
     * URL encodes a value.
     */
    private String encodeValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
