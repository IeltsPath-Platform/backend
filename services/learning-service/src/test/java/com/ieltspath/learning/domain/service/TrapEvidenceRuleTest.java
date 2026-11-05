package com.ieltspath.learning.domain.service;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrapEvidenceRuleTest {
    private static final UUID X = UUID.randomUUID();
    private static final UUID Y = UUID.randomUUID();

    @Test
    void correctAnswerCreditsEachDistinctMappedTrapOnce() {
        var result = TrapEvidenceRule.outcomes(Map.of("distractors", Map.of("A", "X", "B", "X", "C", "Y")),
                true, null, Map.of("X", X, "Y", Y));

        assertEquals(java.util.Set.of(new TrapEvidenceRule.TrapOutcome(X, true),
                new TrapEvidenceRule.TrapOutcome(Y, true)), java.util.Set.copyOf(result));
    }

    @Test
    void wrongAnswerCreditsOnlyTheSelectedMappedTrap() {
        var spec = Map.<String, Object>of("distractors", Map.of("A", "X", "B", "Y"));

        assertEquals(java.util.List.of(new TrapEvidenceRule.TrapOutcome(Y, false)),
                TrapEvidenceRule.outcomes(spec, false, "Y", Map.of("X", X, "Y", Y)));
        assertEquals(java.util.List.of(), TrapEvidenceRule.outcomes(spec, false, null, Map.of("X", X, "Y", Y)));
        assertEquals(java.util.List.of(), TrapEvidenceRule.outcomes(spec, false, "Z", Map.of("X", X, "Y", Y)));
    }

    @Test
    void ignoresUnmappedCodesAndMissingOrMalformedDistractors() {
        assertEquals(java.util.List.of(), TrapEvidenceRule.outcomes(
                Map.of("distractors", Map.of("A", "X")), true, null, Map.of()));
        assertEquals(java.util.List.of(), TrapEvidenceRule.outcomes(Map.of(), true, null, Map.of("X", X)));
        assertEquals(java.util.List.of(), TrapEvidenceRule.outcomes(
                Map.of("distractors", "X"), true, null, Map.of("X", X)));
    }
}
