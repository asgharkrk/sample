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

public class APIUtils {

  public static String buildPayload(String testCaseName,String status,String notes){
      JSONObject payloadJson=null;
      try{
          String content = new String(Files.readAllBytes(Paths.get(APIConstants.PAYLOAD_FILE_PATH)), StandardCharsets.UTF_8);

          // Parse the content into a JSONObject
           payloadJson = new JSONObject(content);

          String currentTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

          JSONArray testLogs = payloadJson.getJSONArray("test_logs");
          if (testLogs.length() > 0) {
              JSONObject testLog = testLogs.getJSONObject(0);
              testLog.put("name", testCaseName);
              testLog.put("status", status);
             // testLog.put("exe_start_date", currentTime);
              //testLog.put("exe_end_date", currentTime);
              testLog.put("note", "Test '" + testCaseName + "' executed with status: " + status);
          }

      }catch (Exception e){
     e.printStackTrace();
      }
      return payloadJson.toString(4); //
  }

  public static void postAPI(String payload){
      Response response=null;
      try {

           response = RestAssured.given()
                  .relaxedHTTPSValidation()
                  .contentType(ContentType.JSON)
                  .header("Authorization", "Bearer " + APIConstants.TOKEN)  // Provide your token here.
                  .body(payload)
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