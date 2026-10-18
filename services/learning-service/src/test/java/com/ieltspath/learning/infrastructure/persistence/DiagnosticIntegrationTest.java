package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.command.SubmitReviewCommand;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.result.DiagnosticResult;
import com.ieltspath.learning.application.result.ReviewResult;
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

/**
 * A blocking review of point B that fails twice, or runs out of unseen sets, opens probes on the points B builds on. A
 * failed probe walks back to its own prerequisites; the gap points found get blocking reviews and B waits for them.
 */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class DiagnosticIntegrationTest {
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
    @Autowired GetReviewUseCase getReview;
    @Autowired SubmitReviewUseCase submitReview;
    @Autowired SubmitTheoryCheckUseCase theoryCheck;
    @Autowired GetDiagnosticUseCase getDiagnostic;
    @Autowired GetDiagnosticProbeUseCase getProbe;
    @Autowired SubmitDiagnosticProbeUseCase submitProbe;
    @Autowired GetLearnerReportUseCase report;
    @MockitoBean LearningContentClient content;

    private final UUID topic = UUID.randomUUID();
    private final UUID lesson = UUID.randomUUID();
    private final UUID block = UUID.randomUUID();
    private final UUID kpB = UUID.randomUUID();
    private final UUID kpA = UUID.randomUUID();
    private final UUID kpZ = UUID.randomUUID();
    private final UUID lessonQuestion = UUID.randomUUID();
    private final Map<UUID, PackageVersion> versions = new LinkedHashMap<>();
    private final Map<UUID, UUID> versionKnowledgePoint = new HashMap<>();
    private final Map<UUID, List<UUID>> prerequisites = new HashMap<>();

    @BeforeEach
    void stubContent() {
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(topic, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(kpB, "B", "Point B", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(kpA, "A", "Point A", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(kpZ, "Z", "Point Z", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, true)));
        when(content.getTopicLessons(topic)).thenReturn(List.of(new LessonSummary(lesson, topic, "L1", "Lesson",
                null, 1, List.of(kpB), List.of(block))));
        when(content.getLesson(lesson)).thenReturn(new Lesson(lesson, topic, "L1", "Lesson", null, 1,
                List.of(kpB), List.of(
                new Block(UUID.randomUUID(), "TEXT", 1, "Theory B", null, List.of(), List.of(), null, List.of(kpB)),
                new Block(block, "EXERCISE", 2, null, null, List.of(), List.of(new Question(lessonQuestion, 1,
                        "Choose", options(), Map.of("type", "CHOICE", "correct", "A"), "Because A", List.of(kpB))),
                        "EXERCISE", List.of(kpB))), LearningSkill.READING));
        when(content.lessonPracticeSets(lesson)).thenReturn(List.of());
        when(content.getPackageVersion(any())).thenAnswer(invocation -> versions.get(invocation.<UUID>getArgument(0)));
        when(content.prerequisites(anyList())).thenAnswer(invocation -> {
            List<UUID> asked = invocation.getArgument(0);
            Map<UUID, List<UUID>> found = new HashMap<>();
            asked.forEach(kp -> found.put(kp, prerequisites.getOrDefault(kp, List.of())));
            return found;
        });
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(1), any())).thenAnswer(invocation ->
                unseen(invocation.getArgument(0), invocation.getArgument(1)));
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(1))).thenAnswer(invocation ->
                unseen(invocation.getArgument(0), invocation.getArgument(1)));
    }

    private List<PracticeSet> unseen(UUID kp, List<UUID> excluded) {
        return versions.values().stream().filter(version -> kp.equals(versionKnowledgePoint.get(version.packageVersionId())))
                .filter(version -> !excluded.contains(version.packageId())).limit(1)
                .map(version -> new PracticeSet(version.packageId(), version.packageVersionId(), "PS", 3, 3)).toList();
    }

    @Test
    void aFailedPrerequisiteIsTheGapAndTheReviewWaitsForItsReview() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB), b3 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA);
        UUID review = failTwice(user, b1, b2);

        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE user_id = ?", UUID.class, user);
        DiagnosticResult opened = getDiagnostic.execute(user, sessionId);
        assertEquals("PROBING", opened.status());
        assertEquals(1, opened.probes().size());
        assertEquals("OPEN", opened.probes().getFirst().status());
        assertEquals("A", opened.probes().getFirst().knowledgePoint().code());

        ReviewResult diagnosing = getReview.execute(user, review);
        assertEquals(ReviewResult.DIAGNOSING, diagnosing.state());
        assertEquals(sessionId, diagnosing.diagnosticSessionId());
        assertNull(diagnosing.set());
        assertEquals("DIAGNOSING", report.execute(user).knowledgePoints().stream()
                .filter(point -> point.knowledgeId().equals(kpB)).findFirst().orElseThrow().action().type());

        UUID probe = opened.probes().getFirst().probeId();
        var questions = getProbe.execute(user, sessionId, probe);
        assertEquals(3, questions.questions().size());
        var failed = submitProbe.execute(user, sessionId, probe, answers(a1, "B", "B", "B"));
        assertFalse(failed.passed());
        assertEquals("CONCLUDED", failed.sessionStatus());
        assertEquals(List.of(kpA), failed.conclusionKnowledgePointIds());
        assertEquals("A", failed.results().getFirst().correctAnswer());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ? "
                + "AND source = 'diagnostic_probe'", Integer.class, user));

        UUID gapReview = jdbc.queryForObject("SELECT id FROM review_items WHERE user_id = ? "
                + "AND knowledge_point_id = ? AND status = 'PENDING'", UUID.class, user, kpA);
        assertEquals("DIAGNOSTIC", jdbc.queryForObject("SELECT trigger_kind FROM review_items WHERE id = ?",
                String.class, gapReview));
        assertTrue(jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?", Boolean.class, gapReview));
        ReviewResult waiting = getReview.execute(user, review);
        assertEquals(ReviewResult.WAITING_ON_PREREQUISITES, waiting.state());
        assertEquals(List.of(gapReview), waiting.waitingOnReviewIds());
        assertNull(waiting.set());

        DiagnosticResult concluded = getDiagnostic.execute(user, sessionId);
        assertEquals(List.of(kpA), concluded.conclusion().stream().map(DiagnosticResult.Point::knowledgePointId).toList());
        assertEquals("FAILED", concluded.probes().getFirst().result());
        assertFalse(concluded.rootIsGap());
        assertEquals(List.of(gapReview), concluded.waitingOnReviewIds());

        jdbc.update("UPDATE review_items SET status = 'DONE' WHERE id = ?", gapReview);
        ReviewResult resumed = getReview.execute(user, review);
        assertEquals(ReviewResult.ACTIVE, resumed.state());
        assertEquals(b3.packageId(), resumed.set().packageId());
    }

    @Test
    void passedProbesPutTheBlameOnTheReviewedPointItself() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB), b3 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA);
        UUID review = failTwice(user, b1, b2);
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE user_id = ?", UUID.class, user);
        UUID probe = getDiagnostic.execute(user, sessionId).probes().getFirst().probeId();

        SubmitExerciseCommand passing = answers(a1, "A", "A", "A");
        var first = submitProbe.execute(user, sessionId, probe, passing);
        assertTrue(first.passed());
        assertEquals("CONCLUDED", first.sessionStatus());
        assertEquals(List.of(kpB), first.conclusionKnowledgePointIds());
        int evidence = jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?", Integer.class, user);

        // The same request again returns the same result and writes nothing.
        assertEquals(first, submitProbe.execute(user, sessionId, probe, passing));
        assertEquals(evidence, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?",
                Integer.class, user));
        var closed = assertThrows(com.ieltspath.learning.application.exception.LearningRequestException.class,
                () -> submitProbe.execute(user, sessionId, probe, answers(a1, "A", "A", "A")));
        assertEquals("PROBE_CLOSED", closed.getCode());

        assertTrue(getDiagnostic.execute(user, sessionId).rootIsGap());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? "
                + "AND trigger_kind = 'DIAGNOSTIC'", Integer.class, user));
        ReviewResult resumed = getReview.execute(user, review);
        assertEquals(ReviewResult.ACTIVE, resumed.state());
        assertEquals(b3.packageId(), resumed.set().packageId());
    }

    @Test
    void aFailedProbeWalksBackToItsOwnPrerequisite() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA));
        prerequisites.put(kpA, List.of(kpZ));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA);
        PackageVersion z1 = practiceSet(kpZ);
        failTwice(user, b1, b2);
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE user_id = ?", UUID.class, user);
        UUID probeA = getDiagnostic.execute(user, sessionId).probes().getFirst().probeId();

        var afterA = submitProbe.execute(user, sessionId, probeA, answers(a1, "B", "B", "B"));
        assertEquals("PROBING", afterA.sessionStatus());
        DiagnosticResult walking = getDiagnostic.execute(user, sessionId);
        assertEquals(2, walking.probes().size());
        var probeZ = walking.probes().get(1);
        assertEquals("Z", probeZ.knowledgePoint().code());
        assertEquals(2, probeZ.depth());
        assertEquals(kpA, probeZ.parentKnowledgePointId());

        var afterZ = submitProbe.execute(user, sessionId, probeZ.probeId(), answers(z1, "B", "B", "B"));
        assertEquals("CONCLUDED", afterZ.sessionStatus());
        assertEquals(List.of(kpZ), afterZ.conclusionKnowledgePointIds());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? "
                + "AND knowledge_point_id = ? AND trigger_kind = 'DIAGNOSTIC'", Integer.class, user, kpZ));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? "
                + "AND knowledge_point_id = ?", Integer.class, user, kpA));
    }

    @Test
    void aReviewWithoutUnseenSetsIsSkippedTellsContentAndStillDiagnoses() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA));
        PackageVersion a1 = practiceSet(kpA);
        topics.execute(user);
        UUID review = insertReview(user);

        ReviewResult skipped = getReview.execute(user, review);
        assertEquals("SKIPPED", skipped.reviewStatus());
        assertNotNull(skipped.diagnosticSessionId());
        assertEquals(1, jdbc.queryForObject("SELECT occurrences FROM content_gap_signals WHERE kp_id = ?",
                Integer.class, kpB));

        DiagnosticResult opened = getDiagnostic.execute(user, skipped.diagnosticSessionId());
        var concluded = submitProbe.execute(user, skipped.diagnosticSessionId(),
                opened.probes().getFirst().probeId(), answers(a1, "B", "B", "B"));
        assertEquals(List.of(kpA), concluded.conclusionKnowledgePointIds());
        // The skipped review is not held back: only the gap point gets a review.
        assertTrue(jdbc.queryForObject("SELECT waiting_on_review_ids = '{}' FROM review_items WHERE id = ?",
                Boolean.class, review));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? "
                + "AND status = 'PENDING' AND trigger_kind = 'DIAGNOSTIC'", Integer.class, user));
    }

    @Test
    void probesWithoutAnUnseenSetStayUnknownAndTheReviewCarriesOn() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB), b3 = practiceSet(kpB);
        UUID review = failTwice(user, b1, b2);

        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE user_id = ?", UUID.class, user);
        DiagnosticResult result = getDiagnostic.execute(user, sessionId);
        assertEquals("CONCLUDED", result.status());
        assertEquals("UNKNOWN", result.probes().getFirst().result());
        assertTrue(result.rootIsGap());
        assertEquals(b3.packageId(), getReview.execute(user, review).set().packageId());
    }

    @Test
    void aProbeWhoseSetWasSeenElsewhereGetsAnotherSetOrNone() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA), a2 = practiceSet(kpA);
        failTwice(user, b1, b2);
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE user_id = ?", UUID.class, user);
        UUID probe = getDiagnostic.execute(user, sessionId).probes().getFirst().probeId();

        // The learner saw a1 in Practice, so the probe cannot use it.
        jdbc.update("INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id, "
                + "submitted_at, request_id) VALUES (?, ?, ?, 'READING', ?, ?, clock_timestamp(), ?)",
                UUID.randomUUID(), user, lesson, a1.packageId(), a1.packageVersionId(), UUID.randomUUID());
        var reassigned = getProbe.execute(user, sessionId, probe);
        assertEquals("OPEN", reassigned.status());
        assertEquals(3, reassigned.questions().size());
        assertEquals(a2.packageId(), jdbc.queryForObject("SELECT package_id FROM diagnostic_probes WHERE id = ?",
                UUID.class, probe));
    }

    private UUID insertReview(UUID user) {
        UUID review = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, "
                + "trigger_kind) VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'ASSESSMENT')", review, user, kpB, lesson);
        return review;
    }

    /** Fails the first set, does the theory step, then fails the second set: the point where the diagnosis opens. */
    private UUID failTwice(UUID user, PackageVersion first, PackageVersion second) {
        topics.execute(user);
        UUID review = insertReview(user);
        var firstSet = getReview.execute(user, review);
        assertEquals(first.packageId(), firstSet.set().packageId());
        assertEquals("THEORY", submitReview.execute(user, review, new SubmitReviewCommand(firstSet.set().reviewSetId(),
                UUID.randomUUID(), answers(first, "B", "B", "B").answers())).stage());
        theoryCheck.execute(user, review, new SubmitExerciseCommand(UUID.randomUUID(),
                List.of(new SubmitExerciseCommand.Answer(lessonQuestion, "A"))));
        var secondSet = getReview.execute(user, review);
        assertEquals(second.packageId(), secondSet.set().packageId());
        var failed = submitReview.execute(user, review, new SubmitReviewCommand(secondSet.set().reviewSetId(),
                UUID.randomUUID(), answers(second, "B", "B", "B").answers()));
        assertEquals("PENDING", failed.reviewStatus());
        assertEquals(2, failed.failedSets());
        return review;
    }

    private PackageVersion practiceSet(UUID kp) {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        List<Item> items = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            items.add(new Item(UUID.randomUUID(), index + 1, "Choose", options(), Map.of("type", "CHOICE",
                    "correct", "A"), "Because A", "Look again", BigDecimal.ONE,
                    List.of(new KnowledgePointMapping(kp, BigDecimal.ONE))));
        }
        PackageVersion version = new PackageVersion(versionId, packageId, "PRACTICE_SET", null, Map.of(),
                List.of(new Section(UUID.randomUUID(), "Practice", "READING", null, 1, "Passage", items)));
        versions.put(versionId, version);
        versionKnowledgePoint.put(versionId, kp);
        return version;
    }

    private static List<Option> options() {
        return List.of(new Option("A", "Correct", 1), new Option("B", "Wrong", 2));
    }

    private static SubmitExerciseCommand answers(PackageVersion version, String... values) {
        List<Item> items = version.sections().getFirst().items();
        List<SubmitExerciseCommand.Answer> answers = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            answers.add(new SubmitExerciseCommand.Answer(items.get(index).questionVersionId(), values[index]));
        }
        return new SubmitExerciseCommand(UUID.randomUUID(), answers);
    }
}
