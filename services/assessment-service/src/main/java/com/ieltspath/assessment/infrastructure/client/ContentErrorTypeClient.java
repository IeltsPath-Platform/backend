package com.ieltspath.assessment.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ieltspath.assessment.application.exception.ContentUnavailableException;
import com.ieltspath.assessment.application.port.ErrorTypeCatalog;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import org.springframework.beans.factory.annotation.Autowired;
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

/** Reads approved active error codes with the assessment service identity. */
@Component
public class ContentErrorTypeClient implements ErrorTypeCatalog {
    private final RestClient restClient;
    private final AssessmentServiceTokenProvider serviceTokens;
    private final ResilientCall resilientCall;

    @Autowired
    public ContentErrorTypeClient(RestClient.Builder builder,
                                  @Value("${assessment.content.base-url:http://localhost:8082}") String contentBaseUrl,
                                  AssessmentServiceTokenProvider serviceTokens, ResilientCall resilientCall) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(contentBaseUrl).build();
        this.serviceTokens = serviceTokens;
        this.resilientCall = resilientCall;
    }

    ContentErrorTypeClient(RestClient.Builder builder, String contentBaseUrl, AssessmentServiceTokenProvider tokens) {
        this(builder, contentBaseUrl, tokens, ResilientCall.isolatedDefault());
    }

    @Override
    public Set<String> activeCodes() {
        return activeErrorTypes().stream().map(ErrorType::code).collect(Collectors.toSet());
    }

    @Override
    public Set<String> staffCodes() {
        return fetch("/internal/learning-content/error-types/staff").stream().map(ErrorType::code)
                .collect(Collectors.toSet());
    }

    @Override
    public List<ErrorType> activeErrorTypes() {
        return fetch("/internal/learning-content/error-types");
    }

    /** {@code skill} is a fixed enum name from the grader, so it needs no escaping in the query. */
    @Override
    public List<ErrorType> activeErrorTypes(String skill) {
        return fetch("/internal/learning-content/error-types?skill=" + skill);
    }

    private List<ErrorType> fetch(String path) {
        List<ErrorTypeResponse> response;
        try {
            response = resilientCall.call("content", CallKind.READ, () -> restClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, serviceTokens.authorizationHeader())
                    .header("X-Correlation-Id", GatewayRequestContext.correlationId())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<ErrorTypeResponse>>() {}));
        } catch (DownstreamUnavailableException exception) {
            throw new ContentUnavailableException("Content Service could not provide the error types", exception);
        } catch (RestClientException exception) {
            throw new ContentUnavailableException("Content Service could not provide the error types", exception);
        }
        if (response == null) {
            throw new ContentUnavailableException("Content Service returned no error types");
        }
        return response.stream().filter(Objects::nonNull).filter(item -> item.code() != null)
                .map(item -> new ErrorType(item.code(), Objects.toString(item.name(), item.code()),
                        Objects.toString(item.fixHint(), ""))).toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorTypeResponse(String code, String name, String category, int severity, String fixHint) {}
}
