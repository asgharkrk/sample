package org.example;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Scenario;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestHooks {
    
    // Store scenarios grouped by feature file
    private static Map<String, List<Scenario>> scenariosByFeature = new HashMap<>();
    
    @After
    public void afterScenario(Scenario scenario) {
        // Extract feature file name
        String featureFileName = extractFeatureFileName(scenario.getId());
        
        // Get or create list for this feature
        scenariosByFeature.computeIfAbsent(featureFileName, k -> new ArrayList<>())
                         .add(scenario);
    }
    
    @AfterAll
    public static void processAllScenarios() {
        

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