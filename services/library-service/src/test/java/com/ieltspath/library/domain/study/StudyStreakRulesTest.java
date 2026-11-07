package com.ieltspath.library.domain.study;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class StudyStreakRulesTest {
    final LocalDate today=LocalDate.of(2026,11,7);
    @Test void sameDayOnceAndMissedDayOnlyExpiresAfterMidnight() {
        var one=StudyStreakRules.recordAnswer(StudyStreakState.empty(),today);assertEquals(1,one.currentDays());
        assertEquals(one,StudyStreakRules.recordAnswer(one,today));assertEquals(1,StudyStreakRules.forRead(one,today.plusDays(1)).currentDays());
        var lost=StudyStreakRules.forRead(one,today.plusDays(2));assertEquals(0,lost.currentDays());assertEquals(1,lost.lostStreakDays());
        assertEquals(1,StudyStreakRules.recordAnswer(lost,today.plusDays(2)).currentDays());
    }
    @Test void freezeCoversOneEmptyDayAndPriceTracksConsecutiveFrozenDays() {
        var state=StudyStreakRules.recordAnswer(StudyStreakState.empty(),today);
        for(int i=1;i<=5;i++) {
            LocalDate purchase=today.plusDays(i);assertEquals(i*10,StudyStreakRules.freezeCost(state,purchase,10));assertFalse(StudyStreakRules.overLimit(state,purchase,5));
            state=StudyStreakRules.buyFreeze(state,purchase);assertTrue(state.freezeAvailable());
            state=StudyStreakRules.forRead(state,purchase.plusDays(1));assertFalse(state.freezeAvailable());assertEquals(1,state.currentDays());
        }
        assertTrue(StudyStreakRules.overLimit(state,today.plusDays(6),5));assertEquals(60,StudyStreakRules.freezeCost(state,today.plusDays(6),10));
    }
    @Test void activityAfterFrozenGapContinuesWithoutCountingTheFrozenDay() {
        var state=StudyStreakRules.recordAnswer(StudyStreakState.empty(),today);state=StudyStreakRules.buyFreeze(state,today.plusDays(1));
        state=StudyStreakRules.recordAnswer(state,today.plusDays(2));assertEquals(2,state.currentDays());assertEquals(2,state.longestDays());
        assertFalse(state.freezeAvailable());assertEquals(20,StudyStreakRules.freezeCost(state,today.plusDays(2),10));
    }
}
