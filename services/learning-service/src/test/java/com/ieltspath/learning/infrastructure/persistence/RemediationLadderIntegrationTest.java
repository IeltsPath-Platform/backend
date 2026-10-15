package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.command.SubmitReviewCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.result.LessonPracticeSetsResult;
import com.ieltspath.learning.application.usecase.*;
import com.ieltspath.learning.domain.vo.PracticeStatus;
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
    @Autowired StartPracticeAttemptUseCase start;
    @Autowired SubmitPracticeAttemptUseCase practice;
    @Autowired GetReviewUseCase getReview;
    @Autowired GetLessonPracticeSetsUseCase practiceSets;
    @Autowired SubmitReviewUseCase submitReview;
    @Autowired SubmitTheoryCheckUseCase theoryCheck;
    @MockitoBean LearningContentClient content;

    private final UUID topic = UUID.randomUUID();
    private final UUID lesson = UUID.randomUUID();
    private final UUID kpA = UUID.randomUUID();
    private final UUID kpB = UUID.randomUUID();
    private final Map<UUID, PackageVersion> versions = new LinkedHashMap<>();
    /** The sets bound to the lesson; the rest of {@link #versions} is review-only material. */
    private final Set<UUID> lessonPackages = new HashSet<>();

    @BeforeEach
    void stubContent() {
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(topic, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(kpA, "A", "A", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(kpB, "B", "B", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, true)));
        when(content.getTopicLessons(topic)).thenReturn(List.of(new LessonSummary(lesson, topic, "L1", "Lesson",
                null, 1, List.of(kpA, kpB))));
        when(content.getLesson(lesson)).thenReturn(new Lesson(lesson, topic, "L1", "Lesson", null,
                "Theory of the lesson", 1, List.of(kpA, kpB), LearningSkill.READING));
        when(content.lessonPracticeSets(lesson)).thenAnswer(invocation -> versions.values().stream()
                .filter(version -> lessonPackages.contains(version.packageId()))
                .map(version -> new LessonPracticeSet(version.packageId(), version.packageVersionId(), "PS",
                        "Practice", 2, List.of(), null)).toList());
        when(content.topicPracticeSets(topic)).thenAnswer(invocation -> new TopicPracticeSets(List.of(
                new TopicPracticeSets.LessonSets(lesson, content.lessonPracticeSets(lesson)))));
        when(content.getPackageVersion(any())).thenAnswer(invocation -> versions.get(invocation.<UUID>getArgument(0)));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenAnswer(invocation -> {
            List<UUID> kps = invocation.getArgument(0);
            List<UUID> excluded = invocation.getArgument(1);
            Map<UUID, Integer> available = new HashMap<>();
            for (UUID kp : kps) {
                available.put(kp, (int) versions.values().stream()
                        .filter(version -> !excluded.contains(version.packageId()) && covers(version, kp)).count());
            }
            return available;
        });
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(1), any())).thenAnswer(invocation -> {
            UUID kp = invocation.getArgument(0);
            List<UUID> excluded = invocation.getArgument(1);
            return versions.values().stream()
                    .filter(version -> !excluded.contains(version.packageId()) && covers(version, kp)).limit(1)
                    .map(version -> new PracticeSet(version.packageId(), version.packageVersionId(), "PS", 2, 2))
                    .toList();
        });
    }

    @Test
    void failedReviewSetsLeadToTheoryOnceThenNewSetsUntilNoneIsLeftToSkip() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA);
        PackageVersion other = practiceSet(kpA, kpA);
        PackageVersion second = reviewSet(kpA, kpA);
        PackageVersion third = reviewSet(kpA, kpA);
        topics.execute(user);

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
        assertEquals(List.of("Theory of the lesson"), theory.theory());
        assertEquals("LESSON", theory.theoryScope());
        assertEquals("SECOND_FAIL", theory.theoryReason());
        assertTrue(theory.quickCheck().isEmpty());
        var blocked = assertThrows(LearningRequestException.class, () -> submitReview.execute(user, reviewId,
                new SubmitReviewCommand(withSet.set().reviewSetId(), UUID.randomUUID(),
                        answers(items(second), "A", "A").answers())));
        assertEquals("THEORY_REQUIRED", blocked.getCode());

        int evidenceBefore = count("kp_evidence");
        // The theory is the lesson summary: there is nothing to answer, so an answer is refused.
        var refused = assertThrows(LearningRequestException.class, () -> theoryCheck.execute(user, reviewId,
                new SubmitExerciseCommand(UUID.randomUUID(),
                        List.of(new SubmitExerciseCommand.Answer(UUID.randomUUID(), "B")))));
        assertEquals("INVALID_ANSWERS", refused.getCode());

        var check = new SubmitExerciseCommand(UUID.randomUUID(), List.of());
        var checked = theoryCheck.execute(user, reviewId, check);
        assertEquals("PRACTICE", checked.stage());
        assertTrue(checked.results().isEmpty());
        assertEquals(checked, theoryCheck.execute(user, reviewId, check));
        assertEquals(evidenceBefore, count("kp_evidence"));

        var next = getReview.execute(user, reviewId);
        assertEquals(third.packageId(), next.set().packageId());
        // A second failure does not skip the review or read the theory again: it goes straight to a new set.
        var failedAgain = submitReview.execute(user, reviewId, new SubmitReviewCommand(next.set().reviewSetId(),
                UUID.randomUUID(), answers(items(third), "B", "B").answers()));
        assertEquals("PENDING", failedAgain.reviewStatus());
        assertEquals("PRACTICE", failedAgain.stage());
        assertEquals(2, failedAgain.failedSets());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? AND status = 'PENDING'",
                Integer.class, user));
        assertEquals(PracticeStatus.REQUIRED, practiceSets.execute(user, lesson).practiceStatus());

        // Only when no unrevealed package is left does the review let the learner continue.
        var last = getReview.execute(user, reviewId);
        assertEquals("SKIPPED", last.reviewStatus());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? AND status = 'PENDING'",
                Integer.class, user));
        // The review used only review-only sets; the lesson's other required set is still to practise.
        assertEquals(PracticeStatus.REQUIRED, practiceSets.execute(user, lesson).practiceStatus());
        assertFalse(lessonCompleted(user));
        var again = start.execute(user, lesson, other.packageId());
        var otherResult = practice.execute(user, again.attemptId(), answers(items(other), "A", "A"));
        assertTrue(otherResult.countedAsEvidence());
        // Finishing the last required set passes the Practice, which completes the lesson.
        assertEquals(PracticeStatus.PASSED, practiceSets.execute(user, lesson).practiceStatus());
        assertTrue(lessonCompleted(user));
    }

    @Test
    void aReviewNeverUsesASetOfItsLesson() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA);
        PackageVersion other = practiceSet(kpA, kpA);
        PackageVersion spare = reviewSet(kpA, kpA);
        topics.execute(user);

        var attempt = start.execute(user, lesson, first.packageId());
        var submitted = practice.execute(user, attempt.attemptId(), answers(items(first), "A", "B"));
        UUID reviewId = submitted.reviewsCreated().getFirst().reviewId();
        var withSet = getReview.execute(user, reviewId);
        assertEquals(spare.packageId(), withSet.set().packageId());
        var passed = submitReview.execute(user, reviewId, new SubmitReviewCommand(withSet.set().reviewSetId(),
                UUID.randomUUID(), answers(items(spare), "A", "A").answers()));
        assertEquals("DONE", passed.reviewStatus());

        // The learner has not practised the lesson's other set, and the review did not touch it.
        assertEquals(PracticeStatus.REQUIRED, practiceSets.execute(user, lesson).practiceStatus());
        assertFalse(lessonCompleted(user));
        var otherAttempt = start.execute(user, lesson, other.packageId());
        var otherResult = practice.execute(user, otherAttempt.attemptId(), answers(items(other), "A", "A"));
        assertTrue(otherResult.countedAsEvidence());
        assertEquals(PracticeStatus.PASSED, practiceSets.execute(user, lesson).practiceStatus());
        assertTrue(lessonCompleted(user));
    }

    @Test
    void aFailedFirstSetIsNotPractisedAgainOnceItsReviewIsDone() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA, kpA, kpA);
        PackageVersion other = practiceSet(kpA, kpA);
        PackageVersion spare = reviewSet(kpA, kpA);
        topics.execute(user);

        // Practice 1 is failed outright, so the review it creates opens at the theory.
        var attempt = start.execute(user, lesson, first.packageId());
        var failedFirst = practice.execute(user, attempt.attemptId(), answers(items(first), "B", "B", "B", "B"));
        assertFalse(failedFirst.passed());
        assertEquals(1, failedFirst.reviewsCreated().size());
        assertEquals("THEORY", failedFirst.reviewsCreated().getFirst().stage());
        UUID reviewId = failedFirst.reviewsCreated().getFirst().reviewId();
        assertEquals(PracticeStatus.REQUIRED, practiceSets.execute(user, lesson).practiceStatus());

        theoryCheck.execute(user, reviewId, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));
        var withSet = getReview.execute(user, reviewId);
        assertEquals(spare.packageId(), withSet.set().packageId());
        var done = submitReview.execute(user, reviewId, new SubmitReviewCommand(withSet.set().reviewSetId(),
                UUID.randomUUID(), answers(items(spare), "A", "A").answers()));
        assertEquals("DONE", done.reviewStatus());

        // Practice 1 stays as it was: attempted, never required again. Only the lesson's other set is left.
        var afterReview = practiceSets.execute(user, lesson);
        assertEquals(PracticeStatus.REQUIRED, afterReview.practiceStatus());
        assertEquals("ATTEMPTED", status(afterReview, first));
        assertEquals("AVAILABLE", status(afterReview, other));
        assertFalse(lessonCompleted(user));

        var otherAttempt = start.execute(user, lesson, other.packageId());
        practice.execute(user, otherAttempt.attemptId(), answers(items(other), "A", "A"));
        var finished = practiceSets.execute(user, lesson);
        assertEquals(PracticeStatus.PASSED, finished.practiceStatus());
        assertEquals("REVIEW_FINISHED", finished.practicePassReason().name());
        assertTrue(lessonCompleted(user));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM practice_attempts WHERE user_id = ? AND package_id = ?",
                Integer.class, user, first.packageId()));
    }

    private static String status(LessonPracticeSetsResult result, PackageVersion set) {
        return result.items().stream().filter(item -> item.packageId().equals(set.packageId())).findFirst()
                .orElseThrow().status();
    }

    @Test
    void failingTheLessonsOtherSetAfterAReviewStillCountsAndStartsAReviewWhileMaterialRemains() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA);
        PackageVersion other = practiceSet(kpA, kpA);
        PackageVersion spareOne = reviewSet(kpA, kpA);
        reviewSet(kpA, kpA);
        topics.execute(user);

        var attempt = start.execute(user, lesson, first.packageId());
        UUID reviewId = practice.execute(user, attempt.attemptId(), answers(items(first), "A", "B"))
                .reviewsCreated().getFirst().reviewId();
        var withSet = getReview.execute(user, reviewId);
        assertEquals(spareOne.packageId(), withSet.set().packageId());
        submitReview.execute(user, reviewId, new SubmitReviewCommand(withSet.set().reviewSetId(), UUID.randomUUID(),
                answers(items(spareOne), "A", "A").answers()));

        // The lesson's other set was never shown by a review, so it counts, and a review-only set is left.
        var otherAttempt = start.execute(user, lesson, other.packageId());
        var failed = practice.execute(user, otherAttempt.attemptId(), answers(items(other), "A", "B"));
        assertTrue(failed.countedAsEvidence());
        assertEquals(1, failed.reviewsCreated().size());
    }

    @Test
    void noReviewIsCreatedWhenEveryReviewOnlySetWasSeen() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA);
        PackageVersion other = practiceSet(kpA, kpA);
        PackageVersion spare = reviewSet(kpA, kpA);
        topics.execute(user);

        var attempt = start.execute(user, lesson, first.packageId());
        UUID reviewId = practice.execute(user, attempt.attemptId(), answers(items(first), "A", "B"))
                .reviewsCreated().getFirst().reviewId();
        var withSet = getReview.execute(user, reviewId);
        submitReview.execute(user, reviewId, new SubmitReviewCommand(withSet.set().reviewSetId(), UUID.randomUUID(),
                answers(items(spare), "A", "A").answers()));

        // The only review-only set was shown, so there is nothing left to review with: the failure still counts.
        var otherAttempt = start.execute(user, lesson, other.packageId());
        var failed = practice.execute(user, otherAttempt.attemptId(), answers(items(other), "A", "B"));
        assertTrue(failed.countedAsEvidence());
        assertTrue(failed.reviewsCreated().isEmpty());
    }

    private boolean lessonCompleted(UUID user) {
        return jdbc.queryForObject("SELECT count(*) FROM lesson_progress WHERE user_id = ? AND lesson_id = ? "
                + "AND completed_at IS NOT NULL", Integer.class, user, lesson) == 1;
    }

    @Test
    void newReviewsStartWithTheoryOnlyForLowScores() {
        UUID user = UUID.randomUUID();
        PackageVersion mixed = practiceSet(kpA, kpA, kpB, kpB);
        reviewSet(kpA, kpB);
        topics.execute(user);

        // kpA is half right, kpB is all wrong: only the low score starts a review at the theory.
        var attempt = start.execute(user, lesson, mixed.packageId());
        var submitted = practice.execute(user, attempt.attemptId(), answers(items(mixed), "A", "B", "B", "B"));
        Map<UUID, String> stages = new HashMap<>();
        Map<UUID, String> reasons = new HashMap<>();
        for (var created : submitted.reviewsCreated()) {
            stages.put(created.knowledgePointId(), created.stage());
            reasons.put(created.knowledgePointId(), jdbc.queryForObject(
                    "SELECT theory_reason FROM review_items WHERE id = ?", String.class, created.reviewId()));
        }
        assertEquals(Map.of(kpA, "PRACTICE", kpB, "THEORY"), stages);
        assertNull(reasons.get(kpA));
        assertEquals("LOW_SCORE", reasons.get(kpB));

        // A review from an assessment result opens at the practice, with a set.
        jdbc.update("UPDATE review_items SET status = 'DONE' WHERE user_id = ?", user);
        UUID assessmentReview = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind) "
                + "VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'ASSESSMENT')", assessmentReview, user, kpA, lesson);
        var opened = getReview.execute(user, assessmentReview);
        assertEquals("PRACTICE", opened.stage());
        assertNull(opened.theoryReason());
        assertNotNull(opened.set());
    }

    /** A set bound to the lesson: practice the learner must do. */
    private PackageVersion practiceSet(UUID... kps) {
        PackageVersion version = reviewSet(kps);
        lessonPackages.add(version.packageId());
        return version;
    }

    /** A set outside the lesson: material for reviews only. */
    private PackageVersion reviewSet(UUID... kps) {
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

    private static boolean covers(PackageVersion version, UUID kp) {
        return version.sections().stream().flatMap(section -> section.items().stream())
                .anyMatch(item -> item.knowledgePointMappings().stream()
                        .anyMatch(mapping -> mapping.knowledgePointId().equals(kp)));
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
