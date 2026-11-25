package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.port.AccessClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/** Calls Access Service with the learner's bearer token; the balance and debit are the learner's own. */
@Component
public class RestAccessClient implements AccessClient {
    static final String REFERENCE_TYPE = "LESSON_WRITING";
    private final RestClient restClient;
    private final ResilientCall resilientCall;
    private final java.util.function.Supplier<java.util.Optional<String>> workerToken;

    @Autowired
    public RestAccessClient(RestClient.Builder builder,
                            @Value("${learning.access.base-url:http://localhost:8084}") String accessBaseUrl,
                            ResilientCall resilientCall, LearningWorkerTokenProvider workerTokens) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        restClient = builder.requestFactory(requestFactory).baseUrl(accessBaseUrl).build();
        this.resilientCall = resilientCall;
        this.workerToken = () -> java.util.Optional.of(workerTokens.token());
    }

    RestAccessClient(RestClient restClient) {
        this(restClient, defaultResilientCall(), java.util.Optional::empty);
    }

    RestAccessClient(RestClient restClient, ResilientCall resilientCall) {
        this(restClient, resilientCall, java.util.Optional::empty);
    }

    RestAccessClient(RestClient restClient, java.util.function.Supplier<java.util.Optional<String>> workerToken) {
        this(restClient, defaultResilientCall(), workerToken);
    }

    private RestAccessClient(RestClient restClient, ResilientCall resilientCall,
                             java.util.function.Supplier<java.util.Optional<String>> workerToken) {
        this.restClient = restClient;
        this.resilientCall = resilientCall;
        this.workerToken = workerToken;
    }

    @Override
    public long balance() {
        Wallet wallet = call(() -> restClient.get().uri("/api/access/me/points")
                .headers(this::forwardRequestHeaders).retrieve().body(Wallet.class), CallKind.READ);
        return wallet.balance();
    }

    @Override
    public boolean premium() {
        // Access answers 200 while a subscription is active and 204 when there is none; other statuses are failures.
        ResponseEntity<Void> response = call(() -> restClient.get().uri("/api/access/me/subscription")
                .headers(this::forwardRequestHeaders).retrieve().toBodilessEntity(), CallKind.READ);
        return response.getStatusCode().value() == 200;
    }

    @Override
    public boolean premiumOf(UUID userId) {
        try {
            Premium response = call(() -> restClient.get().uri("/internal/access/users/{userId}/premium", userId)
                    .headers(headers -> {
                        headers.setBearerAuth(workerToken.get().orElseThrow(() -> new AccessUnavailableException(null)));
                        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
                    }).retrieve().body(Premium.class), CallKind.READ);
            return response.premium();
        } catch (RuntimeException failure) {
            return false;
        }
    }

    @Override
    public UUID debit(UUID userId, int amount, String referenceType, UUID referenceId, String idempotencyKey,
                      String description) {
        Ledger ledger = call(() -> restClient.post().uri("/internal/access/points/debit")
                .headers(this::forwardRequestHeaders)
                .body(new Debit(userId, amount, referenceType, referenceId, idempotencyKey, description))
                .retrieve().body(Ledger.class), CallKind.IDEMPOTENT_WRITE);
        if (ledger.id() == null) throw new AccessUnavailableException(null);
        return ledger.id();
    }

    private <T> T call(Supplier<T> request, CallKind kind) {
        try {
            T body = resilientCall.call("access", kind, request);
            if (body == null) throw new AccessUnavailableException(null);
            return body;
        } catch (DownstreamUnavailableException exception) {
            throw new AccessUnavailableException(null);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 402) throw new InsufficientPointsException();
            throw new AccessUnavailableException(exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new AccessUnavailableException(null);
        }
    }

    private static ResilientCall defaultResilientCall() {
        return ResilientCall.isolatedDefault();
    }

    private void forwardRequestHeaders(HttpHeaders headers) {
        headers.setBearerAuth(GatewayRequestHeaders.bearerToken()
                .orElseThrow(() -> new AccessUnavailableException(401)));
        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
    }

    private record Wallet(long balance) {}
    private record Premium(boolean premium) {}
    private record Ledger(UUID id) {}
    private record Debit(UUID userId, long amount, String referenceType, UUID referenceId, String idempotencyKey,
                         String description) {}
}
