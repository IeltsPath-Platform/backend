package com.ieltspath.assessment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.PlacementWritingGradingStore;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcPlacementWritingGradingStore;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.events.assessment.PlacementWritingGradingCompletedV1;
import com.ieltspath.events.assessment.PlacementWritingGradingRequestedV1;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** Placement essays handed to Learning: the grading jobs move with the results, and a result counts only once. */
@Testcontainers(disabledWithoutDocker = true)
class PlacementWritingGradingStoreIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static PlacementWritingGradingStore store;
    static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @BeforeAll static void setup() {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(source).load().migrate();
        jdbc = new JdbcTemplate(source);
        store = new JdbcPlacementWritingGradingStore(jdbc, JSON, mock(OutboxTraceContext.class));
    }

    @Test void handsTheQueuedEssayToLearningAndCompletesItWithLearningsGrade() {
        UUID user = UUID.randomUUID();
        UUID attempt = placementAttempt(user);
        UUID item = essay(user, attempt);

        var queued = store.queued(attempt, user);
        assertEquals(1, queued.size());
        assertEquals("Cities grow.", queued.getFirst().text());

        Instant now = Instant.now();
        store.delegate(request(attempt, user, item), now);
        assertEquals("PROCESSING", status(item));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type = ? AND aggregate_id = ?", Integer.class,
                PlacementWritingGradingRequestedV1.EVENT_TYPE, attempt.toString()));

        store.record(UUID.randomUUID(), result(attempt, user, item, "GRADED", new BigDecimal("6.5")), now);

        assertEquals("COMPLETED", status(item));
        assertEquals(0, new BigDecimal("6.5").compareTo(jdbc.queryForObject(
                "SELECT llm_band FROM grading_jobs WHERE idempotency_key = ?", BigDecimal.class, key(item))));
        assertTrue(jdbc.queryForObject("SELECT llm_feedback::text FROM grading_jobs WHERE idempotency_key = ?",
                String.class, key(item)).contains("Clear position."));
    }

    @Test void aRedeliveredFailureDoesNotTakeTheEssayBackFromTheLocalGrader() {
        UUID user = UUID.randomUUID();
        UUID attempt = placementAttempt(user);
        UUID item = essay(user, attempt);
        Instant now = Instant.now();
        store.delegate(request(attempt, user, item), now);
        UUID eventId = UUID.randomUUID();
        var failed = result(attempt, user, item, "FAILED", null);

        store.record(eventId, failed, now);
        assertEquals("QUEUED", status(item));
        // The local grader claims it again, then the same result arrives once more.
        jdbc.update("UPDATE grading_jobs SET status = 'PROCESSING', processing_started_at = now() WHERE idempotency_key = ?",
                key(item));
        store.record(eventId, failed, now);

        assertEquals("PROCESSING", status(item));
    }

    private static String key(UUID item) { return "gate-essay:" + item + ":ai"; }

    private static String status(UUID item) {
        return jdbc.queryForObject("SELECT status FROM grading_jobs WHERE idempotency_key = ?", String.class, key(item));
    }

    private static PlacementWritingGradingRequestedV1 request(UUID attempt, UUID user, UUID item) {
        return new PlacementWritingGradingRequestedV1(UUID.randomUUID(), PlacementWritingGradingRequestedV1.EVENT_TYPE,
                Instant.now(), PlacementWritingGradingRequestedV1.SOURCE, new PlacementWritingGradingRequestedV1.Data(
                attempt, user, List.of(new PlacementWritingGradingRequestedV1.Item(item, UUID.randomUUID(), "Discuss.",
                "TASK_2", 250, new BigDecimal("6.0"), null, List.of(), "Cities grow."))));
    }

    private static PlacementWritingGradingCompletedV1.Data result(UUID attempt, UUID user, UUID item, String status,
                                                                  BigDecimal band) {
        var criteria = band == null ? List.<PlacementWritingGradingCompletedV1.Criterion>of()
                : List.of(new PlacementWritingGradingCompletedV1.Criterion("TR", band, "Clear position."));
        return new PlacementWritingGradingCompletedV1.Data(attempt, user, List.of(new PlacementWritingGradingCompletedV1.Item(
                item, status, band, band == null ? null : "Fair.", criteria, List.of(), band == null ? null : "WT2@1",
                band == null ? "INVALID_GRADE" : null)));
    }

    private static UUID placementAttempt(UUID user) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status) "
                + "VALUES(?,?,?,'PLACEMENT','STANDARD','WEB','SUBMITTED')", id, user, UUID.randomUUID());
        return id;
    }

    /** A Writing item with a sent essay and its queued AI job, as submitting the test leaves them. */
    private static UUID essay(UUID user, UUID attempt) {
        UUID section = UUID.randomUUID();
        jdbc.update("INSERT INTO attempt_sections(id,attempt_id,content_section_id,sort_order,section_snapshot) "
                + "VALUES(?,?,?,0,CAST(? AS JSONB))", section, attempt, UUID.randomUUID(), "{\"skill\":\"WRITING\"}");
        UUID item = UUID.randomUUID();
        jdbc.update("INSERT INTO attempt_items(id,attempt_section_id,question_version_id,sort_order,question_snapshot,"
                        + "answer_snapshot) VALUES(?,?,?,0,CAST(? AS JSONB),CAST(? AS JSONB))", item, section,
                UUID.randomUUID(), "{\"stem\":\"Discuss.\"}", "{\"answerSpec\":{\"task\":\"TASK_2\",\"minWords\":250}}");
        UUID submission = UUID.randomUUID();
        jdbc.update("INSERT INTO learner_submissions(id,user_id,attempt_item_id,prompt_snapshot,skill,text_payload,"
                        + "status,submission_key) VALUES(?,?,?,CAST('{}' AS JSONB),'WRITING','Cities grow.','SUBMITTED',?)",
                submission, user, item, UUID.randomUUID().toString());
        jdbc.update("INSERT INTO grading_jobs(id,submission_id,user_id,skill,grading_mode,status,point_cost_snapshot,"
                + "idempotency_key) VALUES(gen_random_uuid(),?,?,'WRITING','AI','QUEUED',0,?)", submission, user, key(item));
        return item;
    }
}
