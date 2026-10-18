package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.port.LlmClient;
import com.ieltspath.learning.application.usecase.GetWritingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.SubmitFreeWritingUseCase;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** An essay written for a library prompt, outside any lesson: graded once, charged once, readable by its owner. */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false",
        "learning.messaging.consumer-enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class FreeWritingIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());

    private static final byte[] INTERNAL_KEY = new byte[32];
    static { new SecureRandom().nextBytes(INTERNAL_KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID OTHER_USER = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final UUID ESSAY = UUID.randomUUID();
    private static final UUID OBJECTIVE = UUID.randomUUID();
    private static final String TEXT = ("The line graph shows that mobile phones became the most common device while "
            + "tablets appeared only after 2010. ").repeat(5);

    @Autowired JdbcTemplate jdbc;
    @Autowired SubmitFreeWritingUseCase submitFree;
    @Autowired GetWritingSubmissionUseCase getSubmission;
    @MockitoBean LearningContentClient content;
    @MockitoBean AccessClient access;
    @MockitoBean LlmClient llm;
    @MockitoBean com.ieltspath.learning.application.port.EssayPromptActivationProvider promptActivations;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE lesson_writing_submissions, llm_daily_usage CASCADE");
        reset(content, access, llm);
        when(content.getPackageVersion(VERSION)).thenReturn(new PackageVersion(VERSION, UUID.randomUUID(),
                "PRACTICE_TEST", null, Map.of(), List.of(new Section(UUID.randomUUID(), "Task 1", "WRITING", null, 1,
                null, List.of(
                new Item(ESSAY, 1, "The line graph below shows device ownership.", null,
                        Map.of("type", "ESSAY", "task", "TASK_1", "minWords", 150, "passBand", 5.5,
                                "chartFacts", "Mobile phone: 1995 5%, 2015 95%."), "Model essay",
                        BigDecimal.ONE, List.of()),
                new Item(OBJECTIVE, 2, "Pick", null, Map.of("type", "CHOICE", "correct", "A"), "Because",
                        BigDecimal.ONE, List.of()))))));
        when(llm.configured()).thenReturn(true);
        // No prompt version has been activated yet, so the grader bootstraps from the active rubric of the task.
        when(content.getActiveRubric(anyString())).thenAnswer(invocation -> rubric(invocation.getArgument(0)));
        when(content.activeErrorTypes()).thenReturn(List.of());
        when(access.balance()).thenReturn(30L);
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenAnswer(invocation -> UUID.randomUUID());
    }

    @Test
    void anEssayIsGradedAndChargedOnceAndOnlyItsOwnerReadsIt() {
        when(llm.completeJson(any(), any(), anyBoolean())).thenReturn(grade("6.0"));
        UUID request = UUID.randomUUID();

        var essay = submitFree.execute(USER, VERSION, ESSAY, request, TEXT);

        assertEquals("GRADED", essay.status());
        assertTrue(essay.passed());
        assertEquals("TASK_1", essay.task());
        assertEquals("Model essay", essay.sampleAnswer());
        assertEquals(TEXT, essay.essayText());
        assertEquals("FREE_WRITING", jdbc.queryForObject("SELECT source FROM lesson_writing_submissions WHERE id = ?",
                String.class, essay.submissionId()));
        assertNull(jdbc.queryForObject("SELECT lesson_id FROM lesson_writing_submissions WHERE id = ?", UUID.class,
                essay.submissionId()));

        assertEquals(essay.submissionId(), submitFree.execute(USER, VERSION, ESSAY, request, TEXT).submissionId());
        verify(llm, times(1)).completeJson(any(), any(), anyBoolean());
        verify(access, times(1)).debit(eq(USER), eq(3), eq(essay.submissionId()),
                eq("free-writing:" + USER + ":" + request), any());

        assertEquals("GRADED", getSubmission.execute(USER, essay.submissionId()).status());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> getSubmission.execute(OTHER_USER, essay.submissionId())).getStatus());
        assertEquals(409, assertThrows(LearningRequestException.class,
                () -> submitFree.execute(OTHER_USER, VERSION, ESSAY, request, TEXT)).getStatus());
    }

    @Test
    void thePromptMustBeAnEssayOfTheTestAndTheLearnerMustAffordTheGrade() {
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> submitFree.execute(USER, VERSION, OBJECTIVE, UUID.randomUUID(), TEXT)).getStatus());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> submitFree.execute(USER, VERSION, UUID.randomUUID(), UUID.randomUUID(), TEXT)).getStatus());

        when(access.balance()).thenReturn(0L);
        assertEquals("INSUFFICIENT_POINTS", assertThrows(LearningRequestException.class,
                () -> submitFree.execute(USER, VERSION, ESSAY, UUID.randomUUID(), TEXT)).getCode());
        verify(llm, never()).completeJson(any(), any(), anyBoolean());
    }

    @Test
    void onlyThePromptsOfAPracticeTestCanBeGradedAsFreeWriting() {
        for (String type : List.of("PLACEMENT_TEST", "MOCK_TEST", "COURSE_TEST", "TOPIC_TEST", "LESSON")) {
            UUID version = UUID.randomUUID();
            when(content.getPackageVersion(version)).thenReturn(new PackageVersion(version, UUID.randomUUID(), type,
                    null, Map.of(), List.of(new Section(UUID.randomUUID(), "Task 1", "WRITING", null, 1, null, List.of(
                    new Item(ESSAY, 1, "Prompt", null, Map.of("type", "ESSAY", "task", "TASK_2", "minWords", 250,
                            "passBand", 5.5), "Model essay", BigDecimal.ONE, List.of()))))));

            var error = assertThrows(LearningRequestException.class,
                    () -> submitFree.execute(USER, version, ESSAY, UUID.randomUUID(), TEXT));

            assertEquals("NOT_A_PRACTICE_TEST", error.getCode(), type);
            assertEquals(422, error.getStatus(), type);
        }
        verify(access, never()).debit(any(), anyInt(), any(), anyString(), any());
        verify(llm, never()).completeJson(any(), any(), anyBoolean());
    }

    @Test
    void aFailedDebitKeepsTheGradeWithheldUntilAResendIsCharged() {
        when(llm.completeJson(any(), any(), anyBoolean())).thenReturn(grade("5.0"));
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenThrow(new InsufficientPointsException());
        UUID request = UUID.randomUUID();
        assertEquals("INSUFFICIENT_POINTS", assertThrows(LearningRequestException.class,
                () -> submitFree.execute(USER, VERSION, ESSAY, request, TEXT)).getCode());

        reset(access);
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenAnswer(invocation -> UUID.randomUUID());
        var essay = submitFree.execute(USER, VERSION, ESSAY, request, TEXT);

        assertEquals("GRADED", essay.status());
        assertFalse(essay.passed());
        verify(llm, times(1)).completeJson(any(), any(), anyBoolean());
    }

    private static LearningContentClient.Rubric rubric(String task) {
        var descriptor = new LearningContentClient.RubricBandDescriptor(new BigDecimal("6.0"), "Band six.");
        var criteria = (task.equals("TASK_1") ? List.of("TA", "CC", "LR", "GRA") : List.of("TR", "CC", "LR", "GRA"))
                .stream().map(code -> new LearningContentClient.RubricCriterion(code, code, "Description", 1,
                        List.of(descriptor))).toList();
        return new LearningContentClient.Rubric("IELTS_WRITING", task, 1, "source", criteria, List.of());
    }

    private static String grade(String band) {
        String criteria = String.join(",", List.of("TA", "CC", "LR", "GRA").stream()
                .map(code -> "{\"code\":\"%s\",\"band\":%s,\"strengths\":[\"s\"],\"improvements\":[\"i\"]}"
                        .formatted(code, band)).toList());
        return "{\"criteria\":[" + criteria + "],\"corrections\":[],\"summary\":\"ok\"}";
    }
}
