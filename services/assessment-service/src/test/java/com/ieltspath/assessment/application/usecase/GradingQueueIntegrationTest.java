package com.ieltspath.assessment.application.usecase;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GradingQueueIntegrationTest extends ExaminerIntegrationSupport {
    @Test void concurrentClaimsHaveExactlyOneWinner() throws Exception {
        UUID job=humanJob(courseEssay());
        var ready=new java.util.concurrent.CountDownLatch(2);
        var go=new java.util.concurrent.CountDownLatch(1);
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> claim=()-> {
                ready.countDown();
                if (!go.await(10,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Claim start timeout");
                return mvc.perform(post("/api/assessments/grading-jobs/{id}/claim",job)
                        .with(user(UUID.randomUUID().toString()).roles("EXAMINER"))).andReturn().getResponse().getStatus();
            };
            var first=pool.submit(claim);
            var second=pool.submit(claim);
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));
            go.countDown();
            org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of(200,409),java.util.Set.of(first.get(20,java.util.concurrent.TimeUnit.SECONDS),second.get(20,java.util.concurrent.TimeUnit.SECONDS)));
        }
    }
    @Test void queueFiltersPaginationAndMissingJobsAreValidated() throws Exception {
        courseEssay();
        mvc.perform(get("/api/assessments/grading-jobs").param("size","1").with(user(examiner.toString()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.size").value(1));
        for (String query:java.util.List.of("?mode=AI","?page=-1","?size=101","?status=OTHER"))
            mvc.perform(get("/api/assessments/grading-jobs"+query).with(user(examiner.toString()).roles("EXAMINER")))
                    .andExpect(status().isBadRequest());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim",UUID.randomUUID()).with(user(examiner.toString()).roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
    @Test void queueClaimConflictAndExpiredReclaimAreBlind() throws Exception {
        UUID attempt = courseEssay();
        UUID job = humanJob(attempt);
        mvc.perform(get("/api/assessments/grading-jobs").with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$..llmBand").doesNotExist());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk());
        UUID other = UUID.randomUUID();
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("JOB_ALREADY_CLAIMED"));
        jdbc.update("UPDATE grading_jobs SET claimed_at=CURRENT_TIMESTAMP-INTERVAL '25 hours' WHERE id=?", job);
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignedExaminerId").value(other.toString()));
        mvc.perform(get("/api/assessments/grading-jobs/{id}/essay", job).with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.essay").value("My essay about public transport."))
                .andExpect(jsonPath("$..llmBand").doesNotExist());
    }
}
