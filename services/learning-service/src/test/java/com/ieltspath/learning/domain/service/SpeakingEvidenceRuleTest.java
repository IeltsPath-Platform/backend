package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.SpeakingEvidenceRule.Criterion;
import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeakingEvidenceRuleTest {
    private static final UUID QUESTION_KP = UUID.randomUUID(), FC = UUID.randomUUID(), LR = UUID.randomUUID(),
            GRA = UUID.randomUUID(), PR = UUID.randomUUID(), TRAP_A = UUID.randomUUID(), TRAP_B = UUID.randomUUID();
    private static final BigDecimal PASS = new BigDecimal("5.5");
    private final SpeakingEvidenceRule rule = new SpeakingEvidenceRule();

    private static Criterion criterion(String code, UUID kp, String band) {
        return new Criterion(code, kp, band == null ? null : new BigDecimal(band));
    }

    private static List<Criterion> allFour(String fc, String lr, String gra, String pr) {
        return List.of(criterion("FC", FC, fc), criterion("LR", LR, lr), criterion("GRA", GRA, gra), criterion("PR", PR, pr));
    }

    @Test
    void theQuestionPointTakesTheAnswersPassAndEachCriterionPointItsOwnBand() {
        var outcome = rule.derive(List.of(QUESTION_KP), PASS, true, allFour("6.0", "5.5", "5.0", "7.0"), List.of(), Map.of());

        assertEquals(Map.of(QUESTION_KP, true, FC, true, LR, true, GRA, false, PR, true), outcome.correctByKnowledgePoint());
        assertEquals(Set.of(FC, LR, GRA, PR), outcome.criterionKnowledgePoints());
        assertEquals(List.of(QUESTION_KP, FC, LR, GRA, PR), List.copyOf(outcome.correctByKnowledgePoint().keySet()),
                "question points first, then criterion points");
    }

    @Test
    void aCriterionExactlyAtThePassBandHasReachedIt() {
        var outcome = rule.derive(List.of(), PASS, true, List.of(criterion("FC", FC, "5.5")), List.of(), Map.of());

        assertEquals(true, outcome.correctByKnowledgePoint().get(FC));
        assertTrue(outcome.failedCriteria().isEmpty());
    }

    @Test
    void onlyMappedCriteriaBelowThePassBandAreFailed() {
        var outcome = rule.derive(List.of(QUESTION_KP), PASS, false,
                List.of(criterion("FC", FC, "4.5"), criterion("LR", LR, "6.5"), criterion("GRA", null, "3.0")),
                List.of(), Map.of());

        assertEquals(List.of(new FailedWritingCriterion(FC, "FC", new BigDecimal("4.5"))), outcome.failedCriteria());
        assertEquals(Map.of(QUESTION_KP, false, FC, false, LR, true), outcome.correctByKnowledgePoint());
    }

    @Test
    void aPointThatIsBothAQuestionPointAndACriterionPointTakesTheCriterionResult() {
        var outcome = rule.derive(List.of(FC), PASS, true, List.of(criterion("FC", FC, "4.0")), List.of(), Map.of());

        assertEquals(Map.of(FC, false), outcome.correctByKnowledgePoint());
        assertEquals(Set.of(FC), outcome.criterionKnowledgePoints());
    }

    @Test
    void twoCriteriaOnOnePointMustBothPass() {
        var outcome = rule.derive(List.of(), PASS, true,
                List.of(criterion("FC", FC, "6.5"), criterion("LR", FC, "4.0")), List.of(), Map.of());

        assertEquals(false, outcome.correctByKnowledgePoint().get(FC));
        assertEquals(1, outcome.failedCriteria().size());
    }

    @Test
    void criteriaWithoutABandOrACodeAreSkipped() {
        var outcome = rule.derive(List.of(QUESTION_KP), PASS, true,
                Arrays.asList(criterion("FC", FC, null), new Criterion(null, LR, PASS), null), List.of(), Map.of());

        assertEquals(Map.of(QUESTION_KP, true), outcome.correctByKnowledgePoint());
        assertTrue(outcome.criterionKnowledgePoints().isEmpty());
        assertTrue(outcome.failedCriteria().isEmpty());
    }

    @Test
    void withoutAPassBandOrCriteriaOnlyTheQuestionPointCounts() {
        assertEquals(Map.of(QUESTION_KP, false),
                rule.derive(List.of(QUESTION_KP), null, false, allFour("2.0", "2.0", "2.0", "2.0"), List.of(), Map.of())
                        .correctByKnowledgePoint());
        assertEquals(Map.of(QUESTION_KP, true),
                rule.derive(List.of(QUESTION_KP), PASS, true, null, null, null).correctByKnowledgePoint());
    }

    @Test
    void aMarkedErrorWithATrapPointCountsOnceNoMatterHowOftenItWasMarked() {
        var outcome = rule.derive(List.of(), PASS, true, List.of(),
                List.of("SPK_NO_REASON", "SPK_NO_REASON", "SPK_NO_LINKER"),
                Map.of("SPK_NO_REASON", TRAP_A, "SPK_NO_LINKER", TRAP_B));

        assertEquals(List.of(TRAP_A, TRAP_B), List.copyOf(outcome.trapKnowledgePoints()));
    }

    @Test
    void anErrorCodeWithNoTrapPointIsIgnored() {
        var outcome = rule.derive(List.of(), PASS, true, List.of(), Arrays.asList("SPK_UNKNOWN", null, ""),
                Map.of("SPK_NO_REASON", TRAP_A));

        assertTrue(outcome.trapKnowledgePoints().isEmpty());
    }

    @Test
    void theOutcomeCannotBeChangedAfterwards() {
        var outcome = rule.derive(List.of(QUESTION_KP), PASS, true, allFour("6.0", "6.0", "6.0", "6.0"), List.of(), Map.of());

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> outcome.correctByKnowledgePoint().put(UUID.randomUUID(), true));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> outcome.trapKnowledgePoints().add(UUID.randomUUID()));
    }
}
