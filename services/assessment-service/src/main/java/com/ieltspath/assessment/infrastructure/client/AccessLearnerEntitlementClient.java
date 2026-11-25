package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.exception.EntitlementUnavailableException;
import com.ieltspath.assessment.application.port.LearnerEntitlement;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class AccessLearnerEntitlementClient implements LearnerEntitlement {
    private final RestClient client;
    private final ResilientCall resilientCall;

    @Autowired
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
                    .header(org.springframework.http.HttpHeaders.AUTHORIZATION, GatewayRequestContext.bearerToken())
                    .header("X-Correlation-Id", GatewayRequestContext.correlationId())
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
}
