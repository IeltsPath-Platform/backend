package com.group01.content.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * An IELTS band range a topic or knowledge point is meant for. Either end may be open; an unbounded range means
 * "every band". Values are 0.0-9.0 in half-band steps.
 */
public record BandRange(BigDecimal min, BigDecimal max) {
    public static final BandRange UNBOUNDED = new BandRange(null, null);

    private static final BigDecimal LOWEST = BigDecimal.ZERO;
    private static final BigDecimal HIGHEST = new BigDecimal("9.0");
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    public BandRange {
        min = normalize(min, "bandMin");
        max = normalize(max, "bandMax");
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("bandMin must not be greater than bandMax");
        }
    }

    public static BandRange of(BigDecimal min, BigDecimal max) {
        return min == null && max == null ? UNBOUNDED : new BandRange(min, max);
    }

    public boolean isUnbounded() {
        return min == null && max == null;
    }

    /**
     * The band that applies to a knowledge point: its own range when it has one, otherwise its topic's. An own
     * range replaces the topic's as a whole, so an override can never combine into an inverted range.
     */
    public BandRange orInherit(BandRange topicBand) {
        return isUnbounded() ? topicBand : this;
    }

    /** Validates a band and fixes its scale to one decimal, so 5, 5.0 and 5.00 are the same value. */
    private static BigDecimal normalize(BigDecimal band, String name) {
        if (band == null) {
            return null;
        }
        if (band.compareTo(LOWEST) < 0 || band.compareTo(HIGHEST) > 0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 9.0");
        }
        if (band.multiply(TWO).stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(name + " must be a multiple of 0.5");
        }
        return band.setScale(1, RoundingMode.UNNECESSARY);
    }
}
