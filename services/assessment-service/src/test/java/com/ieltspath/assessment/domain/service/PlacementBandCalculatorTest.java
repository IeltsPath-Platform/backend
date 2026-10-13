package com.ieltspath.assessment.domain.service;

import com.ieltspath.assessment.domain.vo.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlacementBandCalculatorTest {
    @ParameterizedTest
    @CsvSource({"0,0.0", "1,1.0", "2,1.5", "3,2.0", "4,2.5", "5,2.5", "6,3.0", "8,3.5", "10,4.0", "12,4.0", "13,4.5",
            "16,5.0", "17,5.0", "18,5.5", "22,5.5", "23,6.0", "26,6.5", "29,6.5", "30,7.0", "32,7.5", "34,7.5",
            "35,8.0", "37,8.5", "39,9.0", "40,9.0"})
    void listeningBandFollowsTheFortyQuestionTable(int correct, double band) {
        assertThat(PlacementBandCalculator.bandForCorrect(Skill.LISTENING, correct, 40)).isEqualTo(band);
    }

    @ParameterizedTest
    @CsvSource({"0,0.0", "1,1.0", "10,4.0", "14,4.5", "15,5.0", "18,5.0", "19,5.5", "22,5.5", "23,6.0", "26,6.0",
            "27,6.5", "29,6.5", "30,7.0", "32,7.0", "33,7.5", "34,7.5", "36,8.0", "38,8.5", "40,9.0"})
    void readingBandFollowsTheFortyQuestionTable(int correct, double band) {
        assertThat(PlacementBandCalculator.bandForCorrect(Skill.READING, correct, 40)).isEqualTo(band);
    }

    @Test
    void aShorterTestIsScaledToFortyQuestions() {
        // 7 of 14 is 20 of 40; 1 of 7 is about 6 of 40
        assertThat(PlacementBandCalculator.bandForCorrect(Skill.READING, 7, 14)).isEqualTo(5.5);
        assertThat(PlacementBandCalculator.bandForCorrect(Skill.LISTENING, 1, 7)).isEqualTo(3.0);
        // a single correct answer never rounds down to band 0
        assertThat(PlacementBandCalculator.bandForCorrect(Skill.LISTENING, 1, 400)).isEqualTo(1.0);
        assertThat(PlacementBandCalculator.bandForCorrect(Skill.LISTENING, 3, 0)).isEqualTo(0.0);
    }

    @Test
    void onlyListeningAndReadingAreConvertedFromCorrectAnswers() {
        assertThatThrownBy(() -> PlacementBandCalculator.bandForCorrect(Skill.WRITING, 5, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({"6.0;6.0;6.0;6.0,6.0", "7.0;6.0;5.5;5.5,6.0", "6.0;6.0;6.0;5.5,6.0", "5.0;5.0;5.0;5.5,5.0",
            "4.0;0.0;4.0;5.5,3.5", "7.0,7.0"})
    void overallBandIsTheMeanRoundedToAHalfBand(String bands, double expected) {
        var values = java.util.Arrays.stream(bands.split(";")).map(Double::valueOf).toList();
        assertThat(PlacementBandCalculator.overallBand(values)).isEqualTo(expected);
    }

    @Test
    void aTieBetweenTwoHalfBandsRoundsUp() {
        // mean 5.75 lies between 5.5 and 6.0 and rounds up
        assertThat(PlacementBandCalculator.overallBand(List.of(5.5, 5.5, 6.0, 6.0))).isEqualTo(6.0);
        assertThat(PlacementBandCalculator.overallBand(List.of(5.5, 5.5, 5.5, 6.0))).isEqualTo(5.5);
    }

    @Test
    void anEmptyListOfSkillBandsIsRejected() {
        assertThatThrownBy(() -> PlacementBandCalculator.overallBand(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
