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
 * HTTP client for JIRA REST API.
 *
 * <p>Provides methods to interact with JIRA Cloud and Server instances
 * using the REST API v2 and v3.
 *
 * @author SDLC Tools Team
 * @version 2.0.0
 */
public class JiraClient {

    private static final Logger logger = LoggerFactory.getLogger(JiraClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient client;
    private final Gson gson;
    private final String authHeader;

    /**
     * Creates a new JIRA client.
     *
     * @param baseUrl JIRA instance URL (e.g., "https://your-domain.atlassian.net")
     * @param email user email for authentication
     * @param apiToken API token for authentication
     */
    public JiraClient(String baseUrl, String email, String apiToken) {
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
     * Searches for JIRA issues using JQL.
     *
     * @param jql JQL query string
     * @param maxResults maximum number of results
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String searchIssues(String jql, int maxResults) throws IOException {
        String url = baseUrl + "/rest/api/3/search?jql=" +
                     encodeValue(jql) + "&maxResults=" + maxResults;

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .get()
                .build();

        return executeRequest(request);
    }

    /**
     * Gets details of a specific JIRA issue.
     *
     * @param issueKey issue key (e.g., "PROJ-123")
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String getIssue(String issueKey) throws IOException {
        String url = baseUrl + "/rest/api/3/issue/" + issueKey;

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .get()
                .build();

        return executeRequest(request);
    }

    /**
     * Creates a new JIRA issue.
     *
     * @param projectKey project key
     * @param issueType issue type (e.g., "Bug", "Task", "Story")
     * @param summary issue summary
     * @param description issue description
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String createIssue(String projectKey, String issueType, String summary, String description) throws IOException {
        String url = baseUrl + "/rest/api/3/issue";

        JsonObject fields = new JsonObject();

        JsonObject project = new JsonObject();
        project.addProperty("key", projectKey);
        fields.add("project", project);

        JsonObject type = new JsonObject();
        type.addProperty("name", issueType);
        fields.add("issuetype", type);

        fields.addProperty("summary", summary);

        if (description != null && !description.isEmpty()) {
            fields.addProperty("description", description);
        }

        JsonObject body = new JsonObject();
        body.add("fields", fields);

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
     * Updates a JIRA issue.
     *
     * @param issueKey issue key
     * @param fieldsJson JSON string with fields to update
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String updateIssue(String issueKey, String fieldsJson) throws IOException {
        String url = baseUrl + "/rest/api/3/issue/" + issueKey;

        RequestBody requestBody = RequestBody.create(fieldsJson, JSON);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .put(requestBody)
                .build();

        return executeRequest(request);
    }

    /**
     * Adds a comment to a JIRA issue.
     *
     * @param issueKey issue key
     * @param comment comment text
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String addComment(String issueKey, String comment) throws IOException {
        String url = baseUrl + "/rest/api/3/issue/" + issueKey + "/comment";

        JsonObject body = new JsonObject();
        body.addProperty("body", comment);

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
     * Gets transitions available for an issue.
     *
     * @param issueKey issue key
     * @return JSON response as string
     * @throws IOException if request fails
     */
    public String getTransitions(String issueKey) throws IOException {
        String url = baseUrl + "/rest/api/3/issue/" + issueKey + "/transitions";

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
        logger.debug("Executing JIRA request: {} {}", request.method(), request.url());

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                logger.error("JIRA request failed: {} - {}", response.code(), responseBody);
                throw new IOException("JIRA API request failed: " + response.code() + " - " + responseBody);
            }

            logger.debug("JIRA request successful: {}", response.code());
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
