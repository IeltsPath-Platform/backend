package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The learner chooses who grades; the examiner then sees only that job; the choice can be redone only when it is late. */
class ExaminerSelectionIntegrationTest extends ExaminerIntegrationSupport {
    @MockitoBean com.ieltspath.assessment.infrastructure.client.AccessHumanGradingCreditClient credits;
    @org.springframework.beans.factory.annotation.Autowired AnnounceOverdueGradingUseCase announce;

    private final UUID second = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();
    private final UUID fourth = UUID.randomUUID();

    @BeforeEach
    void freshJobsAndExaminers() {
        jdbc.update("DELETE FROM human_reviews");
        jdbc.update("DELETE FROM grading_jobs");
        jdbc.update("DELETE FROM human_review_submissions");
        jdbc.update("DELETE FROM outbox_events WHERE event_type='assessment.grading-overdue.v1'");
        for (UUID id : List.of(examiner, second, third, fourth)) accept(id, 10);
    }

    @Test
    void theLearnerChoosesTheExaminerOfAJobThatWaitsForAChoice() throws Exception {
        UUID job = unchosenHumanJob(courseEssay());
        String learnerToken = internalToken(learner, "CUSTOMER");

        mvc.perform(get("/api/assessments/grading-jobs/mine").header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].state").value("AWAITING_CHOICE"))
                .andExpect(jsonPath("$[0].canChoose").value(true))
                .andExpect(jsonPath("$[0].examinerId").doesNotExist());

        mvc.perform(post("/api/assessments/grading-jobs/{id}/examiner", job)
                        .header("Authorization", "Bearer " + internalToken(UUID.randomUUID(), "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(chosen(examiner)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/examiner", job)
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(chosen(examiner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("WAITING"))
                .andExpect(jsonPath("$.examinerId").value(examiner.toString()))
                .andExpect(jsonPath("$.canChoose").value(false))
                .andExpect(jsonPath("$.deadline").exists());

        mvc.perform(get("/api/assessments/grading-jobs").header("Authorization", "Bearer " + internalToken(examiner, "EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(job.toString()));
        mvc.perform(get("/api/assessments/grading-jobs").header("Authorization", "Bearer " + internalToken(second, "EXAMINER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void theExaminerIsReplacedOnlyWhenLateAndAtMostTwice() throws Exception {
        UUID job = requestReview(examiner).job();
        String learnerToken = internalToken(learner, "CUSTOMER");

        choose(job, learnerToken, second).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXAMINER_CHANGE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.details.reason").value("NOT_OVERDUE"));

        makeLate(job);
        choose(job, learnerToken, examiner).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.reason").value("SAME_EXAMINER"));
        mvc.perform(get("/api/assessments/grading-jobs/mine").header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$[0].state").value("OVERDUE")).andExpect(jsonPath("$[0].canChoose").value(true))
                .andExpect(jsonPath("$[0].changesLeft").value(2));

        choose(job, learnerToken, second).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("WAITING"))
                .andExpect(jsonPath("$.changesLeft").value(1));
        assertEquals(1, count("SELECT examiner_changes FROM grading_jobs WHERE id=?", job));

        choose(job, learnerToken, third).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.reason").value("NOT_OVERDUE"));
        makeLate(job);
        choose(job, learnerToken, third).andExpect(status().isOk()).andExpect(jsonPath("$.changesLeft").value(0));
        makeLate(job);
        choose(job, learnerToken, fourth).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.reason").value("LIMIT_REACHED"));
        mvc.perform(get("/api/assessments/grading-jobs/mine").header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$[0].state").value("OVERDUE")).andExpect(jsonPath("$[0].canChoose").value(false));
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND requested_examiner_id=?", job, third));
    }

    @Test
    void aFinishedJobCannotBeChosenAgain() throws Exception {
        UUID job = requestReview(examiner).job();
        jdbc.update("UPDATE grading_jobs SET status='COMPLETED' WHERE id=?", job);
        makeLate(job);
        choose(job, internalToken(learner, "CUSTOMER"), second).andExpect(status().isConflict());
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND requested_examiner_id=?", job, examiner));
    }

    @Test
    void anExaminerWithNoRoomOrNotAcceptingOrUnknownIsRefusedAndNothingIsCreated() throws Exception {
        accept(second, 1);
        Requested first = requestReview(second);
        // Asking again with the same request id repeats the first request: it is not held to the limit again.
        mvc.perform(post("/internal/assessments/human-reviews").header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(first.body()))
                .andExpect(status().isAccepted());
        assertEquals(1, count("SELECT count(*) FROM human_review_submissions"));

        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXAMINER_UNAVAILABLE")).andExpect(jsonPath("$.details.reason").value("FULL"));
        when(examinerDirectory.find(third)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(third, false, 10)));
        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), third)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.reason").value("NOT_ACCEPTING"));
        UUID nobody = UUID.randomUUID();
        when(examinerDirectory.find(nobody)).thenReturn(Optional.empty());
        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), nobody)).andExpect(status().isNotFound());
        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), null)).andExpect(status().isBadRequest());
        assertEquals(1, count("SELECT count(*) FROM human_review_submissions"));
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE grading_mode='HUMAN'"));

        // A job that waits for a choice cannot be given to the full examiner either.
        UUID waiting = unchosenHumanJob(courseEssay());
        choose(waiting, internalToken(learner, "CUSTOMER"), second).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.reason").value("FULL"));
    }

    @Test
    void twoLearnersRacingForTheLastPlaceCannotBothGetIt() throws Exception {
        accept(second, 1);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var go = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.function.Supplier<java.util.concurrent.Callable<Integer>> attempt = () -> {
                UUID someone = UUID.randomUUID();
                return () -> {
                    ready.countDown();
                    if (!go.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                    return mvc.perform(post("/internal/assessments/human-reviews")
                                    .header("Authorization", "Bearer " + internalToken(someone, "CUSTOMER"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)))
                            .andReturn().getResponse().getStatus();
                };
            };
            var one = pool.submit(attempt.get());
            var other = pool.submit(attempt.get());
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            go.countDown();
            assertEquals(Set.of(202, 409), Set.of(one.get(20, java.util.concurrent.TimeUnit.SECONDS),
                    other.get(20, java.util.concurrent.TimeUnit.SECONDS)));
        }
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE requested_examiner_id=?", second));
    }

    @Test
    void humanGradingJobsAreCreatedWithAnExaminerAndAiJobsWithout() throws Exception {
        jdbc.update("DELETE FROM outbox_events WHERE event_type=?", REQUESTED);
        UUID submission = submissions.execute(new com.ieltspath.assessment.application.command.CreateLearnerSubmissionCommand(
                learner, null, "{}", com.ieltspath.assessment.domain.vo.Skill.WRITING, "A practice essay.", null,
                "selection-" + UUID.randomUUID())).id();
        String token = internalToken(learner, "CUSTOMER");
        mvc.perform(post("/api/assessments/grading-jobs").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(job(submission, "HUMAN", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/assessments/grading-jobs").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(job(submission, "AI", examiner)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/assessments/grading-jobs").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(job(submission, "HUMAN", examiner)))
                .andExpect(status().isCreated());
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE submission_id=? AND requested_examiner_id=? AND requested_at IS NOT NULL", submission, examiner));
        // The chosen examiner is told once; the refused requests above announced nothing.
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type=? AND payload->>'examinerId'=?", REQUESTED, examiner.toString()));
    }

    @Test
    void theLimitCountsWhatTheExaminerReceivedTodayAndStartsAgainTomorrow() throws Exception {
        accept(second, 2);
        UUID first = requestReview(second).job();
        requestReview(second);

        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.reason").value("FULL"));

        // Grading a submission does not give the place back: the limit is about what is received in a day.
        jdbc.update("UPDATE grading_jobs SET status='COMPLETED' WHERE id=?", first);
        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)).andExpect(status().isConflict());

        // What was received before today does not count against today.
        jdbc.update("UPDATE grading_jobs SET requested_at=CURRENT_TIMESTAMP-INTERVAL '2 days' WHERE id=?", first);
        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)).andExpect(status().isAccepted());
        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)).andExpect(status().isConflict());
    }

    @Test
    void writingAndSpeakingShareTheOneDailyLimit() throws Exception {
        accept(second, 2);
        requestReview(second);
        UUID speaking = requestReview(second).job();
        jdbc.update("UPDATE grading_jobs SET skill='SPEAKING' WHERE id=?", speaking);

        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.reason").value("FULL"));
        mvc.perform(get("/api/assessments/examiners/availability").param("ids", second.toString())
                        .header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER")))
                .andExpect(jsonPath("$[0].full").value(true));
        mvc.perform(get("/api/assessments/examiners/me/summary").header("Authorization", "Bearer " + internalToken(second, "EXAMINER")))
                .andExpect(jsonPath("$.receivedToday").value(2)).andExpect(jsonPath("$.dailyLimit").value(2));
    }

    @Test
    void theLearnerListShowsWhichExaminersCannotTakeAnotherSubmissionToday() throws Exception {
        accept(second, 1);
        requestReview(second);
        when(examinerDirectory.find(fourth)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(fourth, false, 10)));
        UUID nobody = UUID.randomUUID();
        when(examinerDirectory.find(nobody)).thenReturn(Optional.empty());
        String learnerToken = internalToken(learner, "CUSTOMER");

        mvc.perform(get("/api/assessments/examiners/availability")
                        .param("ids", second.toString(), third.toString(), fourth.toString(), nobody.toString())
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.examinerId=='" + second + "')].full").value(true))
                .andExpect(jsonPath("$[?(@.examinerId=='" + third + "')].full").value(false))
                .andExpect(jsonPath("$[?(@.examinerId=='" + fourth + "')].full").value(true));

        mvc.perform(get("/api/assessments/examiners/availability").header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isBadRequest());
        String tooMany = String.join(",", java.util.Collections.nCopies(51, third.toString()));
        mvc.perform(get("/api/assessments/examiners/availability").param("ids", tooMany)
                        .header("Authorization", "Bearer " + learnerToken)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/assessments/examiners/availability").param("ids", second.toString())
                        .header("Authorization", "Bearer " + internalToken(examiner, "EXAMINER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void theLearnerIsToldOnceWhenTheChosenExaminerIsLate() throws Exception {
        UUID job = requestReview(examiner).job();
        assertEquals(0, announce.execute(java.time.Instant.now()), "not late yet");

        makeLate(job);
        assertEquals(1, announce.execute(java.time.Instant.now()));
        assertEquals(0, announce.execute(java.time.Instant.now()), "already told");

        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type='assessment.grading-overdue.v1'"));
        var event = json.readTree(jdbc.queryForObject(
                "SELECT payload::text FROM outbox_events WHERE event_type='assessment.grading-overdue.v1'", String.class));
        assertEquals(learner.toString(), event.get("learnerId").asText());
        assertEquals(examiner.toString(), event.get("examinerId").asText());
        assertEquals(job.toString(), event.get("gradingJobId").asText());
        assertEquals("WRITING", event.get("skill").asText());
        assertEquals("assessment.grading-overdue.v1", event.get("eventType").asText());
        assertEquals(java.time.Instant.parse(event.get("deadline").asText()).isBefore(java.time.Instant.now()), true);
        // What the learner is told is only who and which job: no essay, no band.
        assertEquals(java.util.Set.of("eventId", "eventType", "occurredAt", "learnerId", "examinerId", "gradingJobId",
                "skill", "deadline"), json.convertValue(event, java.util.Map.class).keySet());
    }

    @Test
    void aJobGivenToAnotherExaminerIsToldAgainWhenThatOneIsLateToo() throws Exception {
        UUID job = requestReview(examiner).job();
        makeLate(job);
        assertEquals(1, announce.execute(java.time.Instant.now()));

        choose(job, internalToken(learner, "CUSTOMER"), second).andExpect(status().isOk());
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND overdue_notified_at IS NULL", job));
        assertEquals(0, announce.execute(java.time.Instant.now()), "the new examiner has a fresh deadline");

        makeLate(job);
        assertEquals(1, announce.execute(java.time.Instant.now()));
        assertEquals(2, count("SELECT count(*) FROM outbox_events WHERE event_type='assessment.grading-overdue.v1'"));
    }

    @Test
    void anAttemptWithSeveralLateJobsIsAnnouncedOnceAndAllItsJobsAreMarked() throws Exception {
        UUID first = humanJob(courseEssay());
        // A second job of the same attempt for the same examiner (the attempt has two essays).
        jdbc.update("""
                INSERT INTO grading_jobs (id, submission_id, user_id, skill, grading_mode, status, idempotency_key, reason,
                    requested_examiner_id, requested_at)
                SELECT gen_random_uuid(), submission_id, user_id, skill, grading_mode, status,
                    'sibling-' || gen_random_uuid(), reason, requested_examiner_id, requested_at
                FROM grading_jobs WHERE id=?
                """, first);
        // And a job of someone else's attempt, late too: it is told separately.
        UUID other = humanJob(courseEssay());
        jdbc.update("UPDATE grading_jobs SET requested_at=CURRENT_TIMESTAMP-INTERVAL '49 hours' WHERE requested_examiner_id=?", examiner);

        assertEquals(2, announce.execute(java.time.Instant.now()), "one notice per attempt");

        assertEquals(3, count("SELECT count(*) FROM grading_jobs WHERE overdue_notified_at IS NOT NULL"));
        assertEquals(0, announce.execute(java.time.Instant.now()));
        assertEquals(2, count("SELECT count(*) FROM outbox_events WHERE event_type='assessment.grading-overdue.v1'"));
        var ofTheAttempt = jdbc.queryForList("SELECT id::text FROM grading_jobs WHERE submission_id=(SELECT submission_id FROM grading_jobs WHERE id=?) AND grading_mode='HUMAN' AND COALESCE(reason,'')<>'QA_SAMPLE'", String.class, first);
        assertEquals(2, ofTheAttempt.size());
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE payload->>'gradingJobId' IN (?, ?)",
                ofTheAttempt.get(0), ofTheAttempt.get(1)), "the attempt is told once, not once per job");
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE payload->>'gradingJobId'=?", other.toString()));
    }

    @Test
    void finishedJobsAndJobsNobodyWasChosenForAreNotAnnounced() throws Exception {
        UUID finished = requestReview(examiner).job();
        makeLate(finished);
        jdbc.update("UPDATE grading_jobs SET status='COMPLETED' WHERE id=?", finished);
        UUID waiting = unchosenHumanJob(courseEssay());
        jdbc.update("UPDATE grading_jobs SET created_at=CURRENT_TIMESTAMP-INTERVAL '5 days' WHERE id=?", waiting);

        assertEquals(0, announce.execute(java.time.Instant.now()));
        assertEquals(0, count("SELECT count(*) FROM outbox_events WHERE event_type='assessment.grading-overdue.v1'"));
    }

    private static final String REQUESTED = "assessment.grading-requested.v1";

    @Test
    void theExaminerIsToldAtOnceWhenALearnerChoosesThem() throws Exception {
        jdbc.update("DELETE FROM outbox_events WHERE event_type=?", REQUESTED);
        Requested first = requestReview(examiner);

        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type=?", REQUESTED));
        var event = json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events WHERE event_type=?", String.class, REQUESTED));
        assertEquals(examiner.toString(), event.get("examinerId").asText());
        assertEquals(first.job().toString(), event.get("gradingJobId").asText());
        assertEquals("WRITING", event.get("skill").asText());
        java.time.Instant chosenAt = jdbc.queryForObject("SELECT requested_at FROM grading_jobs WHERE id=?", java.sql.Timestamp.class, first.job()).toInstant();
        long secondsOff = Math.abs(java.time.Duration.between(chosenAt.plusSeconds(48 * 3600),
                java.time.Instant.parse(event.get("deadline").asText())).getSeconds());
        org.junit.jupiter.api.Assertions.assertTrue(secondsOff <= 5, "the deadline is 48 hours after the choice");
        // Grading is blind: the notice says which job, never whose.
        assertEquals(java.util.Set.of("eventId", "eventType", "occurredAt", "examinerId", "gradingJobId", "skill", "deadline"),
                json.convertValue(event, java.util.Map.class).keySet());

        // The same request sent again finds the job already announced.
        postReview(first.body()).andExpect(status().isAccepted());
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type=?", REQUESTED));
    }

    @Test
    void aRefusedRequestAnnouncesNothing() throws Exception {
        jdbc.update("DELETE FROM outbox_events WHERE event_type=?", REQUESTED);
        accept(second, 0);

        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)).andExpect(status().isConflict());

        assertEquals(0, count("SELECT count(*) FROM outbox_events WHERE event_type=?", REQUESTED));
    }

    @Test
    void theNewExaminerIsToldWhenTheLearnerReplacesALateOne() throws Exception {
        UUID job = requestReview(examiner).job();
        jdbc.update("DELETE FROM outbox_events WHERE event_type=?", REQUESTED);
        makeLate(job);

        choose(job, internalToken(learner, "CUSTOMER"), second).andExpect(status().isOk());

        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type=?", REQUESTED));
        var event = json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events WHERE event_type=?", String.class, REQUESTED));
        assertEquals(second.toString(), event.get("examinerId").asText());
        assertEquals(job.toString(), event.get("gradingJobId").asText());
    }

    @Test
    void anotherLearnerCannotReplaceTheExaminerOfSomeoneElsesLateJob() throws Exception {
        UUID job = requestReview(examiner).job();
        makeLate(job);
        jdbc.update("DELETE FROM outbox_events WHERE event_type=?", REQUESTED);
        String stranger = internalToken(UUID.randomUUID(), "CUSTOMER");

        // The job is looked up by its id and its owner: for anyone else it does not exist.
        choose(job, stranger, second).andExpect(status().isNotFound());

        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND requested_examiner_id=? AND examiner_changes=0", job, examiner));
        assertEquals(0, count("SELECT count(*) FROM outbox_events WHERE event_type=?", REQUESTED), "nobody is told about a refused change");
        // And the stranger's own list never shows the job.
        mvc.perform(get("/api/assessments/grading-jobs/mine").header("Authorization", "Bearer " + stranger))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        // The owner still can, so the refusal was about who asked.
        choose(job, internalToken(learner, "CUSTOMER"), second).andExpect(status().isOk());
    }

    @Test
    void twoRequestsToChooseTheSameExaminerForTheLastPlaceCannotBothSucceed() throws Exception {
        accept(second, 1);
        UUID firstJob = unchosenHumanJob(courseEssay());
        UUID otherJob = unchosenHumanJob(courseEssay());
        String token = internalToken(learner, "CUSTOMER");
        var ready = new java.util.concurrent.CountDownLatch(2);
        var go = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.function.Function<UUID, java.util.concurrent.Callable<Integer>> attempt = jobId -> () -> {
                ready.countDown();
                if (!go.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                return choose(jobId, token, second).andReturn().getResponse().getStatus();
            };
            var one = pool.submit(attempt.apply(firstJob));
            var other = pool.submit(attempt.apply(otherJob));
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            go.countDown();
            assertEquals(Set.of(200, 409), Set.of(one.get(20, java.util.concurrent.TimeUnit.SECONDS),
                    other.get(20, java.util.concurrent.TimeUnit.SECONDS)));
        }
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE requested_examiner_id=?", second));
    }
    @Test
    void anExaminerWithFiveLateJobsTakesNoNewSubmissionUntilTheyGradeSome() throws Exception {
        String learnerToken = internalToken(learner, "CUSTOMER");
        java.util.List<UUID> late = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) late.add(requestReview(second).job());
        for (UUID job : late) makeLate(job);

        // Four late jobs are still below the limit: the fifth is taken.
        UUID fifth = requestReview(second).job();
        mvc.perform(get("/api/assessments/examiners/availability").param("ids", second.toString())
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$[0].full").value(false)).andExpect(jsonPath("$[0].backlog").value(false));

        makeLate(fifth);
        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXAMINER_UNAVAILABLE"))
                .andExpect(jsonPath("$.details.reason").value("OVERDUE_BACKLOG"));
        UUID waiting = unchosenHumanJob(courseEssay());
        choose(waiting, learnerToken, second).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.reason").value("OVERDUE_BACKLOG"));
        mvc.perform(get("/api/assessments/examiners/availability").param("ids", second.toString(), third.toString())
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$[?(@.examinerId=='" + second + "')].full").value(true))
                .andExpect(jsonPath("$[?(@.examinerId=='" + second + "')].backlog").value(true))
                .andExpect(jsonPath("$[?(@.examinerId=='" + third + "')].backlog").value(false));
        mvc.perform(get("/api/assessments/examiners/me/summary").header("Authorization", "Bearer " + internalToken(second, "EXAMINER")))
                .andExpect(jsonPath("$.overdue").value(5)).andExpect(jsonPath("$.overdueLimit").value(5));

        // Their jobs that are already late stay with them, and grading some opens the door again.
        assertEquals(5, count("SELECT count(*) FROM grading_jobs WHERE requested_examiner_id=? AND status='QUEUED'", second));
        jdbc.update("UPDATE grading_jobs SET status='COMPLETED' WHERE id=?", fifth);
        postReview(reviewBody(UUID.randomUUID(), UUID.randomUUID(), second)).andExpect(status().isAccepted());
        choose(waiting, learnerToken, second).andExpect(status().isOk());
    }

    @Test
    void aLateJobWithNoChangeLeftCanBeCancelledByItsLearnerAndOnlyByThem() throws Exception {
        UUID job = requestReview(examiner).job();
        UUID review = jdbc.queryForObject("SELECT human_review_submission_id FROM grading_jobs WHERE id=?", UUID.class, job);
        String learnerToken = internalToken(learner, "CUSTOMER");

        cancel(review, learnerToken).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GRADING_CANCEL_NOT_ALLOWED"))
                .andExpect(jsonPath("$.details.reason").value("NOT_OVERDUE"));
        makeLate(job);
        cancel(review, learnerToken).andExpect(status().isConflict()).andExpect(jsonPath("$.details.reason").value("CHANGES_LEFT"));
        choose(job, learnerToken, second).andExpect(status().isOk());
        makeLate(job);
        choose(job, learnerToken, third).andExpect(status().isOk());
        makeLate(job);
        mvc.perform(get("/api/assessments/grading-jobs/mine").header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$[0].state").value("OVERDUE")).andExpect(jsonPath("$[0].canChoose").value(false))
                .andExpect(jsonPath("$[0].canCancel").value(true));

        cancel(review, internalToken(UUID.randomUUID(), "CUSTOMER")).andExpect(status().isNotFound());
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND status='QUEUED'", job));

        cancel(review, learnerToken).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        cancel(review, learnerToken).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND status='CANCELLED'", job));
        mvc.perform(get("/api/assessments/grading-jobs/mine").header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$[0].state").value("CANCELLED")).andExpect(jsonPath("$[0].canChoose").value(false))
                .andExpect(jsonPath("$[0].canCancel").value(false));
        mvc.perform(get("/internal/assessments/human-reviews/{id}", review).header("Authorization", "Bearer " + learnerToken))
                .andExpect(jsonPath("$.status").value("CANCELLED")).andExpect(jsonPath("$.band").doesNotExist());
        // The examiner's queue no longer holds it, and nobody can take it.
        mvc.perform(get("/api/assessments/grading-jobs").header("Authorization", "Bearer " + internalToken(third, "EXAMINER")))
                .andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim", job).header("Authorization", "Bearer " + internalToken(third, "EXAMINER")))
                .andExpect(status().isConflict());
        choose(job, learnerToken, fourth).andExpect(status().isConflict());
    }

    @Test
    void aJobThatWasGradedInTheMeantimeCannotBeCancelled() throws Exception {
        UUID job = requestReview(examiner).job();
        UUID review = jdbc.queryForObject("SELECT human_review_submission_id FROM grading_jobs WHERE id=?", UUID.class, job);
        jdbc.update("UPDATE grading_jobs SET status='COMPLETED',examiner_changes=2 WHERE id=?", job);
        jdbc.update("UPDATE human_review_submissions SET band=6.5,criteria='[]'::jsonb,error_codes='{}',feedback='Done.',completed_at=CURRENT_TIMESTAMP WHERE id=?", review);
        makeLate(job);
        cancel(review, internalToken(learner, "CUSTOMER")).andExpect(status().isConflict());
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND status='COMPLETED'", job));
    }

    private org.springframework.test.web.servlet.ResultActions cancel(UUID review, String token) throws Exception {
        return mvc.perform(post("/internal/assessments/human-reviews/{id}/cancel", review).header("Authorization", "Bearer " + token));
    }

    private void accept(UUID id, int limit) {
        when(examinerDirectory.find(id)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(id, true, limit)));
    }

    private void makeLate(UUID job) {
        jdbc.update("UPDATE grading_jobs SET requested_at=CURRENT_TIMESTAMP-INTERVAL '49 hours' WHERE id=?", job);
    }

    private org.springframework.test.web.servlet.ResultActions choose(UUID job, String token, UUID examinerId) throws Exception {
        return mvc.perform(post("/api/assessments/grading-jobs/{id}/examiner", job).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(chosen(examinerId)));
    }

    private static String chosen(UUID examinerId) {
        return "{\"examinerId\":\"" + examinerId + "\"}";
    }

    private static String job(UUID submission, String mode, UUID examinerId) {
        return "{\"submissionId\":\"" + submission + "\",\"skill\":\"WRITING\",\"gradingMode\":\"" + mode + "\","
                + (mode.equals("AI") ? "\"pointCostSnapshot\":3," : "") + "\"idempotencyKey\":\"" + UUID.randomUUID() + "\""
                + (examinerId == null ? "" : ",\"examinerId\":\"" + examinerId + "\"") + "}";
    }

    private String reviewBody(UUID requestId, UUID sourceId, UUID examinerId) {
        return """
                {"requestId":"%s","sourceSubmissionId":"%s","source":"PRACTICE","task":"TASK_2",
                 "promptSnapshot":{"stem":"Discuss both views."},"essay":"An essay.",
                 "rubric":{"code":"IELTS_WRITING_TASK_2","version":1,"criteria":[]}%s}
                """.formatted(requestId, sourceId, examinerId == null ? "" : ",\"examinerId\":\"" + examinerId + "\"");
    }

    private org.springframework.test.web.servlet.ResultActions postReview(String body) throws Exception {
        return mvc.perform(post("/internal/assessments/human-reviews").header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Requested requestReview(UUID examinerId) throws Exception {
        String body = reviewBody(UUID.randomUUID(), UUID.randomUUID(), examinerId);
        String answer = postReview(body).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        UUID review = UUID.fromString(json.readTree(answer).get("reviewSubmissionId").asText());
        return new Requested(body, jdbc.queryForObject("SELECT id FROM grading_jobs WHERE human_review_submission_id=?", UUID.class, review));
    }

    private record Requested(String body, UUID job) {}
}
