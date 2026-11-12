package com.ieltspath.learning.domain.vo;

import com.ieltspath.learning.domain.service.StreakRules;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StreakViewTest {
    private static final LocalDate MON = LocalDate.of(2026, 3, 2);

    private static StreakState answered(LocalDate from, int days) {
        StreakState state = StreakState.empty();
        for (int index = 0; index < days; index++) {
            state = StreakRules.recordAnswer(state, from.plusDays(index)).state();
        }
        return state;
    }

    @Test
    void aLearnerWhoNeverUsedAFreezeHasNoFrozenDays() {
        StreakView view = StreakView.of(StreakRules.forRead(answered(MON, 3), MON.plusDays(2)), MON.plusDays(2));

        assertThat(view.frozenDays()).isEmpty();
    }

    @Test
    void theDayAFreezeCoveredIsListed() {
        StreakState owned = StreakRules.buyFreeze(answered(MON, 3), MON.plusDays(2));
        LocalDate friday = MON.plusDays(4);

        StreakView view = StreakView.of(StreakRules.forRead(owned, friday), friday);

        assertThat(view.currentDays()).isEqualTo(3);
        assertThat(view.freezeAvailable()).isFalse();
        assertThat(view.frozenDays()).containsExactly(MON.plusDays(3));
    }

    @Test
    void consecutiveFrozenDaysAreListedOldestFirst() {
        StreakState state = StreakRules.buyFreeze(answered(MON, 3), MON.plusDays(2));
        // The first freeze covers Thursday; a second bought on Friday covers Friday.
        state = StreakRules.buyFreeze(StreakRules.forRead(state, MON.plusDays(4)), MON.plusDays(4));
        LocalDate sunday = MON.plusDays(6);

        StreakView view = StreakView.of(StreakRules.forRead(state, sunday), sunday);

        assertThat(view.frozenDays()).containsExactly(MON.plusDays(3), MON.plusDays(4));
    }
}
