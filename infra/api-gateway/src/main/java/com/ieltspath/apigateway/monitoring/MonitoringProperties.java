package com.ieltspath.apigateway.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.monitoring")
public class MonitoringProperties {
    private List<String> expectedServices = new ArrayList<>(List.of(
            "user-service",
            "access-service",
            "assessment-service",
            "content-service",
            "learning-service",
            "library-service",
            "community-service",
            "game-service",
            "notification-service"
    ));

    public List<String> getExpectedServices() {
        return expectedServices;
    }

    public void setExpectedServices(List<String> expectedServices) {
        this.expectedServices = expectedServices == null ? List.of() : List.copyOf(expectedServices);
    }
}
