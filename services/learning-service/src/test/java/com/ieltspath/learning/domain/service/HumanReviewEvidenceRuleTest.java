package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.HumanReviewEvidenceRule.ErrorMark;
import com.ieltspath.learning.domain.service.HumanReviewEvidenceRule.Mark;
import com.ieltspath.learning.domain.service.HumanReviewEvidenceRule.Outcome;
import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanReviewEvidenceRuleTest {
    private static final UUID TA = id(1), TR = id(2), CC = id(3), LR = id(4), GRA = id(5);
    private static final UUID TRAP_LINKER = id(10), TRAP_ARTICLE = id(11), TRAP_WORD = id(12);
    private static final Map<String, UUID> TASK_2_POINTS = Map.of("TR", TR, "CC", CC, "LR", LR, "GRA", GRA);
    private static final Map<String, UUID> TRAPS = Map.of("LINKER_MISUSE", TRAP_LINKER, "ARTICLE", TRAP_ARTICLE,
            "WORD_CHOICE", TRAP_WORD);
    private static final Set<String> APPROVED = Set.of("LINKER_MISUSE", "ARTICLE", "TENSE");
    private static final BigDecimal SIX = new BigDecimal("6.0");

    private final HumanReviewEvidenceRule rule = new HumanReviewEvidenceRule();

    @Test
    void aBandAtThePassBandIsCorrectAndBelowItFailsWithTheRubricName() {
        Outcome met = rule.derive("TASK_2", SIX, List.of(new Mark("coherence", SIX)), TASK_2_POINTS, List.of(),
                TRAPS, APPROVED);
        assertEquals(true, met.correctByKnowledgePoint().get(CC));
        assertTrue(met.failedCriteria().isEmpty());

        Outcome missed = rule.derive("TASK_2", SIX, List.of(new Mark("coherence", new BigDecimal("5.5"))), TASK_2_POINTS,
                List.of(), TRAPS, APPROVED);
        assertEquals(false, missed.correctByKnowledgePoint().get(CC));
        assertEquals(List.of(new FailedWritingCriterion(CC, "DEMO_WRITING_CRITERION_CC", new BigDecimal("5.5"))),
                missed.failedCriteria());
        assertEquals(Set.of(CC), missed.criterionKnowledgePoints());
    }

    @Test
    void taskResponseIsTheTaskAchievementPointForTaskOne() {
        Map<String, UUID> task1 = Map.of("TA", TA, "CC", CC, "LR", LR, "GRA", GRA);

        Outcome outcome = rule.derive("TASK_1", SIX, List.of(new Mark("taskResponse", new BigDecimal("5.0"))), task1,
                List.of(), TRAPS, APPROVED);

        assertEquals(false, outcome.correctByKnowledgePoint().get(TA));
        assertEquals("DEMO_WRITING_CRITERION_TA", outcome.failedCriteria().get(0).code());
    }

    @Test
    void taskResponseIsTheTaskResponsePointForTaskTwo() {
        Outcome outcome = rule.derive("TASK_2", SIX, List.of(new Mark("taskResponse", new BigDecimal("6.5"))),
                TASK_2_POINTS, List.of(), TRAPS, APPROVED);

        assertEquals(true, outcome.correctByKnowledgePoint().get(TR));
    }

    @Test
    void withoutAPassBandNoCriterionIsJudgedButErrorTrapsStillCount() {
        Outcome outcome = rule.derive("TASK_2", null, List.of(new Mark("grammar", new BigDecimal("4.0"))), TASK_2_POINTS,
                List.of(new ErrorMark("LINKER_MISUSE", 2)), TRAPS, APPROVED);

        assertTrue(outcome.correctByKnowledgePoint().isEmpty());
        assertTrue(outcome.failedCriteria().isEmpty());
        assertEquals(List.of(new HumanReviewEvidenceRule.TrapOutcome("LINKER_MISUSE", TRAP_LINKER, 2)), outcome.traps());
    }

    @Test
    void unknownCriterionNamesAndPointsWithoutAMappingAreIgnored() {
        Map<String, UUID> onlyCoherence = Map.of("CC", CC);

        Outcome outcome = rule.derive("TASK_2", SIX,
                List.of(new Mark("creativity", new BigDecimal("3.0")), new Mark("lexical", new BigDecimal("3.0"))),
                onlyCoherence, List.of(), TRAPS, APPROVED);

        assertTrue(outcome.correctByKnowledgePoint().isEmpty());
        assertTrue(outcome.failedCriteria().isEmpty());
    }

    @Test
    void twoCriteriaOnOnePointMustBothPass() {
        Map<String, UUID> shared = Map.of("LR", LR, "GRA", LR);

        Outcome outcome = rule.derive("TASK_2", SIX,
                List.of(new Mark("lexical", new BigDecimal("7.0")), new Mark("grammar", new BigDecimal("5.0"))),
                shared, List.of(), TRAPS, APPROVED);

        assertFalse(outcome.correctByKnowledgePoint().get(LR));
    }

    @Test
    void onlyApprovedCodesWithATrapPointCount() {
        Outcome outcome = rule.derive("TASK_2", SIX, List.of(), TASK_2_POINTS,
                List.of(new ErrorMark("LINKER_MISUSE", 2),
                        new ErrorMark("TENSE", 1),
                        new ErrorMark("WORD_CHOICE", 3)),
                TRAPS, APPROVED);

        assertEquals(List.of(new HumanReviewEvidenceRule.TrapOutcome("LINKER_MISUSE", TRAP_LINKER, 2)), outcome.traps(),
                "TENSE has no trap point and WORD_CHOICE is not approved");
    }

    @Test
    void trapsComeMostFrequentFirstAndATieGoesToTheAlphabeticallyFirstCode() {
        Outcome outcome = rule.derive("TASK_2", SIX, List.of(), TASK_2_POINTS,
                List.of(new ErrorMark("LINKER_MISUSE", 1), new ErrorMark("ARTICLE", 2)),
                TRAPS, APPROVED);

        assertEquals(List.of(
                        new HumanReviewEvidenceRule.TrapOutcome("ARTICLE", TRAP_ARTICLE, 2),
                        new HumanReviewEvidenceRule.TrapOutcome("LINKER_MISUSE", TRAP_LINKER, 1)),
                outcome.traps());
    }

    private static UUID id(int suffix) {
        return UUID.fromString("40000000-0000-4000-8000-" + String.format("%012d", suffix));
    }
}
