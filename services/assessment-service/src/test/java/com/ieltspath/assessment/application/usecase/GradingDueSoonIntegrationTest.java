package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flow: the examiner is chosen; after 36 hours without a result the examiner is reminded once; after 48 hours the learner is
 * told (the late examiner is not). The reminder never carries the learner.
 */
class GradingDueSoonIntegrationTest extends ExaminerIntegrationSupport {
    private static final String DUE_SOON = "assessment.grading-due-soon.v1";
    private static final String OVERDUE = "assessment.grading-overdue.v1";

    @MockitoBean com.ieltspath.assessment.infrastructure.client.AccessHumanGradingCreditClient credits;
    @Autowired AnnounceDueSoonGradingUseCase remind;
    @Autowired AnnounceOverdueGradingUseCase announce;
    private final UUID second = UUID.randomUUID();

    @BeforeEach
    void freshJobs() {
        jdbc.update("DELETE FROM human_reviews");
        jdbc.update("DELETE FROM grading_jobs");
        jdbc.update("DELETE FROM human_review_submissions");
        jdbc.update("DELETE FROM outbox_events WHERE event_type IN ('" + DUE_SOON + "','" + OVERDUE + "')");
        for (UUID id : new UUID[]{examiner, second}) {
            when(examinerDirectory.find(id)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(id, true, 30)));
        }
    }

    @Test
    void theExaminerIsRemindedOnceAfterThirtySixHours() throws Exception {
        UUID job = requestReview(examiner);
        assertEquals(0, remind.execute(Instant.now()), "chosen just now");

        chosenHoursAgo(job, 35);
        assertEquals(0, remind.execute(Instant.now()), "not 36 hours yet");

        chosenHoursAgo(job, 37);
        assertEquals(1, remind.execute(Instant.now()));
        assertEquals(0, remind.execute(Instant.now()), "already reminded");

        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type=?", DUE_SOON));
        var event = json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events WHERE event_type=?", String.class, DUE_SOON));
        assertEquals(examiner.toString(), event.get("examinerId").asText());
        assertEquals(job.toString(), event.get("gradingJobId").asText());
        assertEquals("WRITING", event.get("skill").asText());
        Instant chosenAt = jdbc.queryForObject("SELECT requested_at FROM grading_jobs WHERE id=?", java.sql.Timestamp.class, job).toInstant();
        assertEquals(chosenAt.plusSeconds(48 * 3600), Instant.parse(event.get("deadline").asText()));
        // Grading is blind: the reminder says which job, never whose.
        assertEquals(Set.of("eventId", "eventType", "occurredAt", "examinerId", "gradingJobId", "skill", "deadline"),
                json.convertValue(event, Map.class).keySet());
        assertEquals(0, count("SELECT count(*) FROM outbox_events WHERE event_type=?", OVERDUE), "the learner is not told yet");
    }

    @Test
    void aJobThatIsAlreadyLateIsNotRemindedAndTheLearnerIsToldInstead() throws Exception {
        UUID job = requestReview(examiner);
        chosenHoursAgo(job, 49);

        assertEquals(0, remind.execute(Instant.now()));
        assertEquals(1, announce.execute(Instant.now()));
        assertEquals(0, count("SELECT count(*) FROM outbox_events WHERE event_type=?", DUE_SOON));
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type=?", OVERDUE));
        // Only the learner is told of the late job: the examiner's reminder was the one chance to hurry.
        var late = json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events WHERE event_type=?", String.class, OVERDUE));
        assertEquals(learner.toString(), late.get("learnerId").asText());
    }

    @Test
    void theWholeFlowGoesReminderThenLateNoticeThenAFreshStartWithTheNewExaminer() throws Exception {
        UUID job = requestReview(examiner);

        chosenHoursAgo(job, 40);
        assertEquals(1, remind.execute(Instant.now()));
        assertEquals(0, announce.execute(Instant.now()), "40 hours is not late");

        chosenHoursAgo(job, 50);
        assertEquals(1, announce.execute(Instant.now()));

        // The learner replaces the late examiner: both marks are cleared and the new examiner's 36 and 48 hours start.
        mvc.perform(post("/api/assessments/grading-jobs/{id}/examiner", job)
                        .header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"examinerId\":\"" + second + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("WAITING"));
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND due_soon_notified_at IS NULL AND overdue_notified_at IS NULL", job));
        assertEquals(0, remind.execute(Instant.now()), "the new examiner has just been chosen");

        chosenHoursAgo(job, 37);
        assertEquals(1, remind.execute(Instant.now()));
        var reminders = jdbc.queryForList("SELECT payload->>'examinerId' FROM outbox_events WHERE event_type=? ORDER BY created_at", String.class, DUE_SOON);
        assertEquals(java.util.List.of(examiner.toString(), second.toString()), reminders);
    }

    @Test
    void anAttemptWithSeveralJobsIsRemindedOnceAndFinishedOrUnchosenJobsNever() throws Exception {
        UUID first = humanJob(courseEssay());
        jdbc.update("""
                INSERT INTO grading_jobs (id, submission_id, user_id, skill, grading_mode, status, idempotency_key, reason,
                    requested_examiner_id, requested_at)
                SELECT gen_random_uuid(), submission_id, user_id, skill, grading_mode, status,
                    'sibling-' || gen_random_uuid(), reason, requested_examiner_id, requested_at
                FROM grading_jobs WHERE id=?
                """, first);
        UUID finished = requestReview(examiner);
        jdbc.update("UPDATE grading_jobs SET status='COMPLETED' WHERE id=?", finished);
        UUID unchosen = unchosenHumanJob(courseEssay());
        jdbc.update("UPDATE grading_jobs SET created_at=CURRENT_TIMESTAMP-INTERVAL '5 days' WHERE id=?", unchosen);
        jdbc.update("UPDATE grading_jobs SET requested_at=CURRENT_TIMESTAMP-INTERVAL '40 hours' WHERE requested_examiner_id IS NOT NULL");

        assertEquals(1, remind.execute(Instant.now()), "one reminder for the attempt, none for the finished or unchosen job");
        assertEquals(2, count("SELECT count(*) FROM grading_jobs WHERE due_soon_notified_at IS NOT NULL AND status='QUEUED'"));
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type=?", DUE_SOON));
        assertEquals(0, remind.execute(Instant.now()));
    }

    private void chosenHoursAgo(UUID job, int hours) {
        jdbc.update("UPDATE grading_jobs SET requested_at=CURRENT_TIMESTAMP-make_interval(hours => ?) WHERE id=?", hours, job);
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
