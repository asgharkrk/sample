package org.example;

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
import java.util.*;


public class APIUtils {

  public static String buildPayload(Map<String, List<Scenario>> scenariosByFeature){
      JSONObject payloadJson=null;
      try{
          String content = new String(Files.readAllBytes(Paths.get(APIConstants.PAYLOAD_FILE_PATH)), StandardCharsets.UTF_8);

          // Parse the content into a JSONObject
           payloadJson = new JSONObject(content);

       //   String currentTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

          JSONArray testLogs = payloadJson.getJSONArray("test_logs");



          // Process each feature file with its scenarios
          for (Map.Entry<String, List<Scenario>> entry : scenariosByFeature.entrySet()) {
              String featureFile = entry.getKey();
              List<Scenario> scenarios = entry.getValue();

              for (Scenario scenario : scenarios) {
                  JSONObject tsLObj=new JSONObject(testLogs.getJSONObject(0));

                  tsLObj.put("name", scenario.getName());

                  if(scenario.getStatus().toString().toLowerCase().contains("pass")){
                      tsLObj.put("status", "PASS");
                     JSONArray jsonArray= tsLObj.getJSONArray("module_names");
                     jsonArray.clear();
                     jsonArray.put(featureFile);
                     JSONArray jsonArray1=tsLObj.getJSONArray("test_step_logs");
                     JSONObject test_step_logs=jsonArray1.getJSONObject(0);

                      test_step_logs.put("expected_result", "Source to Target should match");
                      test_step_logs.put("actual_result", "Source to Target is matching");
                  } else {
                      tsLObj.put("status", "FAIL");
                      JSONArray jsonArray= tsLObj.getJSONArray("module_names");
                      jsonArray.clear();
                      jsonArray.put(featureFile);
                      JSONArray jsonArray1=tsLObj.getJSONArray("test_step_logs");
                      JSONObject test_step_logs=jsonArray1.getJSONObject(0);
                      test_step_logs.put("expected_result", "Source to Target should match");
                      test_step_logs.put("actual_result", "Source to Target is Not matching");
                  }

                  tsLObj.put("note", "Test '" + scenario.getName() + "' executed with status: " + scenario.getStatus());
                  testLogs.put(tsLObj);
                  }
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