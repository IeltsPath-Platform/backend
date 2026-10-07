package com.ieltspath.learning.domain.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Picks the package the learner has gone longest without: never used first, then the oldest last use; ties by
 * {@code tieBreaker}, then package id, so the choice is stable.
 */
public final class PackageRotation {
    private PackageRotation() {}

    public static Optional<UUID> leastRecentlyUsed(List<Candidate> candidates, Map<UUID, Instant> lastUsed) {
        return candidates.stream().min(Comparator
                        .comparing((Candidate candidate) -> lastUsed.getOrDefault(candidate.packageId(), Instant.MIN))
                        .thenComparing(Candidate::tieBreaker)
                        .thenComparing(candidate -> candidate.packageId().toString()))
                .map(Candidate::packageId);
    }

    public record Candidate(UUID packageId, String tieBreaker) {
        public Candidate {
            tieBreaker = tieBreaker == null ? "" : tieBreaker;
        }
    }
}
