package org.example;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;


public class QTestApiClientCopy {


    private static final String PROJECT_ID = "";
    private static final String REQUIREMENT_ID = "";

    private static final String QTEST_BASE_URL ="";


    private static final String AUTH_HEADER_VALUE = "Bearer <YOUR_BEARER_TOKEN>";


    public static void setDisableSSLVerification(boolean disable) {
        if (disable) {
            try {
                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                                // No need to implement.
                            }
                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                                // No need to implement.
                            }
                        }
                };

                // Install the all-trusting trust manager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                // Create an all-trusting host name verifier
                HostnameVerifier allHostsValid = new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void linkTestCasesToRequirement(List<String> testCaseIds) throws IOException {
        setDisableSSLVerification(true);

        if (testCaseIds == null || testCaseIds.isEmpty()) {
            System.err.println("No testCaseIds provided to link.");
            return;
        }

        String requestBodyJson = testCaseIds.toString();

        // Prepare the connection
        URL url = new URL(QTEST_BASE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", AUTH_HEADER_VALUE);
            connection.setRequestProperty("Content-Type", "application/json");

            // Write the JSON array into the request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("QTest: Successfully linked test case(s) " + testCaseIds);
            } else {
                System.err.println("QTest: Failed to link test case(s). HTTP code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
}