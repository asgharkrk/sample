package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class MainTestLogApiCall {

    // Path to the payload JSON file (adjust the path as needed)
    private static final String PAYLOAD_FILE_PATH = "src/test/resources/payload.json";
    
    // Define your token here. You might also choose to load this from a properties file.
    private static final String TOKEN = "your_actual_token_here";

    public static void main(String[] args) {
        try {
            // Read the JSON payload from file
            String content = new String(Files.readAllBytes(Paths.get(PAYLOAD_FILE_PATH)), StandardCharsets.UTF_8);

            // Parse the content into a JSONObject
            JSONObject payloadJson = new JSONObject(content);

            // Create sample test details
            String testName = "Dummy Test Name";
            String testStatus = "PASS";  // Change to "FAIL" if needed.
            String currentTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            // Update the JSON payload with sample details.
            // This example assumes that the payload contains an array "test_logs" and updates the first object.
            JSONArray testLogs = payloadJson.getJSONArray("test_logs");
            if (testLogs.length() > 0) {
                JSONObject testLog = testLogs.getJSONObject(0);
                testLog.put("name", testName);
                testLog.put("status", testStatus);
                testLog.put("exe_start_date", currentTime);
                testLog.put("exe_end_date", currentTime);
                testLog.put("note", "Test '" + testName + "' executed with status: " + testStatus);
            }

            // Convert the updated JSON payload back to a String (formatted)
            String updatedPayload = payloadJson.toString(4); // pretty-print with an indent of 4 spaces
            System.out.println("Updated Payload:\n" + updatedPayload);

            // Make the API POST call using REST-assured, adding the token to the header.
            Response response = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + TOKEN)  // Provide your token here.
                    .body(updatedPayload)
                    .when()
                    .post(ApiEndpoints.AUTO_TEST_LOGS)
                    .then()
                    .extract()
                    .response();

            // Print the response details
            System.out.println("Response Status Code: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody().asString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}