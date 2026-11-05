package com.ieltspath.learning.domain.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PracticeReviewRuleTest {
    private static List<PracticeReviewRule.ItemOutcome> outcomes(UUID kp, boolean... correct) {
        List<PracticeReviewRule.ItemOutcome> items = new ArrayList<>();
        for (boolean value : correct) items.add(new PracticeReviewRule.ItemOutcome(Set.of(kp), value));
        return items;
    }

    @Test
    void createsOnlyAvailableWeakKnowledgePointReviewsForCountedFailures() {
        UUID weak = UUID.randomUUID();
        UUID strong = UUID.randomUUID();
        var outcomes = List.of(new PracticeReviewRule.ItemOutcome(Set.of(weak, strong), true),
                new PracticeReviewRule.ItemOutcome(Set.of(weak), false),
                new PracticeReviewRule.ItemOutcome(Set.of(weak, strong), false),
                new PracticeReviewRule.ItemOutcome(Set.of(strong), true),
                new PracticeReviewRule.ItemOutcome(Set.of(strong), true));
        var rule = new PracticeReviewRule();
        assertTrue(rule.derive(0.70, true, outcomes, Set.of(), Map.of(weak, 1, strong, 1)).isEmpty());
        assertEquals(List.of(weak), rule.derive(0.50, true, outcomes, Set.of(),
                Map.of(weak, 1, strong, 1)).stream().map(PracticeReviewRule.Need::knowledgePointId).toList());
        assertTrue(rule.derive(0.50, false, outcomes, Set.of(), Map.of(weak, 1, strong, 1)).isEmpty());
        assertTrue(rule.derive(0.50, true, outcomes, Set.of(), Map.of(weak, 0, strong, 0)).isEmpty());
    }

    @Test
    void aPointNeedsTwoWrongAnswersAndUnderSeventyPercent() {
        var rule = new PracticeReviewRule();
        UUID kp = UUID.randomUUID();
        Map<UUID, Integer> spare = Map.of(kp, 1);
        assertTrue(rule.derive(0.0, true, outcomes(kp, false), Set.of(), spare).isEmpty());          // 0/1
        assertTrue(rule.derive(0.5, true, outcomes(kp, true, false), Set.of(), spare).isEmpty());    // 1/2
        assertTrue(rule.derive(0.66, true, outcomes(kp, true, true, false), Set.of(), spare).isEmpty()); // 2/3
        assertEquals(2, rule.derive(0.0, true, outcomes(kp, false, false), Set.of(), spare).getFirst().wrong()); // 0/2
        assertEquals(2, rule.derive(0.33, true, outcomes(kp, true, false, false), Set.of(), spare).getFirst().wrong()); // 1/3
        assertEquals(3, rule.derive(0.4, true, outcomes(kp, true, true, false, false, false), Set.of(), spare)
                .getFirst().wrong());                                                                 // 2/5
    }

    @Test
    void seventyPercentOnAPointIsNotWeakEvenWithManyWrongAnswers() {
        var rule = new PracticeReviewRule();
        UUID kp = UUID.randomUUID();
        boolean[] sevenOfTen = {true, true, true, true, true, true, true, false, false, false};
        assertTrue(rule.derive(0.5, true, outcomes(kp, sevenOfTen), Set.of(), Map.of(kp, 1)).isEmpty());
    }
}
