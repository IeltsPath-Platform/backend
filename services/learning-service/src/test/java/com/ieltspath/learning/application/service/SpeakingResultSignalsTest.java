package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.service.SpeakingEvidenceRule.Criterion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpeakingResultSignalsTest {
    private static SpeakingSubmission withResult(Map<String, Object> result) {
        SpeakingSubmission submission = mock(SpeakingSubmission.class);
        when(submission.result()).thenReturn(result);
        return submission;
    }

    private static SpeakingSubmission withSignals(Map<String, Object> signals) {
        return withResult(Map.of("display", Map.of(), "signals", signals));
    }

    @Test
    void anAnswerIsUsableOnlyWhenTheGraderSaidSo() {
        assertThat(SpeakingResultSignals.usableAsEvidence(withSignals(Map.of("usableAsEvidence", true)))).isTrue();
        assertThat(SpeakingResultSignals.usableAsEvidence(withSignals(Map.of("usableAsEvidence", false)))).isFalse();
        assertThat(SpeakingResultSignals.usableAsEvidence(withSignals(Map.of("usableAsEvidence", "true")))).isFalse();
        assertThat(SpeakingResultSignals.usableAsEvidence(withSignals(Map.of()))).isFalse();
    }

    @Test
    void anAnswerWithNoResultOrNoSignalsHasNothingToRead() {
        for (SpeakingSubmission empty : List.of(withResult(null), withResult(Map.of("display", Map.of())),
                withResult(Map.of("signals", "garbage")))) {
            assertThat(SpeakingResultSignals.usableAsEvidence(empty)).isFalse();
            assertThat(SpeakingResultSignals.criteria(empty)).isEmpty();
            assertThat(SpeakingResultSignals.errorCodes(empty)).isEmpty();
        }
    }

    @Test
    void criteriaCarryTheirCodeBandAndPoint() {
        UUID kp = UUID.randomUUID();
        var criteria = SpeakingResultSignals.criteria(withSignals(Map.of("criteria", List.of(
                Map.of("code", "FC", "band", new BigDecimal("6.5"), "kpId", kp.toString()),
                Map.of("code", "PR", "band", 5.5)))));

        assertThat(criteria).containsExactly(new Criterion("FC", kp, new BigDecimal("6.5")),
                new Criterion("PR", null, new BigDecimal("5.5")));
    }

    @Test
    void aStoredBandThatCameBackFromJsonAsAnIntegerIsStillRead() {
        var criteria = SpeakingResultSignals.criteria(withSignals(Map.of("criteria", List.of(
                Map.of("code", "FC", "band", 6)))));

        assertThat(criteria).singleElement().satisfies(row -> assertThat(row.band()).isEqualByComparingTo("6"));
    }

    @Test
    void malformedCriterionRowsAreSkippedAndABadPointIdIsReadAsNone() {
        List<Object> rows = new ArrayList<>(Arrays.asList(
                "not a map", Map.of("band", 6.0), Map.of("code", "LR"), Map.of("code", "LR", "band", "high"),
                Map.of("code", "GRA", "band", 6.0, "kpId", "not-a-uuid")));
        var criteria = SpeakingResultSignals.criteria(withSignals(Map.of("criteria", rows)));

        assertThat(criteria).containsExactly(new Criterion("GRA", null, new BigDecimal("6.0")));
        assertThat(SpeakingResultSignals.criteria(withSignals(Map.of("criteria", "oops")))).isEmpty();
    }

    @Test
    void errorCodesKeepRepeatsAndSkipBlankOrMalformedRows() {
        List<Object> rows = new ArrayList<>(Arrays.asList(
                Map.of("code", "SPK_NO_REASON"), Map.of("code", "SPK_NO_REASON"), Map.of("code", " "), Map.of("code", 5),
                Map.of("other", "x"), "nope"));

        assertThat(SpeakingResultSignals.errorCodes(withSignals(Map.of("errors", rows))))
                .containsExactly("SPK_NO_REASON", "SPK_NO_REASON");
        assertThat(SpeakingResultSignals.errorCodes(withSignals(Map.of("errors", 3)))).isEmpty();
    }
}
