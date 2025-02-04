package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * This class demonstrates how to:
 * 1. Find all test run IDs under a specific folder (or test cycle) in qTest.
 * 2. Update each found test run's status (e.g., "Passed").
 */
public class QTestGetIDs {

    // ------------------- CONFIGURATION -------------------
    // Path to the qTest API (e.g. "https://<your-domain>.qtestnet.com/api/v3")
    private static final String QTEST_BASE_URL    = "/api/v3";

    // Bearer token for authorization: "Bearer <TOKEN>"
    private static final String AUTH_HEADER_VALUE = "Bearer <YOUR_BEARER_TOKEN>";

    // Numeric ID of the project in qTest (e.g., 118109)
    private static final long   PROJECT_ID        = 118109;

    // Numeric ID of the folder/test-cycle from the UI (object=4 in the URL), e.g. 21008378
    private static final long   FOLDER_ID         = 21008378;


    /**
     * (Optional) Disables SSL certificate checks if needed.
     * This is only used if your environment has self-signed certificates or similar.
     */
    public static void setDisableSSLVerification(boolean disable) {
        if (disable) {
            try {
                // Create a TrustManager that trusts all certificates
                TrustManager[] trustAllCerts = {
                        new X509TrustManager() {
                            @Override
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };

                // Initialize an SSLContext that uses this TrustManager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                // Accept all hostnames (not recommended for production)
                HostnameVerifier allHostsValid = (hostname, session) -> true;
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves a list of test run IDs that are *directly* under the specified folder or test cycle.
     * If subfolders exist, they might not be returned unless qTest supports recursion or we call subfolders separately.
     */
    public static List<Long> getTestRunIdsInFolder() {
        // Optionally disable SSL checks
        setDisableSSLVerification(true);

        // A list to hold all the test run IDs we find
        List<Long> testRunIds = new ArrayList<>();

        // Build the URL for the GET request, adding the folder ID as a "parentId"
        String urlString = String.format(
                "%s/projects/%d/test-runs?parentId=%d",
                QTEST_BASE_URL, PROJECT_ID, FOLDER_ID
        );

        HttpURLConnection conn = null;
        try {
            // Open the connection
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();

            // Set the request method to GET
            conn.setRequestMethod("GET");

            // Include authorization and JSON content type headers
            conn.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            conn.setRequestProperty("Content-Type", "application/json");

            // Get the response code (e.g. 200 for success)
            int responseCode = conn.getResponseCode();

            // Read the entire response body
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    responseBody.append(line);
                }
            } catch (IOException ignored) {
                // If there's an error, we might read from conn.getErrorStream() instead
            }

            String responseStr = responseBody.toString().trim();

            // Print for debugging
            System.out.println("Response code: " + responseCode);
            System.out.println("Response body: " + responseStr);

            if (responseCode == 200) {
                // Depending on qTest version, we might get a JSON array or an object with "items"
                if (responseStr.startsWith("[")) {
                    // If it's an array, parse it directly
                    JSONArray runsArray = new JSONArray(responseStr);
                    for (int i = 0; i < runsArray.length(); i++) {
                        JSONObject runObj = runsArray.getJSONObject(i);
                        long runId = runObj.getLong("id");
                        testRunIds.add(runId);
                    }
                } else if (responseStr.startsWith("{")) {
                    // If it's an object, look for "items"
                    JSONObject obj = new JSONObject(responseStr);
                    if (obj.has("items")) {
                        JSONArray itemsArray = obj.getJSONArray("items");
                        for (int i = 0; i < itemsArray.length(); i++) {
                            JSONObject runObj = itemsArray.getJSONObject(i);
                            long runId = runObj.getLong("id");
                            testRunIds.add(runId);
                        }
                    } else {
                        System.err.println("JSON is an object but doesn't contain 'items'.");
                    }
                } else {
                    System.err.println("Response is not valid JSON:\n" + responseStr);
                }
            } else {
                // If not HTTP 200, there's an error (e.g. 401 or 404)
                System.err.println("Failed to retrieve test runs. HTTP code: " + responseCode);

                // Optionally read the error body for more info
                try (BufferedReader errIn =
                             new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder errBody = new StringBuilder();
                    String line;
                    while ((line = errIn.readLine()) != null) {
                        errBody.append(line);
                    }
                    System.err.println("Error body: " + errBody.toString());
                } catch (IOException ex) {
                    // ignore
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Disconnect when done
            if (conn != null) conn.disconnect();
        }

        // Return all the test run IDs we found
        return testRunIds;
    }

    /**
     * Given a test run ID and a desired status (e.g. "Passed"),
     * this sends a PUT request to update that run in qTest.
     */
    public static void updateTestRunStatus(long testRunId, String status, String note) {
        // Build the URL for a specific test run in this project
        String urlString = String.format(
                "%s/projects/%d/test-runs/%d",
                QTEST_BASE_URL, PROJECT_ID, testRunId
        );

        // The JSON body that sets the new status, plus optional start/end dates and notes
        String requestBodyJson = String.format(
                "{" +
                        "  \"status\": \"%s\"," +
                        "  \"exe_start_date\": \"2025-02-04T10:00:00\"," +
                        "  \"exe_end_date\":   \"2025-02-04T10:05:00\"," +
                        "  \"note\": \"%s\"" +
                        "}",
                status, note
        );

        HttpURLConnection connection = null;
        try {
            // Open the connection
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            // We'll do a PUT request to update the resource
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);  // We need to write a JSON body

            // Set authorization and content type
            connection.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            connection.setRequestProperty("Content-Type", "application/json");

            // Write our JSON body to the request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get the response code to check if it succeeded (2xx)
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("Test Run #" + testRunId + " updated to " + status + ".");
            } else {
                // If not successful, log an error
                System.err.println("Failed to update Test Run #" + testRunId +
                        ". HTTP code: " + responseCode);

                // Attempt to read any error details
                try (BufferedReader errIn =
                             new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errBody = new StringBuilder();
                    String line;
                    while ((line = errIn.readLine()) != null) {
                        errBody.append(line);
                    }
                    System.err.println("Error body: " + errBody.toString());
                } catch (IOException ex) {
                    // ignore
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close connection
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * The main method does two things:
     *  1) Call getTestRunIdsInFolder() to retrieve all test run IDs under the folder.
     *  2) Update each test run to "Passed".
     */
    public static void main(String[] args) {
        // Step 1: Retrieve all test runs in the specified folder/cycle
        List<Long> runIds = getTestRunIdsInFolder();

        // If no runs are found, we stop
        if (runIds.isEmpty()) {
            System.err.println("No Test Runs found in folder #" + FOLDER_ID);
            return;
        }

        // Step 2: For each run ID, update the status to "Passed"
        for (Long runId : runIds) {
            updateTestRunStatus(runId, "Passed",
                    "Automated execution via API in folder " + FOLDER_ID);
        }
    }
}