package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.usecase.*;
import com.ieltspath.learning.domain.exception.LearningGateException;
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
                new LessonSummary(firstLesson, topic, "L1", "First", null, 1, List.of(firstKp), List.of()),
                new LessonSummary(secondLesson, topic, "L2", "Second", null, 2, List.of(secondKp), List.of())));
        when(content.getLesson(firstLesson)).thenReturn(new Lesson(firstLesson, topic, "L1", "First", null,
                1, List.of(firstKp), List.of(), LearningSkill.READING));
        when(content.getLesson(secondLesson)).thenReturn(new Lesson(secondLesson, topic, "L2", "Second", null,
                2, List.of(secondKp), List.of(), LearningSkill.READING));
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
        assertEquals("LOCKED", catalog.execute(user, firstLesson).items().getFirst().status());
        var locked = assertThrows(LearningRequestException.class,
                () -> start.execute(user, firstLesson, firstPackage));
        assertEquals(409, locked.getStatus());
        assertEquals("PRACTICE_LOCKED", locked.getCode());

        complete.execute(user, firstLesson);
        complete.execute(user, secondLesson);
        var required = assertThrows(LearningRequestException.class, () -> tests.execute(user, topic));
        assertEquals("PRACTICE_REQUIRED", required.getCode());
        assertEquals(Set.of(firstLesson, secondLesson), Set.copyOf(required.getLessonIds()));

        var first = start.execute(user, firstLesson, firstPackage);
        var passed = submit.execute(user, first.attemptId(), answers(firstQuestion, secondQuestion, "A", "A"));
        assertTrue(passed.passed());
        assertTrue(passed.countedAsEvidence());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE source='practice_set'",
                Integer.class));
        assertEquals("PASSED", catalog.execute(user, firstLesson).practiceStatus().name());
        assertEquals(List.of(secondLesson), assertThrows(LearningRequestException.class,
                () -> tests.execute(user, topic)).getLessonIds());

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

        var failed = start.execute(user, secondLesson, failedPackage);
        var failedResult = submit.execute(user, failed.attemptId(), answers(thirdQuestion, fourthQuestion, "A", "B"));
        assertEquals(0.5, failedResult.percent());
        assertEquals(1, failedResult.reviewsCreated().size());
        assertEquals("PRACTICE", jdbc.queryForObject(
                "SELECT trigger_kind FROM review_items WHERE id = ?", String.class,
                failedResult.reviewsCreated().getFirst().reviewId()));
        var gate = assertThrows(LearningGateException.class,
                () -> start.execute(user, secondLesson, sparePackage));
        assertEquals("REVIEW_REQUIRED", gate.getCode());
        jdbc.update("UPDATE review_items SET status='DONE' WHERE id=?",
                failedResult.reviewsCreated().getFirst().reviewId());
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
