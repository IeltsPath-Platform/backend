package com.group01.assessment.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the Gateway Swagger UI. The relative server sends "Try it out" calls through the Gateway,
 * which verifies the external JWT and forwards an internal one; calling this service directly would get 401.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Assessment Service", version = "v1",
                description = "Test attempts and answers, results, examiner and AI grading, learner submissions and video practice. Contract: docs/contracts/lesson-learning-v1.md."),
        servers = @Server(url = "/", description = "API Gateway"),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
