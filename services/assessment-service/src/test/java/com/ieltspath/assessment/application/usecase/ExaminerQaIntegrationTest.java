package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"assessment.qa.sample-rate=1.0", "app.auth.internal-jwt-issuer=urn:test:gateway"})
class ExaminerQaIntegrationTest extends ExaminerIntegrationSupport {
    @Test
    void finalizedEssayCreatesBlindQaSampleAndQualityAggregate() throws Exception {
        UUID attempt = courseEssay();
        UUID originalJob = humanJob(attempt);
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", originalJob)
                        .with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk());
        String resultBody = mvc.perform(post("/api/assessments/grading/attempts/{id}/results", attempt)
                        .with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID resultId = UUID.fromString(json.readTree(resultBody).get("id").asText());
        UUID itemId = item(attempt);
        String details = json.writeValueAsString(Map.of("overallBand", 6.5, "itemResults", List.of(Map.of(
                "attemptItemId", itemId, "score", 1, "maxScore", 1, "correct", true,
                "feedbackSnapshot", "{\"band\":6.5,\"criteria\":[]}"))));
        mvc.perform(put("/api/assessments/grading/results/{id}/details", resultId)
                        .with(user(examiner.toString()).roles("EXAMINER"))
                        .contentType(MediaType.APPLICATION_JSON).content(details))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/assessments/grading/results/{id}/finalize", resultId)
                        .with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk());

        UUID qaJob = jdbc.queryForObject("SELECT id FROM grading_jobs WHERE submission_id=(SELECT submission_id FROM grading_jobs WHERE id=?) AND reason='QA_SAMPLE'",
                UUID.class, originalJob);
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND reason='QA_SAMPLE' AND status='QUEUED'", qaJob));

        UUID reviewer = UUID.randomUUID();
        MvcResult essay = mvc.perform(get("/api/assessments/qa-reviews/{id}/essay", qaJob)
                        .with(user(reviewer.toString()).roles("CONTENT_AUTHOR")))
                .andExpect(status().isForbidden()).andReturn();
        mvc.perform(get("/api/assessments/qa-reviews/queue").with(user(reviewer.toString()).roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].reason").value("QA_SAMPLE"));
        mvc.perform(get("/api/assessments/qa-reviews/mine").with(user(reviewer.toString()).roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/api/assessments/qa-reviews/{id}/claim", qaJob)
                        .with(user(reviewer.toString()).roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/assessments/qa-reviews/mine").with(user(reviewer.toString()).roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(qaJob.toString()))
                .andExpect(jsonPath("$.content[0].reason").value("QA_SAMPLE"));
        mvc.perform(get("/api/assessments/qa-reviews/mine")
                        .with(user(UUID.randomUUID().toString()).roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        essay = mvc.perform(get("/api/assessments/qa-reviews/{id}/essay", qaJob)
                        .with(user(reviewer.toString()).roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk()).andReturn();
        String essayJson = essay.getResponse().getContentAsString();
        assertFalse(essayJson.contains("6.5"));
        assertFalse(essayJson.contains(examiner.toString()));
        mvc.perform(post("/api/assessments/qa-reviews").with(user(reviewer.toString()).roles("CONTENT_AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "gradingJobId", qaJob, "reviewerBand", 6.0, "reviewerCriteria", Map.of("TR", 6.0),
                                "note", "Review"))))
                .andExpect(status().isNoContent());
        assertEquals(1, count("SELECT count(*) FROM examiner_qa_reviews WHERE submission_id=(SELECT submission_id FROM grading_jobs WHERE id=?) AND reviewer_id=? AND examiner_band=6.5 AND reviewer_band=6.0",
                qaJob, reviewer));
        mvc.perform(get("/api/assessments/qa-reviews/mine").with(user(reviewer.toString()).roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        // The User Service is not reachable here, so the table comes back without names.
        mvc.perform(get("/api/assessments/admin/examiner-quality").with(user(UUID.randomUUID().toString()).roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].reviewedEssays").value(1))
                .andExpect(jsonPath("$[0].examinerId").value(examiner.toString()))
                .andExpect(jsonPath("$[0].examinerName").isEmpty());
    }
}
