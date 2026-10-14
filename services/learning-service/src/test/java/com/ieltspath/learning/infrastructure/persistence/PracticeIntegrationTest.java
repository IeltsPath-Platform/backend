package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.usecase.*;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import com.ieltspath.learning.domain.vo.LearningSkill;
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
class PracticeIntegrationTest {
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
    @Autowired CompleteLessonUseCase complete;
    @Autowired GetTopicLessonsUseCase topicLessons;
    @Autowired ListReviewsUseCase listReviews;
    @Autowired GetLessonPracticeSetsUseCase catalog;
    @Autowired StartPracticeAttemptUseCase start;
    @Autowired SubmitPracticeAttemptUseCase submit;
    @Autowired AssignTopicTestUseCase tests;
    @MockitoBean LearningContentClient content;

    @Test
    void practiceEvidenceReviewAndTopicClearanceFollowPackageHistory() {
        UUID user = UUID.randomUUID();
        UUID topic = UUID.randomUUID();
        UUID firstLesson = UUID.randomUUID();
        UUID secondLesson = UUID.randomUUID();
        UUID firstKp = UUID.randomUUID();
        UUID secondKp = UUID.randomUUID();
        UUID firstPackage = UUID.randomUUID();
        UUID revealedPackage = UUID.randomUUID();
        UUID failedPackage = UUID.randomUUID();
        UUID sparePackage = UUID.randomUUID();
        UUID firstVersion = UUID.randomUUID();
        UUID revealedVersion = UUID.randomUUID();
        UUID failedVersion = UUID.randomUUID();
        UUID spareVersion = UUID.randomUUID();
        UUID firstQuestion = UUID.randomUUID();
        UUID secondQuestion = UUID.randomUUID();
        UUID thirdQuestion = UUID.randomUUID();
        UUID fourthQuestion = UUID.randomUUID();
        var firstSets = List.of(set(firstPackage, firstVersion), set(revealedPackage, revealedVersion));
        var secondSets = List.of(set(failedPackage, failedVersion), set(sparePackage, spareVersion));
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(topic, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(firstKp, "KP1", "First", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(secondKp, "KP2", "Second", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, true)));
        when(content.getTopicLessons(topic)).thenReturn(List.of(
                new LessonSummary(firstLesson, topic, "L1", "First", null, 1, List.of(firstKp)),
                new LessonSummary(secondLesson, topic, "L2", "Second", null, 2, List.of(secondKp))));
        when(content.getLesson(firstLesson)).thenReturn(new Lesson(firstLesson, topic, "L1", "First", null,
                null, 1, List.of(firstKp), LearningSkill.READING));
        when(content.getLesson(secondLesson)).thenReturn(new Lesson(secondLesson, topic, "L2", "Second", null,
                null, 2, List.of(secondKp), LearningSkill.READING));
        when(content.lessonPracticeSets(firstLesson)).thenReturn(firstSets);
        when(content.lessonPracticeSets(secondLesson)).thenReturn(secondSets);
        when(content.topicPracticeSets(topic)).thenReturn(new TopicPracticeSets(List.of(
                new TopicPracticeSets.LessonSets(firstLesson, firstSets),
                new TopicPracticeSets.LessonSets(secondLesson, secondSets))));
        when(content.getPackageVersion(firstVersion)).thenReturn(version(firstPackage, firstVersion,
                firstKp, firstQuestion, secondQuestion));
        when(content.getPackageVersion(revealedVersion)).thenReturn(version(revealedPackage, revealedVersion,
                firstKp, UUID.randomUUID(), UUID.randomUUID()));
        when(content.getPackageVersion(failedVersion)).thenReturn(version(failedPackage, failedVersion,
                secondKp, thirdQuestion, fourthQuestion));
        when(content.getPackageVersion(spareVersion)).thenReturn(version(sparePackage, spareVersion,
                secondKp, UUID.randomUUID(), UUID.randomUUID()));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(secondKp, 1));
        when(content.getTopicTestPackages(topic)).thenReturn(List.of(new TestPackage(UUID.randomUUID(),
                UUID.randomUUID(), "TEST")));

        topics.execute(user);
        // The Practice opens with its lesson; the next lesson and its Practice stay closed until this one passes.
        assertEquals("AVAILABLE", catalog.execute(user, firstLesson).items().getFirst().status());
        assertEquals("LOCKED", catalog.execute(user, secondLesson).items().getFirst().status());
        var closed = assertThrows(LearningGateException.class,
                () -> start.execute(user, secondLesson, failedPackage));
        assertEquals("LESSON_LOCKED", closed.getCode());

        // A lesson with required Practice cannot be confirmed by hand.
        var refused = assertThrows(LearningRequestException.class, () -> complete.execute(user, firstLesson));
        assertEquals("PRACTICE_REQUIRED", refused.getCode());
        assertEquals(List.of(firstLesson), refused.getLessonIds());
        assertEquals("TEST_LOCKED", assertThrows(LearningRequestException.class,
                () -> tests.execute(user, topic)).getCode());

