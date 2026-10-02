package com.group01.learning.domain.service;

import java.math.BigDecimal;

/** A lesson block, review set or final test passes at 70% of its questions or score. */
public final class PassMark {
    private static final BigDecimal PASS_PERCENT = BigDecimal.valueOf(70);

    private PassMark() {}

    public static boolean passes(long correct, int total) {
        return correct * 10L >= total * 7L;
    }

    public static boolean passesPercent(BigDecimal percent) {
        return percent.compareTo(PASS_PERCENT) >= 0;
    }
}
