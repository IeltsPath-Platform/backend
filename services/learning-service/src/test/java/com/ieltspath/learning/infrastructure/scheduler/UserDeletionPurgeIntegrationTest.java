package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.infrastructure.persistence.JdbcDeletedUserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the account-deletion purge on real PostgreSQL with every Learning migration: the learner's rows go from every
 * table, including the ones that point at each other, and another learner's rows stay.
 */
@Testcontainers(disabledWithoutDocker = true)
class UserDeletionPurgeIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    /** Columns that name a learner. A new table with one of them must be purged or kept on purpose. */
    static final Set<String> USER_COLUMNS = Set.of("user_id", "subject_user_id");
    /** Rows kept after deletion on purpose: the tombstone and the job that records the deletion. */
    static final Set<String> KEPT_TABLES = Set.of("deleted_users", "user_deletion_jobs");

    private static JdbcTemplate jdbc;
    private static DriverManagerDataSource dataSource;
    private static RowSeeder seeder;

    @BeforeAll
    static void migrate() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        seeder = new RowSeeder(jdbc);
    }

    @Test
    void everyTableWithALearnerColumnIsPurgedOrKeptOnPurpose() {
        Set<String> handled = new HashSet<>(UserDeletionJobWorker.USER_TABLES);
        handled.addAll(UserDeletionJobWorker.LINKED_TABLES);
        handled.add("work_items");
        handled.addAll(KEPT_TABLES);

        Set<String> missing = new TreeSet<>(seeder.userColumns(USER_COLUMNS).keySet());
        missing.removeAll(handled);

        assertThat(missing).as("tables with learner data that account deletion neither removes nor keeps").isEmpty();
    }

    @Test
    void purgeRemovesEveryRowOfTheLearnerAndLeavesOthersAlone() {
        UUID learner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        seedLearner(learner);
        seedLearner(other);
        Map<String, List<String>> userTables = seeder.userColumns(USER_COLUMNS);
        KEPT_TABLES.forEach(userTables::remove);
        Map<String, Integer> othersBefore = seeder.rowsMentioning(userTables, other);
        assertThat(seeder.rowsMentioning(userTables, learner)).containsKeys(
                "review_items", "practice_attempts", "human_review_requests", "lesson_writing_submissions",
                "speaking_submissions", "learner_xp", "llm_daily_usage", "work_items", "course_outcomes");

        var repository = new JdbcDeletedUserRepository(jdbc);
        UUID eventId = UUID.randomUUID();
        repository.recordDeletion(eventId, learner, Instant.now());
        ObjectStorage storage = mock(ObjectStorage.class);
        SpeakingPracticeSettings settings = mock(SpeakingPracticeSettings.class);
        when(settings.bucket()).thenReturn("speaking-recordings");
        var worker = new UserDeletionJobWorker(repository,
                new StaticListableBeanFactory(Map.of("storage", storage)).getBeanProvider(ObjectStorage.class),
                settings, new DataSourceTransactionManager(dataSource), 300, 10);

        worker.processPending();

        assertThat(seeder.rowsMentioning(userTables, learner)).as("rows left for the deleted learner").isEmpty();
        assertThat(seeder.rowsMentioning(userTables, other)).as("another learner's rows").isEqualTo(othersBefore);
        assertThat(jdbc.queryForObject("SELECT status FROM user_deletion_jobs WHERE event_id = ?", String.class, eventId))
                .isEqualTo("DONE");
        verify(storage).delete("speaking-recordings", "practice/" + learner + "/answer.wav");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM speaking_recordings r JOIN speaking_submissions s ON s.id = r.submission_id",
                Integer.class)).isEqualTo(1);
    }

    /** One row in every learner table, plus the chains the purge must undo in the right order. */
    private static void seedLearner(UUID learner) {
        // A row in every table that names the learner, where a generic row is accepted.
        seeder.userColumns(USER_COLUMNS).forEach((table, cols) -> {
            if (KEPT_TABLES.contains(table)) return;
            try {
                seeder.insert(table, Map.of(cols.get(0), learner));
            } catch (RuntimeException rejected) {
                // Tables with stricter rows are seeded below.
            }
        });

        UUID attempt = seeder.insert("practice_attempts", Map.of("user_id", learner, "origin", "LESSON", "lesson_id", UUID.randomUUID()));
        UUID review = seeder.insert("review_items", Map.of("user_id", learner, "source_attempt_id", attempt));
        seeder.insert("review_sets", Map.of("user_id", learner, "review_item_id", review));
        seeder.insert("review_theory_checks", Map.of("user_id", learner, "review_item_id", review));
        UUID session = seeder.insert("diagnostic_sessions", Map.of("user_id", learner, "root_review_id", review));
        seeder.insert("diagnostic_probes", Map.of("session_id", session));

        UUID firstEssay = seeder.insert("lesson_writing_submissions", Map.of("user_id", learner, "source", "FREE_WRITING"));
        UUID revision = seeder.insert("lesson_writing_submissions", Map.of("user_id", learner, "source", "FREE_WRITING",
                "previous_submission_id", firstEssay, "revision_no", 2));
        seeder.insert("human_review_requests", Map.of("user_id", learner, "submission_id", firstEssay));
        seeder.insert("human_review_outcomes", Map.of("user_id", learner, "submission_id", revision));

        // Only one open submission is allowed, and the generic pass above already made it.
        UUID speaking = jdbc.queryForObject("SELECT id FROM speaking_submissions WHERE user_id = ?", UUID.class, learner);
        seeder.insert("speaking_recordings", Map.of("submission_id", speaking,
                "object_key", "practice/" + learner + "/answer.wav", "uploaded_at", "2026-11-01T08:00:00Z",
                "content_type", "audio/wav", "size_bytes", 100));

        seeder.insert("work_items", Map.of("subject_user_id", learner));
    }
}
