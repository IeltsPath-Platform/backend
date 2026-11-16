package com.ieltspath.learning.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs Flyway with the demo callback the way the service does at startup. With demo data on, the demo learner gets the
 * IELTS 5.0 story (progress, every review case, mastery in all tiers, gamification); a second run changes nothing; with
 * demo data off nothing is written.
 */
@Testcontainers(disabledWithoutDocker = true)
class DemoDataCallbackIntegrationTest {
    private static final String LEARNER = "00000000-0000-0000-0000-000000000003";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static void migrate(String database, boolean demoData, String learner) {
        new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()))
                .execute("CREATE DATABASE " + database);
        String url = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + database + "$1");
        Flyway.configure().dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/callback")
                .placeholders(Map.of("demoData", String.valueOf(demoData), "demoUserId", learner)).load().migrate();
    }

    private static JdbcTemplate jdbc(String database) {
        String url = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + database + "$1");
        return new JdbcTemplate(new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    private static int count(JdbcTemplate jdbc, String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @BeforeAll
    static void migrateBoth() {
        migrate("demo_on", true, LEARNER);
        migrate("demo_off", false, LEARNER);
    }

    @Test
    void theDemoLearnerGetsTheCourseStoryAndEveryReviewCase() {
        JdbcTemplate db = jdbc("demo_on");
        assertEquals(11, count(db, "SELECT count(*) FROM lesson_progress WHERE completed_at IS NOT NULL"));
        assertEquals(12, count(db, "SELECT count(*) FROM lesson_progress"));
        assertEquals(4, count(db, "SELECT count(*) FROM topic_progress WHERE passed_at IS NOT NULL"));
        assertEquals(2, count(db, "SELECT count(*) FROM topic_progress WHERE tested_at IS NOT NULL AND passed_at IS NULL"));

        assertEquals(9, count(db, "SELECT count(*) FROM review_items"));
        assertEquals(2, count(db, "SELECT count(*) FROM review_items WHERE status = 'PENDING' AND blocking"));
        assertEquals(1, count(db, "SELECT count(*) FROM review_items WHERE blocking AND stage = 'THEORY'"));
        assertEquals(5, count(db, "SELECT count(*) FROM review_items WHERE status = 'PENDING' AND NOT blocking"));
        assertEquals(1, count(db, "SELECT count(*) FROM review_items WHERE status = 'DONE'"));
        assertEquals(1, count(db, "SELECT count(*) FROM review_items WHERE status = 'SKIPPED'"));
        assertEquals(1, count(db, "SELECT count(*) FROM review_items WHERE trigger_kind = 'WRITING_CRITERION'"));

        for (String tier : new String[] {"PRIORITY", "CONSOLIDATE", "MAINTAIN"}) {
            assertTrue(count(db, "SELECT count(*) FROM kp_mastery_profiles WHERE tier = '" + tier + "'") > 0, tier);
        }
        assertTrue(count(db, "SELECT count(*) FROM kp_mastery_profiles WHERE next_review_at <= now()") > 0);
        assertEquals(1, count(db, "SELECT count(*) FROM learner_streaks WHERE current_days = 11"));
        assertEquals(3, count(db, "SELECT count(*) FROM daily_quests"));
        assertEquals(10, count(db, "SELECT count(*) FROM league_members"));
        assertEquals(4, count(db, "SELECT count(*) FROM lesson_writing_submissions WHERE status = 'GRADED'"));
        assertEquals(1, count(db, "SELECT count(*) FROM human_review_outcomes WHERE passed"));
        assertEquals(3, count(db, "SELECT count(*) FROM practice_attempts WHERE origin = 'FREE'"));
    }


    @Test
    void theSeededEssaysAreReadableByTheWritingRepository() {
        JdbcWritingSubmissionRepository repository = new JdbcWritingSubmissionRepository(jdbc("demo_on"),
                new com.fasterxml.jackson.databind.ObjectMapper());
        java.util.UUID learner = java.util.UUID.fromString(LEARNER);

        var task2 = repository.findFirstGradedEssays(learner, "TASK_2", 10);
        var task1 = repository.findFirstGradedEssays(learner, "TASK_1", 10);

        assertEquals(2, task2.size(), "the free essay's first try and the practice essay; the revision is revision 2");
        assertEquals(4, task2.get(0).criteria().size());
        assertEquals(4, task2.get(0).criteria().size());
        assertEquals(1, task1.size());
        assertEquals(4, task1.get(0).criteria().size());
        assertEquals(1, count(jdbc("demo_on"), "SELECT count(*) FROM review_items WHERE source_submission_id IS NOT NULL"));
    }

    @Test
    void runningTheCallbackAgainChangesNothing() {
        JdbcTemplate db = jdbc("demo_on");
        int evidence = count(db, "SELECT count(*) FROM kp_evidence");
        int xp = count(db, "SELECT total_xp FROM learner_xp");
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/demo_on$1"),
                        POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/callback")
                .placeholders(Map.of("demoData", "true", "demoUserId", LEARNER)).load().migrate();
        assertEquals(evidence, count(db, "SELECT count(*) FROM kp_evidence"));
        assertEquals(xp, count(db, "SELECT total_xp FROM learner_xp"));
    }

    @Test
    void withDemoDataOffNothingIsWritten() {
        JdbcTemplate db = jdbc("demo_off");
        assertEquals(0, count(db, "SELECT count(*) FROM lesson_progress"));
        assertEquals(0, count(db, "SELECT count(*) FROM review_items"));
        assertEquals(0, count(db, "SELECT count(*) FROM learner_xp"));
    }
}
