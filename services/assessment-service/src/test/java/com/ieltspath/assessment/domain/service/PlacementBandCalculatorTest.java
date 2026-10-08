package com.ieltspath.assessment.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlacementBandCalculatorTest {
    @ParameterizedTest
    @CsvSource({"0,4.0", "29.9,4.0", "30,4.5", "44.9,4.5", "45,5.0", "59.9,5.0", "60,5.5", "69.9,5.5",
            "70,6.0", "79.9,6.0", "80,6.5", "89.9,6.5", "90,7.0", "100,7.0"})
    void objectiveSkillBandFollowsThePercentTable(double percent, double band) {
        assertThat(PlacementBandCalculator.bandForPercent(percent)).isEqualTo(band);
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