        var first = start.execute(user, firstLesson, firstPackage);
        var passed = submit.execute(user, first.attemptId(), answers(firstQuestion, secondQuestion, "A", "A"));
        assertTrue(passed.passed());
        assertTrue(passed.countedAsEvidence());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE source='practice_set'",
                Integer.class));
        // One passed set of the lesson's two does not finish the Practice: the lesson stays open and the next stays shut.
        assertEquals("REQUIRED", catalog.execute(user, firstLesson).practiceStatus().name());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lesson_progress WHERE user_id = ? "
                + "AND lesson_id = ? AND completed_at IS NOT NULL", Integer.class, user, firstLesson));
        assertEquals("LOCKED", catalog.execute(user, secondLesson).items().getFirst().status());
        assertEquals("TEST_LOCKED", assertThrows(LearningRequestException.class,
                () -> tests.execute(user, topic)).getCode());

        var second = start.execute(user, firstLesson, firstPackage);
        assertFalse(submit.execute(user, second.attemptId(), answers(firstQuestion, secondQuestion, "A", "A"))
                .countedAsEvidence());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE source='practice_set'",
                Integer.class));

        UUID reviewId = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id,user_id,knowledge_point_id,lesson_id,status,skill) "
                + "VALUES (?,?,?,?,'DONE','READING')", reviewId, user, firstKp, firstLesson);
        jdbc.update("INSERT INTO review_sets (id,review_item_id,user_id,package_id,package_version_id,"
                + "submitted_at,passed,request_id) VALUES (?,?,?,?,?,clock_timestamp(),true,?)",
                UUID.randomUUID(), reviewId, user, revealedPackage, revealedVersion, UUID.randomUUID());
        var revealed = start.execute(user, firstLesson, revealedPackage);
        var revealedItems = ItemIds.of(content.getPackageVersion(revealedVersion));
        assertFalse(submit.execute(user, revealed.attemptId(), answers(revealedItems.first(),
                revealedItems.second(), "A", "A")).countedAsEvidence());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE source='practice_set'",
                Integer.class));
        // Both sets of the first lesson were seen and its review is finished: the Practice passes, the lesson completes
        // and the next lesson opens.
        assertEquals("PASSED", catalog.execute(user, firstLesson).practiceStatus().name());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM lesson_progress WHERE user_id = ? "
                + "AND lesson_id = ? AND completed_at IS NOT NULL", Integer.class, user, firstLesson));
        assertEquals("AVAILABLE", catalog.execute(user, secondLesson).items().getFirst().status());

        var failed = start.execute(user, secondLesson, failedPackage);
        var failedResult = submit.execute(user, failed.attemptId(), answers(thirdQuestion, fourthQuestion, "A", "B"));
        assertEquals(0.5, failedResult.percent());
        assertEquals(1, failedResult.reviewsCreated().size());
        assertEquals("PRACTICE", jdbc.queryForObject(
                "SELECT trigger_kind FROM review_items WHERE id = ?", String.class,
                failedResult.reviewsCreated().getFirst().reviewId()));
        // The review list says which set created each review, so the lesson can show it under that set.
        var pendingList = listReviews.execute(user, ReviewStatus.PENDING, null, 20);
        assertEquals(1, pendingList.size());
        assertEquals(failedPackage, pendingList.getFirst().sourcePackageId());
        var gate = assertThrows(LearningGateException.class,
                () -> start.execute(user, secondLesson, sparePackage));
        assertEquals("REVIEW_REQUIRED", gate.getCode());
        // The second lesson stays unfinished while its review is pending. It created the review, so the list keeps it
        // open: the review holds back the lessons after it, not itself.
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lesson_progress WHERE user_id = ? "
                + "AND lesson_id = ? AND completed_at IS NOT NULL", Integer.class, user, secondLesson));
        var blockedList = topicLessons.execute(user, topic).lessons();
        assertEquals("COMPLETED", blockedList.get(0).status());
        assertNull(blockedList.get(0).blockingReviewId());
        assertEquals("AVAILABLE", blockedList.get(1).status());
        assertNull(blockedList.get(1).blockingReviewId());
        jdbc.update("UPDATE review_items SET status='DONE' WHERE id=?",
                failedResult.reviewsCreated().getFirst().reviewId());
        var spare = start.execute(user, secondLesson, sparePackage);
        var spareItems = ItemIds.of(content.getPackageVersion(spareVersion));
        assertTrue(submit.execute(user, spare.attemptId(), answers(spareItems.first(), spareItems.second(),
                "A", "A")).passed());
        var openedList = topicLessons.execute(user, topic).lessons();
        assertEquals("COMPLETED", openedList.get(1).status());
        assertNull(openedList.get(1).blockingReviewId());
        assertNotNull(tests.execute(user, topic).assignmentId());
    }

    private static LessonPracticeSet set(UUID packageId, UUID versionId) {
        return new LessonPracticeSet(packageId, versionId, "PS", "Practice", 2, List.of(), null);
    }

    private static PackageVersion version(UUID packageId, UUID versionId, UUID kpId, UUID first, UUID second) {
        List<Item> items = List.of(first, second).stream().map(id -> new Item(id, id.equals(first) ? 1 : 2,
                "Choose", List.of(new Option("A", "Correct", 1), new Option("B", "Wrong", 2)),
                Map.of("type", "CHOICE", "correct", "A"), "Because A", BigDecimal.ONE,
                List.of(new KnowledgePointMapping(kpId, BigDecimal.ONE)))).toList();
        return new PackageVersion(versionId, packageId, "PRACTICE_SET", null, Map.of(),
                List.of(new Section(UUID.randomUUID(), "Practice", "READING", null, 1, "Passage", items)));
    }

    private static SubmitExerciseCommand answers(UUID first, UUID second, String a, String b) {
        return new SubmitExerciseCommand(UUID.randomUUID(), List.of(new SubmitExerciseCommand.Answer(first, a),
                new SubmitExerciseCommand.Answer(second, b)));
    }

    private record ItemIds(UUID first, UUID second) {
        static ItemIds of(PackageVersion version) {
            return new ItemIds(version.sections().getFirst().items().get(0).questionVersionId(),
                    version.sections().getFirst().items().get(1).questionVersionId());
        }
    }
}
