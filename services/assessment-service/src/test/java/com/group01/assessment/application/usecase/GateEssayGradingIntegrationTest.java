package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.CreateLearnerSubmissionCommand;
import com.group01.assessment.application.command.StartAssessmentAttemptCommand;
import com.group01.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.group01.assessment.application.port.ContentPackageProvider;
import com.group01.assessment.application.port.ContentPackageProvider.Item;
import com.group01.assessment.application.port.ContentPackageProvider.KnowledgePointWeight;
import com.group01.assessment.application.port.ContentPackageProvider.Option;
import com.group01.assessment.application.port.ContentPackageProvider.PackageVersion;
import com.group01.assessment.application.port.ContentPackageProvider.Section;
import com.group01.assessment.application.port.EssayGradingPort;
import com.group01.assessment.application.exception.EssayGradingException;
import com.group01.assessment.domain.vo.AttemptChannel;
import com.group01.assessment.domain.vo.AttemptMode;
import com.group01.assessment.domain.vo.Skill;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "assessment.outbox.relay.enabled=false",
        "assessment.llm-grading.enabled=false",
        "assessment.llm-grading.daily-limit=10",
        "assessment.llm-grading.batch-size=10"
})
class GateEssayGradingIntegrationTest {
    private static final byte[] INTERNAL_KEY = new byte[32];
    static { new SecureRandom().nextBytes(INTERNAL_KEY); }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @MockitoBean ContentPackageProvider content;
    @MockitoBean EssayGradingPort essayGrader;
    @Autowired StartAssessmentAttemptUseCase start;
    @Autowired CreateLearnerSubmissionUseCase createSubmission;
    @Autowired SubmitAssessmentAttemptUseCase submit;
    @Autowired GradeGateEssayJobUseCase gradeJobs;
    @Autowired JdbcTemplate jdbc;

    private final UUID userId = UUID.randomUUID();

