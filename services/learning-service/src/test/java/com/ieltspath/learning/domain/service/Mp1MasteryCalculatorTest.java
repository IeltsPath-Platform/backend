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

    /** Defaults with a half-life so long that age changes nothing, to read counts straight from the score. */
    private static Mp1MasteryCalculator withoutDecay() {
        Mp1Params d = Mp1Params.defaults();
        return new Mp1MasteryCalculator(new Mp1Params(d.window(), d.maxPracticeInWindow(), 1e12, d.minEvidence(),
                d.minDistinctDays(), d.minConfidence(), d.hysteresis(), d.sameItemDays(), d.weights()));
    }

    @Test
    void theSameQuestionExactlySevenDaysLaterCountsAgain() {
        UUID question = UUID.randomUUID();
        var result = withoutDecay().compute(List.of(
                point(false, EvidenceSource.REVIEW_SET, Duration.ofDays(8), question),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(1), question)), NOW);
        assertThat(result.mastery()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void repeatsAreMeasuredFromTheLastAnswerThatCounted() {
        UUID question = UUID.randomUUID();
        // day 0 wrong (counts), day 5 right (repeat), day 8 right (counts), day 12 wrong (repeat of day 8)
        var result = withoutDecay().compute(List.of(
                point(false, EvidenceSource.REVIEW_SET, Duration.ofDays(12), question),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(7), question),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(4), question),
                point(false, EvidenceSource.REVIEW_SET, Duration.ZERO, question)), NOW);
        assertThat(result.mastery()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void evidenceDatedInTheFutureIsLeftOutAndTakesNoWindowSlot() {
        List<EvidencePoint> valid = new ArrayList<>();
        for (int day = 8; day >= 1; day--) valid.add(point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(day)));
        List<EvidencePoint> withFuture = new ArrayList<>(valid);
        withFuture.add(new EvidencePoint(false, EvidenceSource.ASSESSMENT, NOW.plus(Duration.ofHours(1)), null));
        var expected = calculator.compute(valid, NOW);
        var actual = calculator.compute(withFuture, NOW);
        assertThat(actual.mastery()).isEqualTo(1.0);
        assertThat(actual.confidence()).isCloseTo(expected.confidence(), within(1e-12));
    }

    @Test
    void anAnswerExactlyOneHalfLifeOldWeighsHalf() {
        var result = calculator.compute(List.of(point(true, EvidenceSource.REVIEW_SET, Duration.ofDays(30))), NOW);
        assertThat(result.confidence()).isCloseTo(0.5, within(1e-12));
    }

    @Test
    void aConfidenceExactlyAtTheFloorIsEnough() {
        List<EvidencePoint> history = List.of(
                point(true, EvidenceSource.PRACTICE_SET, Duration.ofDays(2)),
                point(true, EvidenceSource.PRACTICE_SET, Duration.ofDays(1)),
                point(true, EvidenceSource.REVIEW_SET, Duration.ofHours(1)));
        double total = calculator.compute(history, NOW).confidence();
        Mp1Params d = Mp1Params.defaults();
        var atFloor = new Mp1MasteryCalculator(new Mp1Params(d.window(), d.maxPracticeInWindow(), d.halfLifeDays(),
                d.minEvidence(), d.minDistinctDays(), total, d.hysteresis(), d.sameItemDays(), d.weights()));
        var justAbove = new Mp1MasteryCalculator(new Mp1Params(d.window(), d.maxPracticeInWindow(), d.halfLifeDays(),
                d.minEvidence(), d.minDistinctDays(), total + 1e-9, d.hysteresis(), d.sameItemDays(), d.weights()));
        assertThat(atFloor.compute(history, NOW).sufficient()).isTrue();
        assertThat(justAbove.compute(history, NOW).sufficient()).isFalse();
    }

    @Test
    void aSourceWeightOfZeroGivesZeroAndNeverNotANumber() {
        Mp1Params d = Mp1Params.defaults();
        var w = d.weights();
        var zeroPractice = new Mp1MasteryCalculator(new Mp1Params(d.window(), d.maxPracticeInWindow(), d.halfLifeDays(),
                d.minEvidence(), d.minDistinctDays(), d.minConfidence(), d.hysteresis(), d.sameItemDays(),
                new Mp1Params.SourceWeights(w.exam(), w.review(), w.probe(), 0.0, w.essay(), w.errorTrap(), w.humanReview())));
        var result = zeroPractice.compute(List.of(
                point(true, EvidenceSource.PRACTICE_SET, Duration.ofDays(2)),
                point(true, EvidenceSource.PRACTICE_SET, Duration.ofDays(1)),
                point(true, EvidenceSource.PRACTICE_SET, Duration.ofHours(1))), NOW);
        assertThat(result.mastery()).isZero();
        assertThat(Double.isNaN(result.mastery())).isFalse();
        assertThat(result.confidence()).isZero();
        assertThat(result.sufficient()).isFalse();
    }

    @Test
    void theParametersRefuseImpossibleValues() {
        var w = Mp1Params.defaults().weights();
        for (Runnable invalid : List.<Runnable>of(
                () -> new Mp1Params(0, 3, 30, 3, 2, 1.0, 0.03, 7, w),
                () -> new Mp1Params(8, 3, 0, 3, 2, 1.0, 0.03, 7, w),
                () -> new Mp1Params(8, 3, 30, 3, 2, 1.0, 0.1, 7, w),
                () -> new Mp1Params(8, 3, 30, 3, 2, 1.0, 0.03, -1, w))) {
            org.assertj.core.api.Assertions.assertThatThrownBy(invalid::run)
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid MP-1.0 parameters");
        }
        assertThat(new Mp1Params(8, 3, 30, 3, 2, 1.0, 0.099, 7, w).hysteresis()).isEqualTo(0.099);
    }

    @Test
    void masteryStaysBetweenZeroAndOneForAnyHistory() {
        java.util.Random random = new java.util.Random(20261113L);
        EvidenceSource[] sources = EvidenceSource.values();
        for (int run = 0; run < 500; run++) {
            List<EvidencePoint> history = new ArrayList<>();
            int size = random.nextInt(20);
            for (int i = 0; i < size; i++) {
                history.add(new EvidencePoint(random.nextBoolean(), sources[random.nextInt(sources.length)],
                        NOW.minus(Duration.ofHours(random.nextInt(24 * 200))),
                        random.nextInt(3) == 0 ? null : new UUID(0, random.nextInt(4))));
            }
            history.sort(java.util.Comparator.comparing(EvidencePoint::occurredAt));
            var result = calculator.compute(history, NOW);
            assertThat(result.mastery()).as("run %d", run).isBetween(0.0, 1.0);
            assertThat(result.confidence()).isGreaterThanOrEqualTo(0.0);
        }
    }
}
