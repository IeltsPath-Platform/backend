package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * The band of one recorded answer inside a lesson's practice set. A full test has an overall band; a single answer has none,
 * so its band is the mean of its criterion bands to the nearest half band, rounded the way the overall band is.
 */
public final class SpeakingPracticeBand {
    private SpeakingPracticeBand() {
    }

    /** The overall band when the grade has one, else the mean of the criterion bands; null when neither exists. */
    public static BigDecimal of(SpeakingSubmission submission) {
        if (submission.overallBand() != null) return submission.overallBand();
        if (submission.result() == null || !(submission.result().get("display") instanceof Map<?, ?> display)
                || !(display.get("criteria") instanceof List<?> criteria)) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Object row : criteria) {
            if (row instanceof Map<?, ?> criterion && criterion.get("band") instanceof Number band) {
                sum = sum.add(new BigDecimal(band.toString()));
                count++;
            }
        }
        if (count == 0) return null;
        BigDecimal mean = sum.divide(BigDecimal.valueOf(count), 3, RoundingMode.HALF_UP);
        return mean.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
    }
}
