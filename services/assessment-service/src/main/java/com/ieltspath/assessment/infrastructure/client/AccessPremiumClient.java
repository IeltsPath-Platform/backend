package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.port.PremiumLookup;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.ResilientCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

@Component
public class AccessPremiumClient implements PremiumLookup {
    private final RestClient client;
    private final ResilientCall resilientCall;
    private final AssessmentServiceTokenProvider serviceTokens;

    @Autowired
    public AccessPremiumClient(RestClient.Builder builder,
                              @Value("${assessment.access.base-url:http://localhost:8084}") String baseUrl,
                              ResilientCall resilientCall, AssessmentServiceTokenProvider serviceTokens) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.resilientCall = resilientCall;
        this.serviceTokens = serviceTokens;
    }

    AccessPremiumClient(RestClient client, ResilientCall resilientCall,
                        AssessmentServiceTokenProvider serviceTokens) {
        this.client = client;
        this.resilientCall = resilientCall;
        this.serviceTokens = serviceTokens;
    }

    @Override
    public boolean isPremium(UUID userId) {
        try {
            PremiumResponse response = resilientCall.call("access", CallKind.READ, () -> client.get()
                    .uri("/internal/access/users/{userId}/premium", userId)
                    .header(HttpHeaders.AUTHORIZATION, serviceTokens.authorizationHeader())
                    .header("X-Correlation-Id", GatewayRequestContext.correlationId())
                    .retrieve().body(PremiumResponse.class));
            return response != null && response.premium();
        } catch (RuntimeException failure) {
            return false;
        }
    }

    private record PremiumResponse(boolean premium) {}
}
