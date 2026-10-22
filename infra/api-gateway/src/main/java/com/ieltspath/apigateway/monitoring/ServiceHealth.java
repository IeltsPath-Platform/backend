package com.ieltspath.apigateway.monitoring;

import java.util.List;

public record ServiceHealth(String serviceId, HealthStatus status, List<ServiceHealthInstance> instances) {
}
