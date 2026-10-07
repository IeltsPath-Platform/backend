package com.group01.assessment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Runtime settings for the optional OpenAI-compatible essay grader. */
@Component
@ConfigurationProperties(prefix = "assessment.llm-grading")
public class AssessmentLlmProperties {
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public boolean configured() {
        return hasText(baseUrl) && hasText(apiKey) && hasText(model);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
