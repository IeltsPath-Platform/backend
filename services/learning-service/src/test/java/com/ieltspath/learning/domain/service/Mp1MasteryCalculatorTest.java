package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.EvidencePoint;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.Mp1Params;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class Mp1MasteryCalculatorTest {
    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    private final Mp1MasteryCalculator calculator = new Mp1MasteryCalculator(Mp1Params.defaults());

    private static EvidencePoint point(boolean correct, EvidenceSource source, Duration age) {
        return new EvidencePoint(correct, source, NOW.minus(age), null);
    }

    private static EvidencePoint point(boolean correct, EvidenceSource source, Duration age, UUID question) {
        return new EvidencePoint(correct, source, NOW.minus(age), question);
    }

    @Test
    void noEvidenceIsNotSufficient() {
        var result = calculator.compute(List.of(), NOW);
        assertThat(result.mastery()).isZero();
        assertThat(result.sufficient()).isFalse();
    }

    @Test
    void oneOrTwoPiecesOfEvidenceAreNotEnough() {
        var one = calculator.compute(List.of(point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(1))), NOW);
        var two = calculator.compute(List.of(point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(3)),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(1))), NOW);
        assertThat(one.sufficient()).isFalse();
        assertThat(two.sufficient()).isFalse();
    }

    @Test
    void threePiecesOnTheSameDayAreNotEnough() {
        var result = calculator.compute(List.of(
                point(true, EvidenceSource.REVIEW_SET, Duration.ofHours(3)),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofHours(2)),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofHours(1))), NOW);
        assertThat(result.sufficient()).isFalse();
    }

    @Test
    void threePiecesOverTwoDaysWithEnoughWeightAreSufficient() {
        var result = calculator.compute(List.of(
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(1).plusHours(2)),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofHours(2)),
                point(false, EvidenceSource.REVIEW_SET, Duration.ofHours(1))), NOW);
        assertThat(result.sufficient()).isTrue();
        assertThat(result.mastery()).isBetween(0.6, 0.7);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    void lightEvidenceAloneCannotReachTheWeightFloor() {
        // Three essay answers over three days meet the count and day rules, but 60-day-old essays carry about
        // 0.5 x 0.25 each, far under the weight floor.
        var stale = calculator.compute(List.of(
                point(true, EvidenceSource.LESSON_WRITING, Duration.ofDays(61)),
                point(true, EvidenceSource.LESSON_WRITING, Duration.ofDays(60)),
                point(true, EvidenceSource.LESSON_WRITING, Duration.ofDays(59))), NOW);
        assertThat(stale.sufficient()).isFalse();
    }

    @Test
    void evidenceHalvesEveryThirtyDays() {
        var fresh = calculator.compute(List.of(point(true, EvidenceSource.REVIEW_SET, Duration.ZERO)), NOW);
        var sixtyDays = calculator.compute(List.of(point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(60))), NOW);
        assertThat(sixtyDays.confidence() / fresh.confidence()).isCloseTo(0.25, within(1e-9));
    }

    @Test
    void olderWrongAnswersCountLessThanFreshRightOnes() {
        var result = calculator.compute(List.of(
                point(false, EvidenceSource.REVIEW_SET, Duration.ofDays(90)),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(2)),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(1))), NOW);
        assertThat(result.mastery()).isGreaterThan(0.9);
    }

    @Test
    void onlyThreePracticeAnswersCountInTheWindow() {
        List<EvidencePoint> history = new ArrayList<>();
        // two exam answers (wrong), then five practice answers (right), oldest first
        history.add(point(false, EvidenceSource.ASSESSMENT, Duration.ofDays(8)));
        history.add(point(false, EvidenceSource.ASSESSMENT, Duration.ofDays(7)));
        for (int day = 5; day >= 1; day--) {
            history.add(point(true, EvidenceSource.PRACTICE_SET, Duration.ofDays(day)));
        }
        var result = calculator.compute(history, NOW);
        // window of 8 holds 5 practice but only the 3 newest practice are valid, so the 2 wrong exams still count
        assertThat(result.mastery()).isLessThan(0.75);
        var withoutCap = new Mp1MasteryCalculator(new Mp1Params(8, 99, 30, 3, 2, 1.0, 0.03, 7,
                Mp1Params.defaults().weights()));
        assertThat(withoutCap.compute(history, NOW).mastery()).isGreaterThan(result.mastery());
    }

    @Test
    void theWindowKeepsTheEightNewestValidAnswers() {
        List<EvidencePoint> history = new ArrayList<>();
        for (int day = 20; day >= 13; day--) history.add(point(false, EvidenceSource.REVIEW_SET, Duration.ofDays(day)));
        for (int day = 8; day >= 1; day--) history.add(point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(day)));
        assertThat(calculator.compute(history, NOW).mastery()).isEqualTo(1.0);
    }

    @Test
    void theSameQuestionWithinSevenDaysCountsOnce() {
        UUID question = UUID.randomUUID();
        var repeated = calculator.compute(List.of(
                point(false, EvidenceSource.REVIEW_SET, Duration.ofDays(4), question),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(3), question),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(2), question),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(1), UUID.randomUUID())), NOW);
        // the first wrong answer counts, the two repeats do not
        assertThat(repeated.mastery()).isLessThan(0.7);
        assertThat(repeated.sufficient()).isFalse();
    }

    @Test
    void theSameQuestionAfterSevenDaysCountsAgain() {
        UUID question = UUID.randomUUID();
        var result = calculator.compute(List.of(
                point(false, EvidenceSource.REVIEW_SET, Duration.ofDays(9), question),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(1), question),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofHours(1), UUID.randomUUID())), NOW);
        assertThat(result.sufficient()).isTrue();
    }

    @Test
    void evidenceWithoutAQuestionIsNeverDeduplicated() {
        var result = calculator.compute(List.of(
                point(true, EvidenceSource.ASSESSMENT, Duration.ofDays(3)),
                point(true, EvidenceSource.ASSESSMENT, Duration.ofDays(2)),
                point(true, EvidenceSource.ASSESSMENT, Duration.ofDays(1))), NOW);
        assertThat(result.sufficient()).isTrue();
    }

    @Test
    void practiceWeighsLessThanAnExamAnswer() {
        var examWrong = calculator.compute(List.of(
                point(true, EvidenceSource.PRACTICE_SET, Duration.ofDays(3)),
                point(true, EvidenceSource.PRACTICE_SET, Duration.ofDays(2)),
                point(false, EvidenceSource.ASSESSMENT, Duration.ofDays(1))), NOW);
        var practiceWrong = calculator.compute(List.of(
                point(true, EvidenceSource.ASSESSMENT, Duration.ofDays(3)),
                point(true, EvidenceSource.ASSESSMENT, Duration.ofDays(2)),
                point(false, EvidenceSource.PRACTICE_SET, Duration.ofDays(1))), NOW);
        assertThat(examWrong.mastery()).isLessThan(practiceWrong.mastery());
    }
}
