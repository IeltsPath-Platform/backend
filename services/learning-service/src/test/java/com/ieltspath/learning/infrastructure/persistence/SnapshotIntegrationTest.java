package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.SnapshotReportResult;
import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.application.service.SnapshotService;
import com.ieltspath.learning.application.usecase.GetSnapshotReportsUseCase;
import com.ieltspath.learning.application.usecase.GetSuggestionsUseCase;
import com.ieltspath.learning.domain.repository.SnapshotRepository;
import com.ieltspath.learning.domain.repository.SnapshotRepository.Period;
import com.ieltspath.learning.domain.repository.SnapshotRepository.Snapshot;
import com.ieltspath.learning.domain.service.SkillRollup;
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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** The snapshot run, its retention, and the weekly, history and monthly reports built from the stored rows. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "learning.snapshots.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class SnapshotIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired SnapshotService service;
    @Autowired SnapshotRepository snapshots;
    @Autowired GetSnapshotReportsUseCase reports;
    @MockitoBean KnowledgePointDirectory directory;
    @MockitoBean GetSuggestionsUseCase suggestions;

    private final UUID readingA = UUID.randomUUID();
    private final UUID readingB = UUID.randomUUID();

    @BeforeEach
    void cleanAndDescribe() {
        jdbc.execute("TRUNCATE question_attempt_events, kp_mastery_profiles, learner_placements, "
                + "learner_weekly_snapshots, learner_monthly_snapshots");
        Map<UUID, KnowledgePointDirectory.Entry> described = new LinkedHashMap<>();
        described.put(readingA, new KnowledgePointDirectory.Entry("R1", "Reading 1", "READING", new BigDecimal("6.0")));
        described.put(readingB, new KnowledgePointDirectory.Entry("R2", "Reading 2", "READING", new BigDecimal("6.0")));
        when(directory.describe()).thenReturn(described);
    }

    @Test
    void aRunWritesActiveLearnersOnlyAndCapsEachAnswersMinutes() {
        UUID active = UUID.randomUUID();
        UUID idle = UUID.randomUUID();
        LocalDate week = service.lastWeekStart();
        Instant midWeek = week.plusDays(3).atTime(12, 0).atZone(ZONE).toInstant();
        event(active, true, 60_000, midWeek);
        event(active, true, 30_000, midWeek.plusSeconds(60));
        event(active, false, 7_200_000, midWeek.plusSeconds(120));
        event(idle, true, 1_000, Instant.now().minusSeconds(40L * 24 * 3600));
        profile(active, readingA, "MAINTAIN", 0.9);
        profile(active, readingB, "MAINTAIN", 0.85);

        int written = service.snapshotWeek(week);

        assertEquals(1, written, "the learner idle for a month gets no snapshot");
        Snapshot snapshot = snapshots.find(Period.WEEK, active, week).orElseThrow();
        assertEquals(3, snapshot.answered());
        assertEquals(2, snapshot.correct());
        assertEquals(12, snapshot.activeMinutes(), "1 + 0.5 + the 10 minute cap, not two hours");
        assertEquals(0, new BigDecimal("6.0").compareTo(snapshot.estimatedBand()));
        assertTrue(snapshots.find(Period.WEEK, idle, week).isEmpty());
    }

    @Test
    void runningTheSameWeekTwiceDoesNotDuplicateRows() {
        UUID user = UUID.randomUUID();
        LocalDate week = service.lastWeekStart();
        event(user, true, 1_000, week.plusDays(2).atTime(9, 0).atZone(ZONE).toInstant());
        profile(user, readingA, "CONSOLIDATE", 0.7);

        service.snapshotWeek(week);
        service.snapshotWeek(week);
        service.snapshotMonth(service.lastMonthStart());
        service.snapshotMonth(service.lastMonthStart());

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM learner_weekly_snapshots WHERE user_id = ?",
                Integer.class, user));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM learner_monthly_snapshots WHERE user_id = ?",
                Integer.class, user));
    }

    @Test
    void purgeDropsOnlyWhatIsPastRetention() {
        UUID user = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZONE);
        snapshots.save(Period.WEEK, List.of(blank(user, today.minusWeeks(20)), blank(user, today.minusWeeks(2))));
        snapshots.save(Period.MONTH, List.of(blank(user, today.minusMonths(40)), blank(user, today.minusMonths(2))));

        int purged = service.purge();

        assertEquals(2, purged);
        assertEquals(1, snapshots.range(Period.WEEK, user, today.minusWeeks(30), today).size());
        assertEquals(1, snapshots.range(Period.MONTH, user, today.minusMonths(50), today).size());
    }

    @Test
    void theWeeklyReportShowsTheChangeFromTheWeekBefore() {
        UUID user = UUID.randomUUID();
        LocalDate week = service.lastWeekStart();
        snapshots.save(Period.WEEK, List.of(withReading(user, week.minusWeeks(1), 0.50),
                withReading(user, week, 0.70)));

        SnapshotReportResult.WeeklyReport report = reports.weekly(user, null);

        assertEquals(week, report.weekStart());
        SnapshotReportResult.SkillChange reading = report.changes().stream()
                .filter(change -> change.skill().equals("READING")).findFirst().orElseThrow();
        assertEquals("UP", reading.direction());
        assertEquals(0.50, reading.previousMastery(), 1e-9);
        assertEquals("NONE", report.changes().stream()
                .filter(change -> change.skill().equals("WRITING")).findFirst().orElseThrow().direction());
    }

    @Test
    void aLearnerWithoutASnapshotGetsAnEmptyWeekNotAnError() {
        SnapshotReportResult.WeeklyReport report = reports.weekly(UUID.randomUUID(), null);

        assertNull(report.snapshot());
        assertTrue(report.changes().isEmpty());
    }

    @Test
    void theRequestedDatesAndRangesAreValidated() {
        UUID user = UUID.randomUUID();

        assertEquals("INVALID_WEEK_START", assertThrows(LearningRequestException.class,
                () -> reports.weekly(user, LocalDate.of(2026, 10, 14))).getCode());
        assertEquals("INVALID_MONTH_START", assertThrows(LearningRequestException.class,
                () -> reports.monthly(user, LocalDate.of(2026, 10, 14))).getCode());
        assertEquals("INVALID_WEEKS", assertThrows(LearningRequestException.class,
                () -> reports.history(user, 3)).getCode());
        assertEquals("INVALID_WEEKS", assertThrows(LearningRequestException.class,
                () -> reports.history(user, 13)).getCode());
    }

    @Test
    void theHistoryListsTheWeeksOldestFirst() {
        UUID user = UUID.randomUUID();
        LocalDate last = service.lastWeekStart();
        snapshots.save(Period.WEEK, List.of(withReading(user, last.minusWeeks(2), 0.4),
                withReading(user, last, 0.6), withReading(user, last.minusWeeks(1), 0.5)));

        SnapshotReportResult.History history = reports.history(user, 4);

        assertEquals(List.of(last.minusWeeks(2), last.minusWeeks(1), last),
                history.weeks().stream().map(SnapshotReportResult.HistoryPoint::weekStart).toList());
        assertEquals(0.6, history.weeks().get(2).mastery().get("READING"), 1e-9);
    }

    private void event(UUID user, boolean correct, long durationMs, Instant when) {
        jdbc.update("INSERT INTO question_attempt_events (id, user_id, question_version_id, correct, duration_ms, "
                + "source, source_reference_id, first_submission, occurred_at) VALUES (?, ?, ?, ?, ?, "
                + "'lesson_exercise', ?, true, ?)", UUID.randomUUID(), user, UUID.randomUUID(), correct, durationMs,
                UUID.randomUUID(), Timestamp.from(when));
    }

    private void profile(UUID user, UUID kp, String tier, double mastery) {
        jdbc.update("INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count, "
                + "last_evidence_at) VALUES (?, ?, ?, ?, 5, now())", user, kp, mastery, tier);
    }

    private static Snapshot blank(UUID user, LocalDate start) {
        return new Snapshot(user, start, SkillRollup.summarize(List.of()), Map.of("PRIORITY", 0), 0, 0, 0, List.of(),
                null);
    }

    private static Snapshot withReading(UUID user, LocalDate start, double mastery) {
        List<SkillRollup.Summary> skills = SkillRollup.summarize(List.of(new SkillRollup.Item("READING",
                com.ieltspath.learning.domain.vo.MasteryTier.CONSOLIDATE, mastery, 5)));
        return new Snapshot(user, start, skills, Map.of("CONSOLIDATE", 1), 10, 7, 30, List.of(), null);
    }
}
