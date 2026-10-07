package com.ieltspath.learning.domain.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MasteryCalculatorTest {
    private final MasteryCalculator calculator = new MasteryCalculator();

    @Test
    void retainsOriginalConfidenceCapsAndEmptyHistoryValues() {
        assertThat(calculator.compute(List.of())).isEqualTo(0.0);
        assertThat(calculator.compute(List.of(true))).isEqualTo(0.5);
        assertThat(calculator.compute(List.of(false))).isEqualTo(0.0);
        assertThat(calculator.compute(List.of(true, true))).isEqualTo(0.8);
        assertThat(calculator.compute(List.of(true, true, true))).isEqualTo(1.0);
    }

    @Test
    void mostlyCorrectHistoryScoresHigherOnceUncapped() {
        double mostlyRight = calculator.compute(List.of(true, true, false));
        double mostlyWrong = calculator.compute(List.of(false, false, true));

        assertThat(mostlyRight).isGreaterThan(0.5).isGreaterThan(mostlyWrong);
    }

    @Test
    void weightsRecentAnswersMoreAndUsesOnlyFiveLatestAnswers() {
        assertThat(calculator.compute(List.of(false, true)))
                .isCloseTo(1.0 / 1.95, within(0.000000001));
        assertThat(calculator.compute(List.of(true, false)))
                .isCloseTo(0.95 / 1.95, within(0.000000001));
        assertThat(calculator.compute(List.of(false, false, true, true, true, true, true)))
                .isEqualTo(1.0);
        assertThat(calculator.compute(List.of(true, true, false, false, false, false, false)))
                .isEqualTo(0.0);
    }

    @Test
    void matchesLanSeedValuesWithoutChangingExpectedNumbers() {
        assertThat(calculator.compute(List.of(true, true, false, true)))
                .isCloseTo(0.729, within(0.0005));
        assertThat(calculator.compute(List.of(false, true, true, true, true)))
                .isCloseTo(0.875, within(0.0005));
        assertThat(calculator.compute(List.of(true, false)))
                .isCloseTo(0.487, within(0.0005));
    }
}
