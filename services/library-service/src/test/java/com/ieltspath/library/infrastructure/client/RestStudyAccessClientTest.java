package com.ieltspath.library.infrastructure.client;

import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.library.application.port.StudyAccessClient.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestStudyAccessClientTest {
    private static final String BASE = "http://access.test";
    private final UUID user = UUID.randomUUID();
    private MockRestServiceServer server;
    private RestStudyAccessClient client;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestStudyAccessClient(builder.build(), ResilientCall.isolatedDefault());
        var jwt = Jwt.withTokenValue("study-test-token").header("alg", "HS256").subject(user.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        var request = new MockHttpServletRequest();
        request.addHeader(SecurityHeaders.CORRELATION_ID, "study-correlation");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        server.verify();
    }

    @Test
    void debitForwardsIdentityCorrelationAndTheExactStoredPurchaseParameters() {
        UUID request = UUID.randomUUID(), ledger = UUID.randomUUID();
        server.expect(requestTo(BASE + "/internal/access/points/debit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer study-test-token"))
                .andExpect(header(SecurityHeaders.CORRELATION_ID, "study-correlation"))
                .andExpect(jsonPath("$.userId").value(user.toString()))
                .andExpect(jsonPath("$.amount").value(5))
                .andExpect(jsonPath("$.referenceType").value("HEART_REFILL"))
                .andExpect(jsonPath("$.referenceId").value(request.toString()))
                .andExpect(jsonPath("$.idempotencyKey").value("vocab-study:" + request))
                .andExpect(jsonPath("$.description").value("Hồi tim (từ vựng)"))
                .andRespond(withSuccess("{\"id\":\"" + ledger + "\",\"balanceAfter\":37}", MediaType.APPLICATION_JSON));

        assertEquals(new PointsDebit(ledger, 37), client.debit(user, 5, "HEART_REFILL", request,
                "vocab-study:" + request, "Hồi tim (từ vựng)"));
    }

    @Test
    void premiumUsesTheLearnerTokenAndDistinguishesActiveAndMissingSubscription() {
        server.expect(requestTo(BASE + "/api/access/me/subscription")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer study-test-token"))
                .andExpect(header(SecurityHeaders.CORRELATION_ID, "study-correlation"))
                .andRespond(withSuccess());
        server.expect(requestTo(BASE + "/api/access/me/subscription")).andRespond(withNoContent());
        assertTrue(client.premium());
        assertFalse(client.premium());
    }

    @Test
    void insufficientPointsDoesNotRetryAndHasItsOwnException() {
        server.expect(once(), requestTo(BASE + "/internal/access/points/debit"))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED));
        assertThrows(InsufficientPointsException.class, this::debit);
    }

    @Test
    void idempotencyConflictDoesNotRetryAndHasItsOwnException() {
        server.expect(once(), requestTo(BASE + "/internal/access/points/debit"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        assertThrows(PointsIdempotencyConflictException.class, this::debit);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404})
    void otherClientErrorsAreUnavailableWithoutRetry(int status) {
        server.expect(once(), requestTo(BASE + "/internal/access/points/debit"))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)));
        assertThrows(AccessUnavailableException.class, this::debit);
    }

    @Test
    void transientDebitFailureUsesTheSharedIdempotentRetryAndKeepsItsKey() {
        UUID request = UUID.randomUUID(), ledger = UUID.randomUUID();
        server.expect(requestTo(BASE + "/internal/access/points/debit"))
                .andExpect(jsonPath("$.idempotencyKey").value("vocab-study:" + request))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(BASE + "/internal/access/points/debit"))
                .andExpect(jsonPath("$.idempotencyKey").value("vocab-study:" + request))
                .andRespond(withSuccess("{\"id\":\"" + ledger + "\",\"balanceAfter\":27}", MediaType.APPLICATION_JSON));
        assertEquals(new PointsDebit(ledger, 27), client.debit(user, 10, "STREAK_FREEZE", request,
                "vocab-study:" + request, "Đóng băng streak (từ vựng)"));
    }

    @Test
    void persistentServerFailureMapsToUnavailableAfterSharedRetry() {
        server.expect(org.springframework.test.web.client.ExpectedCount.times(2), requestTo(BASE + "/internal/access/points/debit"))
                .andRespond(withServerError());
        assertThrows(AccessUnavailableException.class, this::debit);
    }

    @Test
    void premiumTimeoutUsesReadRetryThenUnavailable() {
        server.expect(org.springframework.test.web.client.ExpectedCount.times(2), requestTo(BASE + "/api/access/me/subscription"))
                .andRespond(withException(new SocketTimeoutException("test timeout")));
        assertThrows(AccessUnavailableException.class, client::premium);
    }

    @Test
    void unexpectedSuccessStatusIsNotMistakenForNoSubscription() {
        server.expect(requestTo(BASE + "/api/access/me/subscription"))
                .andRespond(withStatus(HttpStatus.CREATED));
        assertThrows(AccessUnavailableException.class, client::premium);
    }

    @Test
    void emptyOrMalformedLedgerCannotConfirmPayment() {
        server.expect(requestTo(BASE + "/internal/access/points/debit")).andRespond(withNoContent());
        assertThrows(AccessUnavailableException.class, this::debit);
        server.reset();
        server.expect(requestTo(BASE + "/internal/access/points/debit"))
                .andRespond(withSuccess("{\"balanceAfter\":37}", MediaType.APPLICATION_JSON));
        assertThrows(AccessUnavailableException.class, this::debit);
        server.reset();
        server.expect(requestTo(BASE + "/internal/access/points/debit"))
                .andRespond(withSuccess("{\"id\":\"" + UUID.randomUUID() + "\"}", MediaType.APPLICATION_JSON));
        assertThrows(AccessUnavailableException.class, this::debit);
    }

    @Test
    void missingAuthenticatedRequestNeverSendsAnHttpCall() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessUnavailableException.class, this::debit);
        assertThrows(AccessUnavailableException.class, client::premium);
    }

    @Test
    @SuppressWarnings("unchecked")
    void openCircuitMapsToUnavailableAndUsesCorrectCallKinds() {
        var resilient = mock(ResilientCall.class);
        when(resilient.call(eq("access"), any(), any(Supplier.class)))
                .thenThrow(new DownstreamUnavailableException("access", "circuit-open", null));
        var guarded = new RestStudyAccessClient(RestClient.builder().baseUrl(BASE).build(), resilient);
        assertThrows(AccessUnavailableException.class, guarded::premium);
        assertThrows(AccessUnavailableException.class, () -> guarded.debit(user, 5, "HEART_REFILL",
                UUID.randomUUID(), "vocab-study:test", "Hồi tim (từ vựng)"));
        verify(resilient).call(eq("access"), eq(CallKind.READ), any(Supplier.class));
        verify(resilient).call(eq("access"), eq(CallKind.IDEMPOTENT_WRITE), any(Supplier.class));
    }

    private PointsDebit debit() {
        UUID request = UUID.randomUUID();
        return client.debit(user, 5, "HEART_REFILL", request, "vocab-study:" + request, "Hồi tim (từ vựng)");
    }
}
