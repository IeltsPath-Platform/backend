package com.ieltspath.library.infrastructure.client;

import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.library.application.port.StudyAccessClient.*;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class StudyAccessMetricsTest {
    private static final String BASE = "http://access.test";
    private static final String FAILURE_COUNTER = "vocabulary_study_access_failures";
    private SimpleMeterRegistry registry;
    private MockRestServiceServer server;
    private RestStudyAccessClient client;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setup() {
        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);
        var builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestStudyAccessClient(builder.build(), ResilientCall.isolatedDefault());
        var jwt = Jwt.withTokenValue("study-metric-test").header("alg", "HS256").subject(user.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        Metrics.removeRegistry(registry);
        registry.close();
        server.verify();
    }

    @Test
    void failedDebitCountsOneLogicalFailureAfterTheSharedRetry() {
        server.expect(times(2), requestTo(BASE + "/internal/access/points/debit")).andRespond(withServerError());
        assertThrows(AccessUnavailableException.class, this::debit);
        assertEquals(1.0, failures());
    }

    @Test
    void successfulRetryDoesNotCountAsUnavailable() {
        server.expect(requestTo(BASE + "/internal/access/points/debit")).andRespond(withServerError());
        server.expect(requestTo(BASE + "/internal/access/points/debit"))
                .andRespond(withSuccess("{\"id\":\"" + UUID.randomUUID() + "\",\"balanceAfter\":0}", MediaType.APPLICATION_JSON));
        debit();
        assertEquals(0.0, failures());
    }

    @Test
    void businessDeclinesAndStandardSubscriptionAreNotAvailabilityFailures() {
        server.expect(requestTo(BASE + "/internal/access/points/debit")).andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED));
        server.expect(requestTo(BASE + "/internal/access/points/debit")).andRespond(withStatus(HttpStatus.CONFLICT));
        server.expect(requestTo(BASE + "/api/access/me/subscription")).andRespond(withNoContent());
        assertThrows(InsufficientPointsException.class, this::debit);
        assertThrows(PointsIdempotencyConflictException.class, this::debit);
        assertFalse(client.premium());
        assertEquals(0.0, failures());
    }

    @Test
    void malformedPaymentAndUnexpectedSubscriptionStatusCountAsUnavailable() {
        server.expect(requestTo(BASE + "/internal/access/points/debit"))
                .andRespond(withSuccess("{\"balanceAfter\":0}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/access/me/subscription")).andRespond(withStatus(HttpStatus.CREATED));
        assertThrows(AccessUnavailableException.class, this::debit);
        assertThrows(AccessUnavailableException.class, client::premium);
        assertEquals(2.0, failures());
    }

    @Test
    void premiumTimeoutAndMissingCallerEachCountOnce() {
        server.expect(times(2), requestTo(BASE + "/api/access/me/subscription"))
                .andRespond(withException(new SocketTimeoutException("test timeout")));
        assertThrows(AccessUnavailableException.class, client::premium);
        assertEquals(1.0, failures());
        SecurityContextHolder.clearContext();
        assertThrows(AccessUnavailableException.class, client::premium);
        assertEquals(2.0, failures());
    }

    private double failures() { return registry.counter(FAILURE_COUNTER).count(); }

    private PointsDebit debit() {
        UUID request = UUID.randomUUID();
        return client.debit(user, 5, "HEART_REFILL", request, "vocab-study:" + request, "Heart refill");
    }
}
