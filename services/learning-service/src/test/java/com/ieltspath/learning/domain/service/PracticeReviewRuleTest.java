package com.ieltspath.learning.domain.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PracticeReviewRuleTest {
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
}
