package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnowledgeJudgmentRuleTest {
    private static final UUID POINT = UUID.randomUUID();
    private static final UUID OTHER_POINT = UUID.randomUUID();
    private static final String ESSAY = "I used linking words in the essay.";
    private final KnowledgeJudgmentRule rule = new KnowledgeJudgmentRule();
    private final KnowledgeJudgmentRule.Candidate candidate =
            new KnowledgeJudgmentRule.Candidate("CC", POINT, "LINKING", "Linking", "Use linkers");

    private WritingGrade.KnowledgeJudgment judge(String decision, String excerpt) {
        return rule.judge(List.of(candidate), List.of(new KnowledgeJudgmentRule.Raw("CC", "LINKING", decision,
                excerpt, "Reason")), ESSAY, List.of()).getFirst();
    }

    @Test
    void failWithAnExcerptFromTheEssayStays() {
        assertEquals(WritingGrade.Judgment.FAIL, judge("FAIL", "linking words").judgment());
    }

    @Test
    void passWithAnExcerptFromTheEssayStays() {
        assertEquals(WritingGrade.Judgment.PASS, judge("PASS", "linking words").judgment());
    }

    @Test
    void passWithoutExcerptBecomesNotAssessed() {
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, judge("PASS", null).judgment());
    }

    @Test
    void failWithAnInventedExcerptBecomesNotAssessed() {
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, judge("FAIL", "not in the essay").judgment());
    }

    @Test
    void blankExcerptBecomesNotAssessed() {
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, judge("FAIL", "  ").judgment());
    }

    @Test
    void unknownJudgmentValueBecomesNotAssessed() {
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, judge("MAYBE", "linking words").judgment());
    }

    @Test
    void judgmentIsCaseInsensitive() {
        assertEquals(WritingGrade.Judgment.PASS, judge(" pass ", "linking words").judgment());
    }

    @Test
    void rawWithAnUnlistedCodeOrWrongCriterionIsIgnored() {
        var raws = List.of(new KnowledgeJudgmentRule.Raw("TR", "LINKING", "FAIL", "linking words", "r"),
                new KnowledgeJudgmentRule.Raw("CC", "OTHER", "FAIL", "linking words", "r"));
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, rule.judge(List.of(candidate), raws, ESSAY, List.of())
                .getFirst().judgment());
    }

    @Test
    void candidateWithoutRawIsNotAssessed() {
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, rule.judge(List.of(candidate), List.of(), ESSAY, List.of())
                .getFirst().judgment());
    }

    @Test
    void firstRawForACandidateWins() {
        var raws = List.of(new KnowledgeJudgmentRule.Raw("CC", "LINKING", "FAIL", "linking words", "first"),
                new KnowledgeJudgmentRule.Raw("CC", "LINKING", "PASS", "linking words", "second"));
        var judgment = rule.judge(List.of(candidate), raws, ESSAY, List.of()).getFirst();
        assertEquals(WritingGrade.Judgment.FAIL, judgment.judgment());
        assertEquals("first", judgment.reason());
    }

    @Test
    void longExcerptAndReasonAreCut() {
        String longText = "x".repeat(400);
        var candidate = new KnowledgeJudgmentRule.Candidate("CC", POINT, "X", "X", null);
        var judgment = rule.judge(List.of(candidate), List.of(new KnowledgeJudgmentRule.Raw("CC", "X", "FAIL",
                longText, longText)), longText, List.of()).getFirst();
        assertEquals(300, judgment.excerpt().length());
        assertEquals(300, judgment.reason().length());
    }

    @Test
    void aCorrectionTiedToTheKnowledgePointTurnsPassIntoNotAssessed() {
        var judgment = rule.judge(List.of(candidate), List.of(new KnowledgeJudgmentRule.Raw("CC", "LINKING", "PASS",
                "linking words", "r")), ESSAY,
                List.of(new KnowledgeJudgmentRule.CorrectionEvidence(POINT, "linking words", "fix"))).getFirst();
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, judgment.judgment());
        assertNull(judgment.excerpt());
        assertNull(judgment.reason());
    }

    @Test
    void aCorrectionTiedToTheKnowledgePointTurnsNotAssessedIntoFailWithTheCorrectionExcerpt() {
        var judgment = rule.judge(List.of(candidate), List.of(), ESSAY,
                List.of(new KnowledgeJudgmentRule.CorrectionEvidence(POINT, "linking words", "fix it"))).getFirst();
        assertEquals(WritingGrade.Judgment.FAIL, judgment.judgment());
        assertEquals("linking words", judgment.excerpt());
        assertEquals("fix it", judgment.reason());
    }

    @Test
    void aCorrectionTiedToTheKnowledgePointLeavesFailAlone() {
        var judgment = rule.judge(List.of(candidate), List.of(new KnowledgeJudgmentRule.Raw("CC", "LINKING", "FAIL",
                "linking words", "model")), ESSAY,
                List.of(new KnowledgeJudgmentRule.CorrectionEvidence(POINT, "essay", "fix"))).getFirst();
        assertEquals(WritingGrade.Judgment.FAIL, judgment.judgment());
        assertEquals("linking words", judgment.excerpt());
    }

    @Test
    void aCorrectionTiedToAnotherKnowledgePointChangesNothing() {
        var judgment = rule.judge(List.of(candidate), List.of(), ESSAY,
                List.of(new KnowledgeJudgmentRule.CorrectionEvidence(OTHER_POINT, "linking words", "fix"))).getFirst();
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, judgment.judgment());
    }

    @Test
    void studyPointsFollowTheContractOrder() {
        var cc = new WritingGrade.Criterion("CC", new BigDecimal("5.5"), List.of(), List.of(), OTHER_POINT,
                List.of(new WritingGrade.KnowledgeJudgment(POINT, "LINKING", "Linking", WritingGrade.Judgment.FAIL,
                        "linking words", "reason")));
        var tr = new WritingGrade.Criterion("TR", new BigDecimal("6.0"), List.of(), List.of(), null,
                List.of(new WritingGrade.KnowledgeJudgment(OTHER_POINT, "TOPIC", "Topic", WritingGrade.Judgment.FAIL,
                        "essay", "reason")));
        var criterionPoint = new WritingGrade.StudyPoint(WritingGrade.StudyPoint.CRITERION, "CC",
                new BigDecimal("5.5"), OTHER_POINT, "CC_KP", "CC", "CC", "reason");
        var aiPoint = new WritingGrade.StudyPoint(WritingGrade.StudyPoint.AI, null, null, null, null, null,
                "AI", "reason");

        var result = rule.studyPoints(List.of(cc, tr), List.of(criterionPoint, aiPoint), ignored -> POINT);

        assertEquals(List.of(WritingGrade.StudyPoint.CRITERION, WritingGrade.StudyPoint.KNOWLEDGE_POINT,
                WritingGrade.StudyPoint.KNOWLEDGE_POINT, WritingGrade.StudyPoint.AI),
                result.stream().map(WritingGrade.StudyPoint::kind).toList());
        assertEquals(java.util.Arrays.asList("CC", "CC", "TR", null),
                result.stream().map(WritingGrade.StudyPoint::criterionCode).toList());
    }

    @Test
    void studyPointCarriesExcerptReasonAndPracticeTarget() {
        var judgment = new WritingGrade.KnowledgeJudgment(POINT, "LINKING", "Linking", WritingGrade.Judgment.FAIL,
                "linking words", "Use a linker.");
        var criterion = new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), OTHER_POINT,
                List.of(judgment));
        var point = rule.studyPoints(List.of(criterion), List.of(), ignored -> POINT).getFirst();
        assertEquals("linking words", point.excerpt());
        assertEquals("Use a linker.", point.reason());
        assertEquals(POINT, point.practiceKnowledgePointId());
    }

    @Test
    void noFailMeansNoKnowledgePointStudyPoint() {
        var criterion = new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), OTHER_POINT,
                List.of(new WritingGrade.KnowledgeJudgment(POINT, "LINKING", "Linking",
                        WritingGrade.Judgment.NOT_ASSESSED, null, null)));
        assertEquals(List.of(), rule.studyPoints(List.of(criterion), List.of(), ignored -> POINT));
    }
}
