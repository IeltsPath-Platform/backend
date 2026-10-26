package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** An examiner reads again what they graded, and only that: another examiner's work and unfinished work stay closed. */
class ExaminerHistoryIntegrationTest extends ExaminerIntegrationSupport {
    private final UUID other = UUID.randomUUID();

    @BeforeEach
    void freshJobs() {
        jdbc.update("DELETE FROM human_reviews");
        jdbc.update("DELETE FROM grading_jobs");
        jdbc.update("DELETE FROM human_review_submissions");
        for (UUID id : new UUID[]{examiner, other}) {
            when(examinerDirectory.find(id)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(id, true, 30)));
        }
        when(errorTypes.staffCodes()).thenReturn(Set.of("VERB_FORM"));
    }

    @Test
    void theExaminerListsAndOpensWhatTheyGradedNewestFirstAndNothingElse() throws Exception {
        UUID older = requestReview(examiner, "An essay.");
        UUID newer = requestReview(examiner, "An essay.");
        UUID waiting = requestReview(examiner, "Still waiting.");
        UUID someoneElses = requestReview(other, "An essay.");
        grade(older, examiner);
        grade(newer, examiner);
        grade(someoneElses, other);
        jdbc.update("UPDATE grading_jobs SET completed_at=CURRENT_TIMESTAMP-INTERVAL '2 hours' WHERE id=?", older);

        String token = "Bearer " + internalToken(examiner, "EXAMINER");
        mvc.perform(get("/api/assessments/examiners/me/completed").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].jobId").value(newer.toString()))
                .andExpect(jsonPath("$.content[1].jobId").value(older.toString()))
                .andExpect(jsonPath("$.content[0].band").value(6.5))
                .andExpect(jsonPath("$.content[0].source").value("PRACTICE"))
                .andExpect(jsonPath("$.content[0].task").value("TASK_2"))
                .andExpect(jsonPath("$.content[0].learnerId").doesNotExist());

        mvc.perform(get("/api/assessments/examiners/me/completed/{id}", newer).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(newer.toString()))
                .andExpect(jsonPath("$.essay").value("An essay."))
                .andExpect(jsonPath("$.prompt").value("Discuss both views."))
                .andExpect(jsonPath("$.band").value(6.5))
                .andExpect(jsonPath("$.criteria[0].criterion").value("grammar"))
                .andExpect(jsonPath("$.errorCodes[0]").value("VERB_FORM"))
                .andExpect(jsonPath("$.feedback").value("Check tenses."))
                .andExpect(jsonPath("$.annotations[0].excerpt").value("essay"))
                .andExpect(jsonPath("$.annotations[0].suggestion").value("text"))
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.learnerId").doesNotExist());

        // Not finished, someone else's, unknown: all closed.
        mvc.perform(get("/api/assessments/examiners/me/completed/{id}", waiting).header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/assessments/examiners/me/completed/{id}", someoneElses).header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/assessments/examiners/me/completed/{id}", UUID.randomUUID()).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void pagingIsBoundedAndOnlyExaminersMayAsk() throws Exception {
        String token = "Bearer " + internalToken(examiner, "EXAMINER");
        for (String query : new String[]{"?size=0", "?size=101", "?page=-1"}) {
            mvc.perform(get("/api/assessments/examiners/me/completed" + query).header("Authorization", token))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/assessments/examiners/me/completed")
                        .header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/assessments/examiners/me/completed")).andExpect(status().isUnauthorized());
    }

    private void grade(UUID job, UUID examinerId) throws Exception {
        String token = "Bearer " + internalToken(examinerId, "EXAMINER");
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", job).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"band":6.5,"criteria":[{"criterion":"grammar","band":6.0,"comment":"Tense slips."}],
                                 "errorCodes":["VERB_FORM"],"feedback":"Check tenses.",
                                 "annotations":[{"excerpt":"essay","start":3,"end":8,"errorCode":"VERB_FORM","suggestion":"text"}]}
                                """))
                .andExpect(status().isNoContent());
    }

    private UUID requestReview(UUID examinerId, String essay) throws Exception {
        String body = """
                {"requestId":"%s","sourceSubmissionId":"%s","source":"PRACTICE","task":"TASK_2",
                 "promptSnapshot":{"stem":"Discuss both views."},"essay":"%s",
                 "rubric":{"code":"IELTS_WRITING_TASK_2","version":1,"criteria":[]},"examinerId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), essay, examinerId);
        String answer = mvc.perform(post("/internal/assessments/human-reviews")
                        .header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        UUID review = UUID.fromString(json.readTree(answer).get("reviewSubmissionId").asText());
        return jdbc.queryForObject("SELECT id FROM grading_jobs WHERE human_review_submission_id=?", UUID.class, review);
    }
}
