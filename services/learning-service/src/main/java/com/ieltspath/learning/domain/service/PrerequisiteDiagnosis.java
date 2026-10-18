package com.ieltspath.learning.domain.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Walks back through the prerequisites of a knowledge point that keeps failing. A failed probe of A leads to the
 * prerequisites of A, up to {@link #MAX_DEPTH} levels and {@link #MAX_PROBES} probes in all. A failed probe none of whose
 * own probed prerequisites failed is a gap; a probe without a set stays unknown and is never a gap.
 */
public final class PrerequisiteDiagnosis {
    public static final int MAX_DEPTH = 3;
    public static final int MAX_PROBES = 6;

    /** {@code passed} is null until the probe is answered, and for a probe without a set. */
    public record ProbeFact(UUID knowledgePointId, UUID parentKnowledgePointId, int depth, Boolean passed) {
        boolean failed() { return Boolean.FALSE.equals(passed); }
    }

    /** {@code knowledgePointIds} are the gap points, or the root point itself when no probe failed. */
    public record Conclusion(List<UUID> knowledgePointIds, boolean rootIsGap) {}

    /**
     * The prerequisites to probe next after {@code knowledgePointId} failed at {@code depth}: the ones not probed yet,
     * as many as the probe budget still allows. A KP the session already probed (or the root) is never probed twice.
     */
    public List<UUID> nextProbes(int depth, List<UUID> prerequisites, Collection<UUID> alreadyProbed, int probeCount) {
        int room = MAX_PROBES - probeCount;
        if (depth >= MAX_DEPTH || room <= 0 || prerequisites == null) return List.of();
        LinkedHashSet<UUID> fresh = new LinkedHashSet<>(prerequisites);
        fresh.removeAll(alreadyProbed);
        return fresh.stream().limit(room).toList();
    }

    /** Valid once every probe is answered or has no set. */
    public Conclusion conclude(UUID rootKnowledgePointId, List<ProbeFact> probes) {
        List<UUID> gaps = probes.stream().filter(ProbeFact::failed)
                .filter(probe -> probes.stream().noneMatch(child -> child.failed()
                        && child.parentKnowledgePointId().equals(probe.knowledgePointId())))
                .map(ProbeFact::knowledgePointId).distinct().toList();
        return gaps.isEmpty() ? new Conclusion(List.of(rootKnowledgePointId), true) : new Conclusion(gaps, false);
    }
}
