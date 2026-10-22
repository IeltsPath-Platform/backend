package com.ieltspath.apigateway.monitoring;

public record ServiceHealthInstance(String instanceId, String uri, HealthStatus status, long responseMs) {
}
