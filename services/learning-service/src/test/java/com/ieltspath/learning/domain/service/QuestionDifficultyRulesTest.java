package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.QuestionDifficultyRules.Limits;
import com.ieltspath.learning.domain.service.QuestionDifficultyRules.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionDifficultyRulesTest {
    private final QuestionDifficultyRules rules = new QuestionDifficultyRules();
    private final Limits limits = new Limits(0.9, 0.3, 0.8, 0.4);

    @Test
    void anUntaggedQuestionUsesTheGeneralLimits() {
        assertEquals(Verdict.OK, rules.judge(0.9, null, limits), "exactly at the limit is not beyond it");
        assertEquals(Verdict.TOO_EASY, rules.judge(0.91, null, limits));
        assertEquals(Verdict.OK, rules.judge(0.3, null, limits));
        assertEquals(Verdict.TOO_HARD, rules.judge(0.29, null, limits));
        assertEquals(Verdict.OK, rules.judge(0.6, "MEDIUM", limits));
    }

    @Test
    void aQuestionTaggedHardIsTooEasyAlreadyAtTheLowerLimit() {
        assertEquals(Verdict.TOO_EASY, rules.judge(0.8, "HARD", limits));
        assertEquals(Verdict.OK, rules.judge(0.79, "HARD", limits));
        assertEquals(Verdict.OK, rules.judge(0.8, "MEDIUM", limits), "the tag decides, not the rate alone");
        assertEquals(Verdict.TOO_HARD, rules.judge(0.2, "HARD", limits), "a hard question may still be too hard");
    }

    @Test
    void aQuestionTaggedEasyIsTooHardAlreadyAtTheHigherLimit() {
        assertEquals(Verdict.TOO_HARD, rules.judge(0.4, "EASY", limits));
        assertEquals(Verdict.OK, rules.judge(0.41, "EASY", limits));
        assertEquals(Verdict.OK, rules.judge(0.4, "MEDIUM", limits));
        assertEquals(Verdict.TOO_EASY, rules.judge(0.95, "EASY", limits));
    }
}