    @Test
    void submittedGateEssayIsQueuedThenCompletesResultAndOutboxAfterSuccessfulAiGrade() {
        UUID attemptId = startAttempt("TOPIC_TEST");
        UUID itemId = itemId(attemptId);
        createSubmission.execute(new CreateLearnerSubmissionCommand(userId, itemId, "{\"task\":\"TASK_2\"}",
                Skill.WRITING, "A considered response to the question.", null, "gate-essay-submit-" + attemptId));

        submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId));

        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE submission_id IN " +
                "(SELECT id FROM learner_submissions WHERE attempt_item_id = ?)", itemId));
        assertEquals("AI", jdbc.queryForObject("SELECT grading_mode FROM grading_jobs WHERE submission_id IN " +
                "(SELECT id FROM learner_submissions WHERE attempt_item_id = ?)", String.class, itemId));
        assertEquals("QUEUED", jdbc.queryForObject("SELECT status FROM grading_jobs WHERE submission_id IN " +
                "(SELECT id FROM learner_submissions WHERE attempt_item_id = ?)", String.class, itemId));
        assertEquals(0, count("SELECT count(*) FROM assessment_results WHERE attempt_id = ?", attemptId));
        assertEquals(0, count("""
                SELECT count(*) FROM outbox_events o
                WHERE o.event_type = 'AssessmentCompleted.v2'
                  AND o.aggregate_id IN (SELECT id::text FROM assessment_results WHERE attempt_id = ?)
                """, attemptId));

        when(essayGrader.available()).thenReturn(true);
        when(essayGrader.grade(any(), anyString())).thenReturn(new BigDecimal("6.0"));
        assertEquals(1, gradeJobs.gradeBatch());

        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM grading_jobs WHERE submission_id IN " +
                "(SELECT id FROM learner_submissions WHERE attempt_item_id = ?)", String.class, itemId));
        assertEquals(1, count("SELECT count(*) FROM assessment_results WHERE attempt_id = ? AND status = 'COMPLETED'",
                attemptId));
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type = 'AssessmentCompleted.v2' AND aggregate_id IN " +
                "(SELECT id::text FROM assessment_results WHERE attempt_id = ?)", attemptId));
        assertEquals(1, count("SELECT count(*) FROM item_results ir JOIN assessment_results r ON r.id = ir.result_id " +
                "WHERE r.attempt_id = ? AND ir.attempt_item_id = ? AND ir.is_correct = TRUE AND ir.score = 1", attemptId, itemId));
    }

    @Test
    void providerFailureCreatesHumanFallbackWithoutCompletingTheGate() {
        UUID attemptId = startAttempt("COURSE_TEST");
        UUID itemId = itemId(attemptId);
        createSubmission.execute(new CreateLearnerSubmissionCommand(userId, itemId, "{}", Skill.WRITING,
                "Submitted essay", null, "gate-essay-failure-" + attemptId));
        submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId));
        when(essayGrader.available()).thenReturn(true);
        when(essayGrader.grade(any(), anyString())).thenThrow(new EssayGradingException("LLM_REQUEST_FAILED"));

        assertEquals(1, gradeJobs.gradeBatch());

        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE submission_id IN " +
                "(SELECT id FROM learner_submissions WHERE attempt_item_id = ?) AND grading_mode = 'AI' AND status = 'FAILED'",
                itemId));
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE submission_id IN " +
                "(SELECT id FROM learner_submissions WHERE attempt_item_id = ?) AND grading_mode = 'HUMAN' AND status = 'QUEUED'",
                itemId));
        assertEquals(1, count("SELECT count(*) FROM human_reviews hr JOIN grading_jobs gj ON gj.id = hr.grading_job_id " +
                "WHERE gj.submission_id IN (SELECT id FROM learner_submissions WHERE attempt_item_id = ?) AND hr.status = 'QUEUED'",
                itemId));
        assertEquals(0, count("SELECT count(*) FROM assessment_results WHERE attempt_id = ?", attemptId));
        assertEquals(0, count("""
                SELECT count(*) FROM outbox_events o
                WHERE o.event_type = 'AssessmentCompleted.v2'
                  AND o.aggregate_id IN (SELECT id::text FROM assessment_results WHERE attempt_id = ?)
                """, attemptId));
    }

    @Test
    void mockEssayStaysOnHumanPathAndDoesNotCreateAiJob() {
        UUID attemptId = startAttempt("MOCK_TEST");
        UUID itemId = itemId(attemptId);
        createSubmission.execute(new CreateLearnerSubmissionCommand(userId, itemId, "{}", Skill.WRITING,
                "Submitted essay", null, "mock-essay-" + attemptId));

        submit.execute(new SubmitAssessmentAttemptCommand(userId, attemptId));

        assertEquals(0, count("SELECT count(*) FROM grading_jobs WHERE submission_id IN " +
                "(SELECT id FROM learner_submissions WHERE attempt_item_id = ?) AND grading_mode = 'AI'", itemId));
        assertEquals(0, count("SELECT count(*) FROM assessment_results WHERE attempt_id = ?", attemptId));
        assertEquals(0, count("""
                SELECT count(*) FROM outbox_events o
                WHERE o.event_type = 'AssessmentCompleted.v2'
                  AND o.aggregate_id IN (SELECT id::text FROM assessment_results WHERE attempt_id = ?)
                """, attemptId));
    }

    private UUID startAttempt(String packageType) {
        UUID versionId = UUID.randomUUID();
        UUID knowledgePointId = UUID.randomUUID();
        Map<String, Object> essaySpec = Map.of("type", "ESSAY", "task", "TASK_2", "minWords", 250,
                "passBand", 5.5);
        Item essay = new Item(UUID.randomUUID(), 1, "Discuss the topic", List.<Option>of(), essaySpec,
                "Essay explanation", BigDecimal.ONE, List.of(new KnowledgePointWeight(knowledgePointId, BigDecimal.ONE)));
        Section section = new Section(UUID.randomUUID(), "Writing", "WRITING", "Write an essay", 1, null, null,
                List.of(essay));
        when(content.findPackageVersion(versionId)).thenReturn(Optional.of(new PackageVersion(versionId, packageType,
                List.of(section))));
        return start.execute(new StartAssessmentAttemptCommand(userId, versionId, AttemptMode.STANDARD,
                AttemptChannel.WEB)).id();
    }

    private UUID itemId(UUID attemptId) {
        return jdbc.queryForObject("SELECT i.id FROM attempt_items i JOIN attempt_sections s ON s.id = i.attempt_section_id " +
                "WHERE s.attempt_id = ?", UUID.class, attemptId);
    }

    private int count(String sql, Object... args) {
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
