package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.api.dto.response.LearnerAssessmentResultResponse;
import com.ieltspath.assessment.application.command.SaveAttemptResponseCommand;
import com.ieltspath.assessment.application.command.StartAssessmentAttemptCommand;
import com.ieltspath.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.ieltspath.assessment.application.port.ContentPackageProvider;
import com.ieltspath.assessment.application.port.ContentPackageProvider.Item;
import com.ieltspath.assessment.application.port.ContentPackageProvider.KnowledgePointWeight;
import com.ieltspath.assessment.application.port.ContentPackageProvider.Option;
import com.ieltspath.assessment.application.port.ContentPackageProvider.PackageVersion;
import com.ieltspath.assessment.application.port.ContentPackageProvider.Section;
import com.ieltspath.assessment.application.result.LearnerAssessmentResult;
import com.ieltspath.assessment.domain.exception.AttemptExpiredException;
import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(ints = {6, 7})
    void listeningSubmitKeepsTranscriptPrivateUntilSeventyPercent(int correctCount) throws Exception {
        UUID versionId = UUID.randomUUID();
        UUID contentSectionId = UUID.randomUUID();
        String transcript = "The library closes at six.";
        List<Item> questions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            questions.add(new Item(UUID.randomUUID(), i + 1, "Listening question",
                    i % 2 == 0 ? List.of(new Option("B", "six", 1)) : null,
                    i % 2 == 0 ? Q1 : Map.of("type", "FILL", "accepted", List.of("six")),
                    "Listen for the closing time", BigDecimal.ONE,
                    List.of(new KnowledgePointWeight(UUID.randomUUID(), BigDecimal.ONE))));
        }
        Section section = new Section(contentSectionId, "Library", "LISTENING", "Listen and answer", 1, null,
                new ContentPackageProvider.Audio(UUID.randomUUID(), "https://cdn/library.mp3", 95, transcript), questions);
        when(content.findPackageVersion(versionId)).thenReturn(Optional.of(
                new PackageVersion(versionId, "TOPIC_TEST", List.of(section))));
        UUID attemptId = start.execute(new StartAssessmentAttemptCommand(userId, versionId, AttemptMode.STANDARD,
                AttemptChannel.WEB)).id();
        UUID sectionId = jdbc.queryForObject("SELECT id FROM attempt_sections WHERE attempt_id = ?",
                UUID.class, attemptId);
        var snapshot = json.readTree(jdbc.queryForObject(
                "SELECT section_snapshot::text FROM attempt_sections WHERE id = ?", String.class, sectionId));
        assertEquals("LISTENING", snapshot.path("skill").asText());
        assertEquals("https://cdn/library.mp3", snapshot.path("audio").path("url").asText());
        assertEquals(95, snapshot.path("audio").path("durationSeconds").asInt());
        assertEquals(transcript, snapshot.path("solution").path("transcript").asText());
        assertFalse(snapshot.has("passage"));

        List<UUID> items = itemIds(attemptId);
        for (int i = 0; i < correctCount; i++) {
            answer(attemptId, items.get(i), i % 2 == 0 ? "{\"answer\":\"B\"}" : "{\"answer\":\"six\"}");
        }
        submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId));
        submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId));
        var result = learnerResult.execute(userId, attemptId);
        assertEquals(correctCount * 10.0, result.percent());
        var response = json.copy().findAndRegisterModules().valueToTree(LearnerAssessmentResultResponse.from(result));
        if (correctCount < 7) {
            assertNull(result.solutions());
            assertFalse(response.has("solutions"));
            assertFalse(response.has("sectionSolutions"));
        } else {
            assertEquals(10, result.solutions().size());
            assertEquals(1, response.path("sectionSolutions").size());
            assertEquals(sectionId.toString(), response.path("sectionSolutions").get(0).path("attemptSectionId").asText());
            assertEquals(transcript, response.path("sectionSolutions").get(0).path("transcript").asText());
        }
        assertEquals(1, events(attemptId).size());
        JsonNode event = json.readTree(events(attemptId).getFirst());
        assertTrue(event.findValues("transcript").isEmpty());
        assertTrue(event.findValues("solution").isEmpty());
        assertFalse(event.toString().contains(transcript));
        assertEquals("TOPIC_GATE", event.path("data").path("assessment_type").asText());
        assertEquals(versionId.toString(), event.path("data").path("package_version_id").asText());
        assertEquals(10, event.path("data").path("item_results").size());
    }

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
        var readingResponse = json.copy().findAndRegisterModules()
                .valueToTree(LearnerAssessmentResultResponse.from(result));
        assertTrue(readingResponse.path("sectionSolutions").isArray());
        assertEquals(0, readingResponse.path("sectionSolutions").size());

        // A regrade still being graded does not replace what the learner sees.
        createResult.executeForGrader(attemptId, null);
        assertEquals(1, learnerResult.execute(userId, attemptId).resultVersion());
    }

    @Test
    void courseTestPersistsAsCourseGateAndEmitsTheCourseGateType() throws Exception {
        UUID attemptId = startAttempt(UUID.randomUUID(), "COURSE_TEST", List.of(Q1));
        UUID itemId = itemIds(attemptId).getFirst();
        assertEquals("COURSE_GATE", jdbc.queryForObject(
                "SELECT attempt_type FROM assessment_attempts WHERE id = ?", String.class, attemptId));

        answer(attemptId, itemId, "{\"answer\":\"B\"}");
        submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId));

        JsonNode event = json.readTree(events(attemptId).getFirst());
        assertEquals("COURSE_GATE", event.path("data").path("assessment_type").asText());
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
                List.of(new Section(UUID.randomUUID(), "Section", "READING", null, 1, "Passage", null, items)))));
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
