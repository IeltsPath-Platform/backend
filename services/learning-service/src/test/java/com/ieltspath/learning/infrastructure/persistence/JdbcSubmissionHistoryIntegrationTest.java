package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository.Entry;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
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

/** Runs the history queries against real PostgreSQL with every Learning migration applied. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcSubmissionHistoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static JdbcSubmissionHistoryRepository repository;

    @BeforeAll
    static void migrate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        repository = new JdbcSubmissionHistoryRepository(jdbc);
    }

    @Test
    void courseHistoryStacksEverySourceNewestFirstAndHoldsOnlyTheLearnersOwn() {
        UUID learner = UUID.randomUUID();
        UUID practice = insertPractice(learner, 6, "LESSON", true);
        UUID essay = insertEssay(learner, 5, "LESSON_BLOCK", "GRADED");
        UUID speaking = insertSpeaking(learner, 4, true, "GRADED");
        UUID topicTest = insertTopicTest(learner, 3);
        UUID courseTest = insertCourseTest(learner, 2);
        UUID milestone = insertPracticeTest(learner, 1);
        // left out: another learner, free practice, an unfinished attempt, a failed essay, abandoned speaking, an open test
        insertPractice(UUID.randomUUID(), 0, "LESSON", true);
        insertPractice(learner, 0, "FREE", true);
        insertPractice(learner, 0, "LESSON", false);
        insertEssay(learner, 0, "LESSON_BLOCK", "FAILED");
        insertEssay(learner, 0, "FREE_WRITING", "GRADED");
        insertSpeaking(learner, 0, true, "ABANDONED");
        insertSpeaking(learner, 0, false, "GRADED");
        jdbc.update("INSERT INTO topic_test_assignments (id, user_id, topic_id, package_id, package_version_id) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), learner, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        List<Entry> entries = repository.findCourse(learner, 20, 0);

        assertEquals(List.of(milestone, courseTest, topicTest, speaking, essay, practice),
                entries.stream().map(Entry::id).toList());
        assertEquals(List.of("PRACTICE_TEST", "COURSE_TEST", "TOPIC_TEST", "SPEAKING", "WRITING", "PRACTICE"),
                entries.stream().map(Entry::kind).toList());
        Entry practiceEntry = entries.getLast();
        assertEquals(5, practiceEntry.correctCount());
        assertEquals(8, practiceEntry.totalCount());
        assertEquals(Boolean.TRUE, practiceEntry.passed());
        assertEquals("READING", practiceEntry.skill());
        assertEquals(practiceEntry.id(), practiceEntry.attemptId());
        Entry essayEntry = entries.get(4);
        assertEquals(new BigDecimal("6.5"), essayEntry.band());
        assertEquals("TASK_2", essayEntry.task());
        assertEquals("Do schools matter?", essayEntry.stem());
        assertEquals(1, essayEntry.revisionNo());
        Entry topicEntry = entries.get(2);
        assertEquals(72.5, topicEntry.percent());
        assertTrue(topicEntry.attemptId() != null && topicEntry.topicId() != null && topicEntry.courseId() == null);
        assertTrue(entries.get(1).courseId() != null && entries.get(1).topicId() == null);
        assertNull(entries.get(3).stem());
    }

    @Test
    void aLessonLineCarriesTheTopicOfItsLessonWhenTheProgressHoldsIt() {
        UUID learner = UUID.randomUUID();
        UUID lesson = UUID.randomUUID();
        UUID topic = UUID.randomUUID();
        jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order) VALUES (?, ?, ?, 1)", learner, lesson, topic);
        jdbc.update("""
                INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id, submitted_at, request_id,
                    correct_count, total_count, passed)
                VALUES (?, ?, ?, 'READING', ?, ?, now(), ?, 3, 4, true)
                """, UUID.randomUUID(), learner, lesson, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        insertPractice(learner, 1, "LESSON", true); // a lesson the progress does not know

        List<Entry> entries = repository.findCourse(learner, 20, 0);

        assertEquals(2, entries.size(), "the join with progress must not repeat or drop a line");
        assertEquals(topic, entries.getFirst().topicId());
        assertEquals(lesson, entries.getFirst().lessonId());
        assertNull(entries.getLast().topicId());
    }

    @Test
    void courseHistoryPagesThroughTheRows() {
        UUID learner = UUID.randomUUID();
        for (int day = 0; day < 5; day++) insertPractice(learner, day, "LESSON", true);

        assertEquals(3, repository.findCourse(learner, 3, 0).size());
        assertEquals(2, repository.findCourse(learner, 3, 3).size());
        assertEquals(0, repository.findCourse(learner, 3, 6).size());
    }

    @Test
    void freeHistoryHoldsTheSkillsOwnFreeSubmissionsOnly() {
        UUID learner = UUID.randomUUID();
        UUID older = insertEssay(learner, 3, "FREE_WRITING", "GRADED");
        UUID newer = insertEssay(learner, 1, "FREE_WRITING", "AWAITING_EXAMINER");
        insertEssay(learner, 0, "LESSON_BLOCK", "GRADED");
        insertEssay(learner, 0, "FREE_WRITING", "FAILED");
        insertEssay(UUID.randomUUID(), 0, "FREE_WRITING", "GRADED");
        UUID spoken = insertSpeaking(learner, 2, false, "GRADED");
        insertSpeaking(learner, 0, true, "GRADED");

        assertEquals(List.of(newer, older), repository.findFree(learner, "WRITING", 20, 0).stream().map(Entry::id).toList());
        List<Entry> speaking = repository.findFree(learner, "SPEAKING", 20, 0);
        assertEquals(List.of(spoken), speaking.stream().map(Entry::id).toList());
        assertNull(speaking.getFirst().lessonId());
        assertTrue(speaking.getFirst().packageVersionId() != null);
        assertThrows(IllegalArgumentException.class, () -> repository.findFree(learner, "READING", 20, 0));
    }

    @Test
    void knowledgePracticeHoldsTheLearnersSubmittedFreeSetsNewestFirst() {
        UUID learner = UUID.randomUUID();
        UUID point = UUID.randomUUID();
        UUID older = insertFreePractice(learner, point, 3, true, 4, 8, false);
        UUID newer = insertFreePractice(learner, point, 1, true, 7, 8, true);
        insertFreePractice(learner, point, 0, false, 0, 8, false);
        insertFreePractice(UUID.randomUUID(), point, 0, true, 8, 8, true);
        insertPractice(learner, 0, "LESSON", true);

        List<Entry> entries = repository.findKnowledgePractice(learner, 20, 0);

        assertEquals(List.of(newer, older), entries.stream().map(Entry::id).toList());
        Entry latest = entries.getFirst();
        assertEquals("KP_PRACTICE", latest.kind());
        assertEquals(point, latest.knowledgePointId());
        assertEquals(newer, latest.attemptId());
        assertEquals(7, latest.correctCount());
        assertEquals(Boolean.TRUE, latest.passed());
        assertNull(latest.lessonId());
        assertEquals(1, repository.findKnowledgePractice(learner, 1, 1).size());
    }

    @Test
    void anEssaysSubmissionTimeIsReadOnlyForItsOwner() {
        UUID learner = UUID.randomUUID();
        UUID essay = insertEssay(learner, 2, "FREE_WRITING", "GRADED");

        assertTrue(repository.essaySubmittedAt(learner, essay).isPresent());
        assertTrue(repository.essaySubmittedAt(UUID.randomUUID(), essay).isEmpty());
    }

    @Test
    void tallyCountsTheFreeSetsOfEachAskedPointSinceAMomentAndKeepsTheLatest() {
        UUID learner = UUID.randomUUID();
        UUID point = UUID.randomUUID();
        UUID quiet = UUID.randomUUID();
        UUID notAsked = UUID.randomUUID();
        insertFreePractice(learner, point, 10, true, 1, 8, false); // before the moment
        insertFreePractice(learner, point, 3, true, 4, 8, false);
        UUID latest = insertFreePractice(learner, point, 1, true, 7, 8, true);
        insertFreePractice(learner, point, 0, false, 0, 8, false); // never submitted
        insertFreePractice(learner, notAsked, 1, true, 8, 8, true);
        insertFreePractice(UUID.randomUUID(), point, 1, true, 8, 8, true);

        var tallies = repository.tallyKnowledgePractice(learner, List.of(point, quiet),
                java.time.Instant.now().minus(java.time.Duration.ofDays(5)));

        assertEquals(1, tallies.size());
        var tally = tallies.getFirst();
        assertEquals(point, tally.knowledgePointId());
        assertEquals(2, tally.attempts());
        assertEquals(latest, tally.lastAttemptId());
        assertEquals(7, tally.lastCorrectCount());
        assertEquals(8, tally.lastTotalCount());
        assertEquals(Boolean.TRUE, tally.lastPassed());
        assertTrue(repository.tallyKnowledgePractice(learner, List.of(), java.time.Instant.EPOCH).isEmpty());
    }

    private static UUID insertFreePractice(UUID learner, UUID point, int daysAgo, boolean submitted, int correct, int total,
                                           boolean passed) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id, submitted_at,
                    request_id, correct_count, total_count, passed, origin, knowledge_point_id)
                VALUES (?, ?, NULL, 'READING', ?, ?, CASE WHEN ? THEN now() - make_interval(days => ?) END,
                    CASE WHEN ? THEN ? END, ?, ?, ?, 'FREE', ?)
                """, id, learner, UUID.randomUUID(), UUID.randomUUID(), submitted, daysAgo, submitted, UUID.randomUUID(),
                correct, total, passed, point);
        return id;
    }

    private static UUID insertPractice(UUID learner, int daysAgo, String origin, boolean submitted) {
        UUID id = UUID.randomUUID();
        boolean free = "FREE".equals(origin);
        jdbc.update("""
                INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id, submitted_at,
                    request_id, correct_count, total_count, passed, origin, knowledge_point_id)
                VALUES (?, ?, ?, 'READING', ?, ?, CASE WHEN ? THEN now() - make_interval(days => ?) END,
                    CASE WHEN ? THEN ? END, 5, 8, true, ?, ?)
                """, id, learner, free ? null : UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), submitted, daysAgo,
                submitted, UUID.randomUUID(), origin, free ? UUID.randomUUID() : null);
        return id;
    }

    /** An essay; free practice has no lesson, a lesson block names a block, and the result columns follow the status. */
    private static UUID insertEssay(UUID learner, int daysAgo, String source, String status) {
        UUID id = UUID.randomUUID();
        boolean graded = "GRADED".equals(status);
        jdbc.update("""
                INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, question_version_id,
                    knowledge_point_ids, request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                    debit_ledger_entry_id, result, overall_band, passed, grading_started_at, submitted_at, graded_at,
                    source, practice_attempt_id, revision_no)
                VALUES (?, ?, ?, ?, ?, '{}'::uuid[], ?, 'essay text', 250,
                    CAST('{"task":"TASK_2","stem":"Do schools matter?"}' AS jsonb), ?, 3,
                    CASE WHEN ? THEN ? END, CASE WHEN ? THEN CAST('{}' AS jsonb) END, CASE WHEN ? THEN 6.5 END,
                    CASE WHEN ? THEN true END, now(), now() - make_interval(days => ?), CASE WHEN ? THEN now() END,
                    ?, ?, 1)
                """, id, learner, "FREE_WRITING".equals(source) ? null : UUID.randomUUID(),
                "LESSON_BLOCK".equals(source) ? UUID.randomUUID() : null, UUID.randomUUID(), UUID.randomUUID(), status,
                graded, UUID.randomUUID(), graded, graded, graded, daysAgo, graded, source,
                "PRACTICE_ITEM".equals(source) ? UUID.randomUUID() : null);
        return id;
    }

    /** A speaking submission; a lesson one names a lesson and a Practice attempt together. */
    private static UUID insertSpeaking(UUID learner, int daysAgo, boolean inLesson, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO speaking_submissions (id, user_id, request_id, package_version_id, scope, status, point_cost,
                    consent_at, created_at, updated_at, lesson_id, practice_attempt_id, overall_band, result,
                    debit_ledger_entry_id)
                VALUES (?, ?, ?, ?, 'FULL', ?, 5, now(), now() - make_interval(days => ?), now(), ?, ?, 6.0,
                    CASE WHEN ? = 'GRADED' THEN CAST('{}' AS jsonb) END, CASE WHEN ? = 'GRADED' THEN ? END)
                """, id, learner, UUID.randomUUID(), UUID.randomUUID(), status, daysAgo,
                inLesson ? UUID.randomUUID() : null, inLesson ? UUID.randomUUID() : null, status, status, UUID.randomUUID());
        return id;
    }

    private static UUID insertTopicTest(UUID learner, int daysAgo) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO topic_test_assignments (id, user_id, topic_id, package_id, package_version_id, consumed_attempt_id,
                    consumed_at, percent)
                VALUES (?, ?, ?, ?, ?, ?, now() - make_interval(days => ?), 72.5)
                """, id, learner, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), daysAgo);
        return id;
    }

    private static UUID insertCourseTest(UUID learner, int daysAgo) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO course_test_assignments (id, user_id, course_id, package_id, package_version_id, consumed_attempt_id,
                    consumed_at, percent)
                VALUES (?, ?, ?, ?, ?, ?, now() - make_interval(days => ?), 61.25)
                """, id, learner, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), daysAgo);
        return id;
    }

    private static UUID insertPracticeTest(UUID learner, int daysAgo) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO practice_test_assignments (id, user_id, course_id, package_id, package_version_id, consumed_attempt_id,
                    consumed_at, percent)
                VALUES (?, ?, ?, ?, ?, ?, now() - make_interval(days => ?), 80)
                """, id, learner, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), daysAgo);
        return id;
    }
}
