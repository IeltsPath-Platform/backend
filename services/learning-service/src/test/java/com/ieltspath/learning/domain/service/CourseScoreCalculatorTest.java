package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.CourseScoreCalculator.Components;
import com.ieltspath.learning.domain.vo.CourseScoreConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseScoreCalculatorTest {
    private final CourseScoreCalculator calculator = new CourseScoreCalculator();
    private final CourseScoreConfig config = config("0.2", "0.3", "0.5", "0.64", "0.36", "0.7");

    private static CourseScoreConfig config(String process, String mid, String fin, String practice,
                                            String practiceTest, String pass) {
        return new CourseScoreConfig(null, new BigDecimal(process), new BigDecimal(mid), new BigDecimal(fin),
                new BigDecimal(practice), new BigDecimal(practiceTest), new BigDecimal(pass), new BigDecimal("0.6"));
    }

    @Test
    void weighsProcessMidAndFinalWhenTheLearnerDidEverything() {
        var score = calculator.calculate(config, new Components(0.8, 0.5, 0.6, 0.9));
        // process = 0.64 * 0.8 + 0.36 * 0.5 = 0.692
        assertEquals(0.692, score.process(), 1e-9);
        assertEquals(0.2 * 0.692 + 0.3 * 0.6 + 0.5 * 0.9, score.total(), 1e-4);
        assertEquals(0.2, score.processWeight(), 1e-9);
    }

    @Test
    void aComponentTheLearnerHasNotDoneCountsAsZero() {
        var score = calculator.calculate(config, new Components(0.0, 0.0, 0.0, 0.8));
        assertEquals(0.4, score.total(), 1e-9);
        assertFalse(calculator.passes(config, score.total()));
    }

    @Test
    void aComponentTheCourseDoesNotHaveIsLeftOutAndItsWeightSharedOut() {
        // No milestone practice tests: Process is Practice alone.
        var withoutPracticeTest = calculator.calculate(config, new Components(0.8, null, 0.6, 0.9));
        assertEquals(0.8, withoutPracticeTest.process(), 1e-9);
        assertEquals(1.0, withoutPracticeTest.practiceShare(), 1e-9);
        assertEquals(0.0, withoutPracticeTest.practiceTestShare(), 1e-9);

        // No topic tests: the weights of Process and final are scaled to 0.2/0.7 and 0.5/0.7.
        var withoutMid = calculator.calculate(config, new Components(0.7, 0.7, null, 0.8));
        assertNull(withoutMid.mid());
        assertEquals(0.2 / 0.7, withoutMid.processWeight(), 1e-9);
        assertEquals(0.5 / 0.7, withoutMid.finalWeight(), 1e-9);
        assertEquals(0.0, withoutMid.midWeight(), 1e-9);
        assertEquals(0.2 / 0.7 * 0.7 + 0.5 / 0.7 * 0.8, withoutMid.total(), 1e-4);

        var onlyFinal = calculator.calculate(config, new Components(null, null, null, 0.75));
        assertNull(onlyFinal.process());
        assertEquals(0.75, onlyFinal.total(), 1e-9);
    }

    @Test
    void passesAtTheThresholdAndNotJustBelowIt() {
        assertTrue(calculator.passes(config, 0.7));
        assertFalse(calculator.passes(config, 0.6999));
        // 0.7 is reached through floating-point sums that may land a hair under it.
        var score = calculator.calculate(config("0.2", "0.3", "0.5", "0.64", "0.36", "0.7"),
                new Components(0.7, 0.7, 0.7, 0.7));
        assertEquals(0.7, score.total(), 1e-9);
        assertTrue(calculator.passes(config, score.total()));
    }

    @Test
    void anAdminCanMoveTheWeights() {
        var finalHeavy = config("0.1", "0.1", "0.8", "0.5", "0.5", "0.7");
        assertEquals(0.1 * 0.5 + 0.1 * 0.5 + 0.8 * 1.0,
                calculator.calculate(finalHeavy, new Components(0.5, 0.5, 0.5, 1.0)).total(), 1e-4);
    }

    @Test
    void aConfigurationThatDoesNotAddUpIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> config("0.2", "0.3", "0.4", "0.64", "0.36", "0.7"));
        assertThrows(IllegalArgumentException.class, () -> config("0.2", "0.3", "0.5", "0.6", "0.3", "0.7"));
        assertThrows(IllegalArgumentException.class, () -> config("0.2", "0.3", "0.5", "0.64", "0.36", "0"));
        assertThrows(IllegalArgumentException.class, () -> config("0.2", "0.3", "0.5", "0.64", "0.36", "1.1"));
    }
}
