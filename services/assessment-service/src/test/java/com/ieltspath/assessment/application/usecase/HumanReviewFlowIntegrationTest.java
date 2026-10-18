package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.assessment.infrastructure.client.AccessHumanGradingCreditClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HumanReviewFlowIntegrationTest extends ExaminerIntegrationSupport {
    @MockitoBean AccessHumanGradingCreditClient credits;

    @Test
    void standaloneReviewUsesInternalApiQueueAndSeparateHumanResult() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID sourceSubmissionId = UUID.randomUUID();
        String request = """
                {"requestId":"%s","learnerId":"%s","sourceSubmissionId":"%s","source":"PRACTICE",
                 "task":"TASK_2","promptSnapshot":{"stem":"Discuss both views."},
                 "essay":"My independent practice essay.",
                 "rubric":{"code":"IELTS_WRITING_TASK_2","version":1,"criteria":[{"code":"TR"}]}}
                """.formatted(requestId, learner, sourceSubmissionId);
        String learnerToken = internalToken(learner, "CUSTOMER");
        String examinerToken = internalToken(examiner, "EXAMINER");

        String accepted = mvc.perform(post("/internal/assessments/human-reviews")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.band").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode acceptedJson = json.readTree(accepted);
        UUID reviewId = UUID.fromString(acceptedJson.get("reviewSubmissionId").asText());

        mvc.perform(post("/internal/assessments/human-reviews")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reviewSubmissionId").value(reviewId.toString()));
        assertEquals(1, count("SELECT count(*) FROM human_review_submissions"));
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE human_review_submission_id=? AND grading_mode='HUMAN' AND reason='HUMAN_CREDIT' AND status='QUEUED'", reviewId));
        assertEquals(1, count("SELECT count(*) FROM human_reviews h JOIN grading_jobs j ON j.id=h.grading_job_id WHERE j.human_review_submission_id=? AND h.status='QUEUED'", reviewId));
        assertEquals(0, count("SELECT count(*) FROM assessment_attempts"));
        assertEquals(0, count("SELECT count(*) FROM assessment_results"));

        UUID jobId = jdbc.queryForObject("SELECT id FROM grading_jobs WHERE human_review_submission_id=?", UUID.class, reviewId);
        mvc.perform(get("/api/assessments/grading-jobs").param("mode", "HUMAN").param("status", "QUEUED")
                        .header("Authorization", "Bearer " + examinerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("HUMAN_CREDIT"))
                .andExpect(jsonPath("$.content[0].source").value("PRACTICE"));
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", jobId)
                        .header("Authorization", "Bearer " + examinerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/assessments/grading-jobs/{id}/essay", jobId)
                        .header("Authorization", "Bearer " + examinerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.essay").value("My independent practice essay."))
                .andExpect(jsonPath("$.source").value("PRACTICE"))
                .andExpect(jsonPath("$.rubric.code").value("IELTS_WRITING_TASK_2"))
                .andExpect(jsonPath("$.learnerId").doesNotExist());

        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", jobId)
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"band":6.5,"criteria":[{"criterion":"coherence","band":6.0,"comment":"Clear."}],
                                 "errorCodes":["COHESION_DEVICE_OVERUSE"],"feedback":"Develop the position."}
                                """))
                .andExpect(status().isNoContent());
        mvc.perform(get("/internal/assessments/human-reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.band").value(6.5))
                .andExpect(jsonPath("$.criteria[0].criterion").value("coherence"))
                .andExpect(jsonPath("$.errorCodes[0]").value("COHESION_DEVICE_OVERUSE"))
                .andExpect(jsonPath("$.feedback").value("Develop the position."))
                .andExpect(jsonPath("$.graderUserId").doesNotExist());
        mvc.perform(get("/internal/assessments/human-reviews/{id}", reviewId)
                        .param("learnerId", learner.toString())
                        .header("Authorization", "Bearer " + internalToken(UUID.randomUUID(), "CUSTOMER")))
                .andExpect(status().isNotFound());
        assertEquals(0, count("SELECT count(*) FROM assessment_results"));
    }

    @Test
    void internalEndpointsRequireGatewaySignedJwt() throws Exception {
        mvc.perform(post("/internal/assessments/human-reviews")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
