package com.ieltspath.assessment.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.ieltspath.assessment.domain.exception.HumanGradingCreditConflictException;
import com.ieltspath.assessment.domain.exception.HumanGradingCreditInsufficientException;

import java.util.UUID;
import java.time.Duration;

@Component
public class AccessHumanGradingCreditClient {
    private final RestClient client;

    @Autowired
    public AccessHumanGradingCreditClient(RestClient.Builder builder,
            @Value("${ACCESS_SERVICE_URL:http://localhost:8084}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.requestFactory(factory).baseUrl(baseUrl).build();
    }

    AccessHumanGradingCreditClient(RestClient client) {
        this.client = client;
    }

    public void consume(UUID submissionId) {
        post("/consume", submissionId);
    }

    public void refund(UUID submissionId) {
        post("/refund", submissionId);
    }

    private void post(String path, UUID submissionId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new IllegalStateException("A learner bearer token is required for human grading credits");
        }
        try {
            client.post().uri("/internal/access/human-grading" + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getToken().getTokenValue())
                    .body(new CreditRequest(submissionId)).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                throw new HumanGradingCreditInsufficientException();
            }
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                throw new HumanGradingCreditConflictException();
            }
            throw exception;
        }
    }

    private record CreditRequest(UUID submissionId) {}
}
