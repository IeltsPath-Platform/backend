package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.HumanReviewClient.CreateReview;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestHumanReviewClientTest {
    private final UUID examiner = UUID.randomUUID();
    private MockRestServiceServer server;
    private RestHumanReviewClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder assessment = RestClient.builder().baseUrl("http://assessment");
        server = MockRestServiceServer.bindTo(assessment).build();
        client = new RestHumanReviewClient(RestClient.builder().baseUrl("http://access").build(), assessment.build());
        Jwt jwt = Jwt.withTokenValue("learner-token").header("alg", "HS256").subject("learner").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CreateReview request() {
        return new CreateReview(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "LESSON", "TASK_2", "stem",
                "essay", "IELTS_WRITING", 1, examiner);
    }

    @Test
    void theChosenExaminerIsSentWithTheEssay() {
        UUID review = UUID.randomUUID();
        server.expect(requestTo("http://assessment/internal/assessments/human-reviews")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.examinerId").value(examiner.toString()))
                .andRespond(withSuccess("{\"reviewSubmissionId\":\"" + review + "\"}", MediaType.APPLICATION_JSON));

        assertEquals(review, client.createReview(request()));
        server.verify();
    }

    @Test
    void anExaminerAssessmentRefusesIsReportedAsUnavailableSoTheLearnerCanChooseAnother() {
        server.expect(requestTo("http://assessment/internal/assessments/human-reviews"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":409,\"message\":\"full\",\"details\":{\"reason\":\"FULL\"},"
                                + "\"code\":\"EXAMINER_UNAVAILABLE\"}"));

        var refused = assertThrows(LearningRequestException.class, () -> client.createReview(request()));

        assertEquals(409, refused.getStatus());
        assertEquals("EXAMINER_UNAVAILABLE", refused.getCode());
    }

    @Test
    void anyOtherConflictStaysAGradingConflict() {
        server.expect(requestTo("http://assessment/internal/assessments/human-reviews"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":409,\"message\":\"x\"}"));

        assertEquals("HUMAN_GRADING_CONFLICT",
                assertThrows(LearningRequestException.class, () -> client.createReview(request())).getCode());
    }

    @Test
    void anUnreadableConflictBodyIsStillAGradingConflict() {
        server.expect(requestTo("http://assessment/internal/assessments/human-reviews"))
                .andRespond(withStatus(HttpStatus.CONFLICT).body("not json"));

        assertEquals("HUMAN_GRADING_CONFLICT",
                assertThrows(LearningRequestException.class, () -> client.createReview(request())).getCode());
    }
}
