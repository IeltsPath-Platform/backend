package com.group01.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.assessment.application.command.SaveAttemptResponseCommand;
import com.group01.assessment.application.command.StartAssessmentAttemptCommand;
import com.group01.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.group01.assessment.application.port.ContentPackageProvider;
import com.group01.assessment.application.port.ContentPackageProvider.Item;
import com.group01.assessment.application.port.ContentPackageProvider.KnowledgePointWeight;
import com.group01.assessment.application.port.ContentPackageProvider.Option;
import com.group01.assessment.application.port.ContentPackageProvider.PackageVersion;
import com.group01.assessment.application.port.ContentPackageProvider.Section;
import com.group01.assessment.application.result.LearnerAssessmentResult;
import com.group01.assessment.domain.exception.AttemptExpiredException;
import com.group01.assessment.domain.vo.AttemptChannel;
import com.group01.assessment.domain.vo.AttemptMode;
import com.group01.assessment.domain.vo.AttemptStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Start, answer and submit against real PostgreSQL; only the Content read is stubbed. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ==",
        "assessment.outbox.relay.enabled=false"
})
class AutoGradingIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final Map<String, Object> Q1 = Map.of("type", "CHOICE", "correct", "B");
    private static final Map<String, Object> Q2 = Map.of("type", "FILL", "accepted", List.of("critics"));
    private static final Map<String, Object> Q3 = Map.of("type", "CHOICE", "correct", "NOT_GIVEN");
    private static final Map<String, Object> Q4_LEGACY = Map.of("correct", "A");

    @MockitoBean ContentPackageProvider content;
    @Autowired StartAssessmentAttemptUseCase start;
    @Autowired SaveAttemptResponseUseCase saveResponse;
    @Autowired SubmitAssessmentAttemptUseCase submit;
    @Autowired GetAssessmentResultUseCase learnerResult;
    @Autowired CreateAssessmentResultUseCase createResult;
    @Autowired JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();

    @Test
    void passingSubmitCompletesVersionOneWithOneEventAndShowsSolutions() throws Exception {
        UUID versionId = UUID.randomUUID();
        UUID attemptId = startAttempt(versionId, "TOPIC_TEST", List.of(Q1, Q2, Q3, Q4_LEGACY));
        List<UUID> items = itemIds(attemptId);
        answer(attemptId, items.get(0), "{\"answer\":\"B\"}");
        answer(attemptId, items.get(1), "{\"answer\":\" Critics \"}");
        answer(attemptId, items.get(2), "{\"answer\":\"NOT_GIVEN\"}");
        answer(attemptId, items.get(3), "{\"answer\":\"C\"}");

        assertEquals(AttemptStatus.SUBMITTED, submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId)).status());
        submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId));

        assertEquals(List.of(Map.of("result_version", 1, "status", "COMPLETED")), jdbc.queryForList(
                "SELECT result_version, status FROM assessment_results WHERE attempt_id = ?", attemptId));
        assertEquals(4, jdbc.queryForObject("""
                SELECT count(*) FROM item_results ir JOIN assessment_results r ON r.id = ir.result_id
                WHERE r.attempt_id = ? AND ir.max_score = 1
                """, Integer.class, attemptId));
        List<String> events = events(attemptId);
        assertEquals(1, events.size(), "a repeated submit must not announce the result again");
        JsonNode data = json.readTree(events.getFirst()).get("data");
        assertEquals(versionId.toString(), data.get("package_version_id").asText());
        assertTrue(data.get("learning_goal_id").isNull());
        assertEquals("TOPIC_GATE", data.get("assessment_type").asText());
        assertFalse(data.get("item_results").get(3).get("is_correct").asBoolean());

        LearnerAssessmentResult result = learnerResult.execute(userId, attemptId);
        assertEquals(3.0, result.score());
        assertEquals(4.0, result.maxScore());
        assertEquals(75.0, result.percent());
        assertEquals(List.of(true, true, true, false),
                result.items().stream().map(LearnerAssessmentResult.Item::correct).toList());
        assertEquals(List.of("B", "critics", "NOT_GIVEN", "A"),
                result.solutions().stream().map(LearnerAssessmentResult.Solution::correctAnswer).toList());
        assertEquals("Explanation 4", result.solutions().get(3).explanation());

        // A regrade still being graded does not replace what the learner sees.
        createResult.executeForGrader(attemptId, null);
        assertEquals(1, learnerResult.execute(userId, attemptId).resultVersion());
    }

    @Test
    void omittedAndInvalidAnswersScoreZeroAndAFailHidesSolutions() {
        UUID attemptId = startAttempt(UUID.randomUUID(), "MOCK_TEST", List.of(Q1, Q2, Q3, Q4_LEGACY));
        List<UUID> items = itemIds(attemptId);
        answer(attemptId, items.get(0), "{\"answer\":\"B\"}");
        answer(attemptId, items.get(1), "{\"answer\":\"critics\"}");
        answer(attemptId, items.get(3), "{\"answer\":[\"A\"]}");

        submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId));

        LearnerAssessmentResult result = learnerResult.execute(userId, attemptId);
        assertEquals(50.0, result.percent());
        assertEquals(List.of(true, true, false, false),
                result.items().stream().map(LearnerAssessmentResult.Item::correct).toList());
        assertNull(result.solutions());
        assertEquals(1, events(attemptId).size());
    }

    @Test
    void anUngradableItemLeavesTheAttemptForAHumanGrader() {
        Map<String, Object> essay = Map.of("type", "ESSAY", "task", "TASK_2", "minWords", 250, "passBand", 6.0);
        UUID attemptId = startAttempt(UUID.randomUUID(), "MOCK_TEST", List.of(Q1, essay));

        assertEquals(AttemptStatus.SUBMITTED, submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId)).status());

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM assessment_results WHERE attempt_id = ?",
                Integer.class, attemptId));
        assertTrue(events(attemptId).isEmpty());
    }

    @Test
    void lateSubmitPersistsTheExpiry() {
        UUID attemptId = startAttempt(UUID.randomUUID(), "QUIZ", List.of(Q1));
        jdbc.update("UPDATE assessment_attempts SET expires_at = now() - interval '1 minute' WHERE id = ?", attemptId);

        assertThrows(AttemptExpiredException.class,
                () -> submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId)));

        assertEquals("EXPIRED", jdbc.queryForObject("SELECT status FROM assessment_attempts WHERE id = ?",
                String.class, attemptId));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM assessment_results WHERE attempt_id = ?",
                Integer.class, attemptId));
    }

    private UUID startAttempt(UUID versionId, String packageType, List<Map<String, Object>> specs) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            items.add(new Item(UUID.randomUUID(), i + 1, "Question " + (i + 1),
                    List.of(new Option("A", "first", 1), new Option("B", "second", 2)), specs.get(i),
                    "Explanation " + (i + 1), BigDecimal.ONE,
                    List.of(new KnowledgePointWeight(UUID.randomUUID(), BigDecimal.ONE))));
        }
        when(content.findPackageVersion(versionId)).thenReturn(Optional.of(new PackageVersion(versionId, packageType,
                List.of(new Section(UUID.randomUUID(), "Section", "READING", null, 1, "Passage", items)))));
        return start.execute(new StartAssessmentAttemptCommand(userId, versionId, AttemptMode.STANDARD,
                AttemptChannel.WEB)).id();
    }

    private List<UUID> itemIds(UUID attemptId) {
        return jdbc.queryForList("""
                SELECT i.id FROM attempt_items i JOIN attempt_sections s ON s.id = i.attempt_section_id
                WHERE s.attempt_id = ? ORDER BY s.sort_order, i.sort_order
                """, UUID.class, attemptId);
    }

    private void answer(UUID attemptId, UUID itemId, String payload) {
        saveResponse.execute(new SaveAttemptResponseCommand(userId, attemptId, itemId, payload, 1, 0));
    }

    private List<String> events(UUID attemptId) {
        return jdbc.queryForList("""
                SELECT o.payload::text FROM outbox_events o
                JOIN assessment_results r ON o.aggregate_id = r.id::text
                WHERE r.attempt_id = ? AND o.event_type = 'AssessmentCompleted.v2'
                """, String.class, attemptId);
    }
}
