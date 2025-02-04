package org.example;

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

import org.json.JSONArray;
import org.json.JSONObject;

public class QTestGetIDs {

    // Update these accordingly
    private static final String QTEST_BASE_URL    = "/api/v3";
    private static final String AUTH_HEADER_VALUE = "Bearer <YOUR_BEARER_TOKEN>";
    private static final long   PROJECT_ID        = 118109;     // e.g. 118109
    private static final long   FOLDER_ID         = 21008378;   // The folder/cycle you're targeting

    // Optional: disable SSL certificate checking if needed
    public static void setDisableSSLVerification(boolean disable) {
        if (disable) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
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

    /**
     * Retrieves all test runs in the given folder (FOLDER_ID).
     * @return a list of test run IDs
     */
    public static List<Long> getTestRunIdsInFolder() {
        setDisableSSLVerification(true);

        List<Long> testRunIds = new ArrayList<>();
        String urlString = String.format("%s/projects/%d/test-runs?parentId=%d",
                QTEST_BASE_URL, PROJECT_ID, FOLDER_ID);

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    // Parse JSON array
                    JSONArray runsArray = new JSONArray(response.toString());
                    for (int i = 0; i < runsArray.length(); i++) {
                        JSONObject runObj = runsArray.getJSONObject(i);
                        long runId = runObj.getLong("id");
                        testRunIds.add(runId);
                    }
                }
            } else {
                System.err.println("Failed to retrieve test runs. HTTP code: " + responseCode);
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return testRunIds;
    }

    /**
     * Updates the execution status of a single Test Run.
     */
    public static void updateTestRunStatus(long testRunId, String status, String note) {
        String urlString = String.format("%s/projects/%d/test-runs/%d",
                QTEST_BASE_URL, PROJECT_ID, testRunId);

        // Example JSON body - check your qTest docs if you need "executionStatus" etc.
        String requestBodyJson = String.format(
                "{" +
                        "  \"status\": \"%s\"," +
                        "  \"exe_start_date\": \"2025-02-04T10:00:00\"," +
                        "  \"exe_end_date\":   \"2025-02-04T10:05:00\"," +
                        "  \"note\": \"%s\"" +
                        "}", status, note);

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("Test Run #" + testRunId + " updated to " + status + ".");
            } else {
                System.err.println("Failed to update Test Run #" + testRunId +
                        ". HTTP code: " + responseCode);
            }

            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main flow:
     *  1) Retrieve all runs under the folder FOLDER_ID
     *  2) Update each run to "Passed"
     */
    public static void main(String[] args) {
        // Step 1: Get test runs in the folder
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