package com.ieltspath.learning.domain.vo;

/**
 * {@code PRIORITY}, {@code CONSOLIDATE} and {@code MAINTAIN} are ordered weakest to strongest. {@code INSUFFICIENT}
 * (MP-1.0 only) means there is not enough evidence to judge the point; it is last so that sorting by {@link #ordinal()}
 * lists judged points first, and {@link #rank()} is the level to compare.
 */
public enum MasteryTier {
    PRIORITY, CONSOLIDATE, MAINTAIN, INSUFFICIENT;

    /** Level for comparing tiers; an insufficient point ranks below every judged one. */
    public int rank() {
        return this == INSUFFICIENT ? -1 : ordinal();
    }
}
