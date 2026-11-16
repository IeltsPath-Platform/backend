package com.ieltspath.assessment.infrastructure.persistence;

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
 * Runs Flyway with the demo callback the way the service does at startup. With demo data on, the demo learner has a
 * completed examiner review; a second run changes nothing; with demo data off nothing is written.
 */
@Testcontainers(disabledWithoutDocker = true)
class AssessmentDemoDataCallbackIntegrationTest {
    private static final String LEARNER = "00000000-0000-0000-0000-000000000003";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static String url(String database) {
        return POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + database + "$1");
    }

    private static void migrate(String database, boolean demoData) {
        new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()))
                .execute("CREATE DATABASE " + database);
        run(database, demoData);
    }

    private static void run(String database, boolean demoData) {
        Flyway.configure().dataSource(url(database), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/callback")
                .placeholders(Map.of("demoData", String.valueOf(demoData), "demoUserId", LEARNER)).load().migrate();
    }

    private static JdbcTemplate jdbc(String database) {
        return new JdbcTemplate(new DriverManagerDataSource(url(database), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    private static int count(JdbcTemplate jdbc, String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @BeforeAll
    static void migrateBoth() {
        migrate("demo_on", true);
        migrate("demo_off", false);
    }

    @Test
    void theDemoLearnerHasACompletedExaminerReview() {
        JdbcTemplate db = jdbc("demo_on");
        assertEquals(1, count(db, "SELECT count(*) FROM human_review_submissions WHERE completed_at IS NOT NULL AND learner_id = '" + LEARNER + "'"));
        assertEquals(3, count(db, "SELECT jsonb_array_length(annotations) FROM human_review_submissions"));
        assertEquals(1, count(db, "SELECT count(*) FROM grading_jobs WHERE grading_mode = 'HUMAN' AND status = 'COMPLETED'"));
        assertEquals(1, count(db, "SELECT count(*) FROM human_reviews WHERE status = 'COMPLETED'"));
        assertTrue(count(db, "SELECT count(*) FROM human_review_submissions h WHERE substring(h.essay_text from 1 + (h.annotations->0->>'start')::int for "
                + "(h.annotations->0->>'end')::int - (h.annotations->0->>'start')::int) = h.annotations->0->>'excerpt'") == 1,
                "the first annotation marks exactly its excerpt");
    }

    @Test
    void runningTheCallbackAgainChangesNothing() {
        JdbcTemplate db = jdbc("demo_on");
        int jobs = count(db, "SELECT count(*) FROM grading_jobs");
        run("demo_on", true);
        assertEquals(jobs, count(db, "SELECT count(*) FROM grading_jobs"));
    }

    @Test
    void withDemoDataOffNothingIsWritten() {
        JdbcTemplate db = jdbc("demo_off");
        assertEquals(0, count(db, "SELECT count(*) FROM human_review_submissions"));
        assertEquals(0, count(db, "SELECT count(*) FROM grading_jobs"));
    }
}
