package com.group01.commonsecurity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record DownstreamSecurityProperties(
        List<PublicEndpoint> publicEndpoints
) {
    public DownstreamSecurityProperties {
        publicEndpoints = publicEndpoints == null ? List.of() : List.copyOf(publicEndpoints);
    }

    public record PublicEndpoint(
            HttpMethod method,
            List<String> patterns
    ) {
        public PublicEndpoint {
            patterns = patterns == null ? List.of() : patterns.stream()
                    .filter(pattern -> pattern != null && !pattern.isBlank())
                    .toList();
        }
    }
}
