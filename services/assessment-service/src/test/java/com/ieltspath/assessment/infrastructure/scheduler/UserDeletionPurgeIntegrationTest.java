package com.ieltspath.assessment.infrastructure.scheduler;

import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcDeletedUserStore;
import com.ieltspath.commonstorage.ObjectStorage;
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

/**
 * Runs the account-deletion purge on real PostgreSQL with every Assessment migration. The deleted user is both a
 * learner with work in every table and an examiner on another learner's work.
 */
@Testcontainers(disabledWithoutDocker = true)
class UserDeletionPurgeIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    /** Columns that name a person. A new table with one of them must be handled by the purge or kept on purpose. */
    static final Set<String> USER_COLUMNS = Set.of("user_id", "learner_id", "grader_user_id", "assigned_examiner_id",
            "requested_examiner_id", "examiner_id", "reviewer_id", "created_by");
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
    void everyTableWithAPersonColumnIsHandled() {
        Set<String> handled = new HashSet<>(JdbcDeletedUserStore.HANDLED_TABLES);
        handled.addAll(KEPT_TABLES);
        Set<String> missing = new TreeSet<>(seeder.userColumns(USER_COLUMNS).keySet());
        missing.removeAll(handled);
        assertThat(missing).as("tables naming a person that account deletion does not handle").isEmpty();
    }

    @Test
    void purgeRemovesTheLearnersWorkAndReleasesTheirExaminerWork() {
        UUID deleted = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID qaReviewer = UUID.randomUUID();
        UUID deletedSubmission = seedLearner(deleted, qaReviewer);
        UUID otherSubmission = seedLearner(other, qaReviewer);

        // The deleted user also grades the other learner: one review in progress, one finished, one QA row.
        UUID openJob = seeder.insert("grading_jobs", Map.of("user_id", other, "submission_id", otherSubmission,
                "grading_mode", "HUMAN", "status", "PROCESSING", "assigned_examiner_id", deleted,
                "claimed_at", "2026-11-01T08:00:00Z"));
        UUID openReview = seeder.insert("human_reviews", Map.of("grading_job_id", openJob, "grader_user_id", deleted,
                "status", "IN_REVIEW", "draft_payload", "{}", "draft_saved_at", "2026-11-01T08:00:00Z"));
        UUID doneJob = seeder.insert("grading_jobs", Map.of("user_id", other, "submission_id", otherSubmission,
                "grading_mode", "HUMAN", "status", "COMPLETED", "assigned_examiner_id", deleted));
        UUID doneReview = seeder.insert("human_reviews", Map.of("grading_job_id", doneJob, "grader_user_id", deleted,
                "status", "COMPLETED"));
        jdbc.update("UPDATE examiner_qa_reviews SET examiner_id = ? WHERE submission_id = ?", deleted, otherSubmission);

        Map<String, List<String>> personTables = seeder.userColumns(USER_COLUMNS);
        KEPT_TABLES.forEach(personTables::remove);
        Map<String, Integer> othersBefore = seeder.rowsMentioning(personTables, other);

        var store = new JdbcDeletedUserStore(jdbc);
        UUID eventId = UUID.randomUUID();
        store.recordDeletion(eventId, deleted, Instant.now());
        ObjectStorage storage = mock(ObjectStorage.class);
        var worker = new UserDeletionJobWorker(store,
                new StaticListableBeanFactory(Map.of("storage", storage)).getBeanProvider(ObjectStorage.class),
                new SpeakingRecordingProperties(), new DataSourceTransactionManager(dataSource), 300, 10);

        worker.processPending();

        assertThat(jdbc.queryForObject("SELECT status FROM user_deletion_jobs WHERE event_id = ?", String.class, eventId))
                .isEqualTo("DONE");
        assertThat(seeder.rowsMentioning(personTables, deleted)).as("rows that still name the deleted user").isEmpty();
        assertThat(seeder.rowsMentioning(personTables, other)).as("the other learner's rows").isEqualTo(othersBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM learner_submissions WHERE id = ?", Integer.class, deletedSubmission))
                .isZero();
        verify(storage).delete("speaking-recordings", "speaking/" + deleted + "/answer.webm");

        // The unfinished review is back in the queue; the finished one keeps its grade without a grader.
        assertThat(jdbc.queryForMap("SELECT status, grader_user_id, draft_payload FROM human_reviews WHERE id = ?", openReview))
                .containsEntry("status", "QUEUED").containsEntry("grader_user_id", null).containsEntry("draft_payload", null);
        assertThat(jdbc.queryForMap("SELECT assigned_examiner_id, claimed_at FROM grading_jobs WHERE id = ?", openJob))
                .containsEntry("assigned_examiner_id", null).containsEntry("claimed_at", null);
        assertThat(jdbc.queryForMap("SELECT status, grader_user_id FROM human_reviews WHERE id = ?", doneReview))
                .containsEntry("status", "COMPLETED").containsEntry("grader_user_id", null);
        assertThat(jdbc.queryForObject("SELECT examiner_id FROM examiner_qa_reviews WHERE submission_id = ?", UUID.class, otherSubmission))
                .isEqualTo(JdbcDeletedUserStore.DELETED_USER);
    }

    /** A learner with a test attempt, a Speaking answer in it, its grading, comparison, gold-set copy and QA row. */
    private static UUID seedLearner(UUID learner, UUID qaReviewer) {
        seeder.userColumns(Set.of("user_id", "learner_id")).forEach((table, cols) -> {
            if (KEPT_TABLES.contains(table)) return;
            try {
                seeder.insert(table, Map.of(cols.get(0), learner));
            } catch (RuntimeException rejected) {
                // Tables with stricter rows are seeded below.
            }
        });
        UUID attempt = seeder.insert("assessment_attempts", Map.of("user_id", learner));
        UUID section = seeder.insert("attempt_sections", Map.of("attempt_id", attempt));
        UUID item = seeder.insert("attempt_items", Map.of("attempt_section_id", section));
        seeder.insert("attempt_responses", Map.of("attempt_item_id", item));
        UUID result = seeder.insert("assessment_results", Map.of("attempt_id", attempt));
        UUID itemResult = seeder.insert("item_results", Map.of("result_id", result, "attempt_item_id", item));
        seeder.insert("error_analysis_items", Map.of("item_result_id", itemResult));
        seeder.insert("skill_scores", Map.of("result_id", result));

        UUID submission = seeder.insert("learner_submissions", Map.of("user_id", learner, "attempt_item_id", item,
                "skill", "SPEAKING", "audio_reference", "speaking/" + learner + "/answer.webm"));
        UUID job = seeder.insert("grading_jobs", Map.of("user_id", learner, "submission_id", submission,
                "grading_mode", "HUMAN", "status", "COMPLETED"));
        seeder.insert("human_reviews", Map.of("grading_job_id", job, "status", "COMPLETED"));
        seeder.insert("essay_grade_comparisons", Map.of("submission_id", submission));
        UUID gold = seeder.insert("ai_gold_essays", Map.of("comparison_submission_id", submission, "source", "COMPARISON",
                "examiner_band", "6.5", "created_by", qaReviewer));
        UUID run = seeder.insert("ai_eval_runs", Map.of());
        seeder.insert("ai_eval_items", Map.of("run_id", run, "gold_id", gold));
        seeder.insert("examiner_qa_reviews", Map.of("submission_id", submission, "examiner_id", UUID.randomUUID(),
                "reviewer_id", qaReviewer, "examiner_band", "6.5", "reviewer_band", "6.5"));
        return submission;
    }
}
