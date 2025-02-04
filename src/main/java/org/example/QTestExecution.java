package org.example;

// Import necessary classes for SSL handling, networking, and I/O operations
import javax.net.ssl.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

/**
 * This class demonstrates how to update a qTest test run status via the qTest API.
 * It includes optional SSL certificate verification bypass (useful in development environments).
 */
public class QTestExecution {

    // ========================================================================
    // CONFIGURATION CONSTANTS
    // ========================================================================

    // Base URL of your qTest instance. Update with your actual qTest domain.
    private static final String QTEST_BASE_URL = "https://YOUR_QTEST_DOMAIN/api/v3";

    // Bearer token used for API authentication. Replace <YOUR_BEARER_TOKEN> with your actual token.
    private static final String AUTH_HEADER_VALUE = "Bearer <YOUR_BEARER_TOKEN>";

    // The project ID in qTest where the test run exists.
    private static final long PROJECT_ID = 118109;  // Example project ID

    // The specific test run ID that you want to update.
    private static final long TEST_RUN_ID = 21009876;  // The test run you’re updating

    // ========================================================================
    // METHOD TO DISABLE SSL CERTIFICATE VERIFICATION (OPTIONAL)
    // ========================================================================

    /**
     * Optionally disables SSL certificate verification. This is helpful in a development
     * environment where you might not have valid SSL certificates. Use with caution!
     *
     * @param disable if true, SSL verification will be disabled.
     */
    public static void setDisableSSLVerification(boolean disable) {
        if (disable) {
            try {
                // Create a TrustManager that does not validate certificate chains.
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            // Return an empty array of accepted issuers.
                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                            // Do not perform any checks on the client certificate.
                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            // Do not perform any checks on the server certificate.
                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };

                // Initialize an SSLContext to use our TrustManager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                // Set the default SSL socket factory to our all-trusting one
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                // Create a hostname verifier that approves all hostnames
                HostnameVerifier allHostsValid = (hostname, session) -> true;
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            } catch (Exception e) {
                // Print stack trace if any exception occurs during SSL configuration
                e.printStackTrace();
            }
        }
    }

    // ========================================================================
    // METHOD TO UPDATE THE TEST RUN STATUS IN QTEST
    // ========================================================================

    /**
     * Updates the status of a test run in qTest.
     *
     * @param status the new status for the test run (e.g., "Passed", "Failed").
     * @param note   a note or comment about the execution.
     * @throws IOException if an I/O error occurs when communicating with the API.
     */
    public static void updateTestRunStatus(String status, String note) throws IOException {
        // Optionally disable SSL certificate checks (set to true if needed for your environment)
        setDisableSSLVerification(true);

        // Build the API endpoint URL using the base URL, project ID, and test run ID.
        String urlString = String.format("%s/projects/%d/test-runs/%d",
                QTEST_BASE_URL, PROJECT_ID, TEST_RUN_ID);

        // Create a JSON request body with the new status, execution start/end dates, and a note.
        // Make sure to adjust the field names as needed according to your qTest documentation.
        String requestBodyJson = String.format(
                "{" +
                        "  \"status\": \"%s\"," +
                        "  \"exe_start_date\": \"2025-02-04T10:00:00\"," +
                        "  \"exe_end_date\":   \"2025-02-04T10:05:00\"," +
                        "  \"note\": \"%s\"" +
                        "}", status, note);

        // Create a URL object from the endpoint string
        URL url = new URL(urlString);

        // Open an HTTP connection to the URL
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            // Set the HTTP method to PUT since we are updating an existing resource
            connection.setRequestMethod("PUT");

            // Enable output for the connection to send the JSON body
            connection.setDoOutput(true);

            // Set the Authorization header with the bearer token for API authentication
            connection.setRequestProperty("Authorization", AUTH_HEADER_VALUE);

            // Set the Content-Type header to indicate that we are sending JSON data
            connection.setRequestProperty("Content-Type", "application/json");

            // Send the JSON data in the request body
            try (OutputStream os = connection.getOutputStream()) {
                // Convert the JSON string to bytes using UTF-8 encoding
                byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                // Write the bytes to the output stream
                os.write(input, 0, input.length);
            }

            // Retrieve the HTTP response code to check if the update was successful
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                // Print success message if the HTTP response code indicates success (2xx)
                System.out.println("QTest: Successfully updated Test Run status to " + status);
            } else {
                // Print an error message if the update fails (non-2xx HTTP code)
                System.err.println("QTest: Failed to update Test Run. HTTP code: " + responseCode);
            }
        } finally {
            // Disconnect the HTTP connection to free up resources
            connection.disconnect();
        }
    }

    // ========================================================================
    // MAIN METHOD - ENTRY POINT FOR THE EXAMPLE
    // ========================================================================

    /**
     * The main method serves as an example usage of the updateTestRunStatus method.
     * It attempts to update a test run status to "Passed" with an accompanying note.
     *
     * @param args command-line arguments (not used in this example)
     */
    public static void main(String[] args) {
        try {
            // Call updateTestRunStatus with the status "Passed" and a note.
            updateTestRunStatus("Passed", "Automated execution via API");
        } catch (IOException e) {
            // Print the stack trace if an IOException occurs during the update process.
            e.printStackTrace();
        }
    }
}