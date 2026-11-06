package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpeakingPracticeBandTest {
    private static SpeakingSubmission graded(BigDecimal overall, Map<String, Object> result) {
        SpeakingSubmission submission = mock(SpeakingSubmission.class);
        when(submission.overallBand()).thenReturn(overall);
        when(submission.result()).thenReturn(result);
        return submission;
    }

    private static Map<String, Object> criteria(double... bands) {
        return Map.of("display", Map.of("criteria", java.util.Arrays.stream(bands)
                .mapToObj(band -> Map.<String, Object>of("band", new BigDecimal(String.valueOf(band)))).toList()));
    }

    @Test
    void usesTheOverallBandWhenTheGradeHasOne() {
        assertThat(SpeakingPracticeBand.of(graded(new BigDecimal("6.5"), criteria(1, 1, 1, 1)))).isEqualByComparingTo("6.5");
    }

    @Test
    void aSingleAnswerTakesTheMeanOfItsCriteriaToTheNearestHalfBand() {
        assertThat(SpeakingPracticeBand.of(graded(null, criteria(6.0, 6.0, 5.5, 5.5)))).isEqualByComparingTo("6.0");
        assertThat(SpeakingPracticeBand.of(graded(null, criteria(6.0, 6.0, 6.0, 5.5)))).isEqualByComparingTo("6.0");
        assertThat(SpeakingPracticeBand.of(graded(null, criteria(1, 1, 1, 1)))).isEqualByComparingTo("1.0");
    }

    @Test
    void hasNoBandWithoutAnyCriterion() {
        assertThat(SpeakingPracticeBand.of(graded(null, Map.of()))).isNull();
        assertThat(SpeakingPracticeBand.of(graded(null, Map.of("display", Map.of("criteria", List.of()))))).isNull();
        assertThat(SpeakingPracticeBand.of(graded(null, null))).isNull();
    }
}
