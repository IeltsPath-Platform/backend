package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.exception.EntitlementUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class AccessLearnerEntitlementClientTest {
    private static final String BASE_URL = "http://access.test";
    private static final String SUBSCRIPTION = BASE_URL + "/api/access/me/subscription";

    private MockRestServiceServer server;
    private AccessLearnerEntitlementClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AccessLearnerEntitlementClient(builder.build(), ResilientCall.isolatedDefault());
        Jwt jwt = new Jwt("learner-token", Instant.now(), Instant.now().plusSeconds(60),
                java.util.Map.of("alg", "HS256"), java.util.Map.of("sub", "00000000-0000-4000-8000-000000000001"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anActiveSubscriptionIsPremiumAndTheLearnerTokenIsForwarded() {
        server.expect(requestTo(SUBSCRIPTION)).andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer learner-token"))
                .andRespond(withStatus(OK));

        assertTrue(client.premium());
        server.verify();
    }

    @Test
    void noSubscriptionMeansNotPremium() {
        server.expect(requestTo(SUBSCRIPTION)).andRespond(withStatus(NO_CONTENT));

        assertFalse(client.premium());
        server.verify();
    }

    @Test
    void aFailedLookupIsUnavailableNeverPremium() {
        server.expect(times(2), requestTo(SUBSCRIPTION)).andRespond(withStatus(INTERNAL_SERVER_ERROR));

        assertThrows(EntitlementUnavailableException.class, () -> client.premium());
        server.verify();
    }

    @Test
    void withoutALearnerTokenTheLookupIsUnavailable() {
        SecurityContextHolder.clearContext();

        assertThrows(EntitlementUnavailableException.class, () -> client.premium());
        server.verify();
    }
}
