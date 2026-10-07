package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.command.SubmitReviewCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.usecase.*;
import com.ieltspath.learning.domain.vo.LearningSkill;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class RemediationLadderIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RefreshLearningTopicsUseCase topics;
    @Autowired SubmitLessonExerciseUseCase exercise;
    @Autowired StartPracticeAttemptUseCase start;
    @Autowired SubmitPracticeAttemptUseCase practice;
    @Autowired GetReviewUseCase getReview;
    @Autowired SubmitReviewUseCase submitReview;
    @Autowired SubmitTheoryCheckUseCase theoryCheck;
    @MockitoBean LearningContentClient content;

    private final UUID topic = UUID.randomUUID();
    private final UUID lesson = UUID.randomUUID();
    private final UUID block = UUID.randomUUID();
    private final UUID kpA = UUID.randomUUID();
    private final UUID kpB = UUID.randomUUID();
    private final UUID lessonQuestionA = UUID.randomUUID();
    private final UUID lessonQuestionB = UUID.randomUUID();
    private final Map<UUID, PackageVersion> versions = new LinkedHashMap<>();

    @BeforeEach
    void stubContent() {
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(topic, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(kpA, "A", "A", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(kpB, "B", "B", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, true)));
        when(content.getTopicLessons(topic)).thenReturn(List.of(new LessonSummary(lesson, topic, "L1", "Lesson",
                null, 1, List.of(kpA, kpB), List.of(block))));
        when(content.getLesson(lesson)).thenReturn(new Lesson(lesson, topic, "L1", "Lesson", null, 1,
                List.of(kpA, kpB), List.of(
                new Block(UUID.randomUUID(), "TEXT", 1, "Theory A", null, List.of(), List.of(), null, List.of(kpA)),
                new Block(UUID.randomUUID(), "TEXT", 2, "Theory B", null, List.of(), List.of(), null, List.of(kpB)),
                new Block(block, "EXERCISE", 3, null, null, List.of(), List.of(
                        lessonQuestion(lessonQuestionA, 1, kpA), lessonQuestion(lessonQuestionB, 2, kpB)),
                        "EXERCISE", List.of(kpA, kpB))), LearningSkill.READING));
        when(content.lessonPracticeSets(lesson)).thenAnswer(invocation -> versions.values().stream()
                .map(version -> new LessonPracticeSet(version.packageId(), version.packageVersionId(), "PS",
                        "Practice", 2, List.of(), null)).toList());
        when(content.topicPracticeSets(topic)).thenAnswer(invocation -> new TopicPracticeSets(List.of(
                new TopicPracticeSets.LessonSets(lesson, content.lessonPracticeSets(lesson)))));
        when(content.getPackageVersion(any())).thenAnswer(invocation -> versions.get(invocation.<UUID>getArgument(0)));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(kpA, 2, kpB, 2));
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(1), any())).thenAnswer(invocation -> {
            List<UUID> excluded = invocation.getArgument(1);
            return versions.values().stream().filter(version -> !excluded.contains(version.packageId())).limit(1)
                    .map(version -> new PracticeSet(version.packageId(), version.packageVersionId(), "PS", 2, 2))
                    .toList();
        });
    }

    @Test
    void failedReviewSetLeadsToTheoryThenANewUnrevealedSetThenSkip() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA);
        PackageVersion second = practiceSet(kpA, kpA);
        PackageVersion third = practiceSet(kpA, kpA);
        topics.execute(user);
        exercise.execute(user, lesson, block, answers(List.of(lessonQuestionA, lessonQuestionB), "A", "A"));

        var attempt = start.execute(user, lesson, first.packageId());
        var submitted = practice.execute(user, attempt.attemptId(), answers(items(first), "A", "B"));
        assertEquals(1, submitted.reviewsCreated().size());
        assertEquals("PRACTICE", submitted.reviewsCreated().getFirst().stage());
        UUID reviewId = submitted.reviewsCreated().getFirst().reviewId();

        var withSet = getReview.execute(user, reviewId);
        assertEquals(second.packageId(), withSet.set().packageId());
        assertEquals("Look again", withSet.set().questions().getFirst().hint());
        assertTrue(withSet.quickCheck().isEmpty());

        var failed = submitReview.execute(user, reviewId, new SubmitReviewCommand(withSet.set().reviewSetId(),
                UUID.randomUUID(), answers(items(second), "A", "B").answers()));
        assertEquals("PENDING", failed.reviewStatus());
        assertEquals("THEORY", failed.stage());
        assertEquals("A", failed.results().getFirst().correctAnswer());
        assertNotNull(failed.results().get(1).explanation());

        var theory = getReview.execute(user, reviewId);
        assertNull(theory.set());
        assertEquals(List.of("Theory A"), theory.theory());
        assertEquals("KNOWLEDGE_POINT", theory.theoryScope());
        assertEquals("SECOND_FAIL", theory.theoryReason());
        assertEquals(List.of(lessonQuestionA), theory.quickCheck().stream()
                .map(question -> question.questionVersionId()).toList());
        assertEquals("Lesson hint", theory.quickCheck().getFirst().hint());
        var blocked = assertThrows(LearningRequestException.class, () -> submitReview.execute(user, reviewId,
                new SubmitReviewCommand(withSet.set().reviewSetId(), UUID.randomUUID(),
                        answers(items(second), "A", "A").answers())));
        assertEquals("THEORY_REQUIRED", blocked.getCode());

        int evidenceBefore = count("kp_evidence");
        var check = new SubmitExerciseCommand(UUID.randomUUID(),
                List.of(new SubmitExerciseCommand.Answer(lessonQuestionA, "B")));
        var checked = theoryCheck.execute(user, reviewId, check);
        assertEquals("PRACTICE", checked.stage());
        assertEquals(0, checked.correct());
        assertEquals("Because A", checked.results().getFirst().explanation());
        assertEquals(checked, theoryCheck.execute(user, reviewId, check));
        assertEquals(evidenceBefore, count("kp_evidence"));

        var next = getReview.execute(user, reviewId);
        assertEquals(third.packageId(), next.set().packageId());
        var skipped = submitReview.execute(user, reviewId, new SubmitReviewCommand(next.set().reviewSetId(),
                UUID.randomUUID(), answers(items(third), "B", "B").answers()));
        assertEquals("SKIPPED", skipped.reviewStatus());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? AND status = 'PENDING'",
                Integer.class, user));
    }

    @Test
    void newReviewsStartWithTheoryForLowScoresAndMistakesMadeInTheLesson() {
        UUID user = UUID.randomUUID();
        PackageVersion mixed = practiceSet(kpA, kpA, kpB, kpB);
        practiceSet(kpA, kpB);
        topics.execute(user);
        exercise.execute(user, lesson, block, answers(List.of(lessonQuestionA, lessonQuestionB), "B", "A"));
        exercise.execute(user, lesson, block, answers(List.of(lessonQuestionA, lessonQuestionB), "A", "A"));

        var attempt = start.execute(user, lesson, mixed.packageId());
        var submitted = practice.execute(user, attempt.attemptId(), answers(items(mixed), "A", "B", "B", "B"));
        Map<UUID, String> reasons = new HashMap<>();
        for (var created : submitted.reviewsCreated()) {
            assertEquals("THEORY", created.stage());
            reasons.put(created.knowledgePointId(), jdbc.queryForObject(
                    "SELECT theory_reason FROM review_items WHERE id = ?", String.class, created.reviewId()));
        }
        assertEquals(Map.of(kpA, "WRONG_IN_LESSON", kpB, "LOW_SCORE"), reasons);

        // A review from an assessment result is checked against the lesson when first opened.
        jdbc.update("UPDATE review_items SET status = 'DONE' WHERE user_id = ?", user);
        UUID assessmentReview = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind) "
                + "VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'ASSESSMENT')", assessmentReview, user, kpA, lesson);
        var opened = getReview.execute(user, assessmentReview);
        assertEquals("THEORY", opened.stage());
        assertEquals("WRONG_IN_LESSON", opened.theoryReason());
        assertNull(opened.set());
    }

    private PackageVersion practiceSet(UUID... kps) {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        List<Item> items = new ArrayList<>();
        for (int index = 0; index < kps.length; index++) {
            items.add(new Item(UUID.randomUUID(), index + 1, "Choose", options(), Map.of("type", "CHOICE",
                    "correct", "A"), "Because A", "Look again", BigDecimal.ONE,
                    List.of(new KnowledgePointMapping(kps[index], BigDecimal.ONE))));
        }
        PackageVersion version = new PackageVersion(versionId, packageId, "PRACTICE_SET", null, Map.of(),
                List.of(new Section(UUID.randomUUID(), "Practice", "READING", null, 1, "Passage", items)));
        versions.put(versionId, version);
        return version;
    }

    private static Question lessonQuestion(UUID id, int sortOrder, UUID kp) {
        return new Question(id, sortOrder, "Choose", options(), Map.of("type", "CHOICE", "correct", "A"),
                "Because A", List.of(kp), null, "Lesson hint");
    }

    private static List<Option> options() {
        return List.of(new Option("A", "Correct", 1), new Option("B", "Wrong", 2));
    }

    private static List<UUID> items(PackageVersion version) {
        return version.sections().getFirst().items().stream().map(Item::questionVersionId).toList();
    }

    private static SubmitExerciseCommand answers(List<UUID> questions, String... values) {
        List<SubmitExerciseCommand.Answer> answers = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            answers.add(new SubmitExerciseCommand.Answer(questions.get(index), values[index]));
        }
        return new SubmitExerciseCommand(UUID.randomUUID(), answers);
    }

    private int count(String from) {
        return jdbc.queryForObject("SELECT count(*) FROM " + from, Integer.class);
    }
}
