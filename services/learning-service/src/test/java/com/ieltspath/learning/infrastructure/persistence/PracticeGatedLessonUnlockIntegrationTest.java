package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.command.SubmitReviewCommand;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.result.TopicLessonsResult;
import com.ieltspath.learning.application.usecase.*;
import com.ieltspath.learning.domain.exception.LearningGateException;
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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The second lesson of a topic opens only when the first lesson is completed and its Practice is passed. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class PracticeGatedLessonUnlockIntegrationTest {
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
    @Autowired GetLessonUseCase getLesson;
    @Autowired GetTopicLessonsUseCase topicLessons;
    @Autowired GetLessonPracticeSetsUseCase practiceSets;
    @Autowired CompleteLessonUseCase complete;
    @Autowired SubmitLessonExerciseUseCase exercise;
    @Autowired StartPracticeAttemptUseCase start;
    @Autowired SubmitPracticeAttemptUseCase practice;
    @Autowired GetReviewUseCase getReview;
    @Autowired SubmitReviewUseCase submitReview;
    @MockitoBean LearningContentClient content;
    @MockitoBean AccessClient access;

    private final UUID topic = UUID.randomUUID();
    private final UUID lesson1 = UUID.randomUUID();
    private final UUID lesson2 = UUID.randomUUID();
    private final UUID block1 = UUID.randomUUID();
    private final UUID block2 = UUID.randomUUID();
    private final UUID kpA = UUID.randomUUID();
    private final UUID kpB = UUID.randomUUID();
    private final UUID question1a = UUID.randomUUID();
    private final UUID question1b = UUID.randomUUID();
    private final UUID question2 = UUID.randomUUID();
    private final Map<UUID, PackageVersion> versions = new LinkedHashMap<>();

    @BeforeEach
    void stubContent() {
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(topic, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(kpA, "A", "A", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(kpB, "B", "B", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, true)));
        when(content.getTopicLessons(topic)).thenReturn(List.of(
                new LessonSummary(lesson1, topic, "L1", "Lesson one", null, 1, List.of(kpA, kpB), List.of(block1)),
                new LessonSummary(lesson2, topic, "L2", "Lesson two", null, 2, List.of(kpA), List.of(block2))));
        when(content.getLesson(lesson1)).thenReturn(new Lesson(lesson1, topic, "L1", "Lesson one", null, 1,
                List.of(kpA, kpB), List.of(new Block(block1, "EXERCISE", 1, null, null, List.of(), List.of(
                        lessonQuestion(question1a, 1, kpA), lessonQuestion(question1b, 2, kpB)), "EXERCISE",
                        List.of(kpA, kpB))), LearningSkill.READING));
        when(content.getLesson(lesson2)).thenReturn(new Lesson(lesson2, topic, "L2", "Lesson two", null, 2,
                List.of(kpA), List.of(new Block(block2, "EXERCISE", 1, null, null, List.of(), List.of(
                        lessonQuestion(question2, 1, kpA)), "EXERCISE", List.of(kpA))), LearningSkill.READING));
        // Only the first lesson has Practice.
        when(content.lessonPracticeSets(lesson1)).thenAnswer(invocation -> versions.values().stream()
                .map(version -> new LessonPracticeSet(version.packageId(), version.packageVersionId(), "PS",
                        "Practice", 2, List.of(), null)).toList());
        when(content.lessonPracticeSets(lesson2)).thenReturn(List.of());
        when(content.topicPracticeSets(topic)).thenAnswer(invocation -> new TopicPracticeSets(List.of(
                new TopicPracticeSets.LessonSets(lesson1, content.lessonPracticeSets(lesson1)),
                new TopicPracticeSets.LessonSets(lesson2, List.of()))));
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
    void vocabularyBlocksAreReturnedButDoNotBlockExerciseCompletionOrTheNextLesson() {
        UUID user = UUID.randomUUID();
        UUID vocabularyBlockId = UUID.randomUUID();
        List<UUID> senseIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        var vocabulary = new Block(vocabularyBlockId, "VOCABULARY", 1, null, null, senseIds, List.of(), null, List.of());
        var exerciseBlock = new Block(block1, "EXERCISE", 2, null, null, List.of(), List.of(
                lessonQuestion(question1a, 1, kpA), lessonQuestion(question1b, 2, kpB)), "EXERCISE", List.of(kpA, kpB));
        when(content.getLesson(lesson1)).thenReturn(new Lesson(lesson1, topic, "L1", "Lesson one", null, 1,
                List.of(kpA, kpB), List.of(vocabulary, exerciseBlock), LearningSkill.READING));
        topics.execute(user);

        var lesson = getLesson.execute(user, lesson1);
        assertEquals(senseIds, lesson.blocks().getFirst().vocabularySenseIds());
        assertEquals("VOCABULARY", lesson.blocks().getFirst().blockType());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?", Integer.class, user));

        var submitted = exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));
        assertTrue(submitted.lessonCompleted());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?", Integer.class, user));
        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
        assertDoesNotThrow(() -> getLesson.execute(user, lesson2));
    }

    @Test
    void aTextAndVocabularyOnlyLessonCanBeCompletedWithoutCreatingEvidence() {
        UUID user = UUID.randomUUID();
        UUID textBlockId = UUID.randomUUID();
        UUID vocabularyBlockId = UUID.randomUUID();
        List<UUID> senseIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        var text = new Block(textBlockId, "TEXT", 1, "Read the introduction.", null, null, null, null, List.of(kpA));
        var vocabulary = new Block(vocabularyBlockId, "VOCABULARY", 2, null, null, senseIds, List.of(), null, List.of());
        when(content.getTopicLessons(topic)).thenReturn(List.of(
                new LessonSummary(lesson1, topic, "L1", "First lesson", null, 1, List.of(kpA), List.of())));
        when(content.getLesson(lesson1)).thenReturn(new Lesson(lesson1, topic, "L1", "First lesson", null, 1,
                List.of(kpA), List.of(text, vocabulary), LearningSkill.READING));
        topics.execute(user);

        var displayed = getLesson.execute(user, lesson1);
        assertEquals(senseIds, displayed.blocks().get(1).vocabularySenseIds());
        assertDoesNotThrow(() -> complete.execute(user, lesson1));
        assertNotNull(jdbc.queryForObject("SELECT completed_at FROM lesson_progress WHERE user_id = ? AND lesson_id = ?",
                java.sql.Timestamp.class, user, lesson1));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?", Integer.class, user));
    }

    @Test
    void theSecondLessonStaysClosedUntilThePracticeOfTheFirstIsPassed() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA);
        topics.execute(user);
        assertTrue(exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"))
                .lessonCompleted());

        LearningGateException read = assertThrows(LearningGateException.class, () -> getLesson.execute(user, lesson2));
        assertEquals("PRACTICE_REQUIRED", read.getCode());
        assertEquals(List.of(lesson1), read.getLessonIds());
        assertEquals("PRACTICE_REQUIRED", assertThrows(LearningGateException.class, () -> exercise.execute(user,
                lesson2, block2, answers(List.of(question2), "A"))).getCode());
        assertEquals("PRACTICE_REQUIRED", assertThrows(LearningGateException.class,
                () -> complete.execute(user, lesson2)).getCode());
        assertEquals(List.of("COMPLETED", "LOCKED"), statuses(user));

        var attempt = start.execute(user, lesson1, first.packageId());
        assertTrue(practice.execute(user, attempt.attemptId(), answers(items(first), "A", "A")).passed());

        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
        assertDoesNotThrow(() -> getLesson.execute(user, lesson2));
    }

    @Test
    void lessonsBeyondTheFreePreviewReturnPaywallWhileTheFirstThreeStayOpen() {
        UUID courseId = UUID.randomUUID();
        var course = new Course(courseId, "C65", "Band 6.5", new BigDecimal("6.5"), false);
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(topic, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(kpA, "A", "A", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, false, course)));
        when(content.getTopicLessons(topic)).thenReturn(List.of(
                new LessonSummary(lesson1, topic, "L1", "Lesson one", null, 1, List.of(kpA), List.of(),
                        Set.of(LearningSkill.READING), null, courseId, LearningSkill.READING, 2),
                new LessonSummary(lesson2, topic, "L2", "Lesson two", null, 2, List.of(kpA), List.of(),
                        Set.of(LearningSkill.READING), null, courseId, LearningSkill.READING, 4)));
        when(content.getLesson(lesson1)).thenReturn(new Lesson(lesson1, topic, "L1", "Lesson one", null, 1,
                List.of(kpA), List.of(), LearningSkill.READING, Set.of(LearningSkill.READING), courseId, 2));
        when(content.getLesson(lesson2)).thenReturn(new Lesson(lesson2, topic, "L2", "Lesson two", null, 2,
                List.of(kpA), List.of(), LearningSkill.READING, Set.of(LearningSkill.READING), courseId, 4));
        when(access.premium()).thenReturn(false);
        UUID user = UUID.randomUUID();
        topics.execute(user);

        assertDoesNotThrow(() -> getLesson.execute(user, lesson1));
        LearningGateException error = assertThrows(LearningGateException.class, () -> getLesson.execute(user, lesson2));
        assertEquals("PAYWALL", error.getCode());
        TopicLessonsResult result = topicLessons.execute(user, topic);
        assertFalse(result.lessons().get(0).paywall());
        assertTrue(result.lessons().get(0).freePreview());
        assertTrue(result.lessons().get(1).paywall());
        assertFalse(result.lessons().get(1).freePreview());
    }

    @Test
    void premiumExpiryLocksNewPremiumProgressButGrandfatheredProgressStaysReadableDuringAccessOutage() {
        UUID courseId = UUID.randomUUID();
        var course = new Course(courseId, "C65", "Band 6.5", new BigDecimal("6.5"), false);
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(topic, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(kpA, "A", "A", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, false, course)));
        when(content.getTopicLessons(topic)).thenReturn(List.of(new LessonSummary(lesson1, topic, "L1",
                "Lesson one", null, 1, List.of(kpA), List.of(), Set.of(LearningSkill.READING), null, courseId,
                LearningSkill.READING, 4)));
        when(content.getLesson(lesson1)).thenReturn(new Lesson(lesson1, topic, "L1", "Lesson one", null, 1,
                List.of(kpA), List.of(), LearningSkill.READING, Set.of(LearningSkill.READING), courseId, 4));
        UUID user = UUID.randomUUID();
        topics.execute(user);
        when(access.premium()).thenReturn(true);
        complete.execute(user, lesson1);
        assertFalse(jdbc.queryForObject("SELECT grandfathered FROM lesson_progress WHERE user_id = ? AND lesson_id = ?",
                Boolean.class, user, lesson1));

        when(access.premium()).thenReturn(false);
        assertEquals("PAYWALL", assertThrows(LearningGateException.class,
                () -> getLesson.execute(user, lesson1)).getCode());

        jdbc.update("UPDATE lesson_progress SET grandfathered = true WHERE user_id = ? AND lesson_id = ?",
                user, lesson1);
        when(access.premium()).thenThrow(new AccessUnavailableException(503));
        assertDoesNotThrow(() -> getLesson.execute(user, lesson1));
    }

    @Test
    void aFailedPracticeSendsTheLearnerToTheReviewAndTheSecondLessonOpensWhenItIsDone() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA);
        PackageVersion second = practiceSet(kpA, kpA);
        topics.execute(user);
        exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));
        var attempt = start.execute(user, lesson1, first.packageId());
        var submitted = practice.execute(user, attempt.attemptId(), answers(items(first), "A", "B"));
        assertFalse(submitted.passed());
        UUID reviewId = submitted.reviewsCreated().getFirst().reviewId();

        LearningGateException blocked = assertThrows(LearningGateException.class,
                () -> getLesson.execute(user, lesson2));
        assertEquals("REVIEW_REQUIRED", blocked.getCode());
        assertEquals(List.of(reviewId), blocked.getReviews().stream().map(review -> review.reviewId()).toList());
        assertEquals(List.of("COMPLETED", "LOCKED"), statuses(user));

        var review = getReview.execute(user, reviewId);
        assertEquals(second.packageId(), review.set().packageId());
        var done = submitReview.execute(user, reviewId, new SubmitReviewCommand(review.set().reviewSetId(),
                UUID.randomUUID(), answers(items(second), "A", "A").answers()));
        assertEquals("DONE", done.reviewStatus());

        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
        assertDoesNotThrow(() -> getLesson.execute(user, lesson2));
    }

    @Test
    void aReviewThatRunsOutOfPackagesEndsAndOpensTheSecondLesson() {
        UUID user = UUID.randomUUID();
        PackageVersion only = practiceSet(kpA, kpA);
        topics.execute(user);
        exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));
        var attempt = start.execute(user, lesson1, only.packageId());
        var submitted = practice.execute(user, attempt.attemptId(), answers(items(only), "A", "B"));
        UUID reviewId = submitted.reviewsCreated().getFirst().reviewId();
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> getLesson.execute(user, lesson2)).getCode());

        // The only package is revealed, so the review ends instead of leaving the learner stuck.
        assertEquals("SKIPPED", getReview.execute(user, reviewId).reviewStatus());

        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
        assertDoesNotThrow(() -> getLesson.execute(user, lesson2));
    }

    @Test
    void practiceSetListingRejectsAnUnownedPaywalledLesson() {
        var target = practiceListingLesson(4, false, false, false);
        when(access.premium()).thenReturn(false);

        LearningGateException error = assertThrows(LearningGateException.class,
                () -> practiceSets.execute(target.userId(), target.lessonId()));

        assertEquals("PAYWALL", error.getCode());
    }

    @Test
    void completedLessonPracticeSetsRemainListableAfterPremiumExpires() {
        var target = practiceListingLesson(4, true, false, false);
        // Durable completion remains reviewable after Premium expires.
        when(access.premium()).thenReturn(false);

        var result = practiceSets.execute(target.userId(), target.lessonId());

        assertTrue(result.lessonCompleted());
        assertEquals(1, result.items().size());
    }

    @Test
    void premiumLearnerCanListPracticeSetsForAnUnownedPaywalledLesson() {
        var target = practiceListingLesson(4, false, false, false);
        when(access.premium()).thenReturn(true);

        var result = practiceSets.execute(target.userId(), target.lessonId());

        assertEquals(1, result.items().size());
    }

    @Test
    void accessOutageAllowsFreeOwnedGrandfatheredAndCompletedPracticeSetsButFailsClosedForPaywalledLessons() {
        when(access.premium()).thenThrow(new AccessUnavailableException(503));

        var free = practiceListingLesson(3, false, false, false);
        assertDoesNotThrow(() -> practiceSets.execute(free.userId(), free.lessonId()));

        var owned = practiceListingLesson(4, false, false, true);
        assertDoesNotThrow(() -> practiceSets.execute(owned.userId(), owned.lessonId()));

        var grandfathered = practiceListingLesson(4, false, true, false);
        assertDoesNotThrow(() -> practiceSets.execute(grandfathered.userId(), grandfathered.lessonId()));
        var completed = practiceListingLesson(4, true, false, false);
        var completedResult = assertDoesNotThrow(() -> practiceSets.execute(completed.userId(), completed.lessonId()));
        assertTrue(completedResult.lessonCompleted());

        var paywalled = practiceListingLesson(4, false, false, false);
        AccessUnavailableException error = assertThrows(AccessUnavailableException.class,
                () -> practiceSets.execute(paywalled.userId(), paywalled.lessonId()));
        assertTrue(error.getMessage().contains("status 503"));
        verify(access, times(1)).premium();
    }

    private PracticeListingTarget practiceListingLesson(int skillPosition, boolean completed, boolean grandfathered,
                                                         boolean owned) {
        UUID courseId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID kpId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        var course = new Course(courseId, "C60", "Band 6.0", new BigDecimal("6.0"), true, true);
        var topic = new Topic(topicId, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(kpId, "READ", "Reading", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, false, course);
        var summary = new LessonSummary(lessonId, topicId, "L", "Lesson", null, 1, List.of(kpId), List.of(),
                Set.of(LearningSkill.READING), null, courseId, LearningSkill.READING, skillPosition);
        var lesson = new Lesson(lessonId, topicId, "L", "Lesson", null, 1, List.of(kpId), List.of(),
                LearningSkill.READING, Set.of(LearningSkill.READING), courseId, skillPosition);
        when(content.getTopicSequence()).thenReturn(List.of(topic));
        when(content.getTopicLessons(topicId)).thenReturn(List.of(summary));
        when(content.getLesson(lessonId)).thenReturn(lesson);
        when(content.lessonPracticeSets(lessonId)).thenReturn(List.of(new LessonPracticeSet(packageId, versionId,
                "PS", "Practice", 2, List.of(), null)));
        topics.execute(userId);
        if (owned) {
            jdbc.update("INSERT INTO learner_course_unlocks (user_id, course_id, skill, point_cost, request_id, status) "
                    + "VALUES (?, ?, 'READING', 50, ?, 'ACTIVE')", userId, courseId, UUID.randomUUID());
        }
        if (completed || grandfathered) {
            jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, "
                            + "knowledge_point_ids, completed_at, grandfathered) "
                            + "VALUES (?, ?, ?, 1, ARRAY[CAST(? AS uuid)], ?, ?)", userId, lessonId, topicId,
                    kpId.toString(), completed ? java.sql.Timestamp.from(java.time.Instant.now()) : null,
                    grandfathered);
        }
        return new PracticeListingTarget(userId, lessonId);
    }

    private record PracticeListingTarget(UUID userId, UUID lessonId) {}

    @Test
    void aFirstLessonWithoutPracticeOpensTheSecondAndAnOpenedLessonStaysOpenWhenSetsArePublishedLater() {
        UUID user = UUID.randomUUID();
        topics.execute(user);
        exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));

        assertDoesNotThrow(() -> getLesson.execute(user, lesson2));
        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));

        practiceSet(kpA, kpA);

        assertDoesNotThrow(() -> getLesson.execute(user, lesson2));
        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
    }

    @Test
    void aPendingReviewLocksTheNextLessonInTheListWhileTheLessonThatOwnsItStaysReadable() {
        UUID user = UUID.randomUUID();
        topics.execute(user);
        exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));
        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
        UUID reviewId = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status) "
                + "VALUES (?, ?, ?, ?, 'PENDING')", reviewId, user, kpA, lesson1);

        TopicLessonsResult listed = topicLessons.execute(user, topic);

        assertEquals(List.of("COMPLETED", "LOCKED"),
                listed.lessons().stream().map(TopicLessonsResult.LessonSummary::status).toList());
        assertNull(listed.lessons().get(0).blockingReviewId());
        assertEquals(reviewId, listed.lessons().get(1).blockingReviewId());
        assertDoesNotThrow(() -> getLesson.execute(user, lesson1));
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> getLesson.execute(user, lesson2)).getCode());
        // Reading is open, doing new work is not.
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> complete.execute(user, lesson1)).getCode());
    }

    @Test
    void openingTheFirstLessonDoesNotAskContentForPractice() {
        UUID user = UUID.randomUUID();
        topics.execute(user);
        clearInvocations(content);

        assertDoesNotThrow(() -> getLesson.execute(user, lesson1));

        verify(content, never()).topicPracticeSets(any());
    }

    private List<String> statuses(UUID user) {
        TopicLessonsResult listed = topicLessons.execute(user, topic);
        return listed.lessons().stream().map(TopicLessonsResult.LessonSummary::status).toList();
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
}
