package org.example;// ApiClient.java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiClient {

    public static String postTestLog(String jsonPayload) throws Exception {
        URL url = new URL(ApiEndpoints.AUTO_TEST_LOGS);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set request properties
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        
        // Write the JSON payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // Read the response
        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream(),
                "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        connection.disconnect();
        System.out.println("Response Code: " + responseCode);
        System.out.println("Response Body: " + response.toString());
        return response.toString();
    }
}