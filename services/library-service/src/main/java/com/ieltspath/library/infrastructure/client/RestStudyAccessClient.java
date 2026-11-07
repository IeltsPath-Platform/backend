package com.ieltspath.library.infrastructure.client;

import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.library.application.port.StudyAccessClient;
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

@Component
public class RestStudyAccessClient implements StudyAccessClient {
    private final RestClient client;
    private final ResilientCall resilient;

    @Autowired
    public RestStudyAccessClient(RestClient.Builder builder,
                                 @Value("${library.access.base-url:http://localhost:8084}") String baseUrl,
                                 ResilientCall resilient) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.resilient = resilient;
    }

    RestStudyAccessClient(RestClient client, ResilientCall resilient) {
        this.client = client;
        this.resilient = resilient;
    }

    @Override
    public PointsDebit debit(UUID userId, long amount, String referenceType, UUID referenceId,
                             String idempotencyKey, String description) {
        Ledger response = call(() -> client.post().uri("/internal/access/points/debit")
                .headers(this::forwardHeaders)
                .body(new Debit(userId, amount, referenceType, referenceId, idempotencyKey, description))
                .retrieve().body(Ledger.class), CallKind.IDEMPOTENT_WRITE, true);
        if (response == null || response.id() == null || response.balanceAfter() == null) {
            throw new AccessUnavailableException();
        }
        return new PointsDebit(response.id(), response.balanceAfter());
    }

    @Override
    public boolean premium() {
        var response = call(() -> client.get().uri("/api/access/me/subscription")
                .headers(this::forwardHeaders).retrieve().toBodilessEntity(), CallKind.READ, false);
        return switch (response.getStatusCode().value()) {
            case 200 -> true;
            case 204 -> false;
            default -> throw new AccessUnavailableException();
        };
    }

    private <T> T call(Supplier<T> request, CallKind kind, boolean debit) {
        try {
            return resilient.call("access", kind, request);
        } catch (DownstreamUnavailableException unavailable) {
            throw new AccessUnavailableException(unavailable);
        } catch (RestClientResponseException response) {
            if (debit && response.getStatusCode().value() == 402) throw new InsufficientPointsException();
            if (debit && response.getStatusCode().value() == 409) throw new PointsIdempotencyConflictException();
            throw new AccessUnavailableException();
        } catch (RestClientException unavailable) {
            throw new AccessUnavailableException(unavailable);
        }
    }

    private void forwardHeaders(HttpHeaders headers) {
        headers.setBearerAuth(GatewayRequestHeaders.bearerToken().orElseThrow(AccessUnavailableException::new));
        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
    }

    private record Debit(UUID userId, long amount, String referenceType, UUID referenceId,
                         String idempotencyKey, String description) {}
    private record Ledger(UUID id, Long balanceAfter) {}
}
