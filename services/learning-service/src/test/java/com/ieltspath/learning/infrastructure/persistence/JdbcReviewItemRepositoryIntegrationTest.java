package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticeReviewCandidate;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
                UUID.randomUUID(), 0.5, ReviewStage.PRACTICE, null, blocking);
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
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, consolidateId));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, blockingId));
    }

    @Test
    void promotingNothingReturnsNothing() {
        assertTrue(reviews.promoteSuggestionsToBlocking(UUID.randomUUID(), List.of()).isEmpty());
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
                        + "blocking, stage) VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'PRACTICE', false, 'PRACTICE')",
                id, user, UUID.randomUUID(), lessonId);

        var states = reviews.findPracticeByLessons(user, List.of(lessonId));

        assertEquals(1, states.size());
        assertEquals(ReviewStatus.PENDING, states.getFirst().status());
        assertFalse(states.getFirst().blocking());
    }
}
