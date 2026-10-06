package com.group01.learning.infrastructure.persistence;

import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.command.SubmitReviewCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.*;
import com.group01.learning.application.result.ReviewResult;
import com.group01.learning.application.result.ReviewSubmissionResult;
import com.group01.learning.application.usecase.AssignTopicTestUseCase;
import com.group01.learning.application.usecase.GetLessonUseCase;
import com.group01.learning.application.usecase.SubmitLessonExerciseUseCase;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.group01.learning.application.usecase.GetReviewUseCase;
import com.group01.learning.application.usecase.SubmitReviewUseCase;
import com.group01.learning.application.usecase.SubmitTheoryCheckUseCase;
import com.group01.learning.application.usecase.CompleteLessonUseCase;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.TopicStatus;
import com.group01.learning.domain.exception.LearningGateException;
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
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class ReviewAndTestAssignmentIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db")
            .withPassword(UUID.randomUUID().toString());

    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID OTHER_USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID EXERCISE_BLOCK = UUID.randomUUID();
    private static final UUID ESSAY_BLOCK = UUID.randomUUID();
    private static final UUID QUESTION = UUID.randomUUID();
    private static final UUID KP = UUID.randomUUID();
    private static final UUID PACKAGE_A = UUID.randomUUID();
    private static final UUID PACKAGE_B = UUID.randomUUID();
    private static final UUID TEST_X = UUID.randomUUID();
    private static final UUID TEST_Y = UUID.randomUUID();

    @Autowired JdbcTemplate jdbc;
    @Autowired GetReviewUseCase getReviewUseCase;
    @Autowired SubmitTheoryCheckUseCase theoryCheck;
    @Autowired SubmitReviewUseCase submitReviewUseCase;
    @Autowired AssignTopicTestUseCase tests;
    @Autowired GetLessonUseCase getLessonUseCase;
    @Autowired SubmitLessonExerciseUseCase submitLessonExerciseUseCase;
    @Autowired RefreshLearningTopicsUseCase topics;
    @Autowired CompleteLessonUseCase completeLessonUseCase;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void resetDataAndCurriculum() {
        jdbc.execute("""
                TRUNCATE review_theory_checks, review_sets, review_items, practice_attempts, lesson_practice_passes,
                lesson_exercise_submissions, kp_evidence,
                lesson_progress, topic_progress, knowledge_point_catalog, topic_test_assignments,
                assessment_result_versions RESTART IDENTITY
                """);
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "LISTEN", "Listening", 920, null,
                List.of(new KnowledgePoint(KP, "LS_NUM", "Numbers", "PROCEDURE", "LISTENING", "d", true)),
                LearningSkill.LISTENING, true)));
        when(content.getTopicLessons(TOPIC)).thenReturn(List.of(new LessonSummary(LESSON, TOPIC, "LS1", "Form", null,
                1, List.of(KP), List.of(EXERCISE_BLOCK, ESSAY_BLOCK))));
        Asset audio = new Asset(UUID.randomUUID(), "AUDIO", "Caller: It is Thompson.", "listening/demo/ls1.mp3", 45,
                "https://media.example.test/listening/demo/ls1.mp3");
        Question choice = new Question(QUESTION, 1, "Surname?", List.of(new Option("A", "Thompson", 1),
                new Option("B", "Thomson", 2), new Option("C", "Tomson", 3)),
                Map.of("type", "CHOICE", "correct", "A"), "Spelled out", List.of(KP));
        Question essay = new Question(UUID.randomUUID(), 1, "Describe the chart.", null,
                Map.of("type", "ESSAY", "task", "TASK_1", "minWords", 150, "passBand", 6.0, "chartFacts", "secret"),
                "Model answer", List.of(KP), List.of(new QuestionAsset(UUID.randomUUID(), "IMAGE",
                "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=", "Bar chart", 1)));
        when(content.getLesson(LESSON)).thenReturn(new Lesson(LESSON, TOPIC, "LS1", "Form", null, 1, List.of(KP),
                List.of(new Block(UUID.randomUUID(), "TEXT", 1, "Read the gaps first.", null, null, null),
                        new Block(UUID.randomUUID(), "ASSET", 2, null, audio, null, null),
                        new Block(EXERCISE_BLOCK, "EXERCISE", 3, null, null, null, List.of(choice), "EXERCISE"),
                        new Block(ESSAY_BLOCK, "EXERCISE", 4, null, null, null, List.of(essay), "ESSAY"))));
        when(content.getPackageVersion(any())).thenAnswer(invocation -> practiceSet(invocation.getArgument(0)));
        when(content.getTopicTestPackages(TOPIC)).thenReturn(List.of(new TestPackage(TEST_X, UUID.randomUUID(), "X3"),
                new TestPackage(TEST_Y, UUID.randomUUID(), "X4")));
        topics.execute(USER);
    }

    // ---- lessons with essay and audio blocks ----

    @Test
    void essayBlockNeitherBlocksCompletionNorAcceptsExerciseAnswersAndTranscriptWaitsForCompletion() {
        var before = getLessonUseCase.execute(USER, LESSON);
        var audio = before.blocks().get(1).asset();
        assertEquals("https://media.example.test/listening/demo/ls1.mp3", audio.mediaUrl());
        assertNull(audio.transcript());
        assertNull(audio.textContent());
        var essay = before.blocks().get(3);
        assertEquals("ESSAY", essay.blockKind());
        assertNull(essay.questions());
        assertEquals("TASK_1", essay.essay().task());
        assertEquals(150, essay.essay().minWords());
        assertEquals("Bar chart", essay.essay().images().getFirst().altText());

        var essayAnswer = new SubmitExerciseCommand(UUID.randomUUID(),
                List.of(new SubmitExerciseCommand.Answer(essay.essay().questionVersionId(), "An essay")));
        var conflict = assertThrows(LearningRequestException.class,
                () -> submitLessonExerciseUseCase.execute(USER, LESSON, ESSAY_BLOCK, essayAnswer));
        assertEquals("ESSAY_BLOCK", conflict.getCode());

        var result = submitLessonExerciseUseCase.execute(USER, LESSON, EXERCISE_BLOCK, new SubmitExerciseCommand(UUID.randomUUID(),
                List.of(new SubmitExerciseCommand.Answer(QUESTION, "A"))));
        assertTrue(result.lessonCompleted());
        assertEquals("Caller: It is Thompson.", getLessonUseCase.execute(USER, LESSON).blocks().get(1).asset().transcript());
    }

    // ---- reviews ----

    @Test
    void reviewAssignsUnusedPackagesAndSkipsWhenNoneRemain() {
        UUID review = pendingReview(USER);
        when(content.searchPracticeSets(eq(KP), anyList(), eq(3), anyInt(), eq(LESSON))).thenAnswer(invocation -> {
            List<UUID> excluded = invocation.getArgument(1);
            return List.of(practice(PACKAGE_A)).stream()
                    .filter(set -> !excluded.contains(set.packageId())).toList();
        });

        ReviewResult first = getReviewUseCase.execute(USER, review);
        assertEquals(List.of("Read the gaps first."), first.theory());
        assertEquals(PACKAGE_A, first.set().packageId());
        assertEquals("https://media.example.test/numM.mp3", first.set().audio().mediaUrl());
        assertEquals(3, first.set().questions().size());
        assertTrue(first.set().questions().stream().allMatch(question -> question.hint() == null));
        assertEquals(first.set().reviewSetId(), getReviewUseCase.execute(USER, review).set().reviewSetId());

        // A failed set shows its solutions and transcript, then sends the review to the theory.
        ReviewSubmissionResult failed = submit(review, first.set().reviewSetId(), "B");
        assertEquals("PENDING", failed.reviewStatus());
        assertEquals("THEORY", failed.stage());
        assertNotNull(failed.results().getFirst().correctAnswer());
        assertTrue(failed.results().stream().allMatch(answer -> answer.hint() == null));
        assertNotNull(failed.transcript());

        ReviewResult theory = getReviewUseCase.execute(USER, review);
        assertNull(theory.set());
        theoryCheck.execute(USER, review, new SubmitExerciseCommand(UUID.randomUUID(), theory.quickCheck().stream()
                .map(question -> new SubmitExerciseCommand.Answer(question.questionVersionId(), "A")).toList()));

        // The only package has been revealed, so no further set is assigned.
        ReviewResult third = getReviewUseCase.execute(USER, review);
        assertEquals("SKIPPED", third.reviewStatus());
        assertNull(third.set());
        assertEquals("SKIPPED", getReviewUseCase.execute(USER, review).reviewStatus());
        assertNull(getReviewUseCase.execute(USER, review).set());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE source = 'review_set'",
                Integer.class));
        // A skipped review no longer gates lessons.
        assertDoesNotThrow(() -> getLessonUseCase.execute(USER, LESSON));
    }

    @Test
    void passedSetFinishesTheReviewWithSolutionsAndTranscriptAndReplaysByRequestId() {
        UUID review = pendingReview(USER);
        when(content.searchPracticeSets(eq(KP), anyList(), eq(3), anyInt(), eq(LESSON))).thenReturn(List.of(practice(PACKAGE_A)));
        UUID set = getReviewUseCase.execute(USER, review).set().reviewSetId();
        UUID requestId = UUID.randomUUID();

        ReviewSubmissionResult passed = submitReviewUseCase.execute(USER, review, command(set, requestId, "A"));
        assertEquals("DONE", passed.reviewStatus());
        assertEquals("A", passed.results().getFirst().correctAnswer());
        assertTrue(passed.results().stream().allMatch(answer -> answer.hint() == null));
        assertEquals("Clerk: nine thirty.", passed.transcript());
        assertEquals(passed, submitReviewUseCase.execute(USER, review, command(set, requestId, "B")));

        var closed = assertThrows(LearningRequestException.class,
                () -> submitReviewUseCase.execute(USER, review, command(set, UUID.randomUUID(), "A")));
        assertEquals("REVIEW_SET_CLOSED", closed.getCode());
    }

    @Test
    void reviewOfAnotherLearnerIsNotFoundAndNoPackageSkipsTheReview() {
        UUID review = pendingReview(USER);
        var notFound = assertThrows(LearningRequestException.class, () -> getReviewUseCase.execute(OTHER_USER, review));
        assertEquals(404, notFound.getStatus());

        when(content.searchPracticeSets(eq(KP), anyList(), eq(3), anyInt(), eq(LESSON))).thenReturn(List.of());
        ReviewResult skipped = getReviewUseCase.execute(USER, review);
        assertEquals("SKIPPED", skipped.reviewStatus());
        assertNull(skipped.set());
    }

    // ---- final-test assignments ----

    @Test
    void testAssignmentIsGatedIdempotentAndRotatesThroughPackages() {
        var locked = assertThrows(LearningRequestException.class, () -> tests.execute(USER, TOPIC));
        assertEquals("TEST_LOCKED", locked.getCode());

        completeLesson();
        UUID review = pendingReview(USER);
        var gate = assertThrows(LearningGateException.class, () -> tests.execute(USER, TOPIC));
        assertEquals("REVIEW_REQUIRED", gate.getCode());
        jdbc.update("UPDATE review_items SET status = 'DONE' WHERE id = ?", review);

        var first = tests.execute(USER, TOPIC);
        assertEquals(TEST_X, first.packageId());
        assertEquals(first, tests.execute(USER, TOPIC));

        consume(first.assignmentId());
        var second = tests.execute(USER, TOPIC);
        assertEquals(TEST_Y, second.packageId());

        consume(second.assignmentId());
        assertEquals(TEST_X, tests.execute(USER, TOPIC).packageId());
    }

    @Test
    void topicWithoutTestPackagesIsUnavailable() {
        completeLesson();
        when(content.getTopicTestPackages(TOPIC)).thenReturn(List.of());
        var unavailable = assertThrows(LearningRequestException.class, () -> tests.execute(USER, TOPIC));
        assertEquals("TEST_UNAVAILABLE", unavailable.getCode());
    }

    @Test
    void skillTracksKeepReviewsLocalAndPassWritingWithoutATest() {
        UUID readingFirst = UUID.randomUUID();
        UUID readingNext = UUID.randomUUID();
        UUID writing = UUID.randomUUID();
        UUID firstLesson = UUID.randomUUID();
        UUID nextLesson = UUID.randomUUID();
        UUID writingFirst = UUID.randomUUID();
        UUID writingLast = UUID.randomUUID();
        Course readingCourse = new Course(UUID.randomUUID(), "READING", "Reading", new BigDecimal("5.5"), false);
        Course listeningCourse = new Course(UUID.randomUUID(), "LISTENING", "Listening", new BigDecimal("5.5"), false);
        Course writingCourse = new Course(UUID.randomUUID(), "WRITING", "Writing", new BigDecimal("5.5"), false);
        when(content.getTopicSequence()).thenReturn(List.of(
                new Topic(readingFirst, "READ1", "Reading one", 1, null, List.of(), LearningSkill.READING, false,
                        readingCourse),
                new Topic(readingNext, "READ2", "Reading two", 2, null, List.of(), LearningSkill.READING, true,
                        readingCourse),
                new Topic(TOPIC, "LISTEN", "Listening", 3, null,
                        List.of(new KnowledgePoint(KP, "LS_NUM", "Numbers", "PROCEDURE", "LISTENING", "d", true)),
                        LearningSkill.LISTENING, true, listeningCourse),
                new Topic(writing, "WRITE", "Writing", 4, null, List.of(), LearningSkill.WRITING, false,
                        writingCourse)));
        when(content.getTopicLessons(readingFirst)).thenReturn(List.of(new LessonSummary(firstLesson, readingFirst,
                "R1", "First", null, 1, List.of(), List.of())));
        when(content.getTopicLessons(readingNext)).thenReturn(List.of(new LessonSummary(nextLesson, readingNext,
                "R2", "Next", null, 1, List.of(), List.of())));
        when(content.getTopicLessons(writing)).thenReturn(List.of(
                new LessonSummary(writingFirst, writing, "W1", "Writing one", null, 1, List.of(), List.of()),
                new LessonSummary(writingLast, writing, "W2", "Writing two", null, 2, List.of(), List.of())));
        when(content.getLesson(firstLesson)).thenReturn(new Lesson(firstLesson, readingFirst, "R1", "First", null,
                1, List.of(), List.of(), LearningSkill.READING));
        when(content.getLesson(nextLesson)).thenReturn(new Lesson(nextLesson, readingNext, "R2", "Next", null,
                1, List.of(), List.of(), LearningSkill.READING));
        when(content.getLesson(writingFirst)).thenReturn(new Lesson(writingFirst, writing, "W1", "Writing one", null,
                1, List.of(), List.of(), LearningSkill.WRITING));
        when(content.getLesson(writingLast)).thenReturn(new Lesson(writingLast, writing, "W2", "Writing two", null,
                2, List.of(), List.of(), LearningSkill.WRITING));

        var initial = topics.execute(USER);
        assertEquals(TopicStatus.IN_PROGRESS, initial.stream().filter(t -> t.topicId().equals(readingFirst))
                .findFirst().orElseThrow().status());
        assertEquals(TopicStatus.IN_PROGRESS, initial.stream().filter(t -> t.topicId().equals(TOPIC))
                .findFirst().orElseThrow().status());
        assertEquals(TopicStatus.IN_PROGRESS, initial.stream().filter(t -> t.topicId().equals(writing))
                .findFirst().orElseThrow().status());
        completeLessonUseCase.execute(USER, firstLesson);
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill) "
                        + "VALUES (?, ?, ?, ?, 'PENDING', 'READING')",
                UUID.randomUUID(), USER, UUID.randomUUID(), firstLesson);
        var blocked = assertThrows(LearningGateException.class, () -> getLessonUseCase.execute(USER, nextLesson));
        assertEquals("REVIEW_REQUIRED", blocked.getCode());
        assertEquals("AVAILABLE", getLessonUseCase.execute(USER, LESSON).status());

        completeLessonUseCase.execute(USER, writingFirst);
        completeLessonUseCase.execute(USER, writingLast);
        var after = topics.execute(USER);
        assertEquals(TopicStatus.PASSED, after.stream().filter(t -> t.topicId().equals(writing))
                .findFirst().orElseThrow().status());
        var unavailable = assertThrows(LearningRequestException.class, () -> tests.execute(USER, writing));
        assertEquals(409, unavailable.getStatus());
        assertEquals("NO_TOPIC_TEST", unavailable.getCode());
    }

    // ---- helpers ----

    private UUID pendingReview(UUID userId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status) "
                + "VALUES (?, ?, ?, ?, 'PENDING')", id, userId, KP, LESSON);
        return id;
    }

    private void completeLesson() {
        submitLessonExerciseUseCase.execute(USER, LESSON, EXERCISE_BLOCK, new SubmitExerciseCommand(UUID.randomUUID(),
                List.of(new SubmitExerciseCommand.Answer(QUESTION, "A"))));
    }

    private void consume(UUID assignmentId) {
        jdbc.update("UPDATE topic_test_assignments SET consumed_at = clock_timestamp(), consumed_attempt_id = ? "
                + "WHERE id = ?", UUID.randomUUID(), assignmentId);
    }

    private ReviewSubmissionResult submit(UUID review, UUID set, String answer) {
        return submitReviewUseCase.execute(USER, review, command(set, UUID.randomUUID(), answer));
    }

    private SubmitReviewCommand command(UUID set, UUID requestId, String answer) {
        PackageVersion version = practiceSet(versionOf(PACKAGE_A));
        return new SubmitReviewCommand(set, requestId, version.sections().getFirst().items().stream()
                .map(item -> new SubmitExerciseCommand.Answer(questionId(item.sortOrder()), answer)).toList());
    }

    private static PracticeSet practice(UUID packageId) {
        return new PracticeSet(packageId, versionOf(packageId), "PS", 3, 3);
    }

    private static UUID versionOf(UUID packageId) {
        return new UUID(packageId.getMostSignificantBits(), ~packageId.getLeastSignificantBits());
    }

    /** Both practice packages share question ids so one answer sheet fits every set. */
    private static UUID questionId(int sortOrder) {
        return new UUID(42L, sortOrder);
    }

    private static PackageVersion practiceSet(UUID versionId) {
        List<Item> items = List.of(1, 2, 3).stream().map(order -> new Item(questionId(order), order, "Q" + order,
                List.of(new Option("A", "right", 1), new Option("B", "wrong", 2), new Option("C", "other", 3)),
                Map.<String, Object>of("type", "CHOICE", "correct", "A"), "Because A",
                BigDecimal.ONE, List.of(new KnowledgePointMapping(KP, BigDecimal.ONE)))).toList();
        return new PackageVersion(versionId, UUID.randomUUID(), "PRACTICE_SET", null, Map.of(), List.of(
                new Section(UUID.randomUUID(), "Tickets", "LISTENING", null, 1, null, items,
                        new SectionAudio(UUID.randomUUID(), "https://media.example.test/numM.mp3", 30,
                                "Clerk: nine thirty."))));
    }
}
