package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

/**
 * This class finds a Test Run in qTest by its "PID" (like "TR-1120")
 * and then updates that Test Run's status (e.g., "Passed").
 */
public class UpdateSingleTestRunByPID {

    // ============= CONFIG =============
    // Base URL for qTest's API, for example: "https://<your_domain>.qtestnet.com/api/v3"
    private static final String QTEST_BASE_URL    = "";

    // The Authorization header value, which must include "Bearer " + your token
    private static final String AUTH_HEADER_VALUE = "Bearer <YOUR_BEARER_TOKEN>";

    // The numeric ID of your project in qTest (e.g., 118109)
    private static final long   PROJECT_ID        = 118109;

    // The "PID" (the short name or label) of the Test Run in the UI, like "TR-1120"
    private static final String TARGET_PID        = "TR-1120";


    /**
     * This method optionally disables SSL certificate checking.
     * Use it if your server uses a self-signed cert, or if you want to skip certificate validation.
     */
    public static void setDisableSSLVerification(boolean disable) {
        if (disable) {
            try {
                // Create a TrustManager that trusts all certificates
                TrustManager[] trustAllCerts = {
                        new X509TrustManager() {
                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };

                // Initialize a new SSL context that uses this TrustManager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                // Set the default SSL socket factory to the one we just created
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                // Accept any hostname (not recommended for production)
                HostnameVerifier allHostsValid = (hostname, session) -> true;
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Looks up the Test Run's numeric ID by its PID (the label you see in the UI).
     * We call the API: GET /test-runs?limit=5000 to fetch many runs and compare "pid".
     * If we find a Test Run with a matching "pid", return its numeric "id". Otherwise, return -1.
     */
    public static long findTestRunIdByPid(String pid) {
        // Disable SSL checks if needed
        setDisableSSLVerification(true);

        long foundId = -1L;  // Default to -1 if not found

        // Build the URL to get test runs from qTest
        String urlString = String.format("%s/projects/%d/test-runs?limit=5000",
                QTEST_BASE_URL, PROJECT_ID);

        HttpURLConnection conn = null;
        try {
            // Open the connection
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();

            // Make it a GET request
            conn.setRequestMethod("GET");

            // Set the Authorization and Content-Type headers
            conn.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            conn.setRequestProperty("Content-Type", "application/json");

            // Check the response code (e.g., 200 means OK)
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                // If 200, we read the response fully
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;

                // Read all lines from the response
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                in.close();

                // Convert everything to one string
                String responseStr = sb.toString().trim();

                // qTest might return either a direct JSON array or an object with "items"
                // Check if the response starts with "[" (array) or "{" (object)
                if (responseStr.startsWith("[")) {
                    // If it's an array, parse as a JSONArray
                    JSONArray array = new JSONArray(responseStr);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject runObj = array.getJSONObject(i);
                        // Compare the "pid" field to the PID we want
                        if (pid.equalsIgnoreCase(runObj.optString("pid"))) {
                            // If it matches, get the numeric "id"
                            foundId = runObj.getLong("id");
                            break;
                        }
                    }
                } else if (responseStr.startsWith("{")) {
                    // If it's an object, we check for the "items" array inside
                    JSONObject obj = new JSONObject(responseStr);
                    if (obj.has("items")) {
                        JSONArray items = obj.getJSONArray("items");
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject runObj = items.getJSONObject(i);
                            if (pid.equalsIgnoreCase(runObj.optString("pid"))) {
                                foundId = runObj.getLong("id");
                                break;
                            }
                        }
                    } else {
                        System.err.println("JSON is an object but missing 'items' array.");
                    }
                } else {
                    System.err.println("Unexpected response format: " + responseStr);
                }
            } else {
                // If not 200, print an error
                System.err.println("GET test-runs call failed with HTTP code: " + responseCode);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // Disconnect when done
            if (conn != null) conn.disconnect();
        }

        return foundId;
    }

    /**
     * Given the numeric ID of a Test Run and a new status (like "Passed"),
     * this method updates that run in qTest.
     * We do a PUT /test-runs/{runId} call with a small JSON body.
     */
    public static void updateTestRunStatus(long runId, String newStatus, String note) {
        // If the runId isn't valid, show an error and stop
        if (runId <= 0) {
            System.err.println("Invalid runId: " + runId);
            return;
        }

        // Build the URL for the specific test run
        String urlString = String.format("%s/projects/%d/test-runs/%d",
                QTEST_BASE_URL, PROJECT_ID, runId);

        // Create a JSON body that sets the status, start date, end date, and note
        String requestBody = String.format(
                "{" +
                        "  \"status\": \"%s\"," +
                        "  \"exe_start_date\": \"2025-02-05T10:00:00\"," +
                        "  \"exe_end_date\":   \"2025-02-05T10:05:00\"," +
                        "  \"note\": \"%s\"" +
                        "}",
                newStatus, note
        );

        HttpURLConnection conn = null;
        try {
            // Open the connection
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();

            // We use PUT to update the run
            conn.setRequestMethod("PUT");

            // We will send a JSON body, so we need setDoOutput(true)
            conn.setDoOutput(true);

            // Add headers for authorization and content type
            conn.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            conn.setRequestProperty("Content-Type", "application/json");

            // Write the JSON body to the request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = conn.getResponseCode();
            // If it's 2xx, success
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("Successfully set run #" + runId + " to " + newStatus);
            } else {
                // Otherwise, print an error
                System.err.println("Failed to update run #" + runId + ". HTTP " + responseCode);

                // Try to read more details from the error stream
                try (BufferedReader errIn = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = errIn.readLine()) != null) {
                        sb.append(line);
                    }
                    System.err.println("Error response body: " + sb.toString());
                } catch (Exception ignored) {}
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // Disconnect when done
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Main method:
     * 1) Find the numeric ID for a Test Run with PID "TR-1120".
     * 2) If found, update its status to "Passed".
     */
    public static void main(String[] args) {
        // 1) Find the numeric Test Run ID by the UI label (PID) "TR-1120"
        long runId = findTestRunIdByPid(TARGET_PID);
        if (runId > 0) {
            // If found, print the numeric ID
            System.out.println("Found run ID = " + runId + " for PID " + TARGET_PID);

            // 2) Now update that run to "Passed"
            updateTestRunStatus(runId, "Passed", "Updated via API for " + TARGET_PID);
        } else {
            // If not found, print a message
            System.err.println("Could not find a run with PID: " + TARGET_PID);
        }
    }
}