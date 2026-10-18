package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.BandEstimator.Estimate;
import com.ieltspath.learning.domain.service.BandEstimator.Point;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ieltspath.learning.domain.vo.MasteryTier.CONSOLIDATE;
import static com.ieltspath.learning.domain.vo.MasteryTier.INSUFFICIENT;
import static com.ieltspath.learning.domain.vo.MasteryTier.MAINTAIN;
import static com.ieltspath.learning.domain.vo.MasteryTier.PRIORITY;
import static org.assertj.core.api.Assertions.assertThat;

class BandEstimatorTest {
    private static final BigDecimal B55 = new BigDecimal("5.5");
    private static final BigDecimal B60 = new BigDecimal("6.0");
    private static final BigDecimal B65 = new BigDecimal("6.5");

    private static List<Point> points(String skill, BigDecimal band, MasteryTier... tiers) {
        List<Point> list = new ArrayList<>();
        for (MasteryTier tier : tiers) list.add(new Point(UUID.randomUUID(), skill, band, tier, 0.5));
        return list;
    }

    private static List<Point> join(List<Point>... parts) {
        List<Point> all = new ArrayList<>();
        for (List<Point> part : parts) all.addAll(part);
        return all;
    }

    @Test
    void aSkillBandIsTheHighestBandWhereEightyPercentOfPointsMaintain() {
        List<Point> reading = join(
                points("READING", B60, MAINTAIN, MAINTAIN, MAINTAIN, MAINTAIN, CONSOLIDATE),
                points("READING", B65, MAINTAIN, MAINTAIN, CONSOLIDATE, PRIORITY));

        Estimate estimate = BandEstimator.estimate(reading, null);

        assertThat(estimate.skillBands().get("READING")).isEqualByComparingTo(B60);
    }

    @Test
    void aBandBelowEightyPercentDoesNotCount() {
        List<Point> reading = points("READING", B60, MAINTAIN, MAINTAIN, MAINTAIN, CONSOLIDATE, CONSOLIDATE);

        Estimate estimate = BandEstimator.estimate(reading, null);

        assertThat(estimate.skillBands()).doesNotContainKey("READING");
        assertThat(estimate.overall()).isNull();
    }

    @Test
    void thePlacementBandStandsInWhenNoBandIsReached() {
        List<Point> reading = points("READING", B60, CONSOLIDATE, PRIORITY);

        Estimate estimate = BandEstimator.estimate(reading, B55);

        assertThat(estimate.skillBands().get("READING")).isEqualByComparingTo(B55);
    }

    @Test
    void aSkillWithoutPointsGetsNoBandEvenWithAPlacement() {
        Estimate estimate = BandEstimator.estimate(points("READING", B60, MAINTAIN), B55);

        assertThat(estimate.skillBands()).containsOnlyKeys("READING");
        assertThat(estimate.speakingIncluded()).isFalse();
    }

    @Test
    void theOverallBandIsTheAverageOfTheSkillsRoundedToAHalf() {
        List<Point> all = join(points("LISTENING", B60, MAINTAIN), points("READING", B65, MAINTAIN));

        Estimate estimate = BandEstimator.estimate(all, null);

        assertThat(estimate.overall()).as("6.25 rounds up to 6.5").isEqualByComparingTo(B65);
    }

    @Test
    void speakingCountsOnlyWhenItHasABand() {
        List<Point> all = join(points("LISTENING", B60, MAINTAIN), points("READING", B60, MAINTAIN),
                points("WRITING", B60, MAINTAIN), points("SPEAKING", B65, MAINTAIN));

        Estimate estimate = BandEstimator.estimate(all, null);

        assertThat(estimate.speakingIncluded()).isTrue();
        assertThat(estimate.overall()).as("6.125 rounds down to 6.0").isEqualByComparingTo(B60);
    }

    @Test
    void grammarAndVocabularyNeverEnterTheBand() {
        List<Point> all = join(points("GRAMMAR", B65, MAINTAIN, MAINTAIN), points("READING", B60, MAINTAIN));

        Estimate estimate = BandEstimator.estimate(all, null);

        assertThat(estimate.skillBands()).containsOnlyKeys("READING");
        assertThat(estimate.overall()).isEqualByComparingTo(B60);
    }

    @Test
    void insufficientPointsCountAsNotMaintained() {
        List<Point> reading = points("READING", B60, MAINTAIN, MAINTAIN, MAINTAIN, MAINTAIN, INSUFFICIENT);

        Estimate estimate = BandEstimator.estimate(reading, null);

        assertThat(estimate.skillBands().get("READING")).as("4 of 5 is exactly 80%").isEqualByComparingTo(B60);
        assertThat(BandEstimator.estimate(points("READING", B60, MAINTAIN, MAINTAIN, MAINTAIN, INSUFFICIENT), null)
                .skillBands()).as("3 of 4 is 75%").isEmpty();
    }

    @Test
    void closeToTheNextBandListsAtMostFivePointsStillToDo() {
        List<Point> reading = join(
                points("READING", B60, MAINTAIN, MAINTAIN),
                points("READING", B65, CONSOLIDATE, CONSOLIDATE, CONSOLIDATE, CONSOLIDATE, CONSOLIDATE, PRIORITY));

        Estimate estimate = BandEstimator.estimate(reading, null);

        assertThat(estimate.hints()).hasSize(1);
        assertThat(estimate.hints().get(0).skill()).isEqualTo("READING");
        assertThat(estimate.hints().get(0).nextBand()).isEqualByComparingTo(B65);
        assertThat(estimate.hints().get(0).pointIds()).hasSize(5);
    }

    @Test
    void noHintWhenTheNextBandIsStillFarOff() {
        List<Point> reading = join(
                points("READING", B60, MAINTAIN, MAINTAIN),
                points("READING", B65, CONSOLIDATE, PRIORITY, PRIORITY, PRIORITY));

        assertThat(BandEstimator.estimate(reading, null).hints()).isEmpty();
    }
}
