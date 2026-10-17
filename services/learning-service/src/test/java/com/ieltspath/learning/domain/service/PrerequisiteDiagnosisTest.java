package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.PrerequisiteDiagnosis.ProbeFact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrerequisiteDiagnosisTest {
    private final PrerequisiteDiagnosis rule = new PrerequisiteDiagnosis();
    private final UUID b = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private final UUID x = UUID.randomUUID();
    private final UUID z = UUID.randomUUID();

    private ProbeFact probe(UUID kp, UUID parent, int depth, Boolean passed) {
        return new ProbeFact(kp, parent, depth, passed);
    }

    @Test
    void whenEveryProbePassesTheRootIsTheConclusion() {
        var result = rule.conclude(b, List.of(probe(a, b, 1, true), probe(x, b, 1, true)));
        assertTrue(result.rootIsGap());
        assertEquals(List.of(b), result.knowledgePointIds());
    }

    @Test
    void aFailedProbeWithoutPrerequisitesIsTheGap() {
        var result = rule.conclude(b, List.of(probe(a, b, 1, false), probe(x, b, 1, true)));
        assertFalse(result.rootIsGap());
        assertEquals(List.of(a), result.knowledgePointIds());
    }

    @Test
    void aFailedProbeWhosePrerequisiteWasProbedAndPassedIsTheGap() {
        var result = rule.conclude(b, List.of(probe(a, b, 1, false), probe(z, a, 2, true)));
        assertEquals(List.of(a), result.knowledgePointIds());
    }

    @Test
    void aFailedPrerequisiteOfAFailedProbeIsTheGapInstead() {
        var result = rule.conclude(b, List.of(probe(a, b, 1, false), probe(z, a, 2, false)));
        assertEquals(List.of(z), result.knowledgePointIds());
    }

    @Test
    void severalIndependentFailuresAreAllGaps() {
        var result = rule.conclude(b, List.of(probe(a, b, 1, false), probe(x, b, 1, false)));
        assertEquals(Set.of(a, x), Set.copyOf(result.knowledgePointIds()));
    }

    @Test
    void aProbeWithoutASetIsUnknownAndNeverAGap() {
        var onlyUnknown = rule.conclude(b, List.of(probe(a, b, 1, null)));
        assertTrue(onlyUnknown.rootIsGap());
        var failedWithUnknownChild = rule.conclude(b, List.of(probe(a, b, 1, false), probe(z, a, 2, null)));
        assertEquals(List.of(a), failedWithUnknownChild.knowledgePointIds());
    }

    @Test
    void aFailedProbeExpandsToItsUnprobedPrerequisites() {
        assertEquals(List.of(z), rule.nextProbes(1, List.of(z, b), Set.of(a, b, x), 2));
        assertEquals(List.of(), rule.nextProbes(1, List.of(), Set.of(b), 1));
    }

    @Test
    void depthThreeStops() {
        assertEquals(List.of(), rule.nextProbes(PrerequisiteDiagnosis.MAX_DEPTH, List.of(z), Set.of(), 1));
    }

    @Test
    void theProbeBudgetStops() {
        List<UUID> many = java.util.stream.IntStream.range(0, 5).mapToObj(index -> UUID.randomUUID()).toList();
        assertEquals(2, rule.nextProbes(1, many, Set.of(), PrerequisiteDiagnosis.MAX_PROBES - 2).size());
        assertEquals(List.of(), rule.nextProbes(1, many, Set.of(), PrerequisiteDiagnosis.MAX_PROBES));
    }
}
