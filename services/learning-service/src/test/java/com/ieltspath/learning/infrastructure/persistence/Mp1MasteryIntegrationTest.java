package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.command.AssessmentResult.ItemResult;
import com.ieltspath.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.ReviewBasketResult;
import com.ieltspath.learning.application.service.MasteryProfileMaintenance;
import com.ieltspath.learning.application.usecase.ApplyAssessmentResultUseCase;
import com.ieltspath.learning.application.usecase.GetReviewBasketUseCase;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.vo.EvidencePoint;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
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
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The service running the MP-1.0 mastery formula end to end against a real database. */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false",
        "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false",
        "learning.mastery.policy=MP1",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class Mp1MasteryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db")
            .withPassword(UUID.randomUUID().toString());

    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID KP_STRONG = UUID.randomUUID();
    private static final UUID KP_WEAK = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final Instant COMPLETED = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired JdbcTemplate jdbc;
    @Autowired ApplyAssessmentResultUseCase apply;
    @Autowired MasteryProfileMaintenance maintenance;
    @Autowired GetReviewBasketUseCase basket;
    @Autowired KnowledgeEvidenceRepository evidence;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE learner_placements");
        jdbc.execute("""
                TRUNCATE review_theory_checks, diagnostic_probes, diagnostic_sessions,review_sets, review_items, lesson_exercise_submissions, kp_evidence,
                lesson_progress, topic_progress, knowledge_point_catalog, topic_test_assignments,
                assessment_result_versions, kp_mastery_profiles, question_attempt_events, learning_rule_configs
                RESTART IDENTITY
                """);
        jdbc.update("INSERT INTO topic_progress (user_id, topic_id, sequence_order) VALUES (?, ?, 1)", USER, TOPIC);
        jdbc.update("""
                INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set)
                VALUES (?, ?, TRUE), (?, ?, TRUE)
                """, KP_STRONG, TOPIC, KP_WEAK, TOPIC);
        jdbc.update("""
                INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, knowledge_point_ids, completed_at)
                VALUES (?, ?, ?, 1, ARRAY[?, ?]::uuid[], now())
                """, USER, LESSON, TOPIC, KP_STRONG, KP_WEAK);
        jdbc.update("""
                INSERT INTO topic_test_assignments (id, user_id, topic_id, package_id, package_version_id, assigned_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), USER, TOPIC, UUID.randomUUID(), VERSION,
                Timestamp.from(COMPLETED.minusSeconds(600)));
    }

    @Test
    void aPointWithTooLittleEvidenceIsInsufficientAndStaysOutOfTheBasketButKeepsItsBlockingReview() {
        apply.execute(result());

        // Three right answers on one day are not enough to judge KP_STRONG; one wrong answer is not enough for KP_WEAK.
        assertThat(profile(KP_STRONG).get("tier")).isEqualTo("INSUFFICIENT");
        assertThat(profile(KP_WEAK).get("tier")).isEqualTo("INSUFFICIENT");
        assertThat(profile(KP_STRONG).get("next_review_at")).isNull();
        assertThat(profile(KP_STRONG).get("confidence")).isNotNull();

        // The blocking review that a wrong answer raises does not depend on the tier.
        assertThat(jdbc.queryForList("SELECT knowledge_point_id FROM review_items WHERE user_id = ? "
                + "AND status = 'PENDING'", UUID.class, USER)).contains(KP_WEAK);
        ReviewBasketResult result = basket.execute(USER);
        assertThat(result.required()).extracting(ReviewBasketResult.RequiredItem::knowledgePointId)
                .containsExactly(KP_WEAK);
        assertThat(result.consolidate()).isEmpty();
        assertThat(result.due()).isEmpty();
    }

    @Test
    void evidenceOverTwoDaysIsJudgedAndAnOldPointFadesBackToInsufficient() {
        Instant now = Instant.now();
        addEvidence(KP_STRONG, true, now.minus(Duration.ofHours(26)));
        addEvidence(KP_STRONG, true, now.minus(Duration.ofHours(2)));
        addEvidence(KP_STRONG, true, now.minus(Duration.ofHours(1)));

        maintenance.backfill();

        Map<String, Object> judged = profile(KP_STRONG);
        assertThat(judged.get("tier")).isEqualTo("MAINTAIN");
        assertThat(((BigDecimal) judged.get("confidence")).doubleValue()).isGreaterThan(2.9);
        assertThat(judged.get("success_streak")).isEqualTo(3);

        // Four months pass without a new answer.
        jdbc.update("UPDATE kp_evidence SET created_at = created_at - interval '120 days'");
        jdbc.update("UPDATE kp_mastery_profiles SET last_evidence_at = last_evidence_at - interval '120 days'");
        int refreshed = maintenance.decayStale(Duration.ofHours(24));

        assertThat(refreshed).isEqualTo(1);
        Map<String, Object> faded = profile(KP_STRONG);
        assertThat(faded.get("tier")).isEqualTo("INSUFFICIENT");
        assertThat(faded.get("next_review_at")).isNull();
        assertThat(((BigDecimal) faded.get("confidence")).doubleValue()).isLessThan(1.0);
        assertThat(faded.get("success_streak")).as("no new answer, so the streak is kept").isEqualTo(3);
    }

    @Test
    void aFreshProfileIsNotTouchedByTheDecayRound() {
        addEvidence(KP_STRONG, true, Instant.now().minus(Duration.ofHours(26)));
        addEvidence(KP_STRONG, true, Instant.now().minus(Duration.ofHours(2)));
        addEvidence(KP_STRONG, true, Instant.now().minus(Duration.ofHours(1)));
        maintenance.backfill();
        Timestamp updated = (Timestamp) profile(KP_STRONG).get("updated_at");

        assertThat(maintenance.decayStale(Duration.ofHours(24))).isZero();

        assertThat(profile(KP_STRONG).get("updated_at")).isEqualTo(updated);
    }

    @Test
    void theQuestionOfAnAnswerIsStoredAndReadBack() {
        UUID question = UUID.randomUUID();
        evidence.append(USER, List.of(
                KnowledgeEvidence.of(KP_STRONG, true, EvidenceSource.PRACTICE_SET, UUID.randomUUID())
                        .forQuestion(question),
                KnowledgeEvidence.of(KP_STRONG, false, EvidenceSource.REVIEW_SET, UUID.randomUUID())));

        List<EvidencePoint> points = evidence.findEvidencePoints(USER, List.of(KP_STRONG), 10).get(KP_STRONG);

        assertThat(points).extracting(EvidencePoint::source)
                .containsExactly(EvidenceSource.PRACTICE_SET, EvidenceSource.REVIEW_SET);
        assertThat(points).extracting(EvidencePoint::questionVersionId).containsExactly(question, null);
    }

    @Test
    void rebuildingEveryProfileKeepsTheFormulaInForce() {
        Instant now = Instant.now();
        addEvidence(KP_STRONG, true, now.minus(Duration.ofHours(26)));
        addEvidence(KP_STRONG, true, now.minus(Duration.ofHours(2)));
        addEvidence(KP_STRONG, false, now.minus(Duration.ofHours(1)));
        maintenance.backfill();
        jdbc.update("UPDATE kp_mastery_profiles SET tier = 'MAINTAIN', mastery = 0.95");

        maintenance.recomputeAll();

        Map<String, Object> rebuilt = profile(KP_STRONG);
        assertThat(((BigDecimal) rebuilt.get("mastery")).doubleValue()).isBetween(0.6, 0.7);
        assertThat(rebuilt.get("tier")).isEqualTo("CONSOLIDATE");
    }

    private void addEvidence(UUID knowledgePoint, boolean correct, Instant at) {
        jdbc.update("INSERT INTO kp_evidence (id, user_id, kp_id, correct, source, source_reference_id, created_at) "
                + "VALUES (?, ?, ?, ?, 'review_set', ?, ?)", UUID.randomUUID(), USER, knowledgePoint, correct,
                UUID.randomUUID(), Timestamp.from(at));
    }

    private Map<String, Object> profile(UUID knowledgePoint) {
        return jdbc.queryForMap("SELECT * FROM kp_mastery_profiles WHERE user_id = ? AND knowledge_point_id = ?",
                USER, knowledgePoint);
    }

    /** KP_STRONG gets three right answers and KP_WEAK one wrong answer, all in one submission. */
    private static AssessmentResult result() {
        List<ItemResult> items = new ArrayList<>();
        for (int index = 0; index < 3; index++) items.add(item(true, KP_STRONG, null));
        items.add(item(false, KP_WEAK, "PARAPHRASE_TRAP"));
        return new AssessmentResult(UUID.randomUUID(), USER, VERSION, UUID.randomUUID(), UUID.randomUUID(), 1,
                "TOPIC_GATE", COMPLETED, items);
    }

    private static ItemResult item(boolean correct, UUID knowledgePoint, String errorCode) {
        return new ItemResult(UUID.randomUUID(), UUID.randomUUID(), correct,
                correct ? BigDecimal.ONE : BigDecimal.ZERO, BigDecimal.ONE,
                List.of(new KnowledgePointJudgment(knowledgePoint, null)), 4000L, errorCode);
    }
}
