package org.example;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;

public class QTestHooks {
    private QTestClient qTestClient;
    private String projectId = "12345"; // Your qTest project ID

    public QTestHooks() {
        // Initialize with your qTest credentials
        this.qTestClient = new QTestClient(
            "https://your.qtest.instance",
            "client-id",
            "client-secret"
        );
    }

    @After
    public void updateQTest(Scenario scenario) {
        // Extract qTest Test Case ID from scenario tags (e.g., @QTEST-5678)
        String testCaseId = scenario.getId();

        // Map Cucumber status to qTest status (e.g., PASSED, FAILED)
        String status = scenario.isFailed() ? "FAILED" : "PASSED";

        // Update qTest via API
        qTestClient.updateTestResult(projectId, testCaseId, status);
    }
}