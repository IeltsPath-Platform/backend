package com.group01.learning.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** An IELTS band in the range 0–9, normalized to a half-band step. */
public record BandLevel(BigDecimal value) {
    public BandLevel {
        if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.valueOf(9)) > 0
                || value.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Band must be between 0 and 9 in half-band steps");
        }
        value = value.setScale(1, RoundingMode.UNNECESSARY);
    }
}
