package com.ieltspath.assessment.domain.service;

import com.ieltspath.assessment.domain.vo.PlacementPool;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlacementBandCeilingTest {
    private final PlacementBandCeiling ceiling = new PlacementBandCeiling();

    @Test
    void capsEachPoolAndReportsWhenTheUncappedBandReachesItsCeiling() {
        assertThat(ceiling.apply(2.5, PlacementPool.BAND_0_3))
                .isEqualTo(new PlacementBandCeiling.CeilingResult(2.5, 3.0, false, false));
        assertThat(ceiling.apply(3.0, PlacementPool.BAND_0_3))
                .isEqualTo(new PlacementBandCeiling.CeilingResult(3.0, 3.0, true, false));
        assertThat(ceiling.apply(7.5, PlacementPool.BAND_0_3))
                .isEqualTo(new PlacementBandCeiling.CeilingResult(3.0, 3.0, true, false));
        assertThat(ceiling.apply(7.0, PlacementPool.BAND_3_5))
                .isEqualTo(new PlacementBandCeiling.CeilingResult(5.0, 5.0, true, false));
        assertThat(ceiling.apply(9.0, PlacementPool.BAND_5_9))
                .isEqualTo(new PlacementBandCeiling.CeilingResult(9.0, 9.0, false, false));
    }

    @Test
    void leavesAnAttemptWithoutAPoolUncapped() {
        assertThat(ceiling.apply(8.5, null))
                .isEqualTo(new PlacementBandCeiling.CeilingResult(8.5, null, false, true));
    }
}
