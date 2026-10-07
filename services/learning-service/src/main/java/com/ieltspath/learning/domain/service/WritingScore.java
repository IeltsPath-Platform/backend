package com.ieltspath.learning.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;

/** Word counting and IELTS band arithmetic; the overall band is always computed here, never taken from the LLM. */
public final class WritingScore {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*");
    private static final BigDecimal NINE = BigDecimal.valueOf(9);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private WritingScore() {}

    /** Letters and digits, joined by an apostrophe or hyphen ("don't", "well-known" are one word each). */
    public static int countWords(String text) {
        if (text == null) return 0;
        return (int) WORD.matcher(text).results().count();
    }

    /** A criterion band: 0 to 9 in steps of 0.5. */
    public static boolean isBand(BigDecimal band) {
        return band != null && band.signum() >= 0 && band.compareTo(NINE) <= 0
                && band.multiply(TWO).stripTrailingZeros().scale() <= 0;
    }

    /**
     * The mean of the criteria rounded to the nearest half band, with .25 rounding up to .5 and .75 up to the next
     * whole band: {@code floor(mean * 2 + 0.5) / 2}.
     */
    public static BigDecimal overallBand(List<BigDecimal> bands) {
        if (bands.isEmpty()) throw new IllegalArgumentException("At least one band is required");
        BigDecimal sum = bands.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal doubledMean = sum.multiply(TWO).divide(BigDecimal.valueOf(bands.size()), 10, RoundingMode.HALF_UP);
        return doubledMean.add(new BigDecimal("0.5")).setScale(0, RoundingMode.FLOOR)
                .divide(TWO, 1, RoundingMode.UNNECESSARY);
    }
}
