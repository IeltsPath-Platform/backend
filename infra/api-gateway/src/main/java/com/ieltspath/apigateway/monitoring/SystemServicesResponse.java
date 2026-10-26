package com.ieltspath.apigateway.monitoring;

import java.time.Instant;
import java.util.List;

public record SystemServicesResponse(Instant checkedAt, List<ServiceHealth> services) {
}
