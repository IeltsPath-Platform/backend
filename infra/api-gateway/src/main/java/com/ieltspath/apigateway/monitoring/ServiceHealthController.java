package com.ieltspath.apigateway.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class ServiceHealthController {
    private final ServiceHealthChecker healthChecker;

    @GetMapping("/services")
    public Mono<SystemServicesResponse> services() {
        return healthChecker.checkServices();
    }
}
