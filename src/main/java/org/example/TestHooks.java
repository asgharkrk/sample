package org.example;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Scenario;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestHooks {
    
    private static List<ScenarioData> allScenarioData = new ArrayList<>();
    
    @After
    public void afterScenario(Scenario scenario) {
        ScenarioData data = new ScenarioData();
        data.setName(scenario.getName());
        data.setStatus(scenario.getStatus().toString());
        data.setTagNames(scenario.getSourceTagNames());
        
        // Extract feature file name from the scenario ID
        String featureFileName = extractFeatureFileName(scenario.getId());
        data.setFeatureFileName(featureFileName);
        
        allScenarioData.add(data);
    }
    
    // Helper method to extract feature file name from scenario ID
    private String extractFeatureFileName(String scenarioId) {
        // Scenario ID format is typically: "file:path/to/featurefile.feature:lineNumber"
        Pattern pattern = Pattern.compile(".*?([^/]+\\.feature)");
        Matcher matcher = pattern.matcher(scenarioId);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown feature file";
    }
    
    @AfterAll
    public static void afterAllScenarios() {
        System.out.println("Total Scenarios Run: " + allScenarioData.size());
        
        for (ScenarioData data : allScenarioData) {
            System.out.println("Feature File: " + data.getFeatureFileName());
            System.out.println("Scenario: " + data.getName());
            System.out.println("Status: " + data.getStatus());
            System.out.println("Tags: " + String.join(", ", data.getTagNames()));
            System.out.println("------------------------");
        }
    }
    
    static class ScenarioData {
        private String name;
        private String status;
        private Collection<String> tagNames;
        private String featureFileName;
        
        // Original getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Collection<String> getTagNames() { return tagNames; }
        public void setTagNames(Collection<String> tagNames) { this.tagNames = tagNames; }
        
        // New getter and setter for feature file name
        public String getFeatureFileName() { return featureFileName; }
        public void setFeatureFileName(String featureFileName) { this.featureFileName = featureFileName; }
    }
}