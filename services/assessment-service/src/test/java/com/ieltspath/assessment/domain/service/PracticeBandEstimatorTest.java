package com.ieltspath.assessment.domain.service;

import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PracticeBandEstimatorTest {
    @Test
    void readingFullTestUsesTheFortyQuestionTable() {
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.FULL, 39, 40)).contains(9.0);
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.FULL, 30, 40)).contains(7.0);
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.FULL, 23, 40)).contains(6.0);
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.FULL, 7, 40)).contains(3.0);
    }

    @Test
    void listeningHasItsOwnTable() {
        assertThat(PracticeBandEstimator.estimate(Skill.LISTENING, AttemptScope.FULL, 32, 40)).contains(7.5);
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.FULL, 32, 40)).contains(7.0);
    }

    @Test
    void aShorterTestIsScaledToFortyQuestions() {
        // 12 of 16 is the same share as 30 of 40.
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.FULL, 12, 16)).contains(7.0);
    }

    @Test
    void veryLowScoresStillGetABand() {
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.FULL, 0, 40)).contains(0.0);
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.FULL, 3, 40)).contains(2.0);
    }

    @Test
    void aSinglePartAndTheOtherSkillsHaveNoBand() {
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.PART, 10, 13)).isEmpty();
        assertThat(PracticeBandEstimator.estimate(Skill.WRITING, AttemptScope.FULL, 10, 13)).isEmpty();
        assertThat(PracticeBandEstimator.estimate(Skill.SPEAKING, AttemptScope.FULL, 10, 13)).isEmpty();
        assertThat(PracticeBandEstimator.estimate(Skill.READING, AttemptScope.FULL, 0, 0)).isEmpty();
    }
}
