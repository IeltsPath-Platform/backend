package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.command.AssessmentResult.ItemResult;
import com.ieltspath.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.command.SubmitReviewCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.result.ReviewBasketResult;
import com.ieltspath.learning.application.service.ReviewReevaluation;
import com.ieltspath.learning.application.service.ErrorTrapReviews;
import com.ieltspath.learning.application.usecase.*;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.BeforeEach;
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
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/** The review basket, optional reviews, and the rule that only blocking reviews hold a learner back. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class ReviewBasketIntegrationTest {
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
    @Autowired GetReviewBasketUseCase basket;
    @Autowired StartBasketReviewUseCase start;
    @Autowired GetReviewUseCase getReview;
    @Autowired SubmitReviewUseCase submitReview;
    @Autowired GetLessonUseCase getLesson;
    @Autowired CompleteLessonUseCase completeLesson;
    @Autowired ReviewReevaluation reevaluation;
    @Autowired ErrorTrapReviews errorTrapReviews;
    @Autowired ApplyAssessmentResultUseCase apply;
    @MockitoBean LearningContentClient content;

    private final UUID topic = UUID.randomUUID();
    private final UUID lesson = UUID.randomUUID();
    private final UUID block = UUID.randomUUID();
    private final UUID kpConsolidate = UUID.randomUUID();
    private final UUID kpDue = UUID.randomUUID();
    private final UUID kpLater = UUID.randomUUID();
    private final UUID kpPriority = UUID.randomUUID();
    private final Map<UUID, PackageVersion> versions = new LinkedHashMap<>();
    private Map<UUID, Integer> availability = Map.of();

    @BeforeEach
    void stubContent() {
        List<UUID> kps = List.of(kpConsolidate, kpDue, kpLater, kpPriority);
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(topic, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(kpConsolidate, "CONSOLIDATE_ME", "Consolidate me", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(kpDue, "DUE_ME", "Due me", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(kpLater, "LATER", "Later", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(kpPriority, "PRIORITY_ME", "Priority", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, true, new Course(UUID.randomUUID(), "C55", "Band 5.5",
                new BigDecimal("5.5"), false))));
        when(content.getTopicLessons(topic)).thenReturn(List.of(new LessonSummary(lesson, topic, "L1", "Lesson one",
                null, 1, kps, List.of(block))));
        when(content.getLesson(lesson)).thenReturn(new Lesson(lesson, topic, "L1", "Lesson one", null, 1, kps,
                List.of(new Block(block, "EXERCISE", 1, null, null, List.of(), List.of(
                        lessonQuestion(UUID.randomUUID(), 1, kpConsolidate)), "EXERCISE", List.of(kpConsolidate))),
                LearningSkill.READING));
        when(content.getPackageVersion(any())).thenAnswer(invocation -> versions.get(invocation.<UUID>getArgument(0)));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3)))
                .thenAnswer(invocation -> availability);
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(1), any())).thenAnswer(invocation -> {
            List<UUID> excluded = invocation.getArgument(1);
            return versions.values().stream().filter(version -> !excluded.contains(version.packageId())).limit(1)
                    .map(version -> new PracticeSet(version.packageId(), version.packageVersionId(), "PS", 2, 2))
                    .toList();
        });
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(1), any(), any())).thenAnswer(invocation -> {
            List<UUID> excluded = invocation.getArgument(1);
            return versions.values().stream().filter(version -> !excluded.contains(version.packageId())).limit(1)
                    .map(version -> new PracticeSet(version.packageId(), version.packageVersionId(), "PS", 3, 3,
                            null, null, 2))
                    .toList();
        });
    }

    /** A learner with a completed lesson and one profile per basket case. */
    private UUID learner() {
        UUID user = UUID.randomUUID();
        topics.execute(user);
        jdbc.update("""
                INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, knowledge_point_ids,
                                             completed_at)
                VALUES (?, ?, ?, 1, ARRAY[?, ?, ?, ?]::uuid[], now())
                """, user, lesson, topic, kpConsolidate, kpDue, kpLater, kpPriority);
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        profile(user, kpConsolidate, "0.650", "CONSOLIDATE", now.plus(3, ChronoUnit.DAYS));
        profile(user, kpDue, "0.850", "MAINTAIN", now.minus(1, ChronoUnit.DAYS));
        profile(user, kpLater, "0.900", "MAINTAIN", now.plus(20, ChronoUnit.DAYS));
        profile(user, kpPriority, "0.200", "PRIORITY", null);
        return user;
    }

    private void profile(UUID user, UUID kp, String mastery, String tier, Instant nextReviewAt) {
        jdbc.update("""
                INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count,
                    success_streak, last_evidence_at, next_review_at)
                VALUES (?, ?, ?::numeric, ?, 5, 1, now(), ?)
                """, user, kp, mastery, tier, nextReviewAt == null ? null : Timestamp.from(nextReviewAt));
    }

    @Test
    void theBasketListsTheRequiredReviewTheConsolidatePointsAndTheDueOnes() {
        UUID user = learner();
        practiceSet(kpConsolidate, kpConsolidate);
        availability = Map.of(kpConsolidate, 1, kpDue, 0);
        UUID blockingId = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, "
                + "trigger_kind) VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'PRACTICE')", blockingId, user,
                kpPriority, lesson);

        ReviewBasketResult result = basket.execute(user);

        assertEquals(List.of(kpPriority), result.required().stream()
                .map(ReviewBasketResult.RequiredItem::knowledgePointId).toList());
        assertEquals(blockingId, result.required().getFirst().reviewId());
        assertEquals("PRIORITY_ME", result.required().getFirst().code());

        assertEquals(List.of(kpConsolidate), result.consolidate().stream()
                .map(ReviewBasketResult.OptionalItem::knowledgePointId).toList());
        var consolidate = result.consolidate().getFirst();
        assertEquals("CONSOLIDATE", consolidate.reason());
        assertEquals(0.65, consolidate.mastery());
        assertTrue(consolidate.hasUnrevealedSet());

        assertEquals(List.of(kpDue), result.due().stream()
                .map(ReviewBasketResult.OptionalItem::knowledgePointId).toList());
        assertEquals("DUE", result.due().getFirst().reason());
        assertFalse(result.due().getFirst().hasUnrevealedSet(), "Content has no unseen set for this point");
        assertFalse(result.consolidate().stream().anyMatch(item -> item.knowledgePointId().equals(kpLater)),
                "a maintained point that is not due yet is not in the basket");
    }

    @Test
    void optionalPendingPointsAppearExactlyOnceAcrossTheExistingAndWeakGroups() {
        UUID user = learner();
        for (UUID point : List.of(kpConsolidate, kpDue, kpLater, kpPriority)) {
            jdbc.update("""
                    INSERT INTO review_items(id, user_id, knowledge_point_id, lesson_id, status, skill,
                                             trigger_kind, blocking)
                    VALUES (?, ?, ?, ?, 'PENDING', 'WRITING', 'WRITING_CRITERION', FALSE)
                    """, UUID.randomUUID(), user, point, lesson);
        }
        ReviewBasketResult result = basket.execute(user);
        assertTrue(result.required().isEmpty());
        assertEquals(List.of(kpConsolidate), result.consolidate().stream().map(item -> item.knowledgePointId()).toList());
        assertEquals(List.of(kpDue), result.due().stream().map(item -> item.knowledgePointId()).toList());
        assertEquals(List.of(kpPriority, kpLater), result.weak().stream().map(item -> item.knowledgePointId()).toList());
        assertTrue(result.weak().stream().allMatch(item -> item.reviewId() != null
                && item.reason().equals("WRITING_CRITERION") && item.hasUnrevealedSet()));

        jdbc.update("UPDATE kp_mastery_profiles SET tier='INSUFFICIENT' WHERE user_id=? AND knowledge_point_id=?",
                user, kpPriority);
        assertEquals("INSUFFICIENT", basket.execute(user).weak().getFirst().tier());
    }

    @Test
    void startingAPointCreatesAnOptionalReviewWithOneSetThatBlocksNothing() {
        UUID user = learner();
        PackageVersion only = practiceSet(kpConsolidate, kpConsolidate);

        var review = start.execute(user, kpConsolidate);

        assertEquals("PENDING", review.reviewStatus());
        assertFalse(review.blocking());
        assertEquals(only.packageId(), review.set().packageId());
        assertEquals("PRACTICE", review.stage());
        assertEquals(false, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?", Boolean.class,
                review.reviewId()));
        assertEquals("CONSOLIDATE", jdbc.queryForObject("SELECT trigger_kind FROM review_items WHERE id = ?",
                String.class, review.reviewId()));
        assertDoesNotThrow(() -> getLesson.execute(user, lesson), "an optional review never locks a lesson");
        assertEquals(review.reviewId(), basket.execute(user).consolidate().getFirst().reviewId());
    }

    @Test
    void anOptionalReviewIsOneSetAndAFailedSetEndsItWithoutTheory() {
        UUID user = learner();
        PackageVersion only = practiceSet(kpConsolidate, kpConsolidate);
        var review = start.execute(user, kpConsolidate);

        var result = submitReview.execute(user, review.reviewId(), new SubmitReviewCommand(review.set().reviewSetId(),
                UUID.randomUUID(), answers(items(only), "B", "B").answers()));

        assertEquals("SKIPPED", result.reviewStatus());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? AND status = 'PENDING'",
                Integer.class, user));
        assertEquals("PRACTICE", result.stage());
    }

    @Test
    void aPassedOptionalReviewIsDoneAndFeedsTheMasteryProfile() {
        UUID user = learner();
        PackageVersion only = practiceSet(kpConsolidate, kpConsolidate);
        var review = start.execute(user, kpConsolidate);

        var result = submitReview.execute(user, review.reviewId(), new SubmitReviewCommand(review.set().reviewSetId(),
                UUID.randomUUID(), answers(items(only), "A", "A").answers()));

        assertEquals("DONE", result.reviewStatus());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND source = 'review_set'",
                Integer.class, user));
        assertEquals(2, jdbc.queryForObject("SELECT evidence_count FROM kp_mastery_profiles WHERE user_id = ? "
                + "AND knowledge_point_id = ?", Integer.class, user, kpConsolidate));
    }

    @Test
    void startingAPointThatAlreadyHasABlockingReviewReturnsThatReview() {
        UUID user = learner();
        practiceSet(kpPriority, kpPriority);
        UUID blockingId = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, "
                + "trigger_kind) VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'PRACTICE')", blockingId, user,
                kpPriority, lesson);

        var review = start.execute(user, kpPriority);

        assertEquals(blockingId, review.reviewId());
        assertTrue(review.blocking());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ?", Integer.class, user));
    }

    @Test
    void aPointWithNoUnseenSetAnswers409AndCreatesNothing() {
        UUID user = learner();

        var error = assertThrows(LearningRequestException.class, () -> start.execute(user, kpConsolidate));

        assertEquals(409, error.getStatus());
        assertEquals("NO_UNREVEALED_SET", error.getCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ?", Integer.class, user));
    }

    @Test
    void onlyConsolidateAndDuePointsCanBeStarted() {
        UUID user = learner();
        practiceSet(kpLater, kpLater);

        assertEquals("NOT_IN_REVIEW_BASKET", assertThrows(LearningRequestException.class,
                () -> start.execute(user, kpLater)).getCode());
        assertEquals("NOT_IN_REVIEW_BASKET", assertThrows(LearningRequestException.class,
                () -> start.execute(user, kpPriority)).getCode());
        assertEquals("NOT_IN_REVIEW_BASKET", assertThrows(LearningRequestException.class,
                () -> start.execute(user, UUID.randomUUID())).getCode());
    }

    @Test
    void anErrorPatternReviewUsesTheCurrentMainTrapAndReturnsItsFixHint() {
        UUID user = learner();
        UUID trap = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, parent_kp_id, error_code) "
                + "VALUES (?, ?, FALSE, ?, ?)", trap, topic, kpConsolidate, "TFNG_NG_AS_FALSE");
        profile(user, trap, "0.200", "PRIORITY", null);
        PackageVersion set = practiceSet(kpConsolidate, kpConsolidate, kpConsolidate);
        when(content.activeErrorTypes()).thenReturn(List.of(new ErrorType("TFNG_NG_AS_FALSE", "Wrong distinction",
                "COMPREHENSION", 1, "Separate false from not stated")));

        var review = start.execute(user, kpConsolidate);

        assertEquals("TFNG_NG_AS_FALSE", review.errorTrap().code());
        assertEquals("Wrong distinction", review.errorTrap().name());
        assertEquals("Separate false from not stated", review.errorTrap().fixHint());
        assertEquals(set.packageId(), review.set().packageId());
        assertFalse(review.blocking(), "ERROR_PATTERN reviews are optional and detail carries that state");
        org.mockito.Mockito.verify(content, org.mockito.Mockito.times(2)).searchPracticeSets(eq(kpConsolidate), anyList(), eq(3), eq(1),
                eq(lesson), eq("TFNG_NG_AS_FALSE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DONE", "SKIPPED"})
    void completedOrSkippedReviewDoesNotLookUpTrapText(String status) {
        UUID user = learner();
        UUID reviewId = UUID.randomUUID();
        UUID trap = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, parent_kp_id, error_code) "
                + "VALUES (?, ?, FALSE, ?, 'TFNG_NG_AS_FALSE')", trap, topic, kpConsolidate);
        profile(user, trap, "0.200", "PRIORITY", null);
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, "
                + "trigger_kind, blocking) VALUES (?, ?, ?, ?, ?, 'READING', 'ERROR_PATTERN', FALSE)",
                reviewId, user, kpConsolidate, lesson, status);

        assertDoesNotThrow(() -> getReview.execute(user, reviewId));

        org.mockito.Mockito.verify(content, org.mockito.Mockito.never()).activeErrorTypes();
    }

    @Test
    void aTrapKnowledgePointCannotBeStartedFromTheReviewBasket() {
        UUID user = learner();
        UUID trap = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, parent_kp_id, error_code) "
                + "VALUES (?, ?, FALSE, ?, ?)", trap, topic, kpConsolidate, "TFNG_NG_AS_FALSE");
        profile(user, trap, "0.200", "PRIORITY", null);

        var error = assertThrows(LearningRequestException.class, () -> start.execute(user, trap));
        assertEquals(409, error.getStatus());
        assertEquals("NOT_IN_REVIEW_BASKET", error.getCode());
    }

    @Test
    void aPriorityTrapCreatesOneOptionalReviewForItsParent() {
        UUID user = learner();
        UUID trap = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, parent_kp_id, error_code) "
                + "VALUES (?, ?, FALSE, ?, ?)", trap, topic, kpConsolidate, "TFNG_NG_AS_FALSE");
        profile(user, trap, "0.200", "PRIORITY", null);
        practiceSet(kpConsolidate, kpConsolidate, kpConsolidate);

        errorTrapReviews.create(user, lesson, List.of(trap));
        errorTrapReviews.create(user, lesson, List.of(trap));

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? AND "
                + "knowledge_point_id = ? AND status = 'PENDING'", Integer.class, user, kpConsolidate));
        assertEquals("ERROR_PATTERN", jdbc.queryForObject("SELECT trigger_kind FROM review_items WHERE user_id = ? "
                + "AND knowledge_point_id = ?", String.class, user, kpConsolidate));
        assertEquals(false, jdbc.queryForObject("SELECT blocking FROM review_items WHERE user_id = ? "
                + "AND knowledge_point_id = ?", Boolean.class, user, kpConsolidate));

        jdbc.update("UPDATE review_items SET status = 'DONE', done_at = now() WHERE user_id = ? "
                + "AND knowledge_point_id = ?", user, kpConsolidate);
        errorTrapReviews.create(user, lesson, List.of(trap));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? AND "
                + "knowledge_point_id = ?", Integer.class, user, kpConsolidate));
    }

    @Test
    void anErrorPatternBasketItemShowsTheCurrentMainError() {
        UUID user = learner();
        UUID trap = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, parent_kp_id, error_code) "
                + "VALUES (?, ?, FALSE, ?, ?)", trap, topic, kpConsolidate, "TFNG_NG_AS_FALSE");
        profile(user, trap, "0.200", "PRIORITY", null);
        jdbc.update("UPDATE kp_mastery_profiles SET tier = 'MAINTAIN', next_review_at = now() + interval '10 days' "
                + "WHERE user_id = ? AND knowledge_point_id = ?", user, kpConsolidate);
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind, "
                + "blocking) VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'ERROR_PATTERN', FALSE)",
                UUID.randomUUID(), user, kpConsolidate, lesson);
        when(content.activeErrorTypes()).thenReturn(List.of(new ErrorType("TFNG_NG_AS_FALSE", "Wrong distinction",
                "COMPREHENSION", 1, "Separate false from not stated")));

        var item = basket.execute(user).weak().stream().filter(point -> point.knowledgePointId().equals(kpConsolidate))
                .findFirst().orElseThrow();

        assertEquals("ERROR_PATTERN", item.reason());
        assertEquals("TFNG_NG_AS_FALSE", item.errorTrap().code());
        assertEquals("Wrong distinction", item.errorTrap().name());
        assertEquals("Separate false from not stated", item.errorTrap().fixHint());
    }

    @Test
    void aWrongAnswerInTheMainFlowUpgradesTheOptionalReviewOfThatPointToBlocking() {
        UUID user = learner();
        practiceSet(kpConsolidate, kpConsolidate);
        var optional = start.execute(user, kpConsolidate);
        for (int index = 0; index < 3; index++) {
            jdbc.update("INSERT INTO kp_evidence (id, user_id, kp_id, correct, source, source_reference_id) "
                    + "VALUES (?, ?, ?, FALSE, 'lesson_exercise', ?)", UUID.randomUUID(), user, kpConsolidate,
                    UUID.randomUUID());
        }

        reevaluation.execute(user, Set.of(kpConsolidate), Set.of(kpConsolidate));

        assertEquals(true, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?", Boolean.class,
                optional.reviewId()));
        assertEquals("ASSESSMENT", jdbc.queryForObject("SELECT trigger_kind FROM review_items WHERE id = ?",
                String.class, optional.reviewId()));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? AND status = 'PENDING'",
                Integer.class, user));
        assertDoesNotThrow(() -> getLesson.execute(user, lesson), "completed lessons stay readable for revision");
        assertEquals("REVIEW_REQUIRED", assertThrows(com.ieltspath.learning.domain.exception.LearningGateException.class,
                () -> completeLesson.execute(user, lesson)).getCode());
    }

    @Test
    void aFreePracticeTestRaisesOnlyAnOptionalReviewAndNeverLocksALesson() {
        UUID user = learner();
        practiceSet(kpConsolidate, kpConsolidate);
        List<ItemResult> items = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            items.add(new ItemResult(UUID.randomUUID(), UUID.randomUUID(), false, BigDecimal.ZERO, BigDecimal.ONE,
                    List.of(new KnowledgePointJudgment(kpConsolidate, null))));
        }

        apply.execute(new AssessmentResult(UUID.randomUUID(), user, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 1, "OFFICIAL_PRACTICE", Instant.now(), items));

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ? "
                + "AND source = 'error_trap'", Integer.class, user),
                "official assessments do not record child trap evidence");
        assertEquals("FREE_PRACTICE_TEST", jdbc.queryForObject(
                "SELECT trigger_kind FROM review_items WHERE user_id = ? AND knowledge_point_id = ?", String.class,
                user, kpConsolidate));
        assertEquals(false, jdbc.queryForObject(
                "SELECT blocking FROM review_items WHERE user_id = ? AND knowledge_point_id = ?", Boolean.class,
                user, kpConsolidate));
        assertDoesNotThrow(() -> getLesson.execute(user, lesson));
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
