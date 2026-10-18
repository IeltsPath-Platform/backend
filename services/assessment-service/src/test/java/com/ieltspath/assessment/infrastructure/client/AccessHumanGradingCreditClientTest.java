package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.domain.exception.HumanGradingCreditConflictException;
import com.ieltspath.assessment.domain.exception.HumanGradingCreditInsufficientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AccessHumanGradingCreditClientTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8084");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AccessHumanGradingCreditClient client = new AccessHumanGradingCreditClient(builder.build());
    private final UUID submission = UUID.randomUUID();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardsLearnerBearerForIdempotentCreditConsumption() {
        authenticate("learner-bearer-token");
        server.expect(requestTo("http://localhost:8084/internal/access/human-grading/consume"))
                .andExpect(header("Authorization", "Bearer learner-bearer-token"))
                .andExpect(content().json("{\"submissionId\":\"" + submission + "\"}"))
                .andRespond(withSuccess());

        client.consume(submission);

        server.verify();
    }

    @Test
    void tellsWhetherThisCallSpentTheCredit() {
        authenticate("learner-bearer-token");
        server.expect(requestTo("http://localhost:8084/internal/access/human-grading/consume"))
                .andRespond(withSuccess("{\"submissionId\":\"" + submission + "\",\"remainingCredits\":2,\"charged\":true}",
                        org.springframework.http.MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8084/internal/access/human-grading/consume"))
                .andRespond(withSuccess("{\"submissionId\":\"" + submission + "\",\"remainingCredits\":2,\"charged\":false}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        org.junit.jupiter.api.Assertions.assertTrue(client.consume(submission));
        org.junit.jupiter.api.Assertions.assertFalse(client.consume(submission));
        server.verify();
    }

    @Test
    void anAnswerWithoutTheFlagIsNeverTreatedAsASpentCredit() {
        authenticate("learner-bearer-token");
        server.expect(requestTo("http://localhost:8084/internal/access/human-grading/consume"))
                .andRespond(withSuccess("{\"submissionId\":\"" + submission + "\",\"remainingCredits\":2}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        org.junit.jupiter.api.Assertions.assertFalse(client.consume(submission));
    }

    @Test
    void mapsAccessCreditErrorsToAssessmentStatuses() {
        authenticate("learner-bearer-token");
        server.expect(requestTo("http://localhost:8084/internal/access/human-grading/consume"))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED));
        server.expect(requestTo("http://localhost:8084/internal/access/human-grading/refund"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        assertThrows(HumanGradingCreditInsufficientException.class, () -> client.consume(submission));
        assertThrows(HumanGradingCreditConflictException.class, () -> client.refund(submission));
        server.verify();
    }

    private void authenticate(String token) {
        Jwt jwt = Jwt.withTokenValue(token).header("alg", "none").claim("roles", List.of("CUSTOMER"))
                .subject(UUID.randomUUID().toString()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        SecurityContextHolder.setContext(context);
    }
}
