package com.ieltspath.library.infrastructure.client;

import com.ieltspath.library.application.exception.TopicServiceUnavailableException;
import com.ieltspath.library.application.port.TopicLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.UUID;

@Component
public class ContentTopicClient implements TopicLookup {
    private final RestClient restClient;

    public ContentTopicClient(RestClient.Builder builder,
                              @Value("${library.content.base-url:http://localhost:8082}") String contentBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        restClient = builder.requestFactory(requestFactory).baseUrl(contentBaseUrl).build();
    }

    @Override
    public boolean exists(UUID topicId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !authentication.isAuthenticated()) {
            throw new TopicServiceUnavailableException(new IllegalStateException("Authenticated gateway JWT required"));
        }
        try {
            restClient.get()
                    .uri("/api/content/topics/{id}", topicId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken().getTokenValue())
                    .header("X-Correlation-Id", correlationId())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound exception) {
            return false;
        } catch (RestClientException exception) {
            throw new TopicServiceUnavailableException(exception);
        }
    }

    private String correlationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader("X-Correlation-Id");
            if (value != null && !value.isBlank() && value.length() <= 128) return value;
        }
        return UUID.randomUUID().toString();
    }
}
