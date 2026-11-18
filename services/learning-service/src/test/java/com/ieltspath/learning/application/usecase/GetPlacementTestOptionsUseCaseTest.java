package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.PlacementCatalog;
import com.ieltspath.learning.domain.aggregate.LearnerPlacement;
import com.ieltspath.learning.domain.aggregate.PlacementAssignment;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import com.ieltspath.learning.domain.vo.BandLevel;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetPlacementTestOptionsUseCaseTest {
    private final UUID user = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-01-01T12:00:00Z");
    private final PlacementAssignmentRepository assignments = mock(PlacementAssignmentRepository.class);
    private final LearnerPlacementRepository placements = mock(LearnerPlacementRepository.class);
    private final LearningContentClient content = mock(LearningContentClient.class);

    @Test
    void returnsPoolCountsPlacementAndOpenAssignment() {
        var open = PlacementAssignment.assign(UUID.randomUUID(), user, UUID.randomUUID(), UUID.randomUUID(),
                PlacementPool.BAND_3_5, now.minusSeconds(60));
        when(assignments.countSince(eq(user), any())).thenReturn(1);
        when(assignments.oldestSince(eq(user), any())).thenReturn(Optional.empty());
        when(assignments.findOpen(user)).thenReturn(Optional.of(open));
        when(placements.find(user)).thenReturn(Optional.of(LearnerPlacement.create(user,
                new BandLevel(new BigDecimal("5.5")), UUID.randomUUID(), now.minusSeconds(300))));
        for (PlacementPool pool : PlacementPool.values()) {
            when(content.getPlacementPackages(pool)).thenReturn(pool == PlacementPool.BAND_3_5
                    ? List.of(new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(),
                    "test", pool)) : List.of());
        }
        var catalog = new PlacementCatalog(content, Clock.fixed(now, ZoneOffset.UTC), 30);

        var result = new GetPlacementTestOptionsUseCase(assignments, placements, catalog,
                Clock.fixed(now, ZoneOffset.UTC), 3).execute(user);

        assertThat(result.pools()).containsExactly(
                new com.ieltspath.learning.application.result.PlacementTestOptionsResult.PoolOption(PlacementPool.BAND_0_3, 0),
                new com.ieltspath.learning.application.result.PlacementTestOptionsResult.PoolOption(PlacementPool.BAND_3_5, 1),
                new com.ieltspath.learning.application.result.PlacementTestOptionsResult.PoolOption(PlacementPool.BAND_5_9, 0));
        assertThat(result.limitPer24h()).isEqualTo(3);
        assertThat(result.retakesLeft()).isEqualTo(2);
        assertThat(result.hasPlacement()).isTrue();
        assertThat(result.currentBand()).isEqualTo(5.5);
        assertThat(result.openAssignment().assignmentId()).isEqualTo(open.id());
    }
}
