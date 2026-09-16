package com.group01.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.security")
public record PublicEndpointProperties(List<String> publicEndpoints) {

    public PublicEndpointProperties {
        publicEndpoints = publicEndpoints == null ? List.of() : List.copyOf(publicEndpoints);
    }
}
