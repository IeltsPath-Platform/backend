package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.port.AccessClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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

    @Autowired
    public RestAccessClient(RestClient.Builder builder,
                            @Value("${learning.access.base-url:http://localhost:8084}") String accessBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        restClient = builder.requestFactory(requestFactory).baseUrl(accessBaseUrl).build();
    }

    RestAccessClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public long balance() {
        Wallet wallet = call(() -> restClient.get().uri("/api/access/me/points")
                .headers(this::forwardRequestHeaders).retrieve().body(Wallet.class));
        return wallet.balance();
    }

    @Override
    public UUID debit(UUID userId, int amount, UUID referenceId, String idempotencyKey, String description) {
        Ledger ledger = call(() -> restClient.post().uri("/internal/access/points/debit")
                .headers(this::forwardRequestHeaders)
                .body(new Debit(userId, amount, REFERENCE_TYPE, referenceId, idempotencyKey, description))
                .retrieve().body(Ledger.class));
        if (ledger.id() == null) throw new AccessUnavailableException(null);
        return ledger.id();
    }

    private <T> T call(Supplier<T> request) {
        try {
            T body = request.get();
            if (body == null) throw new AccessUnavailableException(null);
            return body;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 402) throw new InsufficientPointsException();
            throw new AccessUnavailableException(exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new AccessUnavailableException(null);
        }
    }

    private void forwardRequestHeaders(HttpHeaders headers) {
        headers.setBearerAuth(GatewayRequestHeaders.bearerToken()
                .orElseThrow(() -> new AccessUnavailableException(401)));
        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
    }

    private record Wallet(long balance) {}
    private record Ledger(UUID id) {}
    private record Debit(UUID userId, long amount, String referenceType, UUID referenceId, String idempotencyKey,
                         String description) {}
}
