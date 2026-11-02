package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.port.HumanGradingCreditPort;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
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
import org.springframework.web.client.RestClientException;
import com.ieltspath.assessment.domain.exception.HumanGradingCreditConflictException;
import com.ieltspath.assessment.domain.exception.HumanGradingCreditInsufficientException;

import java.util.UUID;
import java.time.Duration;

@Component
public class AccessHumanGradingCreditClient implements HumanGradingCreditPort {
    private final RestClient client;
    private final ResilientCall resilientCall;

    @Autowired
    public AccessHumanGradingCreditClient(RestClient.Builder builder,
            @Value("${ACCESS_SERVICE_URL:http://localhost:8084}") String baseUrl, ResilientCall resilientCall) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.resilientCall = resilientCall;
    }

    AccessHumanGradingCreditClient(RestClient client) {
        this(client, ResilientCall.isolatedDefault());
    }

    AccessHumanGradingCreditClient(RestClient client, ResilientCall resilientCall) {
        this.client = client;
        this.resilientCall = resilientCall;
    }

    @Override
    public boolean consume(UUID submissionId) {
        CreditResponse response = post("/consume", submissionId);
        return response != null && Boolean.TRUE.equals(response.charged());
    }

    @Override
    public void refund(UUID submissionId) {
        post("/refund", submissionId);
    }

    private CreditResponse post(String path, UUID submissionId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new IllegalStateException("A learner bearer token is required for human grading credits");
        }
        try {
            return resilientCall.call("access", CallKind.IDEMPOTENT_WRITE, () -> client.post().uri("/internal/access/human-grading" + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getToken().getTokenValue())
                    .body(new CreditRequest(submissionId)).retrieve().body(CreditResponse.class));
        } catch (DownstreamUnavailableException unavailable) {
            throw new RestClientException("Access Service is temporarily unavailable", unavailable);
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

    private record CreditResponse(UUID submissionId, Integer remainingCredits, Boolean charged) {}
}
