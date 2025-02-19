package org.example;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestHooks {

    // Store scenarios in a static list
    private static List<Scenario> allScenarios = new ArrayList<>();

    @After
    public void afterScenario(Scenario scenario) {
        // Add the current scenario to our list
        allScenarios.add(scenario);
    }

    // Use Cucumber's @AfterAll (not JUnit's)
    @AfterAll
    public static void processAllScenarios() {
        System.out.println("Total Scenarios Run: " + allScenarios.size());

        for (Scenario scenario : allScenarios) {
            String featureFileName = extractFeatureFileName(scenario.getId());
            System.out.println("Feature File: " + featureFileName);
            System.out.println("Scenario: " + scenario.getName());
            System.out.println("Status: " + scenario.getStatus());
            System.out.println("Tags: " + String.join(", ", scenario.getSourceTagNames()));
            System.out.println("------------------------");
        }

        // Calculate statistics
        int passedCount = (int) allScenarios.stream().filter(s -> !s.isFailed()).count();
        System.out.println("Pass rate: " + (passedCount * 100.0 / allScenarios.size()) + "%");
    }

    private static String extractFeatureFileName(String scenarioId) {
        Pattern pattern = Pattern.compile(".*?([^/]+\\.feature)");
        Matcher matcher = pattern.matcher(scenarioId);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown feature file";
    }
}