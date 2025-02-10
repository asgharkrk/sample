package org.example;// TestLogHooks.java
import io.cucumber.java.After;
import io.cucumber.java.Scenario;

import static org.example.APIUtils.postAPI;

public class TestLogHooks {


    @After
    public void afterScenario(Scenario scenario) {

       String payload= APIUtils.buildPayload( scenario.getName(),scenario.getStatus().toString(),"Happy Notes");
        postAPI(payload);
    }
}