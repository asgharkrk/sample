package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class QTestClient {
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private String accessToken;

    public QTestClient(String baseUrl, String clientId, String clientSecret) {
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accessToken = authenticate();
    }

    private String authenticate() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(baseUrl + "/oauth/token");
            post.setHeader("Content-Type", "application/json");
            
            // Request body for OAuth
            String json = String.format(
                "{\"grant_type\":\"client_credentials\", \"client_id\":\"%s\", \"client_secret\":\"%s\"}",
                clientId, clientSecret
            );
            post.setEntity(new StringEntity(json));

            HttpResponse response = client.execute(post);
            // Parse response to extract access_token
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(
                response.getEntity().getContent(), 
                new TypeReference<Map<String, Object>>() {}
            );
            return (String) responseMap.get("access_token");
        } catch (Exception e) {
            throw new RuntimeException("qTest authentication failed: " + e.getMessage());
        }
    }

    public void updateTestResult(String projectId, String testCaseId, String status) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String url = baseUrl + "/api/v3/projects/" + projectId + "/test-runs";
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + accessToken);
            post.setHeader("Content-Type", "application/json");

            // Example payload (adjust based on qTest API requirements)
            String json = String.format(
                "{\"test_case\": {\"id\": %s}, \"status\": \"%s\"}",
                testCaseId, status
            );
            post.setEntity(new StringEntity(json));

            HttpResponse response = client.execute(post);
            // Handle response (e.g., check status code 201 for success)
        } catch (Exception e) {
            throw new RuntimeException("Failed to update qTest result: " + e.getMessage());
        }
    }
}