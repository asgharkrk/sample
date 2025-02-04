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

public class QTestGetIDs {

    // 1) Update these for your environment:
    private static final String QTEST_BASE_URL    = "/api/v3";
    private static final String AUTH_HEADER_VALUE = "Bearer <YOUR_BEARER_TOKEN>";
    private static final long   PROJECT_ID        = 118109;     // your project
    private static final long   FOLDER_ID         = 21008378;   // folder/cycle ID from UI (object=4)

    /**
     * (Optional) Disable SSL verification if you have a self-signed certificate or similar.
     */
    public static void setDisableSSLVerification(boolean disable) {
        if (disable) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
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
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HostnameVerifier allHostsValid = (hostname, session) -> true;
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static List<Long> getTestRunIdsInFolder() {
        setDisableSSLVerification(true);

        List<Long> testRunIds = new ArrayList<>();
        String urlString = String.format(
                "%s/projects/%d/test-runs?parentId=%d",
                QTEST_BASE_URL, PROJECT_ID, FOLDER_ID
        );

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            // Read the entire response (whether success or error) for debugging
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    responseBody.append(line);
                }
            } catch (IOException ignored) {
                // If there's an error response body, you might read from conn.getErrorStream() instead
            }

            String responseStr = responseBody.toString().trim();
            System.out.println("Response code: " + responseCode);
            System.out.println("Response body: " + responseStr);

            if (responseCode == 200) {
                // Some qTest versions return a raw JSON array:  [ {...}, {...} ]
                // Others return an object with 'items': { "items": [ {...}, ...] }
                if (responseStr.startsWith("[")) {
                    // We have a JSON array
                    JSONArray runsArray = new JSONArray(responseStr);
                    for (int i = 0; i < runsArray.length(); i++) {
                        JSONObject runObj = runsArray.getJSONObject(i);
                        long runId = runObj.getLong("id");
                        testRunIds.add(runId);
                    }
                } else if (responseStr.startsWith("{")) {
                    // Possibly an object with "items" array
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
                    System.err.println("Response is not valid JSON array/object:\n" + responseStr);
                }
            } else {
                // Not a 200, so maybe 401 or 404
                System.err.println("Failed to retrieve test runs. HTTP code: " + responseCode);
                // If you want the error body, do:
                try (BufferedReader errIn = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
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
            if (conn != null) conn.disconnect();
        }

        return testRunIds;
    }

    /**
     * Updates the status ("Passed", "Failed", etc.) of a specific Test Run.
     *
     * If your qTest requires a different field name (e.g. "executionStatus"),
     * adjust the JSON body accordingly.
     */
    public static void updateTestRunStatus(long testRunId, String status, String note) {
        String urlString = String.format(
                "%s/projects/%d/test-runs/%d",
                QTEST_BASE_URL, PROJECT_ID, testRunId
        );

        // The JSON body: check your version if "status" or "executionStatus" is needed.
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
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            connection.setRequestProperty("Content-Type", "application/json");

            // Write JSON body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("Test Run #" + testRunId + " updated to " + status + ".");
            } else {
                System.err.println("Failed to update Test Run #" + testRunId + ". HTTP code: " + responseCode);
                try (BufferedReader errIn = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
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
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Main method:
     *  1. Fetch all runs in the specified folder/cycle.
     *  2. Update each run to "Passed" (or any other status you like).
     */
    public static void main(String[] args) {
        // Step 1: Find all test runs in the folder
        List<Long> runIds = getTestRunIdsInFolder();

        if (runIds.isEmpty()) {
            System.err.println("No Test Runs found in folder #" + FOLDER_ID);
            return;
        }

        // Step 2: Update each run's status
        for (Long runId : runIds) {
            updateTestRunStatus(runId, "Passed", "Automated execution via API in folder " + FOLDER_ID);
        }
    }
}