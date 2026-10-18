package com.ieltspath.assessment.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Agreement statistics for a set of human and AI IELTS bands. */
public final class AgreementMetrics {
    private static final int SCALE = 6;
    private AgreementMetrics() {}

    public static Result calculate(List<BigDecimal> human, List<BigDecimal> ai) {
        if (human == null || ai == null || human.size() != ai.size() || human.isEmpty()) {
            throw new IllegalArgumentException("Agreement scores must be non-empty lists of equal size");
        }
        int n = human.size();
        BigDecimal absoluteError = BigDecimal.ZERO;
        int withinHalf = 0;
        long[][] observed = new long[19][19];
        long[] humanCounts = new long[19];
        long[] aiCounts = new long[19];
        for (int i = 0; i < n; i++) {
            BigDecimal h = valid(human.get(i));
            BigDecimal a = valid(ai.get(i));
            BigDecimal difference = h.subtract(a).abs();
            absoluteError = absoluteError.add(difference);
            if (difference.compareTo(new BigDecimal("0.5")) <= 0) withinHalf++;
            int hi = halfBandIndex(h);
            int aiIndex = halfBandIndex(a);
            observed[hi][aiIndex]++;
            humanCounts[hi]++;
            aiCounts[aiIndex]++;
        }
        double observedDisagreement = 0;
        double expectedDisagreement = 0;
        for (int h = 0; h < 19; h++) {
            for (int a = 0; a < 19; a++) {
                double weight = Math.pow(h - a, 2) / Math.pow(18, 2);
                observedDisagreement += weight * observed[h][a] / n;
                expectedDisagreement += weight * humanCounts[h] * aiCounts[a] / (double) (n * n);
            }
        }
        double qwk = expectedDisagreement == 0
                ? (observedDisagreement == 0 ? 1 : 0)
                : 1 - observedDisagreement / expectedDisagreement;
        return new Result(n,
                BigDecimal.valueOf(withinHalf).divide(BigDecimal.valueOf(n), SCALE, RoundingMode.HALF_UP),
                absoluteError.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.HALF_UP),
                BigDecimal.valueOf(qwk).setScale(SCALE, RoundingMode.HALF_UP));
    }

    private static BigDecimal valid(BigDecimal score) {
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(9)) > 0
                || score.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Bands must be between 0 and 9 in half-band steps");
        }
        return score;
    }

    private static int halfBandIndex(BigDecimal score) {
        return score.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    public record Result(int n, BigDecimal withinHalfBand, BigDecimal mae, BigDecimal qwk) {}
}
