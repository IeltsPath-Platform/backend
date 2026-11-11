package com.ieltspath.learning.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.vo.WritingProgressEssay;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Runs the progress query against real PostgreSQL with every Learning migration applied. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcWritingProgressQueryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static JdbcWritingSubmissionRepository repository;

    @BeforeAll
    static void migrate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        repository = new JdbcWritingSubmissionRepository(jdbc, new ObjectMapper());
    }

    @Test
    void returnsOnlyTheLearnersGradedFirstEssaysOfTheTaskNewestFirst() {
        UUID learner = UUID.randomUUID();
        UUID newestLesson = insertGraded(learner, "TASK_2", 1, 1, "LESSON_BLOCK");
        insertGraded(learner, "TASK_2", 2, 2, "PRACTICE_ITEM");           // a revision: left out
        UUID freeWriting = insertGraded(learner, "TASK_2", 3, 1, "FREE_WRITING");
        UUID oldestLesson = insertGraded(learner, "TASK_2", 4, 1, "LESSON_BLOCK");
        insertGraded(learner, "TASK_1", 0, 1, "LESSON_BLOCK");            // other task
        insertGraded(UUID.randomUUID(), "TASK_2", 0, 1, "LESSON_BLOCK");  // other learner
        insertUngraded(learner, "TASK_2", 0);                             // not graded

        List<WritingProgressEssay> essays = repository.findFirstGradedEssays(learner, "TASK_2", 10);

        assertEquals(List.of(newestLesson, freeWriting, oldestLesson),
                essays.stream().map(WritingProgressEssay::submissionId).toList());
        assertEquals("TENSE", essays.getFirst().corrections().getFirst().errorCode());
        assertEquals("Tense", essays.getFirst().corrections().getFirst().errorName());
        assertEquals(4, essays.getFirst().criteria().size());
    }

    @Test
    void honorsTheLimit() {
        UUID learner = UUID.randomUUID();
        for (int day = 0; day < 5; day++) insertGraded(learner, "TASK_1", day, 1, "LESSON_BLOCK");

        assertEquals(2, repository.findFirstGradedEssays(learner, "TASK_1", 2).size());
    }

    /** A graded essay; {@code daysAgo} sets its grading time so the ordering is explicit. Columns follow the source's check. */
    private static UUID insertGraded(UUID learner, String task, int daysAgo, int revisionNo, String source) {
        UUID id = UUID.randomUUID();
        String result = "{\"criteria\":[{\"code\":\"TR\",\"band\":6.0},{\"code\":\"CC\",\"band\":6.0},"
                + "{\"code\":\"LR\",\"band\":6.0},{\"code\":\"GRA\",\"band\":6.0}],"
                + "\"corrections\":[{\"excerpt\":\"x\",\"suggestion\":\"y\",\"category\":\"GRAMMAR\","
                + "\"errorCode\":\"TENSE\",\"errorName\":\"Tense\",\"fixHint\":\"hint\",\"severity\":2}],"
                + "\"summary\":\"s\"}";
        jdbc.update("""
                INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, question_version_id,
                    knowledge_point_ids, request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                    debit_ledger_entry_id, result, overall_band, passed, grading_started_at, submitted_at, graded_at,
                    source, practice_attempt_id, revision_no, prompt_version)
                VALUES (?, ?, ?, ?, ?, '{}'::uuid[], ?, 'essay text', 250, CAST(? AS jsonb), 'GRADED', 3, ?,
                    CAST(? AS jsonb), 6.0, true, now(), now(), now() - make_interval(days => ?),
                    ?, ?, ?, 'rubric@v1+tplv1')
                """, id, learner, lessonIdFor(source), blockIdFor(source), UUID.randomUUID(), UUID.randomUUID(),
                "{\"task\":\"" + task + "\"}", UUID.randomUUID(), result, daysAgo, source,
                practiceAttemptFor(source), revisionNo);
        return id;
    }

    private static void insertUngraded(UUID learner, String task, int daysAgo) {
        jdbc.update("""
                INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, question_version_id,
                    knowledge_point_ids, request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                    grading_started_at, submitted_at, graded_at, source, revision_no)
                VALUES (?, ?, ?, ?, ?, '{}'::uuid[], ?, 'essay text', 250, CAST(? AS jsonb), 'FAILED', 3,
                    now(), now() - make_interval(days => ?), NULL, 'LESSON_BLOCK', 1)
                """, UUID.randomUUID(), learner, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "{\"task\":\"" + task + "\"}", daysAgo);
    }

    /** Free writing has no lesson; lesson and practice essays always have one. */
    private static UUID lessonIdFor(String source) {
        return "FREE_WRITING".equals(source) ? null : UUID.randomUUID();
    }

    /** Only a lesson block essay names a block. */
    private static UUID blockIdFor(String source) {
        return "LESSON_BLOCK".equals(source) ? UUID.randomUUID() : null;
    }

    /** Only a practice essay names an attempt. */
    private static UUID practiceAttemptFor(String source) {
        return "PRACTICE_ITEM".equals(source) ? UUID.randomUUID() : null;
    }
}
