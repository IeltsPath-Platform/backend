package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.service.LessonAccessGate;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticeReviewCandidate;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class JdbcReviewItemRepositoryIntegrationTest {
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

    @Autowired ReviewItemRepository reviews;
    @Autowired DiagnosticRepository diagnostics;
    @Autowired JdbcTemplate jdbc;

    private UUID review(UUID user, UUID kp, String trigger, boolean blocking) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind, "
                + "blocking, stage) VALUES (?, ?, ?, ?, 'PENDING', 'READING', ?, ?, 'PRACTICE')",
                id, user, kp, UUID.randomUUID(), trigger, blocking);
        return id;
    }

    private PracticeReviewCandidate candidate(UUID kp, boolean blocking) {
        return new PracticeReviewCandidate(UUID.randomUUID(), UUID.randomUUID(), kp, LearningSkill.READING,
                null, 0.5, ReviewStage.PRACTICE, null, blocking);
    }

    @Test
    void insertPracticePendingStoresTheBlockingFlag() {
        UUID user = UUID.randomUUID();
        var optional = candidate(UUID.randomUUID(), false);
        var required = candidate(UUID.randomUUID(), true);

        reviews.insertPracticePending(user, List.of(optional, required));

        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, optional.reviewId()));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, required.reviewId()));
    }

    @Test
    void promotionTouchesOnlyReviewsTheSystemRaised() {
        UUID user = UUID.randomUUID();
        UUID practice = UUID.randomUUID();
        UUID errorPattern = UUID.randomUUID();
        UUID consolidate = UUID.randomUUID();
        UUID alreadyBlocking = UUID.randomUUID();
        UUID practiceId = review(user, practice, "PRACTICE", false);
        UUID errorId = review(user, errorPattern, "ERROR_PATTERN", false);
        UUID consolidateId = review(user, consolidate, "CONSOLIDATE", false);
        UUID blockingId = review(user, alreadyBlocking, "PRACTICE", true);

        var promoted = reviews.promoteSuggestionsToBlocking(user,
                List.of(practice, errorPattern, consolidate, alreadyBlocking));

        assertEquals(Set.of(practiceId, errorId), promoted.stream().map(item -> item.reviewId()).collect(
                java.util.stream.Collectors.toSet()));
        assertTrue(promoted.stream().allMatch(item -> item.stage() == ReviewStage.PRACTICE));
        assertEquals("PRACTICE", jdbc.queryForObject("SELECT trigger_kind FROM review_items WHERE id = ?",
                String.class, practiceId));
        assertEquals("ERROR_PATTERN", jdbc.queryForObject("SELECT trigger_kind FROM review_items WHERE id = ?",
                String.class, errorId));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, practiceId));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, errorId));
        assertNotNull(jdbc.queryForObject("SELECT failures_counted_from FROM review_items WHERE id = ?",
                java.sql.Timestamp.class, practiceId));
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, consolidateId));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, blockingId));
    }

    @Test
    void promotionStartsANewFailureBaselineAndKeepsPreviouslyAssignedPackages() {
        UUID user = UUID.randomUUID();
        UUID kp = UUID.randomUUID();
        UUID reviewId = review(user, kp, "CONSOLIDATE", false);
        Set<UUID> oldPackages = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            UUID packageId = UUID.randomUUID();
            oldPackages.add(packageId);
            jdbc.update("INSERT INTO review_sets (id, review_item_id, user_id, package_id, package_version_id, "
                            + "submitted_at, passed, request_id, correct_count, total_count) "
                            + "VALUES (?, ?, ?, ?, ?, clock_timestamp(), FALSE, ?, 0, 2)",
                    UUID.randomUUID(), reviewId, user, packageId, UUID.randomUUID(), UUID.randomUUID());
        }
        jdbc.update("UPDATE review_items SET stage = 'THEORY', theory_reason = 'REPEATED_FAIL', unblocked_at = now() "
                + "WHERE id = ?", reviewId);

        reviews.promoteToBlocking(user, List.of(kp), "ASSESSMENT");

        var reloaded = reviews.findOwned(user, reviewId).orElseThrow();
        assertEquals(0, reloaded.failedSets());
        assertTrue(reloaded.blocking());
        assertFalse(reloaded.unblocked());
        assertEquals(ReviewStage.PRACTICE, reloaded.stage());
        assertNull(reloaded.theoryReason());
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM review_sets WHERE review_item_id = ?",
                Integer.class, reviewId));
        assertTrue(reviews.assignedPackageIds(user).containsAll(oldPackages));
        assertNotNull(jdbc.queryForObject("SELECT failures_counted_from FROM review_items WHERE id = ?",
                java.sql.Timestamp.class, reviewId));
    }

    @Test
    @Transactional
    void unblockingAPrerequisiteReviewReleasesWaitingReviewsWithoutRemovingHistory() {
        UUID user = UUID.randomUUID();
        UUID rootId = review(user, UUID.randomUUID(), "ASSESSMENT", true);
        UUID prerequisiteId = review(user, UUID.randomUUID(), "DIAGNOSTIC", true);
        jdbc.update("UPDATE review_items SET waiting_on_review_ids = ARRAY[CAST(? AS UUID)] WHERE id = ?",
                prerequisiteId, rootId);
        for (int i = 0; i < 3; i++) {
            jdbc.update("INSERT INTO review_sets (id, review_item_id, user_id, package_id, package_version_id, "
                            + "submitted_at, passed, request_id, correct_count, total_count) "
                            + "VALUES (?, ?, ?, ?, ?, clock_timestamp(), FALSE, ?, 0, 2)",
                    UUID.randomUUID(), prerequisiteId, user, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        }
        var prerequisite = reviews.findOwned(user, prerequisiteId).orElseThrow();
        var open = prerequisite.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        prerequisite.recordSetResult(open.id(), UUID.randomUUID(), 0, 2, 4);

        reviews.save(prerequisite);

        var reloaded = reviews.findOwned(user, prerequisiteId).orElseThrow();
        assertTrue(reloaded.unblocked());
        assertFalse(reloaded.blocking());
        assertTrue(diagnostics.pendingWaitingOn(rootId).isEmpty());
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM review_sets WHERE review_item_id = ?",
                Integer.class, prerequisiteId));
        assertEquals(0, jdbc.queryForObject("SELECT cardinality(waiting_on_review_ids) FROM review_items WHERE id = ?",
                Integer.class, rootId));

        reviews.promoteToBlocking(user, List.of(prerequisite.knowledgePointId()), "ASSESSMENT");
        assertTrue(diagnostics.pendingWaitingOn(rootId).isEmpty());
    }

    @Test
    void reviewsFromLessonAssessmentAndPracticeSourcesStopBlockingAfterTheFourthFailure() {
        for (String trigger : List.of("PRACTICE", "ASSESSMENT", "WRITING_CRITERION")) {
            UUID user = UUID.randomUUID();
            UUID kp = UUID.randomUUID();
            UUID reviewId = review(user, kp, trigger, true);
            for (int i = 0; i < 3; i++) {
                jdbc.update("INSERT INTO review_sets (id, review_item_id, user_id, package_id, package_version_id, "
                                + "submitted_at, passed, request_id, correct_count, total_count) "
                                + "VALUES (?, ?, ?, ?, ?, clock_timestamp(), FALSE, ?, 0, 2)",
                        UUID.randomUUID(), reviewId, user, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            }

            var item = reviews.findOwned(user, reviewId).orElseThrow();
            var open = item.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            item.recordSetResult(open.id(), UUID.randomUUID(), 0, 2, 4);
            reviews.save(item);

            var pending = reviews.findPending(user);
            assertEquals(1, pending.size());
            assertFalse(pending.getFirst().blocking(), trigger);
            assertTrue(pending.getFirst().unblocked(), trigger);
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> new LessonAccessGate().authorize(pending, null,
                    LearningSkill.READING, TopicStatus.IN_PROGRESS, true));
            assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM review_sets WHERE review_item_id = ?",
                    Integer.class, reviewId));
        }
    }

    @Test
    void promotingNothingReturnsNothing() {
        assertTrue(reviews.promoteSuggestionsToBlocking(UUID.randomUUID(), List.of()).isEmpty());
    }

    @Test
    void promotingAWritingCriterionReviewResetsItsFailureBaselineAndTheoryState() {
        UUID user = UUID.randomUUID();
        UUID kp = UUID.randomUUID();
        UUID id = review(user, kp, "WRITING_CRITERION", false);
        jdbc.update("UPDATE review_items SET stage = 'THEORY', theory_reason = 'LOW_SCORE', unblocked_at = now() "
                + "WHERE id = ?", id);

        var promoted = reviews.promoteCriterionReviewsToBlocking(user, List.of(kp), UUID.randomUUID());

        assertEquals(List.of(id), promoted.stream().map(item -> item.reviewId()).toList());
        assertNotNull(jdbc.queryForObject("SELECT failures_counted_from FROM review_items WHERE id = ?",
                java.sql.Timestamp.class, id));
        assertEquals("PRACTICE", jdbc.queryForObject("SELECT stage FROM review_items WHERE id = ?", String.class, id));
        assertNull(jdbc.queryForObject("SELECT theory_reason FROM review_items WHERE id = ?", String.class, id));
        assertNull(jdbc.queryForObject("SELECT unblocked_at FROM review_items WHERE id = ?", java.sql.Timestamp.class, id));
    }

    @Test
    void saveWritesTheBlockingFlag() {
        UUID user = UUID.randomUUID();
        UUID kp = UUID.randomUUID();
        UUID id = review(user, kp, "PRACTICE", false);
        var item = reviews.findOwned(user, id).orElseThrow();
        var set = item.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        item.recordSetResult(set.id(), UUID.randomUUID(), 0, 2);

        reviews.save(item);

        var reloaded = reviews.findOwned(user, id).orElseThrow();
        assertTrue(reloaded.blocking());
        assertEquals(ReviewStage.THEORY, reloaded.stage());
    }

    @Test
    void practiceStatesCarryTheBlockingFlag() {
        UUID user = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind, "
                        + "blocking, stage, unblocked_at) "
                        + "VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'PRACTICE', false, 'PRACTICE', now())",
                id, user, UUID.randomUUID(), lessonId);

        var states = reviews.findPracticeByLessons(user, List.of(lessonId));

        assertEquals(1, states.size());
        assertEquals(ReviewStatus.PENDING, states.getFirst().status());
        assertFalse(states.getFirst().blocking());
        assertTrue(states.getFirst().unblocked());
    }

    private void answeredSet(UUID user, UUID reviewId, int correct, int total) {
        jdbc.update("INSERT INTO review_sets (id, review_item_id, user_id, package_id, package_version_id, submitted_at, "
                + "passed, correct_count, total_count) VALUES (?, ?, ?, ?, ?, now(), ?, ?, ?)",
                UUID.randomUUID(), reviewId, user, UUID.randomUUID(), UUID.randomUUID(), correct * 10 >= total * 7,
                correct, total);
    }

    @Test
    void blockingReviewsStayOnTheMapAfterTheyAreFinishedAndAPendingOneWins() {
        UUID user = UUID.randomUUID();
        UUID finishedPoint = UUID.randomUUID();
        UUID optionalPoint = UUID.randomUUID();
        UUID none = UUID.randomUUID();
        UUID done = review(user, finishedPoint, "PRACTICE", true);
        answeredSet(user, done, 8, 10);
        jdbc.update("UPDATE review_items SET status = 'DONE', done_at = now() WHERE id = ?", done);
        review(user, optionalPoint, "CONSOLIDATE", false);

        var finished = reviews.findBlockingByPoint(user, List.of(finishedPoint, optionalPoint, none));

        // A finished review is still returned, with how it went; an optional one and a point without any are not.
        assertEquals(Set.of(finishedPoint), finished.keySet());
        var summary = finished.get(finishedPoint);
        assertEquals(done, summary.reviewId());
        assertEquals(ReviewStatus.DONE, summary.status());
        assertNotNull(summary.doneAt());
        assertEquals(1, summary.setsAnswered());
        assertEquals(8, summary.lastCorrect());
        assertEquals(10, summary.lastTotal());

        // The point fails again: its new pending review is the one the map shows.
        UUID again = review(user, finishedPoint, "PRACTICE", true);
        var current = reviews.findBlockingByPoint(user, List.of(finishedPoint)).get(finishedPoint);
        assertEquals(again, current.reviewId());
        assertEquals(ReviewStatus.PENDING, current.status());
        assertNull(current.doneAt());
        assertEquals(0, current.setsAnswered());
        assertNull(current.lastCorrect());
    }

    @Test
    void blockingReviewsOfAnotherLearnerAreNotShown() {
        UUID kp = UUID.randomUUID();
        review(UUID.randomUUID(), kp, "PRACTICE", true);

        assertTrue(reviews.findBlockingByPoint(UUID.randomUUID(), List.of(kp)).isEmpty());
        assertTrue(reviews.findBlockingByPoint(UUID.randomUUID(), List.of()).isEmpty());
    }
}
