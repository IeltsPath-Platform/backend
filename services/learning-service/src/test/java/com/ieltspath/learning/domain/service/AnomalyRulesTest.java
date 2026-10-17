package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.AnomalyRules.Sample;
import com.ieltspath.learning.domain.service.AnomalyRules.Thresholds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnomalyRulesTest {
    private final AnomalyRules rules = new AnomalyRules();
    private final Thresholds limits = new Thresholds(10, 3000, 0.9, 30);

    private List<Sample> samples(int count, long durationMs, int wrong) {
        List<Sample> list = new ArrayList<>();
        for (int index = 0; index < count; index++) list.add(new Sample(durationMs, index >= wrong));
        return list;
    }

    @Test
    void aFastAndAccurateDayIsFlagged() {
        assertTrue(rules.tooFastHighScore(samples(10, 2000, 1), limits));
    }

    @Test
    void theBoundsOfEachConditionDecide() {
        assertFalse(rules.tooFastHighScore(samples(9, 1000, 0), limits), "fewer than ten timed answers");
        assertFalse(rules.tooFastHighScore(samples(10, 3000, 0), limits), "median of exactly three seconds is not below");
        assertTrue(rules.tooFastHighScore(samples(10, 2999, 0), limits));
        assertFalse(rules.tooFastHighScore(samples(10, 1000, 2), limits), "80% accuracy");
        assertTrue(rules.tooFastHighScore(samples(10, 1000, 1), limits), "90% accuracy is enough");
    }

    @Test
    void answersWithoutADurationOrAGradeAreNotCounted() {
        List<Sample> mixed = new ArrayList<>(samples(9, 1000, 0));
        mixed.add(new Sample(null, true));
        mixed.add(new Sample(500L, null));
        assertFalse(rules.tooFastHighScore(mixed, limits));
    }

    @Test
    void theMedianNotTheMeanIsUsed() {
        List<Sample> list = new ArrayList<>(samples(6, 1000, 0));
        list.addAll(samples(4, 60_000, 0));
        assertTrue(rules.tooFastHighScore(list, limits));
    }

    @Test
    void moreThanThirtySubmissionsInTheWindowIsABurst() {
        assertFalse(rules.burst(30, limits));
        assertTrue(rules.burst(31, limits));
    }

    @Test
    void essaysDifferingOnlyInSpacingAndCaseShareAHash() {
        assertEquals(AnomalyRules.essayHash("Cities  should\nplant TREES."),
                AnomalyRules.essayHash("cities should plant trees."));
        assertNotEquals(AnomalyRules.essayHash("cities should plant trees"),
                AnomalyRules.essayHash("cities should plant tree"));
        assertEquals(64, AnomalyRules.essayHash("x").length());
    }
}
