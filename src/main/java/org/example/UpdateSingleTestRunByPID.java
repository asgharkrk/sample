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

public class UpdateSingleTestRunByPID {

    // ============= CONFIG =============
    private static final String QTEST_BASE_URL    = "";
    private static final String AUTH_HEADER_VALUE = "Bearer <YOUR_BEARER_TOKEN>";
    private static final long   PROJECT_ID        = 118109;   // Your actual project ID
    private static final String TARGET_PID        = "TR-1120"; // The test run's "PID" you see in UI

    // If your qTest requires disabling SSL checks (self-signed cert, etc.).
    public static void setDisableSSLVerification(boolean disable) {
        if (disable) {
            try {
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
     * Search for the Test Run by "PID" (the label you see in UI, e.g. "TR-1120").
     * 
     * This example fetches all runs with "?limit=5000" and looks for matching "pid".
     * If found, returns the numeric "id" qTest needs to update. Otherwise returns -1.
     */
    public static long findTestRunIdByPid(String pid) {
        setDisableSSLVerification(true);

        long foundId = -1L;
        String urlString = String.format("%s/projects/%d/test-runs?limit=5000",
                                          QTEST_BASE_URL, PROJECT_ID);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                // Read response fully
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                in.close();

                String responseStr = sb.toString().trim();

                // Some qTest returns an array; some return { "items":[...] }
                if (responseStr.startsWith("[")) {
                    // Direct array
                    JSONArray array = new JSONArray(responseStr);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject runObj = array.getJSONObject(i);
                        if (pid.equalsIgnoreCase(runObj.optString("pid"))) {
                            foundId = runObj.getLong("id");
                            break;
                        }
                    }
                } else if (responseStr.startsWith("{")) {
                    // Possibly { "items": [...] }
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
                        System.err.println("JSON is an object but missing 'items'.");
                    }
                } else {
                    System.err.println("Unexpected response: " + responseStr);
                }
            } else {
                System.err.println("GET test-runs call failed with HTTP code: " + responseCode);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }

        return foundId;
    }

    /**
     * Updates a single Test Run (by numeric ID) to a given status (e.g. "Passed").
     * Check whether your qTest uses "status" or "executionStatus" key in the body.
     */
    public static void updateTestRunStatus(long runId, String newStatus, String note) {
        if (runId <= 0) {
            System.err.println("Invalid runId: " + runId);
            return;
        }

        String urlString = String.format("%s/projects/%d/test-runs/%d",
                                          QTEST_BASE_URL, PROJECT_ID, runId);

        // The JSON body: for qTest v3, "status" is usually correct. 
        // If your environment uses "executionStatus", change accordingly.
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
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            conn.setRequestProperty("Content-Type", "application/json");

            // Send the JSON body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("Successfully set run #" + runId + " to " + newStatus);
            } else {
                System.err.println("Failed to update run #" + runId + ". HTTP " + responseCode);
                // Read any error text
                try (BufferedReader errIn = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
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
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Main method:
     *  1) Find the numeric ID for a run whose "pid" is "TR-1120".
     *  2) If found, set it to Passed.
     */
    public static void main(String[] args) {
        // 1) Find the numeric ID of the run labeled "TR-1120" in the UI
        long runId = findTestRunIdByPid(TARGET_PID);
        if (runId > 0) {
            System.out.println("Found run ID = " + runId + " for PID " + TARGET_PID);

            // 2) Update the run's status to Passed
            updateTestRunStatus(runId, "Passed", "Updated via API for " + TARGET_PID);
        } else {
            System.err.println("Could not find a run with PID: " + TARGET_PID);
        }
    }
}