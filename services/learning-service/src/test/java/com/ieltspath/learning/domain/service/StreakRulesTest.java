package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.StreakState;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StreakRulesTest {
    private static final LocalDate MON = LocalDate.of(2026, 3, 2);

    private static StreakState answerEachDay(LocalDate from, int days) {
        StreakState state = StreakState.empty();
        for (int index = 0; index < days; index++) {
            state = StreakRules.recordAnswer(state, from.plusDays(index)).state();
        }
        return state;
    }

    @Test
    void consecutiveDaysGrowTheStreakAndTheSameDayChangesNothing() {
        StreakState state = answerEachDay(MON, 3);
        assertThat(state.currentDays()).isEqualTo(3);

        StreakState again = StreakRules.recordAnswer(state, MON.plusDays(2)).state();
        assertThat(again.currentDays()).isEqualTo(3);
    }

    @Test
    void aSevenDayStreakReachesTheSevenMilestoneOnTheSeventhDay() {
        StreakRules.Recorded sixth = StreakRules.recordAnswer(answerEachDay(MON, 6), MON.plusDays(5));
        StreakRules.Recorded seventh = StreakRules.recordAnswer(sixth.state(), MON.plusDays(6));

        assertThat(sixth.milestones()).isEmpty();
        assertThat(seventh.milestones()).containsExactly(7);
        assertThat(seventh.state().currentDays()).isEqualTo(7);
    }

    @Test
    void aMissedDayWithoutAFreezeBreaksTheStreakAndTheLostLengthIsKept() {
        StreakState state = answerEachDay(MON, 16);

        StreakState read = StreakRules.forRead(state, MON.plusDays(17));

        assertThat(read.currentDays()).isZero();
        assertThat(read.lostStreakDays()).isEqualTo(16);
        StreakState restarted = StreakRules.recordAnswer(read, MON.plusDays(17)).state();
        assertThat(restarted.currentDays()).as("a new streak starts at one").isEqualTo(1);
        assertThat(restarted.longestDays()).isEqualTo(16);
    }

    @Test
    void aFreezeBoughtTodayCoversTomorrowAndTheStreakCarriesOn() {
        StreakState state = answerEachDay(MON, 5);
        StreakState bought = StreakRules.buyFreeze(state, MON.plusDays(4));
        assertThat(bought.freezeAvailable()).isTrue();

        StreakState afterGap = StreakRules.recordAnswer(bought, MON.plusDays(6)).state();

        assertThat(afterGap.currentDays()).as("the frozen day adds nothing, today adds one").isEqualTo(6);
        assertThat(afterGap.freezeAvailable()).isFalse();
        assertThat(afterGap.lostStreakDays()).isZero();
    }

    @Test
    void aFreezeBoughtOnAnEmptyDayCoversThatDay() {
        StreakState state = answerEachDay(MON, 4);
        StreakState bought = StreakRules.buyFreeze(state, MON.plusDays(4));

        StreakState read = StreakRules.forRead(bought, MON.plusDays(5));

        assertThat(read.currentDays()).as("the empty purchase day is covered, the streak carries on").isEqualTo(4);
        assertThat(read.freezeAvailable()).isFalse();
        assertThat(read.frozenRunEnd()).isEqualTo(MON.plusDays(4));
    }

    @Test
    void twoEmptyDaysAreNotCoveredByOneFreeze() {
        StreakState state = answerEachDay(MON, 5);
        StreakState bought = StreakRules.buyFreeze(state, MON.plusDays(4));

        StreakState read = StreakRules.forRead(bought, MON.plusDays(7));

        assertThat(read.currentDays()).isZero();
        assertThat(read.lostStreakDays()).isEqualTo(5);
    }

    @Test
    void theFreezeCostRisesWithEachConsecutiveFrozenDayAndStopsAtTheLimit() {
        StreakState state = answerEachDay(MON, 3);
        assertThat(StreakRules.freezeCost(state, MON.plusDays(2))).isEqualTo(10);

        StreakState first = StreakRules.buyFreeze(state, MON.plusDays(2));
        StreakState usedOnThree = StreakRules.recordAnswer(first, MON.plusDays(4)).state();
        // Day 3 was frozen, so a purchase on day 4 would extend that run: the second consecutive freeze costs 20.
        assertThat(usedOnThree.frozenRunEnd()).isEqualTo(MON.plusDays(3));
        assertThat(StreakRules.freezeCost(usedOnThree, MON.plusDays(4))).isEqualTo(20);
    }

    @Test
    void consecutiveFreezesAreCountedAcrossTheRunAndThePriceIsTenTimesTheCount() {
        StreakState state = answerEachDay(MON, 3);
        // Day 3 covered by a freeze bought on day 2.
        StreakState afterOne = StreakRules.recordAnswer(StreakRules.buyFreeze(state, MON.plusDays(2)),
                MON.plusDays(4)).state();
        // Day 4 covered by a freeze bought on day 3 (the run continues). Price on day 4 is the second: 20.
        StreakState boughtOnThreeCovering = StreakRules.buyFreeze(
                StreakRules.forRead(afterOne, MON.plusDays(3)), MON.plusDays(3));
        assertThat(StreakRules.freezeCost(boughtOnThreeCovering, MON.plusDays(3))).isEqualTo(20);
    }

    @Test
    void aSixthConsecutiveFreezeIsOverTheLimit() {
        StreakState run = new StreakState(7, 7, MON, MON, MON.plusDays(10), false, null, MON.plusDays(10), 5, 0);
        assertThat(StreakRules.overLimit(run, MON.plusDays(10))).isTrue();

        StreakState fourRun = new StreakState(7, 7, MON, MON, MON.plusDays(10), false, null, MON.plusDays(10), 4, 0);
        assertThat(StreakRules.overLimit(fourRun, MON.plusDays(10))).isFalse();
    }

    @Test
    void theStreakOfAYearAwayIsReadWithoutWalkingEveryEmptyDay() {
        StreakState state = answerEachDay(MON, 4);

        StreakState read = StreakRules.forRead(state, MON.plusYears(3));

        assertThat(read.currentDays()).isZero();
        assertThat(read.lostStreakDays()).isEqualTo(4);
    }

    @Test
    void activeOnReportsOnlyTheDayOfTheLastAnswer() {
        StreakState state = answerEachDay(MON, 2);

        assertThat(StreakRules.activeOn(state, MON.plusDays(1))).isTrue();
        assertThat(StreakRules.activeOn(state, MON.plusDays(2))).isFalse();
        assertThat(StreakRules.recordAnswer(state, MON.plusDays(2)).state().currentDays()).isEqualTo(3);
    }
}
