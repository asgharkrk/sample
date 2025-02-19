package org.example;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestHooks implements EventListener {
    
    // Store Scenario objects directly
    private static List<Scenario> allScenarios = new ArrayList<>();
    
    @After
    public void afterScenario(Scenario scenario) {
        // Store the scenario directly
        allScenarios.add(scenario);
    }
    
    // Helper method to extract feature file name from scenario ID
    private String extractFeatureFileName(String scenarioId) {
        Pattern pattern = Pattern.compile(".*?([^/]+\\.feature)");
        Matcher matcher = pattern.matcher(scenarioId);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown feature file";
    }
    
    // This will be called when all tests are finished
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunFinished.class, event -> {
            processAllScenarios();
        });
    }
    
    private void processAllScenarios() {
        System.out.println("Total Scenarios Run: " + allScenarios.size());
        
        for (Scenario scenario : allScenarios) {
            String featureFileName = extractFeatureFileName(scenario.getId());
            System.out.println("Feature File: " + featureFileName);
            System.out.println("Scenario: " + scenario.getName());
            System.out.println("Status: " + scenario.getStatus());

            
            // You can access embeds, step info, etc. directly from the scenario object
            System.out.println("Has failed: " + scenario.isFailed());
            
            System.out.println("------------------------");
        }

    }
}