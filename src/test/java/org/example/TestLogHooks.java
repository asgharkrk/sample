package org.example;// TestLogHooks.java
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
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

public class TestLogHooks {

    // Path to the payload JSON file (adjust the path as needed)
    private static final String PAYLOAD_FILE_PATH = "src/test/resources/payload.json";

    @After
    public void afterScenario(Scenario scenario) {
        try {
            // Read the payload JSON file as a string
            String content = new String(Files.readAllBytes(Paths.get(PAYLOAD_FILE_PATH)), StandardCharsets.UTF_8);
            
            // Parse the file content into a JSON object
            JSONObject payloadJson = new JSONObject(content);
            
            // Get the current time formatted as ISO_OFFSET_DATE_TIME (e.g., "2025-02-06T01:27:43-08:00")
            String currentTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            
            // Update the payload with test details.
            // Assuming that the payload contains an array "test_logs" and we want to update the first object.
            JSONArray testLogs = payloadJson.getJSONArray("test_logs");
            if (testLogs.length() > 0) {
                JSONObject testLog = testLogs.getJSONObject(0);
                
                // Update the test name with the scenario's name.
                testLog.put("name", scenario.getName());
                
                // Update the status with the scenario's status.
                // (Convert enum to string if necessary.)
                testLog.put("status", scenario.getStatus().toString());
                
                // Update the execution start and end dates with the current time.
                testLog.put("exe_start_date", currentTime);
                testLog.put("exe_end_date", currentTime);
                
                // Optionally, update the note to include some details about the scenario.
                testLog.put("note", "Test '" + scenario.getName() + "' ended with status: " + scenario.getStatus());
            }
            
            // Convert the updated JSON object back to a string
            String updatedPayload = payloadJson.toString();
            
            // Make the API POST call using REST-assured
            Response response = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(updatedPayload)
                    .when()
                    .post(ApiEndpoints.AUTO_TEST_LOGS)
                    .then()
                    .extract()
                    .response();
            
            // Log the response status and body
            System.out.println("Response Status Code: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody().asString());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}