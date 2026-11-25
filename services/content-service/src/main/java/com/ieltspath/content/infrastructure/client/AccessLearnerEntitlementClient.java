package com.ieltspath.content.infrastructure.client;

import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.content.application.exception.EntitlementUnavailableException;
import com.ieltspath.content.application.port.LearnerEntitlement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.UUID;

@Component
public class AccessLearnerEntitlementClient implements LearnerEntitlement {
    private final RestClient client;
    private final ResilientCall resilientCall;

    public AccessLearnerEntitlementClient(RestClient.Builder builder,
                                         @Value("${ACCESS_SERVICE_URL:http://localhost:8084}") String baseUrl,
                                         ResilientCall resilientCall) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.resilientCall = resilientCall;
    }

    AccessLearnerEntitlementClient(RestClient client, ResilientCall resilientCall) {
        this.client = client;
        this.resilientCall = resilientCall;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean premium() {
        try {
            ResponseEntity<Void> response = resilientCall.call("access", CallKind.READ, () -> client.get()
                    .uri("/api/access/me/subscription")
                    .headers(this::forwardRequestHeaders)
                    .retrieve().toBodilessEntity());
            if (response.getStatusCode().value() == 200) return true;
            if (response.getStatusCode().value() == 204) return false;
            throw new EntitlementUnavailableException(null);
        } catch (EntitlementUnavailableException exception) {
            throw exception;
        } catch (DownstreamUnavailableException | RestClientException | IllegalStateException exception) {
            throw new EntitlementUnavailableException(exception);
        }
    }

    private void forwardRequestHeaders(HttpHeaders headers) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            throw new IllegalStateException("Authenticated learner token is unavailable");
        }
        headers.setBearerAuth(jwt.getToken().getTokenValue());
        String correlationId = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            correlationId = attributes.getRequest().getHeader(SecurityHeaders.CORRELATION_ID);
        }
        headers.set(SecurityHeaders.CORRELATION_ID,
                correlationId == null || correlationId.isBlank() || correlationId.length() > 128
                        ? UUID.randomUUID().toString() : correlationId);
    }
}
