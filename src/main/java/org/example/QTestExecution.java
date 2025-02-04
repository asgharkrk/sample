package org.example;


import javax.net.ssl.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

public class QTestExecution {

    // Update these accordingly
    private static final String QTEST_BASE_URL     = "https://YOUR_QTEST_DOMAIN/api/v3";
    private static final String AUTH_HEADER_VALUE  = "Bearer <YOUR_BEARER_TOKEN>";
    private static final long   PROJECT_ID         = 118109;  // e.g. 118109
    private static final long   TEST_RUN_ID        = 21009876;  // The test run you’re updating

    // Optional: disable SSL certificate checking if needed
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


    public static void updateTestRunStatus(String status, String note) throws IOException {
        // If you must skip SSL certificate checks:
        setDisableSSLVerification(true);

        // Construct the URL for the test-runs endpoint
        String urlString = String.format("%s/projects/%d/test-runs/%d",
                QTEST_BASE_URL, PROJECT_ID, TEST_RUN_ID);

        // Example JSON request body
        // Check your qTest docs for exact field names ("status" vs "executionStatus", etc.).
        String requestBodyJson = String.format(
                "{" +
                        "  \"status\": \"%s\"," +
                        "  \"exe_start_date\": \"2025-02-04T10:00:00\"," +
                        "  \"exe_end_date\":   \"2025-02-04T10:05:00\"," +
                        "  \"note\": \"%s\"" +
                        "}", status, note);

        // Prepare connection
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            connection.setRequestProperty("Content-Type", "application/json");

            // Send JSON data
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("QTest: Successfully updated Test Run status to " + status);
            } else {
                System.err.println("QTest: Failed to update Test Run. HTTP code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    // Example usage
    public static void main(String[] args) {
        try {
            // Mark the test as Passed
            updateTestRunStatus("Passed", "Automated execution via API");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
