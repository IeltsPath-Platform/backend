package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WritingEvidenceRuleTest {
    private static final BigDecimal PASS = new BigDecimal("6.0");
    private static final UUID QUESTION_KP = UUID.randomUUID();
    private static final UUID TR = UUID.randomUUID();
    private static final UUID CC = UUID.randomUUID();
    private static final UUID LR = UUID.randomUUID();
    private static final UUID GRA = UUID.randomUUID();

    private final WritingEvidenceRule rule = new WritingEvidenceRule();

    private static WritingGrade.Criterion criterion(String code, String band, UUID point) {
        return new WritingGrade.Criterion(code, new BigDecimal(band), List.of(), List.of(), point);
    }

    private static WritingGrade.KnowledgeJudgment judgment(UUID point, WritingGrade.Judgment judgment) {
        return new WritingGrade.KnowledgeJudgment(point, "SUB", "Sub point", judgment, "excerpt", "reason");
    }

    private static WritingGrade grade(List<WritingGrade.Criterion> criteria, List<WritingGrade.Correction> corrections) {
        return new WritingGrade(criteria, corrections, "ok", new BigDecimal("6.0"));
    }

    private static WritingGrade.Correction correction(String errorCode) {
        return new WritingGrade.Correction("excerpt", "fix", "GRAMMAR", errorCode);
    }

    @Test
    void eachCriterionIsCorrectAtOrAboveThePassBandAndTheQuestionPointTakesThePass() {
        WritingGrade grade = grade(List.of(criterion("TR", "6.5", TR), criterion("CC", "5.5", CC),
                criterion("LR", "6.0", LR), criterion("GRA", "5.0", GRA)), List.of());

        WritingEvidenceRule.Outcome outcome = rule.derive(List.of(QUESTION_KP), PASS, true, grade);

        assertEquals(Map.of(QUESTION_KP, true, TR, true, CC, false, LR, true, GRA, false),
                outcome.correctByKnowledgePoint());
        assertEquals(List.of(QUESTION_KP, TR, CC, LR, GRA), List.copyOf(outcome.correctByKnowledgePoint().keySet()));
        assertEquals(Set.of(TR, CC, LR, GRA), outcome.criterionKnowledgePoints());
    }

    @Test
    void aFailedEssayStillGivesCriteriaTheirOwnResult() {
        WritingGrade grade = grade(List.of(criterion("TR", "6.5", TR), criterion("CC", "6.0", CC)), List.of());

        WritingEvidenceRule.Outcome outcome = rule.derive(List.of(QUESTION_KP), PASS, false, grade);

        assertFalse(outcome.correctByKnowledgePoint().get(QUESTION_KP));
        assertTrue(outcome.correctByKnowledgePoint().get(TR));
        assertTrue(outcome.correctByKnowledgePoint().get(CC));
    }

    @Test
    void aCriterionOnTheQuestionPointTakesTheCriterionResult() {
        WritingGrade grade = grade(List.of(criterion("CC", "5.5", QUESTION_KP)), List.of());

        WritingEvidenceRule.Outcome outcome = rule.derive(List.of(QUESTION_KP), PASS, true, grade);

        assertEquals(Map.of(QUESTION_KP, false), outcome.correctByKnowledgePoint());
        assertEquals(Set.of(QUESTION_KP), outcome.criterionKnowledgePoints());
    }

    @Test
    void twoCriteriaOnOnePointMustBothReachThePassBand() {
        WritingGrade grade = grade(List.of(criterion("LR", "6.5", LR), criterion("GRA", "5.0", LR)), List.of());

        WritingEvidenceRule.Outcome outcome = rule.derive(List.of(QUESTION_KP), PASS, true, grade);

        assertFalse(outcome.correctByKnowledgePoint().get(LR));
    }

    @Test
    void aCriterionWithoutAPointIsIgnored() {
        WritingGrade grade = grade(List.of(criterion("TR", "3.0", null), criterion("CC", "7.0", CC)), List.of());

        WritingEvidenceRule.Outcome outcome = rule.derive(List.of(QUESTION_KP), PASS, true, grade);

        assertEquals(Map.of(QUESTION_KP, true, CC, true), outcome.correctByKnowledgePoint());
        assertEquals(Set.of(CC), outcome.criterionKnowledgePoints());
    }

    @Test
    void aGradeStoredBeforeTheMappingLeavesOnlyTheQuestionPoints() {
        WritingGrade grade = grade(List.of(new WritingGrade.Criterion("TR", new BigDecimal("3.0"), List.of(), List.of())),
                List.of());

        WritingEvidenceRule.Outcome outcome = rule.derive(List.of(QUESTION_KP), PASS, true, grade);

        assertEquals(Map.of(QUESTION_KP, true), outcome.correctByKnowledgePoint());
        assertTrue(outcome.criterionKnowledgePoints().isEmpty());
    }

    @Test
    void aFailedSubPointIsWrongAndAPassedOneIsRight() {
        var cc = new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), CC, List.of(
                judgment(TR, WritingGrade.Judgment.FAIL), judgment(LR, WritingGrade.Judgment.PASS)));
        var outcome = rule.derive(List.of(), PASS, true, grade(List.of(cc), List.of()));

        assertEquals(Map.of(CC, true, TR, false, LR, true), outcome.correctByKnowledgePoint());
        assertEquals(Set.of(CC, TR, LR), outcome.criterionKnowledgePoints());
    }

    @Test
    void aNotAssessedSubPointIsAbsent() {
        var cc = new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), CC,
                List.of(judgment(TR, WritingGrade.Judgment.NOT_ASSESSED)));
        var outcome = rule.derive(List.of(), PASS, true, grade(List.of(cc), List.of()));
        assertFalse(outcome.correctByKnowledgePoint().containsKey(TR));
        assertEquals(Set.of(CC), outcome.criterionKnowledgePoints());
    }

    @Test
    void aSubPointOverridesTheQuestionPoint() {
        var cc = new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), CC,
                List.of(judgment(QUESTION_KP, WritingGrade.Judgment.FAIL)));
        var outcome = rule.derive(List.of(QUESTION_KP), PASS, true, grade(List.of(cc), List.of()));
        assertEquals(Map.of(QUESTION_KP, false, CC, true), outcome.correctByKnowledgePoint());
        assertEquals(List.of(QUESTION_KP, CC), List.copyOf(outcome.correctByKnowledgePoint().keySet()));
    }

    @Test
    void aCriterionBandWinsOverASubPointJudgment() {
        var cc = new WritingGrade.Criterion("CC", new BigDecimal("6.5"), List.of(), List.of(), CC,
                List.of(judgment(CC, WritingGrade.Judgment.FAIL)));
        var outcome = rule.derive(List.of(), PASS, true, grade(List.of(cc), List.of()));
        assertTrue(outcome.correctByKnowledgePoint().get(CC));
    }

    @Test
    void subPointsAreCriterionKnowledgePointsSoTheyEarnNoMilestone() {
        var cc = new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), CC,
                List.of(judgment(TR, WritingGrade.Judgment.PASS)));
        var outcome = rule.derive(List.of(), PASS, true, grade(List.of(cc), List.of()));
        assertEquals(Set.of(CC, TR), outcome.criterionKnowledgePoints());
    }

    @Test
    void judgmentsCountEvenWithoutAPassBand() {
        var cc = new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), CC,
                List.of(judgment(TR, WritingGrade.Judgment.FAIL)));
        var outcome = rule.derive(List.of(), null, false, grade(List.of(cc), List.of()));
        assertEquals(Map.of(TR, false), outcome.correctByKnowledgePoint());
        assertEquals(Set.of(TR), outcome.criterionKnowledgePoints());
    }

    @Test
    void noPassBandMeansCriteriaAreNotJudged() {
        WritingGrade grade = grade(List.of(criterion("TR", "3.0", TR)), List.of());

        WritingEvidenceRule.Outcome outcome = rule.derive(List.of(QUESTION_KP), null, true, grade);

        assertEquals(Map.of(QUESTION_KP, true), outcome.correctByKnowledgePoint());
    }

    @Test
    void noGradeLeavesOnlyTheQuestionPointsAndNoErrorCode() {
        WritingEvidenceRule.Outcome outcome = rule.derive(List.of(QUESTION_KP, QUESTION_KP), PASS, true, null);

        assertEquals(Map.of(QUESTION_KP, true), outcome.correctByKnowledgePoint());
        assertNull(outcome.dominantErrorCode());
    }

    @Test
    void theMostFrequentErrorCodeWinsAndTiesGoAlphabetically() {
        assertEquals("GRAMMAR", WritingEvidenceRule.dominantErrorCode(
                grade(List.of(), List.of(correction("VOCAB"), correction("GRAMMAR"), correction("GRAMMAR")))));
        assertEquals("GRAMMAR", WritingEvidenceRule.dominantErrorCode(
                grade(List.of(), List.of(correction("VOCAB"), correction("GRAMMAR")))));
    }

    @Test
    void blankAndMissingErrorCodesAreSkippedAndNoCorrectionsGiveNull() {
        assertEquals("VOCAB", WritingEvidenceRule.dominantErrorCode(
                grade(List.of(), List.of(correction(null), correction(" "), correction("VOCAB")))));
        assertNull(WritingEvidenceRule.dominantErrorCode(grade(List.of(), List.of())));
        assertNull(WritingEvidenceRule.dominantErrorCode(grade(List.of(), List.of(correction(null)))));
    }
}
