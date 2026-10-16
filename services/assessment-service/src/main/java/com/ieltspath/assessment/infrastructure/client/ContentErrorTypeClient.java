package com.ieltspath.assessment.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ieltspath.assessment.application.exception.ContentUnavailableException;
import com.ieltspath.assessment.application.port.ErrorTypeCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Reads {@code GET /internal/learning-content/error-types}, the active codes only, with the caller's bearer. */
@Component
public class ContentErrorTypeClient implements ErrorTypeCatalog {
    private final RestClient restClient;

    public ContentErrorTypeClient(RestClient.Builder builder,
                                  @Value("${assessment.content.base-url:http://localhost:8082}") String contentBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(contentBaseUrl).build();
    }

    @Override
    public Set<String> activeCodes() {
        List<ErrorTypeResponse> response;
        try {
            response = restClient.get()
                    .uri("/internal/learning-content/error-types")
                    .header(HttpHeaders.AUTHORIZATION, GatewayRequestContext.bearerToken())
                    .header("X-Correlation-Id", GatewayRequestContext.correlationId())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<ErrorTypeResponse>>() {});
        } catch (RestClientException exception) {
            throw new ContentUnavailableException("Content Service could not provide the error types", exception);
        }
        if (response == null) {
            throw new ContentUnavailableException("Content Service returned no error types");
        }
        return response.stream().map(ErrorTypeResponse::code).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorTypeResponse(String code) {}
}
