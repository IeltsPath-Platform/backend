package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.exception.UserServiceUnavailableException;
import com.ieltspath.assessment.application.port.ExaminerDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The examiner's dashboard counts only the jobs learners chose them for, in the states and days the screen shows. */
class ExaminerDashboardIntegrationTest extends ExaminerIntegrationSupport {
    private final UUID other = UUID.randomUUID();

    @BeforeEach
    void freshJobs() {
        jdbc.update("DELETE FROM human_reviews");
        jdbc.update("DELETE FROM grading_jobs");
        jdbc.update("DELETE FROM human_review_submissions");
        when(examinerDirectory.find(examiner)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(examiner, true, 10)));
        when(examinerDirectory.find(other)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(other, true, 10)));
    }

    @Test
    void countsTheQueueByStateAndWhatWasReceivedAndFinishedToday() throws Exception {
        requestReview(examiner);
        requestReview(examiner);
        UUID late = requestReview(examiner);
        UUID soon = requestReview(examiner);
        UUID working = requestReview(examiner);
        UUID drafted = requestReview(examiner);
        UUID finishedToday = requestReview(examiner);
        UUID finishedLastMonth = requestReview(examiner);
        requestReview(other);

        jdbc.update("UPDATE grading_jobs SET requested_at=CURRENT_TIMESTAMP-INTERVAL '49 hours' WHERE id=?", late);
        jdbc.update("UPDATE grading_jobs SET requested_at=CURRENT_TIMESTAMP-INTERVAL '40 hours' WHERE id=?", soon);
        jdbc.update("UPDATE grading_jobs SET status='PROCESSING' WHERE id=?", working);
        // Saving a draft leaves the job QUEUED, but the examiner has started it.
        jdbc.update("""
                INSERT INTO human_reviews(id,grading_job_id,grader_user_id,status,assigned_at,started_at,draft_payload,draft_saved_at)
                VALUES (gen_random_uuid(),?,?,'IN_REVIEW',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'{"feedback":"half"}',CURRENT_TIMESTAMP)
                ON CONFLICT(grading_job_id) DO UPDATE SET status='IN_REVIEW',started_at=CURRENT_TIMESTAMP,
                    draft_payload='{"feedback":"half"}',draft_saved_at=CURRENT_TIMESTAMP
                """, drafted, examiner);
        jdbc.update("UPDATE grading_jobs SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE id=?", finishedToday);
        jdbc.update("""
                UPDATE grading_jobs SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP-INTERVAL '30 days',
                    requested_at=CURRENT_TIMESTAMP-INTERVAL '31 days' WHERE id=?
                """, finishedLastMonth);
        // A quality-assurance sample is not work the learner gave the examiner.
        UUID sampled = submissions.execute(new com.ieltspath.assessment.application.command.CreateLearnerSubmissionCommand(
                learner, null, "{}", com.ieltspath.assessment.domain.vo.Skill.WRITING, "A sampled essay.", null,
                "dashboard-" + UUID.randomUUID())).id();
        jdbc.update("""
                INSERT INTO grading_jobs (id, submission_id, user_id, skill, grading_mode, status, idempotency_key, reason,
                    requested_examiner_id, requested_at)
                VALUES (gen_random_uuid(), ?, ?, 'WRITING', 'HUMAN', 'QUEUED', 'qa-' || gen_random_uuid(), 'QA_SAMPLE', ?,
                    CURRENT_TIMESTAMP)
                """, sampled, learner, examiner);

        mvc.perform(get("/api/assessments/examiners/me/summary").header("Authorization", "Bearer " + internalToken(examiner, "EXAMINER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queued").value(4))
                .andExpect(jsonPath("$.processing").value(2))
                .andExpect(jsonPath("$.dueSoon").value(1))
                .andExpect(jsonPath("$.overdue").value(1))
                .andExpect(jsonPath("$.completedToday").value(1))
                .andExpect(jsonPath("$.completedThisWeek").value(1))
                .andExpect(jsonPath("$.receivedToday").value(5))
                .andExpect(jsonPath("$.dailyLimit").value(10));
    }

    @Test
    void anExaminerWithNoWorkSeesZeros() throws Exception {
        mvc.perform(get("/api/assessments/examiners/me/summary").header("Authorization", "Bearer " + internalToken(examiner, "EXAMINER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queued").value(0)).andExpect(jsonPath("$.processing").value(0))
                .andExpect(jsonPath("$.dueSoon").value(0)).andExpect(jsonPath("$.overdue").value(0)).andExpect(jsonPath("$.completedToday").value(0))
                .andExpect(jsonPath("$.completedThisWeek").value(0)).andExpect(jsonPath("$.receivedToday").value(0));
    }

    @Test
    void theNumbersStillShowWhenTheDailyLimitCannotBeRead() throws Exception {
        requestReview(examiner);
        when(examinerDirectory.find(any())).thenThrow(new UserServiceUnavailableException("user-service down"));

        mvc.perform(get("/api/assessments/examiners/me/summary").header("Authorization", "Bearer " + internalToken(examiner, "EXAMINER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queued").value(1))
                .andExpect(jsonPath("$.receivedToday").value(1))
                .andExpect(jsonPath("$.dailyLimit").doesNotExist());
    }

    @Test
    void onlyAnExaminerMayAskForTheirDashboard() throws Exception {
        mvc.perform(get("/api/assessments/examiners/me/summary").header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/assessments/examiners/me/summary")).andExpect(status().isUnauthorized());
    }

    private UUID requestReview(UUID examinerId) throws Exception {
        String body = """
                {"requestId":"%s","sourceSubmissionId":"%s","source":"PRACTICE","task":"TASK_2",
                 "promptSnapshot":{"stem":"Discuss both views."},"essay":"An essay.",
                 "rubric":{"code":"IELTS_WRITING_TASK_2","version":1,"criteria":[]},"examinerId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), examinerId);
        String answer = mvc.perform(post("/internal/assessments/human-reviews")
                        .header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        UUID review = UUID.fromString(json.readTree(answer).get("reviewSubmissionId").asText());
        return jdbc.queryForObject("SELECT id FROM grading_jobs WHERE human_review_submission_id=?", UUID.class, review);
    }
}
