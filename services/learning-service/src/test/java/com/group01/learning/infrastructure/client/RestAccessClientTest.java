package com.group01.learning.infrastructure.client;

import com.group01.commonsecurity.header.SecurityHeaders;
import com.group01.learning.application.exception.AccessUnavailableException;
import com.group01.learning.application.exception.InsufficientPointsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestAccessClientTest {
    private static final String BASE_URL = "http://access.test";
    private MockRestServiceServer server;
    private RestAccessClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestAccessClient(builder.build());
        var jwt = Jwt.withTokenValue("learner-token").header("alg", "HS256").subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        var request = new MockHttpServletRequest();
        request.addHeader(SecurityHeaders.CORRELATION_ID, "corr-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        server.verify();
    }

    @Test
    void readsTheLearnersOwnBalance() {
        server.expect(requestTo(BASE_URL + "/api/access/me/points")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer learner-token"))
                .andExpect(header(SecurityHeaders.CORRELATION_ID, "corr-1"))
                .andRespond(withSuccess("{\"userId\":\"%s\",\"balance\":7,\"totalCredited\":10}"
                        .formatted(UUID.randomUUID()), MediaType.APPLICATION_JSON));
        assertEquals(7, client.balance());
    }

    @Test
    void debitSendsTheIdempotencyKeyAndReturnsTheLedgerEntry() {
        UUID user = UUID.randomUUID();
        UUID submission = UUID.randomUUID();
        UUID ledger = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/access/points/debit")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer learner-token"))
                .andExpect(jsonPath("$.userId").value(user.toString()))
                .andExpect(jsonPath("$.amount").value(3))
                .andExpect(jsonPath("$.referenceType").value("LESSON_WRITING"))
                .andExpect(jsonPath("$.referenceId").value(submission.toString()))
                .andExpect(jsonPath("$.idempotencyKey").value("lesson-writing:key"))
                .andRespond(withSuccess("{\"id\":\"%s\",\"delta\":-3}".formatted(ledger), MediaType.APPLICATION_JSON));
        assertEquals(ledger, client.debit(user, 3, submission, "lesson-writing:key", "Lesson essay grading"));
    }

    @Test
    void paymentRequiredIsInsufficientAndEverythingElseIsUnavailable() {
        server.expect(requestTo(BASE_URL + "/internal/access/points/debit"))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED));
        assertThrows(InsufficientPointsException.class,
                () -> client.debit(UUID.randomUUID(), 3, UUID.randomUUID(), "k", null));
        for (HttpStatus status : List.of(HttpStatus.UNAUTHORIZED, HttpStatus.CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR)) {
            server.reset();
            server.expect(requestTo(BASE_URL + "/api/access/me/points")).andRespond(withStatus(status));
            assertThrows(AccessUnavailableException.class, client::balance, status.toString());
        }
    }

    @Test
    void withoutAnAuthenticatedLearnerNothingIsSent() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessUnavailableException.class, client::balance);
    }
}
