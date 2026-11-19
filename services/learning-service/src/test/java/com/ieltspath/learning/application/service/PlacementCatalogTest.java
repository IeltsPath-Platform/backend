package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlacementCatalogTest {
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void dropsCatalogItemsWithoutAPoolAndFailsClosedForOldContent() {
        when(content.getPlacementPackages()).thenReturn(List.of(
                new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "missing", null),
                new LearningContentClient.PlacementPackage(null, UUID.randomUUID(), "no-package", PlacementPool.BAND_3_5)));
        var catalog = new PlacementCatalog(content, clock, 30);

        assertThat(catalog.packages(PlacementPool.BAND_3_5)).isEmpty();
        assertThat(catalog.count(PlacementPool.BAND_3_5)).isZero();
    }

    @Test
    void oneCatalogReadServesEveryPoolUntilTheCacheExpires() {
        var low = packageIn(PlacementPool.BAND_0_3);
        var mid = packageIn(PlacementPool.BAND_3_5);
        var high = packageIn(PlacementPool.BAND_5_9);
        when(content.getPlacementPackages()).thenReturn(List.of(low, mid, high));
        var catalog = new PlacementCatalog(content, clock, 30);

        assertThat(catalog.packages(PlacementPool.BAND_0_3)).containsExactly(low);
        assertThat(catalog.packages(PlacementPool.BAND_3_5)).containsExactly(mid);
        assertThat(catalog.count(PlacementPool.BAND_5_9)).isEqualTo(1);
        verify(content, times(1)).getPlacementPackages();

        clock.advanceSeconds(31);
        assertThat(catalog.count(PlacementPool.BAND_0_3)).isEqualTo(1);
        verify(content, times(2)).getPlacementPackages();
    }

    @Test
    void aPoolWithoutPublishedExamsIsEmptyWhileOthersStillCount() {
        when(content.getPlacementPackages()).thenReturn(List.of(packageIn(PlacementPool.BAND_5_9)));
        var catalog = new PlacementCatalog(content, clock, 30);

        assertThat(catalog.count(PlacementPool.BAND_0_3)).isZero();
        assertThat(catalog.count(PlacementPool.BAND_5_9)).isEqualTo(1);
    }

    private static LearningContentClient.PlacementPackage packageIn(PlacementPool pool) {
        return new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), pool.name(), pool);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) { this.now = now; }

        void advanceSeconds(long seconds) { now = now.plusSeconds(seconds); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
