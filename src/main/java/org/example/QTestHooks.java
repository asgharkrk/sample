package org.example;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;

import java.io.IOException;
import java.util.Collections;

/**
 * A simple Cucumber Hooks class that, after each scenario,
 * links test-case IDs to your QTest requirement.
 */
public class QTestHooks {

    // Example: We'll link only one test case (49888615) after each scenario.
    // You can adjust this logic to handle multiple IDs, read from tags, scenario name, etc.
    private static final String TEST_CASE_ID = "49888615";

    @After
    public void afterScenario(Scenario scenario) {
        try {
            // Call the QTest utility to link the test case
            QTestApiClient.linkTestCasesToRequirement(Collections.singletonList(TEST_CASE_ID));

            // Log scenario outcome
            if (scenario.isFailed()) {
                System.out.println("Scenario FAILED: " + scenario.getName());
            } else {
                System.out.println("Scenario PASSED: " + scenario.getName());
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error linking test case to QTest: " + e.getMessage());
        }
    }
}