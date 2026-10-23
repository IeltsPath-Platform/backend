package com.ieltspath.assessment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.ieltspath.assessment.application.service.AiEvaluationReadService;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcExaminerQueueStore;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The read views of the review screens, on the real schema: the reviewer's own essays and the AI evaluation lists. */
@Testcontainers(disabledWithoutDocker = true)
class ReviewReadsIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static JdbcTemplate jdbc;
    static JdbcExaminerQueueStore queue;
    static AiEvaluationReadService reads;

    @BeforeAll
    static void migrate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        queue = new JdbcExaminerQueueStore(new NamedParameterJdbcTemplate(dataSource), new ObjectMapper());
        reads = new AiEvaluationReadService(jdbc);
    }

    // --- essays the reviewer claimed

    @Test
    void aClaimedQaEssayIsInMineUntilTheReviewIsSubmitted() {
        UUID me = UUID.randomUUID();
        UUID job = insertJob("QA_SAMPLE", "QUEUED", me);

        assertEquals(List.of(job), ids(queue.claimedQaBy(me, 0, 20)));

        jdbc.update("UPDATE grading_jobs SET status = 'COMPLETED' WHERE id = ?", job);
        assertEquals(List.of(), ids(queue.claimedQaBy(me, 0, 20)));
    }

    @Test
    void mineHoldsOnlyMyQaEssays() {
        UUID me = UUID.randomUUID();
        UUID someoneElse = UUID.randomUUID();
        UUID mineQa = insertJob("QA_SAMPLE", "QUEUED", me);
        insertJob("QA_SAMPLE", "QUEUED", someoneElse);
        insertJob("QA_SAMPLE", "QUEUED", null);
        insertJob("HUMAN_CREDIT", "QUEUED", me);

        assertEquals(List.of(mineQa), ids(queue.claimedQaBy(me, 0, 20)));
    }

    @Test
    void mineIsPagedAndCountsEveryEssay() {
        UUID me = UUID.randomUUID();
        for (int i = 0; i < 3; i++) insertJob("QA_SAMPLE", "QUEUED", me);

        ExaminerQueueStore.QueuePage first = queue.claimedQaBy(me, 0, 2);
        ExaminerQueueStore.QueuePage second = queue.claimedQaBy(me, 1, 2);

        assertEquals(2, first.content().size());
        assertEquals(1, second.content().size());
        assertEquals(3, first.totalElements());
        assertEquals(3, second.totalElements());
    }

    // --- AI evaluation lists

    @Test
    void goldEssaysFilterByTaskAndPageNewestFirst() {
        jdbc.update("DELETE FROM ai_gold_essays");
        UUID older = insertGold("TASK_1", "MANUAL", "older essay", "2026-10-01T00:00:00Z");
        UUID newer = insertGold("TASK_1", "COMPARISON", "newer essay", "2026-10-02T00:00:00Z");
        insertGold("TASK_2", "MANUAL", "other task", "2026-10-03T00:00:00Z");

        var task1 = reads.goldEssays("TASK_1", 0, 10);

        assertEquals(2, task1.totalElements());
        assertEquals(List.of(newer, older), task1.content().stream().map(AiEvaluationReadService.GoldEssayRow::id).toList());
        assertEquals("COMPARISON", task1.content().get(0).source());
        assertEquals(3, reads.goldEssays(null, 0, 10).totalElements());
        assertEquals(1, reads.goldEssays(null, 1, 2).content().size());
    }

    @Test
    void goldEssayTextIsCutToAPreview() {
        jdbc.update("DELETE FROM ai_gold_essays");
        insertGold("TASK_2", "MANUAL", "e".repeat(500), "2026-10-01T00:00:00Z");

        assertEquals(200, reads.goldEssays("TASK_2", 0, 10).content().getFirst().essayPreview().length());
    }

    @Test
    void evaluationsListNewestFirstAndFilterByTask() {
        jdbc.update("DELETE FROM ai_prompt_activations");
        jdbc.update("DELETE FROM ai_eval_runs");
        UUID first = insertRun("TASK_2", "DONE", true, "2026-10-01T00:00:00Z");
        UUID second = insertRun("TASK_2", "FAILED", null, "2026-10-02T00:00:00Z");
        insertRun("TASK_1", "RUNNING", null, "2026-10-03T00:00:00Z");

        var task2 = reads.evaluations("TASK_2", 0, 10);

        assertEquals(List.of(second, first), task2.content().stream().map(AiEvaluationReadService.EvaluationRow::id).toList());
        assertEquals(Boolean.TRUE, task2.content().get(1).passed());
        assertNull(task2.content().get(0).passed());
        assertNull(task2.content().get(0).finishedAt());
        assertEquals(3, reads.evaluations(null, 0, 10).totalElements());
    }

    @Test
    void activationsAreNullUntilARunIsActivated() {
        jdbc.update("DELETE FROM ai_prompt_activations");
        jdbc.update("DELETE FROM ai_eval_runs");

        var none = reads.activations();
        assertEquals(List.of("TASK_1", "TASK_2"), none.stream().map(AiEvaluationReadService.ActivationRow::task).toList());
        assertTrue(none.stream().allMatch(row -> row.promptVersion() == null && row.runId() == null
                && row.activatedAt() == null));

        UUID run = insertRun("TASK_2", "DONE", true, "2026-10-01T00:00:00Z");
        jdbc.update("INSERT INTO ai_prompt_activations(task,prompt_version,run_id,activated_by) VALUES('TASK_2','v7',?,?)",
                run, UUID.randomUUID());

        var activated = reads.activations();
        assertNull(activated.get(0).promptVersion());
        assertEquals("v7", activated.get(1).promptVersion());
        assertEquals(run, activated.get(1).runId());
    }

    @Test
    void badTaskOrPagingIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> reads.goldEssays("TASK_9", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> reads.evaluations(null, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> reads.evaluations(null, 0, 101));
        assertThrows(IllegalArgumentException.class, () -> reads.goldEssays(null, 0, 0));
    }

    private static List<UUID> ids(ExaminerQueueStore.QueuePage page) {
        return page.content().stream().map(ExaminerQueueStore.QueueEntry::id).toList();
    }

    private static UUID insertJob(String reason, String status, UUID assignedTo) {
        UUID reviewSubmission = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO human_review_submissions(id,request_id,learner_id,source_submission_id,source,task,
                    prompt_snapshot,essay_text,rubric_snapshot)
                VALUES (?,?,?,?,'LESSON','TASK_2','{}'::jsonb,'essay','{}'::jsonb)
                """, reviewSubmission, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        UUID job = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO grading_jobs(id,human_review_submission_id,user_id,skill,grading_mode,status,
                    idempotency_key,reason,assigned_examiner_id,claimed_at)
                VALUES (?,?,?,'WRITING','HUMAN',?,?,?,?,CASE WHEN ?::uuid IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END)
                """, job, reviewSubmission, UUID.randomUUID(), status, "test:" + job, reason, assignedTo, assignedTo);
        return job;
    }

    private static UUID insertGold(String task, String source, String essay, String createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ai_gold_essays(id,task,prompt,essay,examiner_band,source,created_by,created_at)
                VALUES (?,?,'prompt',?,6.5,?,?,?::timestamptz)
                """, id, task, essay, source, UUID.randomUUID(), createdAt);
        return id;
    }

    private static UUID insertRun(String task, String status, Boolean passed, String startedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ai_eval_runs(id,task,model,prompt_version,status,n,within_half_band,mae,qwk,passed,started_at)
                VALUES (?,?,'model','v1',?,5,?,?,?,?,?::timestamptz)
                """, id, task, status, new BigDecimal("0.80000"), new BigDecimal("0.5000"), new BigDecimal("0.600000"),
                passed, startedAt);
        return id;
    }
}
