package com.example.mcp.clients;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfluenceClient using MockWebServer.
 */
@DisplayName("ConfluenceClient Tests")
class ConfluenceClientTest {

    private MockWebServer mockServer;
    private ConfluenceClient client;
    private String baseUrl;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_TOKEN = "test-token-123";

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        baseUrl = mockServer.url("/").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        client = new ConfluenceClient(baseUrl, TEST_EMAIL, TEST_TOKEN);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    @DisplayName("Should create ConfluenceClient with valid parameters")
    void testConstructor() {
        // Arrange & Act
        ConfluenceClient newClient = new ConfluenceClient(baseUrl, TEST_EMAIL, TEST_TOKEN);

        // Assert
        assertNotNull(newClient, "Client should be created successfully");
    }

    @Test
    @DisplayName("Should search content with CQL query")
    void testSearchContentSuccess() throws Exception {
        // Arrange
        String cql = "space=DEV";
        int limit = 10;
        String mockResponse = "{\"results\": [], \"size\": 0}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        String response = client.searchContent(cql, limit);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertTrue(request.getPath().contains("/rest/api/content/search"));
        assertTrue(request.getPath().contains("cql="));
        assertTrue(request.getPath().contains("limit=" + limit));
        verifyAuthHeader(request);
    }

    @Test
    @DisplayName("Should get page by ID")
    void testGetPageSuccess() throws Exception {
        // Arrange
        String pageId = "12345";
        String expand = "body.storage,version";
        String mockResponse = "{\"id\": \"12345\", \"title\": \"Test Page\"}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        String response = client.getPage(pageId, expand);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertTrue(request.getPath().contains("/rest/api/content/" + pageId));
        assertTrue(request.getPath().contains("expand="));
        verifyAuthHeader(request);
    }

    @Test
    @DisplayName("Should get page by title in space")
    void testGetPageByTitleSuccess() throws Exception {
        // Arrange
        String spaceKey = "DEV";
        String title = "My Page";
        String mockResponse = "{\"results\": [{\"id\": \"123\"}]}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        String response = client.getPageByTitle(spaceKey, title);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertTrue(request.getPath().contains("/rest/api/content/search"));
        verifyAuthHeader(request);
    }

    @Test
    @DisplayName("Should create page successfully")
    void testCreatePageSuccess() throws Exception {
        // Arrange
        String spaceKey = "DEV";
        String title = "New Page";
        String content = "<p>Page content</p>";
        String parentId = null;
        String mockResponse = "{\"id\": \"67890\", \"title\": \"New Page\"}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        String response = client.createPage(spaceKey, title, content, parentId);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertEquals("/rest/api/content", request.getPath());
        assertEquals("POST", request.getMethod());
        verifyAuthHeader(request);

        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains(spaceKey));
        assertTrue(requestBody.contains(title));
        // Content might be escaped in JSON, so just check it's present in some form
        assertTrue(requestBody.contains("Page content") || requestBody.contains(content));
    }

    @Test
    @DisplayName("Should create page with parent ID")
    void testCreatePageWithParent() throws Exception {
        // Arrange
        String spaceKey = "DEV";
        String title = "Child Page";
        String content = "<p>Child content</p>";
        String parentId = "12345";
        String mockResponse = "{\"id\": \"99999\"}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse));

        // Act
        String response = client.createPage(spaceKey, title, content, parentId);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains(parentId));
    }

    @Test
    @DisplayName("Should update page successfully")
    void testUpdatePageSuccess() throws Exception {
        // Arrange
        String pageId = "12345";
        String title = "Updated Title";
        String content = "<p>Updated content</p>";
        int version = 1;
        String mockResponse = "{\"id\": \"12345\", \"version\": {\"number\": 2}}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        String response = client.updatePage(pageId, title, content, version);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertTrue(request.getPath().contains("/rest/api/content/" + pageId));
        assertEquals("PUT", request.getMethod());
        verifyAuthHeader(request);

        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains("\"number\":" + (version + 1)));
    }

    @Test
    @DisplayName("Should get child pages")
    void testGetChildPagesSuccess() throws Exception {
        // Arrange
        String pageId = "12345";
        int limit = 10;
        String mockResponse = "{\"results\": [], \"size\": 0}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        String response = client.getChildPages(pageId, limit);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertTrue(request.getPath().contains("/rest/api/content/" + pageId + "/child/page"));
        assertTrue(request.getPath().contains("limit=" + limit));
        verifyAuthHeader(request);
    }

    @Test
    @DisplayName("Should handle 404 error when page not found")
    void testGetPageNotFound() throws Exception {
        // Arrange
        String pageId = "99999";
        mockServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"message\": \"Page not found\"}"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            client.getPage(pageId, "body.storage");
        });

        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    @DisplayName("Should handle 401 unauthorized error")
    void testUnauthorizedAccess() throws Exception {
        // Arrange
        mockServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"message\": \"Unauthorized\"}"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            client.searchContent("space=DEV", 10);
        });

        assertTrue(exception.getMessage().contains("401"));
    }

    @Test
    @DisplayName("Should get page without expand parameter")
    void testGetPageWithoutExpand() throws Exception {
        // Arrange
        String pageId = "12345";
        String mockResponse = "{\"id\": \"12345\"}";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse));

        // Act
        String response = client.getPage(pageId, null);

        // Assert
        assertEquals(mockResponse, response);

        RecordedRequest request = mockServer.takeRequest();
        assertFalse(request.getPath().contains("expand="));
    }

    /**
     * Verifies that the Authorization header is set correctly.
     */
    private void verifyAuthHeader(RecordedRequest request) {
        String authHeader = request.getHeader("Authorization");
        assertNotNull(authHeader, "Authorization header should be present");
        assertTrue(authHeader.startsWith("Basic "), "Should use Basic authentication");

        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));
        assertEquals(TEST_EMAIL + ":" + TEST_TOKEN, credentials);
    }
}
