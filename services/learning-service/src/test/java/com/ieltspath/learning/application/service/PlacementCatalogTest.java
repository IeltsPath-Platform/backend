package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlacementCatalogTest {
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void dropsCatalogItemsWithoutTheRequestedPoolAndFailsClosedForOldContent() {
        when(content.getPlacementPackages(PlacementPool.BAND_3_5)).thenReturn(List.of(
                new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "missing", null),
                new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "wrong",
                        PlacementPool.BAND_5_9)));
        var catalog = new PlacementCatalog(content, clock, 30);

        assertThat(catalog.packages(PlacementPool.BAND_3_5)).isEmpty();
        assertThat(catalog.count(PlacementPool.BAND_3_5)).isZero();
        verify(content, times(1)).getPlacementPackages(PlacementPool.BAND_3_5);
    }
}
