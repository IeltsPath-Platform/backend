package com.group01.user.config;

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
        info = @Info(title = "User Service", version = "v1",
                description = "Register, login, token refresh and the current user's profile."),
        servers = @Server(url = "/", description = "API Gateway"),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
