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
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Autowired SkipDiagnosticUseCase skipDiagnostic;
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
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(6))).thenAnswer(invocation -> {
            UUID kp = invocation.getArgument(0);
            List<UUID> excluded = invocation.getArgument(1);
            return versions.values().stream().filter(version -> kp.equals(versionKnowledgePoint.get(version.packageVersionId())))
                    .filter(version -> !excluded.contains(version.packageId())).limit(6)
                    .map(version -> new PracticeSet(version.packageId(), version.packageVersionId(), "PS", 3, 3))
                    .toList();
        });
        // Diagnosis preparation asks for up to the probe budget per point, so duplicate packages can be skipped.
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(6))).thenAnswer(invocation ->
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
        assertEquals("GAPS_FOUND", getDiagnostic.execute(user, sessionId).conclusionKind());
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
    void reviewSubmissionPersistsErrorTrapEvidenceInPostgres() {
        UUID reviewUser = UUID.randomUUID();
        topics.execute(reviewUser);
        UUID reviewTrap = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, parent_kp_id, error_code) "
                + "VALUES (?, ?, FALSE, ?, 'TFNG_NG_AS_FALSE')", reviewTrap, topic, kpB);
        PackageVersion reviewFirst = practiceSet(kpB, true), reviewSecond = practiceSet(kpB, true);

        failTwice(reviewUser, reviewFirst, reviewSecond);

        assertEquals(6, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND kp_id = ? "
                + "AND source = 'error_trap'", Integer.class, reviewUser, reviewTrap));
    }

    @Test
    void diagnosticSubmissionPersistsErrorTrapEvidenceInPostgres() {
        UUID diagnosticUser = UUID.randomUUID();
        versions.clear();
        versionKnowledgePoint.clear();
        prerequisites.clear();
        prerequisites.put(kpB, List.of(kpA));
        PackageVersion first = practiceSet(kpB), second = practiceSet(kpB), probeSet = practiceSet(kpA, true);
        UUID diagnosticTrap = UUID.randomUUID();
        failTwice(diagnosticUser, first, second);
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, parent_kp_id, error_code) "
                + "VALUES (?, ?, FALSE, ?, 'TFNG_NG_AS_FALSE')", diagnosticTrap, topic, kpA);
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE user_id = ?", UUID.class,
                diagnosticUser);
        UUID probeId = getDiagnostic.execute(diagnosticUser, sessionId).probes().getFirst().probeId();

        submitProbe.execute(diagnosticUser, sessionId, probeId, answers(probeSet, "B", "B", "B"));

        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND kp_id = ? "
                + "AND source = 'error_trap'", Integer.class, diagnosticUser, diagnosticTrap));
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
        assertEquals("ROOT_IS_GAP", getDiagnostic.execute(user, sessionId).conclusionKind());
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT response IS NOT NULL FROM diagnostic_probes WHERE id = ?",
                Boolean.class, probe));
        int evidence = jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?", Integer.class, user);

        org.mockito.Mockito.doThrow(new com.ieltspath.learning.application.exception.LearningRequestException(
                404, "NOT_FOUND", "Package version was removed")).when(content)
                .getPackageVersion(a1.packageVersionId());
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
        assertEquals("INCONCLUSIVE", result.conclusionKind());
        assertTrue(result.conclusion().isEmpty());
        assertFalse(result.rootIsGap());
        assertEquals(1, jdbc.queryForObject("SELECT occurrences FROM content_gap_signals WHERE kp_id = ?",
                Integer.class, kpA));
        ReviewResult resumed = getReview.execute(user, review);
        assertEquals(sessionId, resumed.diagnosticSessionId());
        assertEquals(b3.packageId(), resumed.set().packageId());
    }

    @Test
    void skipRequiresAtLeastOneAnsweredProbe() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA));
        // The prerequisite needs a set, or its probe is NO_SET and the diagnosis concludes at once.
        practiceSet(kpA);
        UUID review = failTwice(user, practiceSet(kpB), practiceSet(kpB));
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE root_review_id = ?",
                UUID.class, review);

        var error = assertThrows(com.ieltspath.learning.application.exception.LearningRequestException.class,
                () -> skipDiagnostic.execute(user, sessionId));

        assertEquals(409, error.getStatus());
        assertEquals("DIAGNOSIS_PROBE_REQUIRED", error.getCode());
        assertEquals("PROBING", getDiagnostic.execute(user, sessionId).status());
    }

    @Test
    void skipKeepsFailedProbeGapsAndReleasesUnansweredProbePackage() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA, kpZ));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA);
        PackageVersion z1 = practiceSet(kpZ);
        UUID rootReview = failTwice(user, b1, b2);
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE root_review_id = ?",
                UUID.class, rootReview);
        var opened = getDiagnostic.execute(user, sessionId);
        var failed = opened.probes().stream().filter(probe -> kpA.equals(probe.knowledgePoint().knowledgePointId()))
                .findFirst().orElseThrow();
        submitProbe.execute(user, sessionId, failed.probeId(), answers(a1, "B", "B", "B"));

        DiagnosticResult skipped = skipDiagnostic.execute(user, sessionId);
        DiagnosticResult replay = skipDiagnostic.execute(user, sessionId);

        assertEquals(skipped, replay);
        assertEquals("GAPS_FOUND", skipped.conclusionKind());
        assertEquals(List.of(kpA), skipped.conclusion().stream()
                .map(DiagnosticResult.Point::knowledgePointId).toList());
        var skippedProbe = skipped.probes().stream().filter(probe -> kpZ.equals(probe.knowledgePoint().knowledgePointId()))
                .findFirst().orElseThrow();
        assertEquals("SKIPPED", skippedProbe.status());
        assertEquals("UNKNOWN", skippedProbe.result());
        assertNull(jdbc.queryForObject("SELECT package_id FROM diagnostic_probes WHERE id = ?", UUID.class,
                skippedProbe.probeId()));
        assertNull(jdbc.queryForObject("SELECT package_version_id FROM diagnostic_probes WHERE id = ?", UUID.class,
                skippedProbe.probeId()));
        UUID gapReview = jdbc.queryForObject("SELECT id FROM review_items WHERE user_id = ? "
                + "AND knowledge_point_id = ? AND trigger_kind = 'DIAGNOSTIC'", UUID.class, user, kpA);
        assertEquals("WAITING_ON_PREREQUISITES", getReview.execute(user, rootReview).state());

        UUID optional = insertReview(user, kpZ, "ASSESSMENT");
        assertEquals(z1.packageId(), getReview.execute(user, optional).set().packageId());
        assertNotNull(gapReview);
    }

    @Test
    void skippingAfterPassingAProbeKeepsTheRootReviewActive() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA, kpZ));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB), b3 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA);
        practiceSet(kpZ);
        UUID rootReview = failTwice(user, b1, b2);
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE root_review_id = ?",
                UUID.class, rootReview);
        UUID probeOfA = getDiagnostic.execute(user, sessionId).probes().stream()
                .filter(probe -> kpA.equals(probe.knowledgePoint().knowledgePointId()))
                .findFirst().orElseThrow().probeId();
        submitProbe.execute(user, sessionId, probeOfA, answers(a1, "A", "A", "A"));

        DiagnosticResult result = skipDiagnostic.execute(user, sessionId);

        assertEquals("ROOT_IS_GAP", result.conclusionKind());
        assertTrue(result.conclusion().stream().anyMatch(point -> kpB.equals(point.knowledgePointId())));
        assertEquals(ReviewResult.ACTIVE, getReview.execute(user, rootReview).state());
        assertEquals(b3.packageId(), getReview.execute(user, rootReview).set().packageId());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? "
                + "AND trigger_kind = 'DIAGNOSTIC'", Integer.class, user));
    }

    @Test
    void noPrerequisitesAreStoredWithoutOpeningAProbeSessionInTheReviewResponse() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of());
        UUID review = failTwice(user, practiceSet(kpB), practiceSet(kpB));

        ReviewResult result = getReview.execute(user, review);
        String kind = jdbc.queryForObject("SELECT conclusion_kind FROM diagnostic_sessions WHERE root_review_id = ?",
                String.class, review);

        assertEquals(ReviewResult.ACTIVE, result.state());
        assertNull(result.diagnosticSessionId());
        assertEquals("NO_PREREQUISITES", kind);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM diagnostic_probes p JOIN diagnostic_sessions s "
                + "ON s.id = p.session_id WHERE s.root_review_id = ?", Integer.class, review));
    }

    @Test
    void diagnosticReviewWithoutItsKnowledgePointInTheLessonSkipsTheory() {
        UUID user = UUID.randomUUID();
        topics.execute(user);
        UUID review = insertReview(user, kpA, "DIAGNOSTIC");
        jdbc.update("UPDATE review_items SET stage = 'THEORY', theory_reason = 'REPEATED_FAIL' WHERE id = ?", review);
        PackageVersion a1 = practiceSet(kpA);

        ReviewResult result = getReview.execute(user, review);

        assertEquals("NO_LESSON", result.theoryScope());
        assertTrue(result.theory().isEmpty());
        assertTrue(result.quickCheck().isEmpty());
        assertEquals("PRACTICE", result.stage());
        assertEquals(a1.packageId(), result.set().packageId());
    }

    @Test
    void getDiagnosticSupportsSkippedProbesAndLegacySessionsWithoutConclusionKinds() {
        UUID user = UUID.randomUUID();
        UUID rootReview = insertReview(user);
        UUID session = UUID.randomUUID();
        UUID probe = UUID.randomUUID();
        jdbc.update("INSERT INTO diagnostic_sessions (id, user_id, root_review_id, root_kp_id, status, "
                        + "conclusion_kp_ids, conclusion_kind) VALUES (?, ?, ?, ?, 'CONCLUDED', CAST(? AS uuid[]), "
                        + "'INCONCLUSIVE')",
                session, user, rootReview, kpB, "{}");
        jdbc.update("INSERT INTO diagnostic_probes (id, session_id, kp_id, parent_kp_id, depth, status) "
                + "VALUES (?, ?, ?, ?, 1, 'SKIPPED')", probe, session, kpA, kpB);

        DiagnosticResult skipped = getDiagnostic.execute(user, session);
        UUID legacySession = UUID.randomUUID();
        UUID legacyReview = insertReview(user, UUID.randomUUID(), "ASSESSMENT");
        jdbc.update("INSERT INTO diagnostic_sessions (id, user_id, root_review_id, root_kp_id, status, "
                        + "conclusion_kp_ids) VALUES (?, ?, ?, ?, 'CONCLUDED', CAST(? AS uuid[]))",
                legacySession, user, legacyReview, kpB, "{" + kpB + "}");
        DiagnosticResult legacy = getDiagnostic.execute(user, legacySession);

        assertEquals("INCONCLUSIVE", skipped.conclusionKind());
        assertEquals("SKIPPED", skipped.probes().getFirst().status());
        assertEquals("UNKNOWN", skipped.probes().getFirst().result());
        assertNull(legacy.conclusionKind());
        assertTrue(legacy.rootIsGap());
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

    @Test
    void aProbePackageThatWasUnpublishedIsReplacedWithAnotherSet() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA), a2 = practiceSet(kpA);
        when(content.searchPracticeSets(eq(kpA), anyList(), eq(3), eq(6))).thenAnswer(invocation -> {
            List<UUID> excluded = invocation.getArgument(1);
            return List.of(a1, a2).stream().filter(version -> !excluded.contains(version.packageId())
                    || version.packageId().equals(a1.packageId()))
                    .map(version -> new PracticeSet(version.packageId(), version.packageVersionId(), "PS", 3, 3))
                    .toList();
        });
        UUID review = failTwice(user, b1, b2);
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE root_review_id = ?",
                UUID.class, review);
        UUID probe = getDiagnostic.execute(user, sessionId).probes().getFirst().probeId();
        when(content.getPackageVersion(a1.packageVersionId())).thenThrow(
                new com.ieltspath.learning.application.exception.LearningRequestException(
                        404, "NOT_FOUND", "Package version was not found"));

        var reassigned = getProbe.execute(user, sessionId, probe);

        assertEquals("OPEN", reassigned.status());
        assertEquals(3, reassigned.questions().size());
        assertEquals(a2.packageId(), jdbc.queryForObject("SELECT package_id FROM diagnostic_probes WHERE id = ?",
                UUID.class, probe));
    }

    @Test
    void aProbeWithNoReplacementForItsUnpublishedPackageBecomesNoSetAndConcludes() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA);
        UUID review = failTwice(user, b1, b2);
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE root_review_id = ?",
                UUID.class, review);
        UUID probe = getDiagnostic.execute(user, sessionId).probes().getFirst().probeId();
        when(content.getPackageVersion(a1.packageVersionId())).thenThrow(
                new com.ieltspath.learning.application.exception.LearningRequestException(
                        404, "NOT_FOUND", "Package version was not found"));

        var unavailable = getProbe.execute(user, sessionId, probe);
        DiagnosticResult concluded = getDiagnostic.execute(user, sessionId);

        assertEquals("NO_SET", unavailable.status());
        assertTrue(unavailable.questions().isEmpty());
        assertEquals("CONCLUDED", concluded.status());
        assertEquals("UNKNOWN", concluded.probes().getFirst().result());
        assertEquals("INCONCLUSIVE", concluded.conclusionKind());
        assertEquals(1, jdbc.queryForObject("SELECT occurrences FROM content_gap_signals WHERE kp_id = ?",
                Integer.class, kpA));
    }

    @Test
    void twoPrerequisiteProbesReceiveDifferentPackagesFromTheSameCandidates() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA, kpZ));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA), a2 = practiceSet(kpA);
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(6))).thenReturn(List.of(
                new PracticeSet(a1.packageId(), a1.packageVersionId(), "PS", 3, 3),
                new PracticeSet(a2.packageId(), a2.packageVersionId(), "PS", 3, 3)));

        UUID review = failTwice(user, b1, b2);
        UUID sessionId = jdbc.queryForObject("SELECT id FROM diagnostic_sessions WHERE root_review_id = ?",
                UUID.class, review);
        DiagnosticResult result = getDiagnostic.execute(user, sessionId);

        assertEquals(2, result.probes().size());
        assertTrue(result.probes().stream().allMatch(probe -> "OPEN".equals(probe.status())));
        assertEquals(2, result.probes().stream().map(probe -> jdbc.queryForObject(
                "SELECT package_id FROM diagnostic_probes WHERE id = ?", UUID.class, probe.probeId())).distinct().count());
    }

    @Test
    void anotherLearnersReviewOrDiagnosticSessionReturnsNotFoundBeforeContentAccess() {
        UUID owner = UUID.randomUUID();
        UUID requester = UUID.randomUUID();
        UUID reviewId = insertReview(owner);
        UUID sessionId = UUID.randomUUID();
        UUID probeId = UUID.randomUUID();
        jdbc.update("INSERT INTO diagnostic_sessions (id, user_id, root_review_id, root_kp_id, status) "
                        + "VALUES (?, ?, ?, ?, 'PROBING')", sessionId, owner, reviewId, kpB);
        jdbc.update("INSERT INTO diagnostic_probes (id, session_id, kp_id, parent_kp_id, depth, package_id, "
                        + "package_version_id, status) VALUES (?, ?, ?, ?, 1, ?, ?, 'OPEN')",
                probeId, sessionId, kpA, kpB, UUID.randomUUID(), UUID.randomUUID());

        var reviewNotFound = assertThrows(com.ieltspath.learning.application.exception.LearningRequestException.class,
                () -> getReview.execute(requester, reviewId));
        var diagnosticNotFound = assertThrows(com.ieltspath.learning.application.exception.LearningRequestException.class,
                () -> getDiagnostic.execute(requester, sessionId));
        var probeNotFound = assertThrows(com.ieltspath.learning.application.exception.LearningRequestException.class,
                () -> getProbe.execute(requester, sessionId, probeId));
        var submissionNotFound = assertThrows(com.ieltspath.learning.application.exception.LearningRequestException.class,
                () -> submitProbe.execute(requester, sessionId, probeId,
                        new SubmitExerciseCommand(UUID.randomUUID(), List.of())));
        var skipNotFound = assertThrows(com.ieltspath.learning.application.exception.LearningRequestException.class,
                () -> skipDiagnostic.execute(requester, sessionId));

        assertEquals(404, reviewNotFound.getStatus());
        assertEquals(404, diagnosticNotFound.getStatus());
        assertEquals(404, probeNotFound.getStatus());
        assertEquals(404, submissionNotFound.getStatus());
        assertEquals(404, skipNotFound.getStatus());
        verifyNoInteractions(content);
    }

    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

    /**
     * The learner lock only exists inside a transaction, so a Content call made while a transaction is active is a call
     * made under the lock. Every call on the Content mock is observed, whether or not it is stubbed.
     */
    @Test
    void reviewAndDiagnosisUseCasesNeverCallContentInsideATransaction() {
        UUID user = UUID.randomUUID();
        prerequisites.put(kpB, List.of(kpA, kpZ));
        PackageVersion b1 = practiceSet(kpB), b2 = practiceSet(kpB);
        PackageVersion a1 = practiceSet(kpA);
        practiceSet(kpZ);
        topics.execute(user);
        UUID review = insertReview(user);

        java.util.concurrent.atomic.AtomicBoolean watching = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicInteger observed = new java.util.concurrent.atomic.AtomicInteger();
        List<String> insideTransaction = new ArrayList<>();
        org.mockito.listeners.InvocationListener listener = report -> {
            if (!watching.get()) return;
            observed.incrementAndGet();
            if (org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive()) {
                String caller = java.util.Arrays.stream(new Throwable().getStackTrace())
                        .filter(frame -> frame.getClassName().startsWith("com.ieltspath.learning.application"))
                        .limit(3).map(frame -> frame.getClassName().substring(
                                frame.getClassName().lastIndexOf('.') + 1) + "." + frame.getMethodName())
                        .collect(java.util.stream.Collectors.joining(" <- "));
                // The daily-quest listener runs before commit on every submission and reads the topic sequence; it
                // predates the review flow and belongs to the gamification code, so it is not judged here.
                if (caller.contains("QuestService")) return;
                insideTransaction.add(report.getInvocation() + " from " + caller);
            }
        };
        var listeners = org.mockito.Mockito.mockingDetails(content).getMockCreationSettings()
                .getInvocationListeners();
        listeners.add(listener);
        try {
            // Controls: the listener sees calls, and it flags one made inside a transaction.
            watching.set(true);
            content.getTopicSequence();
            assertEquals(1, observed.get());
            assertTrue(insideTransaction.isEmpty());
            new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> content.getTopicSequence());
            assertEquals(1, insideTransaction.size());
            insideTransaction.clear();
            observed.set(0);
            watching.set(false);

            var firstSet = watched(watching, () -> getReview.execute(user, review));
            watched(watching, () -> submitReview.execute(user, review, new SubmitReviewCommand(
                    firstSet.set().reviewSetId(), UUID.randomUUID(), answers(b1, "B", "B", "B").answers())));
            watched(watching, () -> theoryCheck.execute(user, review, new SubmitExerciseCommand(UUID.randomUUID(),
                    List.of(new SubmitExerciseCommand.Answer(lessonQuestion, "A")))));
            var secondSet = watched(watching, () -> getReview.execute(user, review));
            watched(watching, () -> submitReview.execute(user, review, new SubmitReviewCommand(
                    secondSet.set().reviewSetId(), UUID.randomUUID(), answers(b2, "B", "B", "B").answers())));
            var diagnosing = watched(watching, () -> getReview.execute(user, review));
            assertEquals(ReviewResult.DIAGNOSING, diagnosing.state());

            UUID sessionId = diagnosing.diagnosticSessionId();
            DiagnosticResult opening = getDiagnostic.execute(user, sessionId);
            UUID probeId = opening.probes().stream()
                    .filter(probe -> kpA.equals(probe.knowledgePoint().knowledgePointId()))
                    .findFirst().orElseThrow().probeId();
            watched(watching, () -> getProbe.execute(user, sessionId, probeId));
            watched(watching, () -> submitProbe.execute(user, sessionId, probeId, answers(a1, "B", "B", "B")));
            var skipped = watched(watching, () -> skipDiagnostic.execute(user, sessionId));
            assertEquals("CONCLUDED", skipped.status());
            assertEquals("SKIPPED", skipped.probes().stream()
                    .filter(probe -> kpZ.equals(probe.knowledgePoint().knowledgePointId()))
                    .findFirst().orElseThrow().status());
        } finally {
            listeners.remove(listener);
        }

        assertTrue(observed.get() > 0, "the flow reached Content");
        assertEquals(List.of(), insideTransaction);
    }

    private static <T> T watched(java.util.concurrent.atomic.AtomicBoolean watching,
                                 java.util.function.Supplier<T> call) {
        watching.set(true);
        try {
            return call.get();
        } finally {
            watching.set(false);
        }
    }

    private UUID insertReview(UUID user) {
        return insertReview(user, kpB, "ASSESSMENT");
    }

    private UUID insertReview(UUID user, UUID knowledgePointId, String triggerKind) {
        UUID review = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, "
                + "trigger_kind) VALUES (?, ?, ?, ?, 'PENDING', 'READING', ?)", review, user, knowledgePointId,
                lesson, triggerKind);
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
        // The diagnosis opens when the review is next read, not while the second set is submitted.
        getReview.execute(user, review);
        return review;
    }

    private PackageVersion practiceSet(UUID kp) {
        return practiceSet(kp, false);
    }

    private PackageVersion practiceSet(UUID kp, boolean tagged) {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        List<Item> items = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            Map<String, Object> answerSpec = tagged ? Map.of("type", "CHOICE", "correct", "A",
                    "distractors", Map.of("B", "TFNG_NG_AS_FALSE")) : Map.of("type", "CHOICE", "correct", "A");
            items.add(new Item(UUID.randomUUID(), index + 1, "Choose", options(), answerSpec,
                    "Because A", "Look again", BigDecimal.ONE,
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
