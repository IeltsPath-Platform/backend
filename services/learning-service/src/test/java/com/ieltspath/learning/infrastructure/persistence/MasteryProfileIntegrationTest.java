package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.command.AssessmentResult.ItemResult;
import com.ieltspath.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.ieltspath.learning.application.command.UpdateRuleConfigCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Course;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.port.LearningContentClient.Topic;
import com.ieltspath.learning.application.result.LearnerReportResult;
import com.ieltspath.learning.application.result.LearnerReportResult.KnowledgePointReport;
import com.ieltspath.learning.application.service.MasteryProfileMaintenance;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.application.usecase.ApplyAssessmentResultUseCase;
import com.ieltspath.learning.application.usecase.GetLearnerReportUseCase;
import com.ieltspath.learning.application.usecase.ManageRuleConfigUseCase;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false",
        "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class MasteryProfileIntegrationTest {
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
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID NEXT_TOPIC = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID KP_STRONG = UUID.randomUUID();
    private static final UUID KP_WEAK = UUID.randomUUID();
    private static final UUID KP_MIDDLE = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final Instant COMPLETED = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired JdbcTemplate jdbc;
    @Autowired ApplyAssessmentResultUseCase apply;
    @Autowired MasteryProfileMaintenance maintenance;
    @Autowired MasteryProfileUpdater updater;
    @Autowired KnowledgeEvidenceRepository evidence;
    @Autowired MasteryProfileRepository profiles;
    @Autowired ManageRuleConfigUseCase ruleConfig;
    @Autowired GetLearnerReportUseCase report;
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
        jdbc.update("INSERT INTO topic_progress (user_id, topic_id, sequence_order) VALUES (?, ?, 1), (?, ?, 2)",
                USER, TOPIC, USER, NEXT_TOPIC);
        jdbc.update("""
                INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set)
                VALUES (?, ?, TRUE), (?, ?, TRUE), (?, ?, TRUE)
                """, KP_STRONG, TOPIC, KP_WEAK, TOPIC, KP_MIDDLE, TOPIC);
        jdbc.update("""
                INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, knowledge_point_ids, completed_at)
                VALUES (?, ?, ?, 1, ARRAY[?, ?, ?]::uuid[], now())
                """, USER, LESSON, TOPIC, KP_STRONG, KP_WEAK, KP_MIDDLE);
        jdbc.update("""
                INSERT INTO topic_test_assignments (id, user_id, topic_id, package_id, package_version_id, assigned_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), USER, TOPIC, UUID.randomUUID(), VERSION,
                Timestamp.from(COMPLETED.minusSeconds(600)));
    }

    @Test
    void trapEvidenceBuildsChildMasteryButDefaultLearnerReadsExcludeIt() {
        UUID trap = UUID.randomUUID();
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "T1", "Topic one", 1, null,
                List.of(new KnowledgePoint(KP_WEAK, "WEAK", "Weak point", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(trap, "TRAP", "Trap label", "PROCEDURE", "READING", "", false,
                                null, null, KP_WEAK, "PARAPHRASE_TRAP")), LearningSkill.READING, true)));
        KnowledgeEvidence parentEvidence = KnowledgeEvidence.of(KP_WEAK, true, EvidenceSource.LESSON_EXERCISE,
                UUID.randomUUID()).forQuestion(UUID.randomUUID());
        evidence.append(USER, List.of(parentEvidence));
        updater.recordEvidence(USER, List.of(parentEvidence));
        LearnerReportResult beforeTrap = report.execute(USER);
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, parent_kp_id, error_code) " +
                        "VALUES (?, ?, FALSE, ?, ?)", trap, TOPIC, KP_WEAK, "PARAPHRASE_TRAP");
        KnowledgeEvidence answer = KnowledgeEvidence.of(trap, false, EvidenceSource.ERROR_TRAP, UUID.randomUUID())
                .forQuestion(UUID.randomUUID());
        evidence.append(USER, List.of(answer));
        updater.recordEvidence(USER, List.of(answer), java.util.Set.of(trap));

        assertThat(jdbc.queryForObject("SELECT source FROM kp_evidence WHERE kp_id = ?", String.class, trap))
                .isEqualTo("error_trap");
        assertThat(profiles.find(USER, List.of(trap))).containsKey(trap);
        assertThat(profiles.findErrorTrapProfiles(USER)).extracting(profile -> profile.knowledgePointId())
                .containsExactly(trap);
        assertThat(profiles.findAll(USER)).extracting(profile -> profile.knowledgePointId()).doesNotContain(trap);
        assertThat(evidence.findMasteryHistories(USER)).extracting(history -> history.knowledgePointId())
                .doesNotContain(trap);
        assertThat(evidence.findMasteryHistories(USER, List.of(trap)))
                .extracting(history -> history.knowledgePointId()).containsExactly(trap);

        LearnerReportResult afterTrap = report.execute(USER);
        assertThat(afterTrap.estimatedBand()).isEqualTo(beforeTrap.estimatedBand());
        assertThat(afterTrap.skills()).isEqualTo(beforeTrap.skills());
        assertThat(afterTrap.focus()).isEqualTo(beforeTrap.focus());
        assertThat(afterTrap.knowledgePoints()).extracting(KnowledgePointReport::knowledgeId)
                .doesNotContain(trap);
        assertThat(afterTrap.knowledgePoints().stream().filter(point -> point.knowledgeId().equals(KP_WEAK))
                .findFirst().orElseThrow().errorTraps()).extracting(LearnerReportResult.ErrorTrapPoint::knowledgePointId)
                .containsExactly(trap);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM xp_ledger WHERE user_id = ? "
                + "AND source IN ('KP_CONSOLIDATE', 'KP_MAINTAIN')", Integer.class, USER)).isZero();
    }

    @Test
    void anAssessmentResultWritesATierADueDateTheMedianTimeAndTheTopError() {
        apply.execute(result(UUID.randomUUID(), 1, 3, true, true));

        Map<String, Object> strong = profile(KP_STRONG);
        assertThat(strong.get("tier")).isEqualTo("MAINTAIN");
        assertThat(((BigDecimal) strong.get("mastery")).doubleValue()).isEqualTo(1.0);
        assertThat(strong.get("success_streak")).isEqualTo(1);
        assertThat(strong.get("evidence_count")).isEqualTo(3);
        assertThat(strong.get("median_duration_ms")).isEqualTo(4000L);
        assertThat(jdbc.queryForObject("SELECT next_review_at = last_evidence_at + interval '14 days' "
                + "FROM kp_mastery_profiles WHERE user_id = ? AND knowledge_point_id = ?", Boolean.class,
                USER, KP_STRONG)).isTrue();

        Map<String, Object> weak = profile(KP_WEAK);
        assertThat(weak.get("tier")).isEqualTo("PRIORITY");
        assertThat(weak.get("success_streak")).isEqualTo(0);
        assertThat(weak.get("next_review_at")).isNull();
        assertThat(jdbc.queryForObject("SELECT top_error_codes[1] FROM kp_mastery_profiles "
                + "WHERE user_id = ? AND knowledge_point_id = ?", String.class, USER, KP_WEAK))
                .isEqualTo("PARAPHRASE_TRAP");

        assertThat(profile(KP_MIDDLE).get("tier")).isEqualTo("CONSOLIDATE");
        assertThat(jdbc.queryForObject("SELECT next_review_at = last_evidence_at + interval '3 days' "
                + "FROM kp_mastery_profiles WHERE user_id = ? AND knowledge_point_id = ?", Boolean.class,
                USER, KP_MIDDLE)).isTrue();
    }

    @Test
    void redeliveringTheSameResultVersionChangesNothing() {
        AssessmentResult first = result(UUID.randomUUID(), 1, 3, true, true);
        apply.execute(first);
        Timestamp updated = jdbc.queryForObject("SELECT updated_at FROM kp_mastery_profiles WHERE user_id = ? "
                + "AND knowledge_point_id = ?", Timestamp.class, USER, KP_STRONG);

        apply.execute(first);

        assertThat(jdbc.queryForObject("SELECT updated_at FROM kp_mastery_profiles WHERE user_id = ? "
                + "AND knowledge_point_id = ?", Timestamp.class, USER, KP_STRONG)).isEqualTo(updated);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_mastery_profiles", Integer.class)).isEqualTo(3);
    }

    @Test
    void aRegradeRebuildsProfilesAndDropsAPointThatLostAllItsEvidence() {
        UUID attempt = UUID.randomUUID();
        apply.execute(result(attempt, 1, 3, true, true));
        assertThat(profile(KP_WEAK)).isNotNull();

        apply.execute(result(attempt, 2, 1, false, false));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_mastery_profiles WHERE knowledge_point_id = ?",
                Integer.class, KP_WEAK)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_mastery_profiles WHERE knowledge_point_id = ?",
                Integer.class, KP_MIDDLE)).isZero();
        Map<String, Object> strong = profile(KP_STRONG);
        assertThat(strong.get("evidence_count")).isEqualTo(1);
        assertThat(strong.get("success_streak")).isEqualTo(1);
    }

    @Test
    void changingTheRulesMovesTiersOnlyAfterTheRecompute() {
        apply.execute(result(UUID.randomUUID(), 1, 3, true, true));
        assertThat(profile(KP_MIDDLE).get("tier")).isEqualTo("CONSOLIDATE");

        ruleConfig.update(ADMIN, new UpdateRuleConfigCommand(0.75, 0.9, null, null));
        assertThat(profile(KP_MIDDLE).get("tier")).as("stored profiles wait for the recompute").isEqualTo("CONSOLIDATE");

        maintenance.recomputeAll();

        Map<String, Object> middle = profile(KP_MIDDLE);
        assertThat(middle.get("tier")).isEqualTo("PRIORITY");
        assertThat(middle.get("next_review_at")).isNull();
        assertThat(((BigDecimal) middle.get("mastery")).doubleValue()).isEqualTo(0.714);
        assertThat(profile(KP_STRONG).get("tier")).isEqualTo("MAINTAIN");
    }

    @Test
    void theNewThresholdAlsoDecidesWhichPointsGetABlockingReview() {
        ruleConfig.update(ADMIN, new UpdateRuleConfigCommand(0.75, 0.9, null, null));

        apply.execute(result(UUID.randomUUID(), 1, 3, true, true));

        // KP_MIDDLE scores 0.714: not blocking at the default 0.6, blocking at the configured 0.75.
        assertThat(jdbc.queryForList("SELECT knowledge_point_id FROM review_items WHERE user_id = ? "
                + "AND status = 'PENDING'", UUID.class, USER)).contains(KP_MIDDLE, KP_WEAK);
    }

    @Test
    void anInvalidRuleChangeIsRejectedAndStoresNothing() {
        assertThatThrownBy(() -> ruleConfig.update(ADMIN, new UpdateRuleConfigCommand(0.9, null, null, null)))
                .isInstanceOfSatisfying(LearningRequestException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(422);
                    assertThat(exception.getCode()).isEqualTo("INVALID_RULE_CONFIG");
                });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM learning_rule_configs", Integer.class)).isZero();
        assertThat(ruleConfig.get().overridden()).isEmpty();
    }

    @Test
    void anAdminChangeIsStoredWithItsAuthorAndShownAsOverridden() {
        var changed = ruleConfig.update(ADMIN, new UpdateRuleConfigCommand(null, null, List.of(2, 4, 8), 5));

        assertThat(changed.reviewIntervalsDays()).containsExactly(2, 4, 8);
        assertThat(changed.consolidateDays()).isEqualTo(5);
        assertThat(changed.priorityBelow()).isEqualTo(0.6);
        assertThat(changed.overridden()).containsExactlyInAnyOrder("mastery.review-intervals-days",
                "review.consolidate-days");
        assertThat(jdbc.queryForObject("SELECT updated_by FROM learning_rule_configs WHERE key = ?", UUID.class,
                "review.consolidate-days")).isEqualTo(ADMIN);
    }

    @Test
    void backfillBuildsProfilesFromOlderEvidenceAndRunningItAgainChangesNothing() {
        for (boolean correct : new boolean[]{false, true, true}) {
            jdbc.update("INSERT INTO kp_evidence (id, user_id, kp_id, correct, source, source_reference_id) "
                    + "VALUES (?, ?, ?, ?, 'lesson_exercise', ?)", UUID.randomUUID(), USER, KP_STRONG, correct,
                    UUID.randomUUID());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_mastery_profiles", Integer.class)).isZero();

        maintenance.backfill();

        Map<String, Object> built = profile(KP_STRONG);
        assertThat(built.get("evidence_count")).isEqualTo(3);
        assertThat(built.get("success_streak")).isEqualTo(2);
        Timestamp updated = (Timestamp) built.get("updated_at");

        maintenance.backfill();

        assertThat(profile(KP_STRONG).get("updated_at")).isEqualTo(updated);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_mastery_profiles", Integer.class)).isEqualTo(1);
    }

    @Test
    void theReportCarriesScoreErrorsEvidenceAndActionForEveryPointWithEvidence() {
        UUID course = UUID.randomUUID();
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "T1", "Topic one", 1, null,
                List.of(new KnowledgePoint(KP_STRONG, "R_STRONG", "Strong point", "SKILL", "READING", null, true),
                        new KnowledgePoint(KP_WEAK, "R_WEAK", "Weak point", "SKILL", "READING", null, true),
                        new KnowledgePoint(KP_MIDDLE, "R_MIDDLE", "Middle point", "SKILL", "READING", null, true)),
                LearningSkill.READING, true, new Course(course, "C60", "Band 6.0", new BigDecimal("6.0"), true))));
        apply.execute(result(UUID.randomUUID(), 1, 3, true, true));

        LearnerReportResult result = report.execute(USER);

        assertThat(result.knowledgePoints()).extracting(KnowledgePointReport::code)
                .as("weakest tier first").containsExactly("R_WEAK", "R_MIDDLE", "R_STRONG");
        KnowledgePointReport weak = result.knowledgePoints().get(0);
        assertThat(weak.name()).isEqualTo("Weak point");
        assertThat(weak.skill()).isEqualTo("READING");
        assertThat(weak.band()).isEqualByComparingTo("6.0");
        assertThat(weak.score().tier()).isEqualTo("PRIORITY");
        assertThat(weak.errors()).as("a parent error summary requires trap child mastery").isEmpty();
        assertThat(weak.evidence()).singleElement().satisfies(entry -> {
            assertThat(entry.correct()).isFalse();
            assertThat(entry.durationMs()).isEqualTo(4000L);
            assertThat(entry.source()).isEqualTo("assessment");
            assertThat(entry.errorCode()).isEqualTo("PARAPHRASE_TRAP");
        });
        assertThat(weak.action().type()).isEqualTo("REVIEW_REQUIRED");
        assertThat(weak.action().reviewId()).isNotNull();

        KnowledgePointReport strong = result.knowledgePoints().get(2);
        assertThat(strong.evidence()).hasSize(3);
        assertThat(strong.errors()).isEmpty();
        assertThat(strong.action().type()).isEqualTo("NONE");
        assertThat(strong.action().dueAt()).isNotNull();
    }

    @Test
    void aLearnerWithoutEvidenceGetsAnEmptyReportWithoutAskingContent() {
        assertThat(report.execute(UUID.randomUUID()).knowledgePoints()).isEmpty();
    }

    private Map<String, Object> profile(UUID knowledgePoint) {
        return jdbc.queryForMap("SELECT * FROM kp_mastery_profiles WHERE user_id = ? AND knowledge_point_id = ?",
                USER, knowledgePoint);
    }

    /**
     * KP_STRONG gets {@code strongItems} right answers (4 s each). KP_WEAK gets one wrong answer with an error code, and
     * KP_MIDDLE right, right, right, wrong (0.714), each only when {@code includeWeak} / {@code includeMiddle}.
     */
    private static AssessmentResult result(UUID attempt, int version, int strongItems, boolean includeWeak,
                                           boolean includeMiddle) {
        List<ItemResult> items = new ArrayList<>();
        for (int index = 0; index < strongItems; index++) items.add(item(true, KP_STRONG, 4000L, null));
        if (includeWeak) items.add(item(false, KP_WEAK, 4000L, "PARAPHRASE_TRAP"));
        if (includeMiddle) {
            for (int index = 0; index < 3; index++) items.add(item(true, KP_MIDDLE, 5000L, null));
            items.add(item(false, KP_MIDDLE, 5000L, "SPELLING"));
        }
        return new AssessmentResult(UUID.randomUUID(), USER, VERSION, attempt, UUID.randomUUID(), version,
                "TOPIC_GATE", COMPLETED, items);
    }

    private static ItemResult item(boolean correct, UUID knowledgePoint, Long durationMs, String errorCode) {
        return new ItemResult(UUID.randomUUID(), UUID.randomUUID(), correct,
                correct ? BigDecimal.ONE : BigDecimal.ZERO, BigDecimal.ONE,
                List.of(new KnowledgePointJudgment(knowledgePoint, null)), durationMs, errorCode);
    }
}
