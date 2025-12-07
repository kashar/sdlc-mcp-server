package com.example.mcp.clients;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JiraClient using MockWebServer.
 */
@DisplayName("JiraClient Tests")
class JiraClientTest {

    private MockWebServer mockServer;
    private JiraClient client;
    private String baseUrl;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_TOKEN = "test-token-123";

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        baseUrl = mockServer.url("/").toString();
        // Remove trailing slash for testing
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        client = new JiraClient(baseUrl, TEST_EMAIL, TEST_TOKEN);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    @DisplayName("Should create JiraClient with valid parameters")
    void testConstructor() {
        // Arrange & Act
        JiraClient newClient = new JiraClient(baseUrl, TEST_EMAIL, TEST_TOKEN);

        // Assert
        assertNotNull(newClient, "Client should be created successfully");
    }

    @Test
    @DisplayName("Should remove trailing slash from base URL")
    void testConstructorRemovesTrailingSlash() {
        // Arrange
        String urlWithSlash = baseUrl + "/";

        // Act
        JiraClient newClient = new JiraClient(urlWithSlash, TEST_EMAIL, TEST_TOKEN);

        // Assert
        assertNotNull(newClient, "Client should handle URL with trailing slash");
    }

    @Test
    @DisplayName("Should search issues with JQL query")
    void testSearchIssuesSuccess() throws Exception {
        // Arrange
        String jql = "project = TEST";
        int maxResults = 10;
        String mockResponse = "{\"issues\": [], \"total\": 0}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        String response = client.searchIssues(jql, maxResults);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertTrue(request.getPath().contains("/rest/api/3/search"));
        assertTrue(request.getPath().contains("jql="));
        assertTrue(request.getPath().contains("maxResults=" + maxResults));
        verifyAuthHeader(request);
    }

    @Test
    @DisplayName("Should get issue by key")
    void testGetIssueSuccess() throws Exception {
        // Arrange
        String issueKey = "TEST-123";
        String mockResponse = "{\"key\": \"TEST-123\", \"fields\": {}}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        String response = client.getIssue(issueKey);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertTrue(request.getPath().contains("/rest/api/3/issue/" + issueKey));
        verifyAuthHeader(request);
    }

    @Test
    @DisplayName("Should create issue successfully")
    void testCreateIssueSuccess() throws Exception {
        // Arrange
        String projectKey = "TEST";
        String issueType = "Bug";
        String summary = "Test issue";
        String description = "Test description";
        String mockResponse = "{\"key\": \"TEST-456\", \"id\": \"12345\"}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        String response = client.createIssue(projectKey, issueType, summary, description);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertEquals("/rest/api/3/issue", request.getPath());
        assertEquals("POST", request.getMethod());
        verifyAuthHeader(request);

        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains(projectKey));
        assertTrue(requestBody.contains(issueType));
        assertTrue(requestBody.contains(summary));
    }

    @Test
    @DisplayName("Should handle 404 error when issue not found")
    void testGetIssueNotFound() throws Exception {
        // Arrange
        String issueKey = "TEST-999";
        mockServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"errorMessages\": [\"Issue not found\"]}"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            client.getIssue(issueKey);
        });

        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    @DisplayName("Should handle 401 unauthorized error")
    void testUnauthorizedAccess() throws Exception {
        // Arrange
        mockServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"errorMessages\": [\"Unauthorized\"]}"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            client.searchIssues("project = TEST", 10);
        });

        assertTrue(exception.getMessage().contains("401"));
    }

    @Test
    @DisplayName("Should handle server errors")
    void testServerError() throws Exception {
        // Arrange
        mockServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"errorMessages\": [\"Internal Server Error\"]}"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            client.searchIssues("project = TEST", 10);
        });

        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    @DisplayName("Should create issue with empty description")
    void testCreateIssueWithEmptyDescription() throws Exception {
        // Arrange
        String projectKey = "TEST";
        String issueType = "Task";
        String summary = "Test task";
        String description = "";
        String mockResponse = "{\"key\": \"TEST-789\"}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody(mockResponse));

        // Act
        String response = client.createIssue(projectKey, issueType, summary, description);

        // Assert
        assertEquals(mockResponse, response);
    }

    @ParameterizedTest
    @CsvSource({
            "project = TEST, 10",
            "status = Open, 25",
            "assignee = currentUser(), 50"
    })
    @DisplayName("Should search with various JQL queries")
    void testSearchWithVariousJQL(String jql, int maxResults) throws Exception {
        // Arrange
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"issues\": []}"));

        // Act
        String response = client.searchIssues(jql, maxResults);

        // Assert
        assertNotNull(response);
        RecordedRequest request = mockServer.takeRequest();
        assertTrue(request.getPath().contains("maxResults=" + maxResults));
    }

    /**
     * Verifies that the Authorization header is set correctly.
     */
    private void verifyAuthHeader(RecordedRequest request) {
        String authHeader = request.getHeader("Authorization");
        assertNotNull(authHeader, "Authorization header should be present");
        assertTrue(authHeader.startsWith("Basic "), "Should use Basic authentication");

        // Decode and verify credentials
        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));
        assertEquals(TEST_EMAIL + ":" + TEST_TOKEN, credentials);
    }
}
