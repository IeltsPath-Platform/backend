package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.LessonCandidate;
import com.ieltspath.learning.application.port.LearningContentClient.PackageCandidate;
import com.ieltspath.learning.application.port.LearningContentClient.SuggestionCandidates;
import com.ieltspath.learning.application.usecase.GetSuggestionsUseCase;
import com.ieltspath.learning.application.usecase.StaffInsightUseCase;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Suggestions and the learner overview over real learner data (profiles, topics, attempts, reviews). */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class SuggestionsAndOverviewIntegrationTest {
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
    @Autowired GetSuggestionsUseCase suggestions;
    @Autowired StaffInsightUseCase insight;
    @MockitoBean LearningContentClient content;

    private final UUID user = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();
    private final UUID topic = UUID.randomUUID();
    private final UUID consolidate = UUID.randomUUID();
    private final UUID due = UUID.randomUUID();
    private final UUID firm = UUID.randomUUID();
    private final UUID next = UUID.randomUUID();
    private final UUID consolidateSet = UUID.randomUUID();
    private final UUID dueSet = UUID.randomUUID();
    private final UUID dueLesson = UUID.randomUUID();
    private final UUID nextLesson = UUID.randomUUID();

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE kp_mastery_profiles, topic_progress, learner_placements, practice_attempts, "
                + "review_items, review_sets, lesson_progress, practice_test_assignments, learner_flags, work_items, "
                + "diagnostic_probes, diagnostic_sessions, remediation_flags, course_outcomes CASCADE");
        var courseView = new LearningContentClient.Course(course, "IELTS_5_5", "IELTS 5.5", new BigDecimal("5.5"), true);
        when(content.getTopicSequence()).thenReturn(List.of(new LearningContentClient.Topic(topic, "T", "Topic", 1, null,
                List.of(kp(consolidate, "KC"), kp(due, "KD"), kp(firm, "KF"), kp(next, "KN")), LearningSkill.READING,
                true, courseView)));
        when(content.getPracticeTestMilestones(course)).thenReturn(List.of());
        when(content.prerequisites(anyList())).thenAnswer(invocation -> {
            List<UUID> asked = invocation.getArgument(0);
            return asked.contains(next) ? Map.of(next, List.of(firm)) : Map.of();
        });
        when(content.suggestionCandidates(anyList(), any())).thenReturn(new SuggestionCandidates(
                List.of(new PackageCandidate(consolidateSet, UUID.randomUUID(), "PRACTICE_SET", "Consolidate set",
                                List.of(consolidate), new BigDecimal("5.5"), "READING"),
                        new PackageCandidate(dueSet, UUID.randomUUID(), "PRACTICE_SET", "Due set", List.of(due),
                                new BigDecimal("5.5"), "READING")),
                List.of(new LessonCandidate(dueLesson, topic, "Due lesson", List.of(due), new BigDecimal("5.5"),
                                "READING", 1),
                        new LessonCandidate(nextLesson, topic, "Next lesson", List.of(next), new BigDecimal("5.5"),
                                "READING", 2))));
        jdbc.update("INSERT INTO topic_progress (user_id, topic_id, course_id, sequence_order, has_topic_test) "
                + "VALUES (?, ?, ?, 1, TRUE)", user, topic, course);
        profile(consolidate, "0.700", "CONSOLIDATE", "now() + interval '3 days'");
        profile(due, "0.900", "MAINTAIN", "now() - interval '1 day'");
        profile(firm, "0.950", "MAINTAIN", "now() + interval '30 days'");
    }

    private void profile(UUID kp, String mastery, String tier, String nextReview) {
        jdbc.update("INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count, "
                + "last_evidence_at, next_review_at) VALUES (?, ?, ?, ?, 5, now(), " + nextReview + ")", user, kp,
                new BigDecimal(mastery), tier);
    }

    private static LearningContentClient.KnowledgePoint kp(UUID id, String code) {
        return new LearningContentClient.KnowledgePoint(id, code, code, "PROCEDURE", "READING", "", true);
    }

    @Test
    void weakAndDuePointsComeFirstThenTheNextPointInTheTree() {
        var result = suggestions.execute(user);

        assertEquals(List.of("CONSOLIDATE", "DUE", "NEXT_IN_TREE"),
                result.suggestions().stream().map(item -> item.reason()).toList());
        var first = result.suggestions().get(0);
        assertEquals("PRACTICE_SET", first.type());
        assertEquals(consolidateSet, first.id());
        assertEquals("/api/learning/review-basket/" + consolidate + "/start", first.openPath());
        assertEquals("KC", first.knowledgePointCode());
        var last = result.suggestions().get(2);
        assertEquals("LESSON", last.type());
        assertEquals(nextLesson, last.id());
        assertEquals("/api/learning/lessons/" + nextLesson, last.openPath());
        var captor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(content).suggestionCandidates(anyList(), captor.capture());
        assertEquals(0, new BigDecimal("5.5").compareTo(captor.getValue()), "the band of the course in progress");
    }

    @Test
    void aSetTheLearnerAlreadyRevealedIsReplacedByAnUnseenLessonOfTheSamePoint() {
        jdbc.update("INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id, "
                + "submitted_at, request_id) VALUES (?, ?, ?, 'READING', ?, ?, now(), ?)", UUID.randomUUID(), user,
                UUID.randomUUID(), dueSet, UUID.randomUUID(), UUID.randomUUID());

        var items = suggestions.execute(user).suggestions();

        var dueItem = items.stream().filter(item -> item.reason().equals("DUE")).findFirst().orElseThrow();
        assertEquals("LESSON", dueItem.type());
        assertEquals(dueLesson, dueItem.id());
    }

    @Test
    void aCompletedLessonIsNotSuggestedAgain() {
        jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, "
                + "knowledge_point_ids, completed_at) VALUES (?, ?, ?, 2, ARRAY[?]::uuid[], now())", user, nextLesson,
                topic, next);

        var items = suggestions.execute(user).suggestions();

        assertTrue(items.stream().noneMatch(item -> item.reason().equals("NEXT_IN_TREE")));
    }

    @Test
    void aPointWithABlockingReviewIsRequiredNotSuggested() {
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, "
                + "blocking) VALUES (?, ?, ?, ?, 'PENDING', 'READING', TRUE)", UUID.randomUUID(), user, consolidate,
                UUID.randomUUID());

        var items = suggestions.execute(user).suggestions();

        assertTrue(items.stream().noneMatch(item -> consolidate.equals(item.knowledgePointId())));
    }

    @Test
    void withoutACourseInProgressThePlacementBandIsUsedAndWithoutEitherNothingIsSuggested() {
        jdbc.update("UPDATE topic_progress SET passed_at = now() WHERE user_id = ?", user);
        assertTrue(suggestions.execute(user).suggestions().isEmpty(), "no course in progress and no placement");

        jdbc.update("INSERT INTO learner_placements (user_id, band, attempt_id, completed_at, updated_at) "
                + "VALUES (?, 6.0, ?, now(), now())", user, UUID.randomUUID());
        assertFalse(suggestions.execute(user).suggestions().isEmpty());
        var captor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(content, org.mockito.Mockito.atLeastOnce()).suggestionCandidates(anyList(), captor.capture());
        assertEquals(0, new BigDecimal("6.0").compareTo(captor.getValue()));
    }

    @Test
    void theOverviewGathersCoursesTiersReviewsFlagsAndOpenWork() {
        UUID review = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, blocking) "
                + "VALUES (?, ?, ?, ?, 'PENDING', 'READING', TRUE)", review, user, consolidate, UUID.randomUUID());
        UUID flag = UUID.randomUUID();
        jdbc.update("INSERT INTO learner_flags (id, user_id, kind, evidence) VALUES (?, ?, 'BURST_SUBMISSIONS', "
                + "'{}'::jsonb)", flag, user);
        jdbc.update("INSERT INTO work_items (id, queue, kind, subject_user_id, subject_ref) VALUES (?, 'EXAMINER', "
                + "'LEARNER_FLAG', ?, ?)", UUID.randomUUID(), user, flag);

        var overview = insight.overview(user);

        assertEquals(1, overview.courses().size());
        var courseView = overview.courses().getFirst();
        assertEquals(course, courseView.courseId());
        assertEquals(1, courseView.topics());
        assertEquals(0, courseView.topicsDone());
        assertEquals("IN_PROGRESS", courseView.status());
        assertEquals(Map.of("PRIORITY", 0, "CONSOLIDATE", 1, "MAINTAIN", 2), overview.knowledgePointsByTier());
        assertEquals(List.of(review), overview.pendingReviews().stream().map(r -> r.reviewId()).toList());
        assertEquals(1, overview.flags().size());
        assertEquals(1, overview.openWork().size());

        jdbc.update("UPDATE topic_progress SET tested_at = now(), first_test_percent = 40 WHERE user_id = ?", user);
        assertEquals(1, insight.overview(user).courses().getFirst().topicsDone(), "a tested topic counts as done");
    }
}
