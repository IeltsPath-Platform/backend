package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.service.FlagDetector;
import com.ieltspath.learning.application.service.WorkItemSync;
import com.ieltspath.learning.application.usecase.StaffInsightUseCase;
import com.ieltspath.learning.application.usecase.StaffWorkUseCase;
import com.ieltspath.learning.application.usecase.StaffWorkUseCase.UpdateWork;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import com.ieltspath.learning.domain.vo.StaffRecords.FlagKind;
import com.ieltspath.learning.domain.vo.StaffRecords.FlagStatus;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkKind;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Anomaly flags, work queues and the statistics behind them, against real PostgreSQL. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class StaffAreaIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired QuestionEventLog events;
    @Autowired FlagDetector detector;
    @Autowired WorkItemSync sync;
    @Autowired StaffWorkUseCase work;
    @Autowired StaffInsightUseCase insight;
    @Autowired com.ieltspath.learning.infrastructure.scheduler.WorkItemSyncJob job;
    @Autowired javax.sql.DataSource dataSource;
    @MockitoBean LearningContentClient content;

    private static final Set<Queue> EXAMINER = EnumSet.of(Queue.EXAMINER);
    private static final Set<Queue> CONTENT = EnumSet.of(Queue.CONTENT_AUTHOR);
    private static final Set<Queue> ADMIN = EnumSet.allOf(Queue.class);

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE learner_flags, work_items, content_gap_signals, remediation_flags, diagnostic_probes, "
                + "diagnostic_sessions, question_attempt_events, lesson_writing_submissions, review_items CASCADE");
        when(content.getTopicSequence()).thenReturn(List.of());
    }

    private QuestionAttemptEvent event(UUID user, UUID question, boolean correct, Long duration, boolean first) {
        return QuestionAttemptEvent.of(user, question, List.of(UUID.randomUUID()), correct, duration, null,
                QuestionEventSource.PRACTICE_SET, UUID.randomUUID(), first);
    }

    private int flags(UUID user, FlagKind kind) {
        return jdbc.queryForObject("SELECT count(*) FROM learner_flags WHERE user_id = ? AND kind = ?", Integer.class,
                user, kind.name());
    }

    @Test
    void aFastAndAccurateDayRaisesOneFlagAndIsNotRaisedAgainWhileItIsRecent() {
        UUID user = UUID.randomUUID();
        List<QuestionAttemptEvent> batch = new ArrayList<>();
        for (int index = 0; index < 10; index++) batch.add(event(user, UUID.randomUUID(), true, 1500L, true));
        events.append(batch);

        assertEquals(1, flags(user, FlagKind.TOO_FAST_HIGH_SCORE));
        assertEquals(0, flags(user, FlagKind.BURST_SUBMISSIONS));
        assertTrue(detector.check(user).isEmpty(), "already flagged recently");
        assertEquals(1, flags(user, FlagKind.TOO_FAST_HIGH_SCORE));
    }

    @Test
    void moreThanThirtySubmissionsInTenMinutesIsABurstAndOnlyTheWritingLearnerIsChecked() {
        UUID user = UUID.randomUUID();
        UUID bystander = UUID.randomUUID();
        List<QuestionAttemptEvent> batch = new ArrayList<>();
        for (int index = 0; index < 31; index++) batch.add(event(user, UUID.randomUUID(), index % 2 == 0, null, true));
        events.append(batch);

        assertEquals(1, flags(user, FlagKind.BURST_SUBMISSIONS));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM learner_flags WHERE user_id = ?", Integer.class,
                bystander));
    }

    @Test
    void anEssayAnotherLearnerAlsoSentIsFlaggedAsADuplicate() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        String hash = com.ieltspath.learning.domain.service.AnomalyRules.essayHash("Same essay text");
        essay(first, hash);
        assertTrue(detector.check(first).isEmpty(), "nobody else sent it yet");
        essay(second, hash);

        assertEquals(List.of(FlagKind.DUPLICATE_ESSAY), detector.check(second));
        assertEquals(1, flags(second, FlagKind.DUPLICATE_ESSAY));
    }

    private void essay(UUID user, String hash) {
        jdbc.update("""
                INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, question_version_id,
                knowledge_point_ids, request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                grading_started_at, submitted_at, essay_hash)
                VALUES (?, ?, ?, ?, ?, ARRAY[]::uuid[], ?, 'x', 1, '{}'::jsonb, 'GRADING', 3, now(), now(), ?)
                """, UUID.randomUUID(), user, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), hash);
    }

    @Test
    void syncMakesOneItemPerSourceAndEachRoleSeesOnlyItsQueue() {
        UUID user = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        UUID review = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill) "
                + "VALUES (?, ?, ?, ?, 'PENDING', 'READING')", review, user, UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("INSERT INTO remediation_flags (id, user_id, course_id, topic_id, kind, source_attempt_id, "
                + "review_ids) VALUES (?, ?, ?, ?, 'MID_COURSE', ?, ARRAY[?]::uuid[])", UUID.randomUUID(), user, course,
                UUID.randomUUID(), UUID.randomUUID(), review);
        jdbc.update("INSERT INTO content_gap_signals (kp_id, occurrences, first_detected_at, last_detected_at) "
                + "VALUES (?, 2, now(), now())", UUID.randomUUID());
        jdbc.update("INSERT INTO learner_flags (id, user_id, kind, evidence) VALUES (?, ?, 'BURST_SUBMISSIONS', "
                + "'{}'::jsonb)", UUID.randomUUID(), user);

        assertEquals(3, sync.sync());
        assertEquals(0, sync.sync(), "syncing again adds nothing");

        var examiner = work.list(EXAMINER, null, null);
        assertEquals(Set.of(WorkKind.MID_COURSE_REMEDIATION, WorkKind.LEARNER_FLAG),
                examiner.stream().map(item -> item.kind()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(List.of(WorkKind.CONTENT_GAP), work.list(CONTENT, null, null).stream()
                .map(item -> item.kind()).toList());
        assertEquals(3, work.list(ADMIN, null, null).size());
        var denied = assertThrows(LearningRequestException.class, () -> work.list(EXAMINER, Queue.CONTENT_AUTHOR, null));
        assertEquals(403, denied.getStatus());
    }

    @Test
    void staffTakeAnItemRecordTheResultAndShowANoteToTheLearner() {
        UUID user = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        UUID flag = UUID.randomUUID();
        jdbc.update("INSERT INTO learner_flags (id, user_id, kind, evidence) VALUES (?, ?, 'DUPLICATE_ESSAY', "
                + "'{}'::jsonb)", flag, user);
        sync.sync();
        var item = work.list(EXAMINER, null, null).getFirst();

        var taken = work.update(EXAMINER, staff, item.id(), new UpdateWork(WorkStatus.IN_PROGRESS, "called", null,
                null, null));
        assertEquals(staff, taken.assigneeId());
        assertEquals("called", taken.action());
        assertEquals(List.of(), work.notesFor(user));

        assertEquals(422, assertThrows(LearningRequestException.class, () -> work.update(EXAMINER, staff, item.id(),
                new UpdateWork(null, null, null, " ", true))).getStatus());
        var done = work.update(EXAMINER, staff, item.id(), new UpdateWork(WorkStatus.DONE, null, "warned", "Please write your own essay.", true));
        assertEquals(WorkStatus.DONE, done.status());
        assertEquals(List.of("Please write your own essay."), work.notesFor(user).stream().map(n -> n.note()).toList());
        assertEquals(0, sync.sync(), "finished work is not reopened by a later sync");

        // An Examiner cannot touch a content item.
        jdbc.update("INSERT INTO content_gap_signals (kp_id, occurrences, first_detected_at, last_detected_at) "
                + "VALUES (?, 1, now(), now())", UUID.randomUUID());
        sync.sync();
        var contentItem = work.list(CONTENT, null, null).getFirst();
        assertEquals(404, assertThrows(LearningRequestException.class, () -> work.update(EXAMINER, staff,
                contentItem.id(), new UpdateWork(WorkStatus.DONE, null, null, null, null))).getStatus());

        var reviewed = work.reviewFlag(staff, flag, FlagStatus.CONFIRMED, "same text as another learner");
        assertEquals(FlagStatus.CONFIRMED, reviewed.status());
        assertEquals(staff, reviewed.reviewedBy());
        assertEquals(422, assertThrows(LearningRequestException.class,
                () -> work.reviewFlag(staff, flag, FlagStatus.OPEN, null)).getStatus());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> work.reviewFlag(staff, UUID.randomUUID(), FlagStatus.DISMISSED, null)).getStatus());
    }

    @Test
    void questionsTooEasyOrTooHardBecomeContentItemsOnlyWithEnoughFirstAttempts() {
        UUID easy = UUID.randomUUID();
        UUID hard = UUID.randomUUID();
        UUID fewAttempts = UUID.randomUUID();
        List<QuestionAttemptEvent> batch = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            batch.add(event(UUID.randomUUID(), easy, true, null, true));
            batch.add(event(UUID.randomUUID(), hard, index < 3, null, true));
        }
        for (int index = 0; index < 5; index++) batch.add(event(UUID.randomUUID(), fewAttempts, true, null, true));
        batch.add(event(UUID.randomUUID(), easy, false, null, false));
        events.append(batch);

        sync.sync();
        var kinds = work.list(CONTENT, null, null).stream().collect(java.util.stream.Collectors.toMap(
                item -> item.subjectRef(), item -> item.kind()));
        assertEquals(WorkKind.QUESTION_TOO_EASY, kinds.get(easy));
        assertEquals(WorkKind.QUESTION_TOO_HARD, kinds.get(hard));
        assertFalse(kinds.containsKey(fewAttempts));
        assertEquals(2, insight.questionStats(20).questions().size());
    }

    @Test
    void anEdgeThatDoesNotExplainFailuresBecomesASuspectItemWithItsNumbers() {
        UUID prerequisite = UUID.randomUUID();
        UUID failing = UUID.randomUUID();
        for (int index = 0; index < 20; index++) {
            UUID user = UUID.randomUUID();
            UUID review = UUID.randomUUID();
            UUID session = UUID.randomUUID();
            jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status) "
                    + "VALUES (?, ?, ?, ?, 'DONE')", review, user, failing, UUID.randomUUID());
            jdbc.update("INSERT INTO diagnostic_sessions (id, user_id, root_review_id, root_kp_id, status, "
                    + "conclusion_kp_ids) VALUES (?, ?, ?, ?, 'CONCLUDED', ARRAY[?]::uuid[])", session, user, review,
                    failing, failing);
            jdbc.update("INSERT INTO diagnostic_probes (id, session_id, kp_id, parent_kp_id, depth, package_id, "
                    + "package_version_id, status, correct, total, passed, request_id) VALUES (?, ?, ?, ?, 1, ?, ?, "
                    + "'ANSWERED', 3, 3, TRUE, ?)", UUID.randomUUID(), session, prerequisite, failing,
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        }

        sync.sync();
        var suspect = work.list(CONTENT, null, null).stream().filter(item -> item.kind() == WorkKind.EDGE_SUSPECT)
                .toList();
        assertEquals(1, suspect.size());
        assertEquals(WorkItemSync.edgeRef(prerequisite, failing), suspect.getFirst().subjectRef());
        assertTrue(suspect.getFirst().detail().replace(" ", "").contains("\"probes\":20"));

        var quality = insight.treeQuality(null);
        assertEquals(1, quality.edges().size());
        assertEquals(20, quality.edges().getFirst().probes());
        assertEquals(1.0, quality.edges().getFirst().passRate(), 1e-9);
        assertEquals(0.0, quality.edges().getFirst().rootRate(), 1e-9);
    }

    @Test
    void theScheduledRoundAddsNewItemsOnceAndYieldsToAnotherInstanceHoldingTheLock() throws Exception {
        jdbc.update("INSERT INTO content_gap_signals (kp_id, occurrences, first_detected_at, last_detected_at) "
                + "VALUES (?, 1, now(), now())", UUID.randomUUID());

        assertEquals(1, job.runOnce());
        assertEquals(0, job.runOnce(), "nothing new the second time");
        assertEquals(1, work.list(CONTENT, null, null).size());

        jdbc.update("INSERT INTO content_gap_signals (kp_id, occurrences, first_detected_at, last_detected_at) "
                + "VALUES (?, 1, now(), now())", UUID.randomUUID());
        try (var other = dataSource.getConnection()) {
            other.setAutoCommit(false);
            try (var statement = other.createStatement()) {
                statement.execute("SELECT pg_advisory_xact_lock(hashtext('learning:work-item-sync'))");
                assertEquals(-1, job.runOnce(), "another instance is running a round");
            }
            other.rollback();
        }
        assertEquals(1, job.runOnce(), "the skipped round is made up for by the next one");
    }

    @Test
    void theOverviewOfALearnerWithNothingYetIsEmptyNotAnError() {
        var overview = insight.overview(UUID.randomUUID());
        assertTrue(overview.courses().isEmpty());
        assertTrue(overview.flags().isEmpty());
        assertEquals(0, overview.knowledgePointsByTier().get("PRIORITY"));
    }
}
