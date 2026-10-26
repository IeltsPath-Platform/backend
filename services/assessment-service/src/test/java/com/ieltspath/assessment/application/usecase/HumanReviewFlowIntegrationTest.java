package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.assessment.application.exception.ContentUnavailableException;
import com.ieltspath.assessment.application.port.ExaminerDirectory;
import com.ieltspath.assessment.infrastructure.client.AccessHumanGradingCreditClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HumanReviewFlowIntegrationTest extends ExaminerIntegrationSupport {
    private static final String ESSAY = "My independent practice essay.";
    // "independent practice" is ESSAY[3, 23) and "essay" is ESSAY[24, 29).
    private static final String MARKED_ERRORS = """
            [{"excerpt":"independent practice","start":3,"end":23,"errorCode":"TENSE","suggestion":"practised alone"},
             {"excerpt":"essay","start":24,"end":29,"errorCode":"LINKER_MISUSE","suggestion":"text"}]
            """;

    @MockitoBean AccessHumanGradingCreditClient credits;

    /** The tests share one database, so each starts without the reviews of the others. */
    @BeforeEach
    void emptyReviews() {
        jdbc.update("DELETE FROM human_reviews WHERE grading_job_id IN "
                + "(SELECT id FROM grading_jobs WHERE human_review_submission_id IS NOT NULL)");
        jdbc.update("DELETE FROM grading_jobs WHERE human_review_submission_id IS NOT NULL");
        jdbc.update("DELETE FROM human_review_submissions");
    }

    /** TENSE is active but still in review; LINKER_MISUSE is approved. */
    @BeforeEach
    void catalog() {
        when(errorTypes.staffCodes()).thenReturn(Set.of("TENSE", "LINKER_MISUSE"));
        when(errorTypes.activeCodes()).thenReturn(Set.of("LINKER_MISUSE"));
        when(examinerDirectory.find(examiner)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(examiner, true, 10)));
    }

    @Test
    void standaloneReviewUsesInternalApiQueueAndSeparateHumanResult() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID sourceSubmissionId = UUID.randomUUID();
        String request = reviewRequest(requestId, sourceSubmissionId);
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
                .andExpect(jsonPath("$.essay").value(ESSAY))
                .andExpect(jsonPath("$.source").value("PRACTICE"))
                .andExpect(jsonPath("$.rubric.code").value("IELTS_WRITING_TASK_2"))
                .andExpect(jsonPath("$.learnerId").doesNotExist());

        // Nothing is saved yet, so there is no draft to read.
        mvc.perform(get("/api/assessments/grading-jobs/{id}/human-review/draft", jobId)
                        .header("Authorization", "Bearer " + examinerToken))
                .andExpect(status().isNoContent());
        // Writing: the first draft opens the review, and only the examiner who claimed it can use the draft.
        mvc.perform(put("/api/assessments/grading-jobs/{id}/human-review/draft", jobId)
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"band\":6.0,\"feedback\":\"Half done\"}"))
                .andExpect(status().isNoContent());
        assertEquals(1, count("SELECT count(*) FROM human_reviews WHERE status='IN_REVIEW' AND draft_payload IS NOT NULL AND started_at IS NOT NULL AND draft_saved_at IS NOT NULL"));
        mvc.perform(get("/api/assessments/grading-jobs/{id}/human-review/draft", jobId)
                        .header("Authorization", "Bearer " + examinerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.feedback").value("Half done"))
                .andExpect(jsonPath("$.savedAt").exists());
        String otherExaminer = internalToken(UUID.randomUUID(), "EXAMINER");
        mvc.perform(put("/api/assessments/grading-jobs/{id}/human-review/draft", jobId)
                        .header("Authorization", "Bearer " + otherExaminer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/assessments/grading-jobs/{id}/human-review/draft", jobId)
                        .header("Authorization", "Bearer " + otherExaminer))
                .andExpect(status().isForbidden());
        // The learner never sees the draft, and the review is still pending for them.
        mvc.perform(get("/internal/assessments/human-reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.feedback").doesNotExist());

        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", jobId)
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(completeBody(MARKED_ERRORS)))
                .andExpect(status().isNoContent());
        assertEquals(0, count("SELECT count(*) FROM human_reviews WHERE draft_payload IS NOT NULL OR draft_saved_at IS NOT NULL"));
        mvc.perform(get("/api/assessments/grading-jobs/{id}/human-review/draft", jobId)
                        .header("Authorization", "Bearer " + examinerToken))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/assessments/grading-jobs/{id}/human-review/draft", jobId)
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());

        // The learner sees what the examiner wrote. A code still in review is hidden, the examiner's words stay.
        mvc.perform(get("/internal/assessments/human-reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.band").value(6.5))
                .andExpect(jsonPath("$.criteria[0].criterion").value("GRA"))
                .andExpect(jsonPath("$.errorCodes.length()").value(1))
                .andExpect(jsonPath("$.errorCodes[0]").value("LINKER_MISUSE"))
                .andExpect(jsonPath("$.feedback").value("Develop the position."))
                .andExpect(jsonPath("$.annotations.length()").value(2))
                .andExpect(jsonPath("$.annotations[0].excerpt").value("independent practice"))
                .andExpect(jsonPath("$.annotations[0].suggestion").value("practised alone"))
                .andExpect(jsonPath("$.annotations[0].errorCode").doesNotExist())
                .andExpect(jsonPath("$.annotations[1].errorCode").value("LINKER_MISUSE"))
                .andExpect(jsonPath("$.graderUserId").doesNotExist());
        mvc.perform(get("/internal/assessments/human-reviews/{id}", reviewId)
                        .param("learnerId", learner.toString())
                        .header("Authorization", "Bearer " + internalToken(UUID.randomUUID(), "CUSTOMER")))
                .andExpect(status().isNotFound());
        assertEquals(0, count("SELECT count(*) FROM assessment_results"));
        // Both codes are stored: the code review only decides what a learner is shown.
        assertEquals(2, count("SELECT jsonb_array_length(annotations) FROM human_review_submissions WHERE id=?", reviewId));
    }

    @Test
    void completingAgainRepeatsTheSameReviewButNotAnotherOne() throws Exception {
        UUID[] ids = requestAndClaim();
        String examinerToken = internalToken(examiner, "EXAMINER");
        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", ids[1])
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(completeBody(MARKED_ERRORS)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", ids[1])
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(completeBody(MARKED_ERRORS)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", ids[1])
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(completeBody("[]")))
                .andExpect(status().isConflict());
    }

    @Test
    void theLearnerIsToldOnceWhenTheExaminerFinishesAndSeesTheBandInTheirList() throws Exception {
        jdbc.update("DELETE FROM outbox_events WHERE event_type='assessment.grading-completed.v1'");
        UUID[] ids = requestAndClaim();
        String examinerToken = internalToken(examiner, "EXAMINER");
        String learnerToken = internalToken(learner, "CUSTOMER");
        mvc.perform(get("/api/assessments/grading-jobs/mine").header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$[0].state").value("WAITING")).andExpect(jsonPath("$[0].band").doesNotExist());
        assertEquals(0, count("SELECT count(*) FROM outbox_events WHERE event_type='assessment.grading-completed.v1'"),
                "nothing to tell while the examiner has only a draft or has not finished");

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", ids[1])
                            .header("Authorization", "Bearer " + examinerToken)
                            .contentType(MediaType.APPLICATION_JSON).content(completeBody(MARKED_ERRORS)))
                    .andExpect(status().isNoContent());
        }

        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type='assessment.grading-completed.v1'"),
                "a repeated completion does not tell the learner again");
        var event = json.readTree(jdbc.queryForObject(
                "SELECT payload::text FROM outbox_events WHERE event_type='assessment.grading-completed.v1'", String.class));
        assertEquals(learner.toString(), event.get("learnerId").asText());
        assertEquals(ids[1].toString(), event.get("gradingJobId").asText());
        assertEquals(ids[0].toString(), event.get("reviewSubmissionId").asText());
        assertEquals("WRITING", event.get("skill").asText());
        // The notice carries no grade, comment or essay text, and does not name the examiner.
        assertEquals(Set.of("eventId", "eventType", "occurredAt", "learnerId", "gradingJobId", "reviewSubmissionId", "skill"),
                json.convertValue(event, java.util.Map.class).keySet());

        mvc.perform(get("/api/assessments/grading-jobs/mine").header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$[0].state").value("COMPLETED"))
                .andExpect(jsonPath("$[0].reviewSubmissionId").value(ids[0].toString()))
                .andExpect(jsonPath("$[0].band").value(6.5))
                .andExpect(jsonPath("$[0].completedAt").exists());
    }

    @Test
    void anErrorCodeOutsideTheCatalogIsRejectedAndNothingIsStored() throws Exception {
        UUID[] ids = requestAndClaim();
        String examinerToken = internalToken(examiner, "EXAMINER");
        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", ids[1])
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeBody("[{\"excerpt\":\"essay\",\"start\":24,\"end\":29,"
                                + "\"errorCode\":\"MADE_UP\",\"suggestion\":\"text\"}]")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNKNOWN_ERROR_CODE"));
        assertEquals(0, count("SELECT count(*) FROM human_review_submissions WHERE band IS NOT NULL"));
    }

    @Test
    void aMarkedErrorMustQuoteTheEssayAtItsPosition() throws Exception {
        UUID[] ids = requestAndClaim();
        String examinerToken = internalToken(examiner, "EXAMINER");
        for (String annotation : new String[] {
                "{\"excerpt\":\"practice\",\"start\":3,\"end\":23,\"errorCode\":\"TENSE\",\"suggestion\":\"x\"}",
                "{\"excerpt\":\"essay.\",\"start\":24,\"end\":99,\"errorCode\":\"TENSE\",\"suggestion\":\"x\"}"}) {
            mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", ids[1])
                            .header("Authorization", "Bearer " + examinerToken)
                            .contentType(MediaType.APPLICATION_JSON).content(completeBody("[" + annotation + "]")))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, count("SELECT count(*) FROM human_review_submissions WHERE band IS NOT NULL"));
    }

    @Test
    void replacingALateExaminerStartsTheNewOneWithoutTheOldDraft() throws Exception {
        UUID[] ids = requestAndClaim();
        String examinerToken = internalToken(examiner, "EXAMINER");
        mvc.perform(put("/api/assessments/grading-jobs/{id}/human-review/draft", ids[1])
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"feedback\":\"private notes\"}"))
                .andExpect(status().isNoContent());

        UUID next = UUID.randomUUID();
        when(examinerDirectory.find(next)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(next, true, 10)));
        jdbc.update("UPDATE grading_jobs SET requested_at=CURRENT_TIMESTAMP-INTERVAL '49 hours' WHERE id=?", ids[1]);
        mvc.perform(post("/api/assessments/grading-jobs/{id}/examiner", ids[1])
                        .header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"examinerId\":\"" + next + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("WAITING"))
                .andExpect(jsonPath("$.examinerId").value(next.toString()));
        assertEquals(0, count("SELECT count(*) FROM human_reviews WHERE draft_payload IS NOT NULL"));

        String nextToken = internalToken(next, "EXAMINER");
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", ids[1]).header("Authorization", "Bearer " + nextToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/assessments/grading-jobs/{id}/human-review/draft", ids[1])
                        .header("Authorization", "Bearer " + nextToken))
                .andExpect(status().isNoContent());
        // The first examiner no longer holds the job, its draft or the right to claim it again.
        mvc.perform(get("/api/assessments/grading-jobs/{id}/human-review/draft", ids[1])
                        .header("Authorization", "Bearer " + examinerToken))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", ids[1]).header("Authorization", "Bearer " + examinerToken))
                .andExpect(status().isConflict());
    }

    @Test
    void aDraftMustBeAnObjectAndSmall() throws Exception {
        UUID[] ids = requestAndClaim();
        String examinerToken = internalToken(examiner, "EXAMINER");
        mvc.perform(put("/api/assessments/grading-jobs/{id}/human-review/draft", ids[1])
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("[1]"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/assessments/grading-jobs/{id}/human-review/draft", ids[1])
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"" + "a".repeat(210_000) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/assessments/grading-jobs/{id}/human-review/draft", UUID.randomUUID())
                        .header("Authorization", "Bearer " + examinerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        assertEquals(0, count("SELECT count(*) FROM human_reviews WHERE draft_payload IS NOT NULL"));
    }

    @Test
    void aLearnerStillReadsTheReviewWhenTheCatalogIsDownButSeesNoCode() throws Exception {
        UUID[] ids = requestAndClaim();
        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", ids[1])
                        .header("Authorization", "Bearer " + internalToken(examiner, "EXAMINER"))
                        .contentType(MediaType.APPLICATION_JSON).content(completeBody(MARKED_ERRORS)))
                .andExpect(status().isNoContent());
        when(errorTypes.activeCodes()).thenThrow(new ContentUnavailableException("Content Service is down"));
        mvc.perform(get("/internal/assessments/human-reviews/{id}", ids[0])
                        .header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.errorCodes.length()").value(0))
                .andExpect(jsonPath("$.annotations[0].excerpt").value("independent practice"))
                .andExpect(jsonPath("$.annotations[0].errorCode").doesNotExist())
                .andExpect(jsonPath("$.annotations[1].errorCode").doesNotExist());
    }

    @Test
    void internalEndpointsRequireGatewaySignedJwt() throws Exception {
        mvc.perform(post("/internal/assessments/human-reviews")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private String reviewRequest(UUID requestId, UUID sourceSubmissionId) {
        return """
                {"requestId":"%s","learnerId":"%s","sourceSubmissionId":"%s","source":"PRACTICE",
                 "task":"TASK_2","promptSnapshot":{"stem":"Discuss both views."},
                 "essay":"%s",
                 "rubric":{"code":"IELTS_WRITING_TASK_2","version":1,"criteria":[{"code":"TR"}]},
                 "examinerId":"%s"}
                """.formatted(requestId, learner, sourceSubmissionId, ESSAY, examiner);
    }

    /** Requests a review as the learner and claims its job as the examiner; returns [reviewId, jobId]. */
    private UUID[] requestAndClaim() throws Exception {
        String accepted = mvc.perform(post("/internal/assessments/human-reviews")
                        .header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        UUID reviewId = UUID.fromString(json.readTree(accepted).get("reviewSubmissionId").asText());
        UUID jobId = jdbc.queryForObject("SELECT id FROM grading_jobs WHERE human_review_submission_id=?", UUID.class, reviewId);
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", jobId)
                        .header("Authorization", "Bearer " + internalToken(examiner, "EXAMINER")))
                .andExpect(status().isOk());
        return new UUID[] {reviewId, jobId};
    }

    private static String completeBody(String annotations) {
        return """
                {"band":6.5,"criteria":[{"criterion":"GRA","band":6.0,"comment":"Clear."}],
                 "errorCodes":["LINKER_MISUSE","TENSE"],"feedback":"Develop the position.",
                 "annotations":%s}
                """.formatted(annotations);
    }
}
