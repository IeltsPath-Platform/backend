package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.vo.MasteryRules;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasteryPolicyTest {
    private static final Instant LAST = Instant.parse("2026-10-01T00:00:00Z");
    private final MasteryPolicy policy = new MasteryPolicy(MasteryRules.defaults());

    @Test
    void tiersCutAtTheThresholdsOnTheStoredValue() {
        assertThat(policy.tier(0.0)).isEqualTo(MasteryTier.PRIORITY);
        assertThat(policy.tier(0.599)).isEqualTo(MasteryTier.PRIORITY);
        assertThat(policy.tier(0.6)).isEqualTo(MasteryTier.CONSOLIDATE);
        assertThat(policy.tier(0.799)).isEqualTo(MasteryTier.CONSOLIDATE);
        assertThat(policy.tier(0.8)).isEqualTo(MasteryTier.MAINTAIN);
        assertThat(policy.tier(1.0)).isEqualTo(MasteryTier.MAINTAIN);
    }

    @Test
    void aScoreThatRoundsOntoAThresholdTakesTheHigherTier() {
        // 0.5996 is stored as 0.600, so the learner sees and is judged at 0.6.
        assertThat(policy.tier(0.5996)).isEqualTo(MasteryTier.CONSOLIDATE);
        assertThat(policy.tier(0.5994)).isEqualTo(MasteryTier.PRIORITY);
    }

    @Test
    void aPriorityPointHasNoDueDateBecauseABlockingReviewCoversIt() {
        assertThat(policy.nextReviewAt(MasteryTier.PRIORITY, LAST, 0)).isNull();
    }

    @Test
    void aConsolidatingPointIsDueThreeDaysAfterTheLastEvidence() {
        assertThat(policy.nextReviewAt(MasteryTier.CONSOLIDATE, LAST, 5)).isEqualTo(LAST.plusSeconds(3 * 86400L));
    }

    @Test
    void aMaintainedPointFollowsTheIntervalOfItsStreakAndStaysOnTheLastOne() {
        int[] days = {7, 14, 30, 45, 60};
        for (int streak = 0; streak < days.length; streak++) {
            assertThat(policy.nextReviewAt(MasteryTier.MAINTAIN, LAST, streak))
                    .isEqualTo(LAST.plusSeconds(days[streak] * 86400L));
        }
        assertThat(policy.nextReviewAt(MasteryTier.MAINTAIN, LAST, 99)).isEqualTo(LAST.plusSeconds(60 * 86400L));
    }

    @Test
    void aWrongAnswerResetsTheStreak() {
        assertThat(policy.nextStreak(4, MasteryTier.MAINTAIN, MasteryTier.MAINTAIN, false)).isZero();
    }

    @Test
    void aRightAnswerGrowsTheStreakWhenTheTierHolds() {
        assertThat(policy.nextStreak(2, MasteryTier.CONSOLIDATE, MasteryTier.CONSOLIDATE, true)).isEqualTo(3);
        assertThat(policy.nextStreak(2, MasteryTier.CONSOLIDATE, MasteryTier.MAINTAIN, true)).isEqualTo(3);
    }

    @Test
    void aRightAnswerThatStillDropsTheTierLeavesTheStreak() {
        assertThat(policy.nextStreak(3, MasteryTier.MAINTAIN, MasteryTier.CONSOLIDATE, true)).isEqualTo(3);
    }

    @Test
    void theFirstEvidenceStartsTheStreakAtOneWhenRight() {
        assertThat(policy.nextStreak(null, null, MasteryTier.CONSOLIDATE, true)).isEqualTo(1);
        assertThat(policy.nextStreak(null, null, MasteryTier.PRIORITY, false)).isZero();
    }

    @Test
    void aProfileUnderStricterRulesMovesTierAndDueDate() {
        MasteryProfile profile = new MasteryProfile(UUID.randomUUID(), UUID.randomUUID(), 0.7, MasteryTier.CONSOLIDATE,
                6, 2, LAST, LAST.plusSeconds(3 * 86400L), 4000L, List.of("SPELLING"));

        MasteryProfile stricter = profile.under(new MasteryPolicy(
                new MasteryRules(0.75, 0.9, List.of(5, 10), 2)));
        assertThat(stricter.tier()).isEqualTo(MasteryTier.PRIORITY);
        assertThat(stricter.nextReviewAt()).isNull();

        MasteryProfile looser = profile.under(new MasteryPolicy(new MasteryRules(0.5, 0.65, List.of(5, 10), 2)));
        assertThat(looser.tier()).isEqualTo(MasteryTier.MAINTAIN);
        assertThat(looser.nextReviewAt()).isEqualTo(LAST.plusSeconds(10 * 86400L));
        assertThat(looser.mastery()).isEqualTo(0.7);
        assertThat(looser.topErrorCodes()).containsExactly("SPELLING");
    }

    @Test
    void rulesRejectNonsense() {
        assertThatThrownBy(() -> new MasteryRules(0.8, 0.6, List.of(7), 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryRules(0.0, 0.8, List.of(7), 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryRules(0.6, 1.1, List.of(7), 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryRules(0.6, 0.8, List.of(7, 7), 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryRules(0.6, 0.8, List.of(), 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryRules(0.6, 0.8, List.of(7), 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
