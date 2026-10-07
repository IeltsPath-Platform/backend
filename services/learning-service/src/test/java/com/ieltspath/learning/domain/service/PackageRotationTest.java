package com.ieltspath.learning.domain.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PackageRotationTest {
    private static final UUID A = UUID.fromString("00000000-0000-4000-8000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-4000-8000-00000000000b");

    @Test
    void neverUsedComesFirstThenTheOldestUse() {
        var candidates = List.of(new PackageRotation.Candidate(A, "X1"), new PackageRotation.Candidate(B, "X2"));
        Instant earlier = Instant.parse("2026-10-01T00:00:00Z");

        assertThat(PackageRotation.leastRecentlyUsed(candidates, Map.of(A, earlier))).contains(B);
        assertThat(PackageRotation.leastRecentlyUsed(candidates, Map.of(A, earlier, B, earlier.plusSeconds(1))))
                .contains(A);
    }

    @Test
    void tiesFallBackToTheTieBreakerThenPackageId() {
        assertThat(PackageRotation.leastRecentlyUsed(List.of(new PackageRotation.Candidate(A, "X2"),
                new PackageRotation.Candidate(B, "X1")), Map.of())).contains(B);
        assertThat(PackageRotation.leastRecentlyUsed(List.of(new PackageRotation.Candidate(B, null),
                new PackageRotation.Candidate(A, null)), Map.of())).contains(A);
        assertThat(PackageRotation.leastRecentlyUsed(List.of(), Map.of())).isEmpty();
    }
}
