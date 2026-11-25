package com.ieltspath.assessment.domain.service;

import com.ieltspath.assessment.domain.vo.PlacementPool;

/** Applies the ceiling selected when the learner chose a placement pool. */
public final class PlacementBandCeiling {
    public CeilingResult apply(double computedBand, PlacementPool pool) {
        if (pool == null) return new CeilingResult(computedBand, null, false, true);
        double ceiling = pool.ceiling();
        boolean reached = pool != PlacementPool.BAND_5_9 && computedBand >= ceiling;
        return new CeilingResult(Math.min(computedBand, ceiling), ceiling, reached, false);
    }

    public record CeilingResult(double band, Double ceiling, boolean reachedCeiling, boolean uncapped) {}
}
