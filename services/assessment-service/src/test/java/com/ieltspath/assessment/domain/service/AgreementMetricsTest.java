package com.ieltspath.assessment.domain.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgreementMetricsTest {
    @Test void perfectAgreementHasOneQwkAndZeroError() {
        var scores = List.of(new BigDecimal("5.5"), new BigDecimal("6.0"), new BigDecimal("7.0"));
        var result = AgreementMetrics.calculate(scores, scores);
        assertEquals(new BigDecimal("1.000000"), result.qwk());
        assertEquals(new BigDecimal("0.000000"), result.mae());
        assertEquals(new BigDecimal("1.000000"), result.withinHalfBand());
    }

    @Test void computesWithinHalfBandMaeAndQuadraticKappa() {
        var result = AgreementMetrics.calculate(
                List.of(new BigDecimal("5.0"), new BigDecimal("6.0"), new BigDecimal("7.0")),
                List.of(new BigDecimal("5.5"), new BigDecimal("6.0"), new BigDecimal("8.0")));
        assertEquals(new BigDecimal("0.666667"), result.withinHalfBand());
        assertEquals(new BigDecimal("0.500000"), result.mae());
        assertEquals(new BigDecimal("0.800000"), result.qwk());
    }

    @Test void rejectsMismatchedOrInvalidScores() {
        assertThrows(IllegalArgumentException.class, () -> AgreementMetrics.calculate(List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> AgreementMetrics.calculate(
                List.of(new BigDecimal("5.2")), List.of(new BigDecimal("5.0"))));
    }
}
