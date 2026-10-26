package com.ieltspath.assessment.application.usecase;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GradingQueueIntegrationTest extends ExaminerIntegrationSupport {
    /** The tests share one database and count the jobs in a queue, so each starts with none. */
    @org.junit.jupiter.api.BeforeEach void emptyQueue() {
        jdbc.update("DELETE FROM human_reviews");
        jdbc.update("DELETE FROM grading_jobs");
    }
    @Test void onlyTheChosenExaminerWinsEvenWhenOthersClaimAtTheSameTime() throws Exception {
        UUID job=humanJob(courseEssay());
        var ready=new java.util.concurrent.CountDownLatch(3);
        var go=new java.util.concurrent.CountDownLatch(1);
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(3)) {
            java.util.function.Function<UUID,java.util.concurrent.Callable<Integer>> claim=who->()-> {
                ready.countDown();
                if (!go.await(10,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Claim start timeout");
                return mvc.perform(post("/api/assessments/grading-jobs/{id}/claim",job)
                        .with(user(who.toString()).roles("EXAMINER"))).andReturn().getResponse().getStatus();
            };
            var chosen=pool.submit(claim.apply(examiner));
            var strangerOne=pool.submit(claim.apply(UUID.randomUUID()));
            var strangerTwo=pool.submit(claim.apply(UUID.randomUUID()));
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));
            go.countDown();
            org.junit.jupiter.api.Assertions.assertEquals(200,chosen.get(20,java.util.concurrent.TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertEquals(409,strangerOne.get(20,java.util.concurrent.TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertEquals(409,strangerTwo.get(20,java.util.concurrent.TimeUnit.SECONDS));
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
    @Test void aChosenJobIsNeverTakenOverByAnotherExaminerAndTheQueueIsBlind() throws Exception {
        UUID attempt = courseEssay();
        UUID job = humanJob(attempt);
        mvc.perform(get("/api/assessments/grading-jobs").with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].requestedExaminerId").value(examiner.toString()))
                .andExpect(jsonPath("$.content[0].hasDraft").value(false))
                .andExpect(jsonPath("$..llmBand").doesNotExist()).andExpect(jsonPath("$..learnerId").doesNotExist());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk());
        // Claiming again what you already hold is fine and changes nothing.
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignedExaminerId").value(examiner.toString()));
        UUID other = UUID.randomUUID();
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("JOB_ALREADY_CLAIMED"));
        // There is no pool: even an old claim does not let someone else in.
        jdbc.update("UPDATE grading_jobs SET claimed_at=CURRENT_TIMESTAMP-INTERVAL '25 hours' WHERE id=?", job);
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/assessments/grading-jobs/{id}/essay", job).with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/assessments/grading-jobs/{id}/essay", job).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.essay").value("My essay about public transport."))
                .andExpect(jsonPath("$..llmBand").doesNotExist());
        // The other examiner has nothing in the queue; an administrator sees every job.
        mvc.perform(get("/api/assessments/grading-jobs").with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/assessments/grading-jobs").with(user(other.toString()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
    }
    @Test void aJobNobodyWasChosenForIsInNoExaminersQueueAndCannotBeClaimed() throws Exception {
        UUID job = unchosenHumanJob(courseEssay());
        mvc.perform(get("/api/assessments/grading-jobs").with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/assessments/grading-jobs").with(user(examiner.toString()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].requestedExaminerId").doesNotExist());
    }
}
