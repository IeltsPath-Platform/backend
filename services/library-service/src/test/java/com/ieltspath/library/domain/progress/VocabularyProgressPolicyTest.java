package com.ieltspath.library.domain.progress;

import com.ieltspath.library.domain.vo.CefrLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VocabularyProgressPolicyTest {
    @Test void learnedIncludesReviewedOrAtLeastFamiliarCards() {
        assertTrue(VocabularyProgressPolicy.isLearned(1, 1));
        assertTrue(VocabularyProgressPolicy.isLearned(0, 2));
        assertFalse(VocabularyProgressPolicy.isLearned(0, 1));
    }

    @Test void weekStartsMondayAndUsesTheRequestedZone() {
        var window = VocabularyProgressPolicy.weekWindow(LocalDate.of(2026, 10, 26), ZoneId.of("Asia/Ho_Chi_Minh"));
        assertEquals(LocalDate.of(2026, 10, 26), window.start());
        assertEquals(LocalDate.of(2026, 11, 2), window.endExclusive());
        assertEquals("Asia/Ho_Chi_Minh", window.timezone().getId());
        var sunday = VocabularyProgressPolicy.weekWindow(LocalDate.of(2026, 11, 1), ZoneId.of("UTC"));
        assertEquals(LocalDate.of(2026, 10, 26), sunday.start());
    }

    @Test void streakMayEndTodayOrYesterdayAndResetsAcrossAMissingDay() {
        LocalDate today = LocalDate.of(2026, 10, 26);
        assertEquals(3, VocabularyProgressPolicy.reviewStreak(Set.of(today, today.minusDays(1), today.minusDays(2)), today));
        assertEquals(2, VocabularyProgressPolicy.reviewStreak(Set.of(today.minusDays(1), today.minusDays(2)), today));
        assertEquals(0, VocabularyProgressPolicy.reviewStreak(Set.of(today.minusDays(3)), today));
    }

    @Test void achievementsCapVisibleProgressAtTheirTargets() {
        var result = VocabularyProgressPolicy.achievements(10, 2, 350);
        assertEquals(List.of("REVIEW_STREAK_7", "TOPIC_COMPLETED", "MASTERED_300"), result.stream().map(VocabularyProgressPolicy.Achievement::code).toList());
        assertTrue(result.stream().allMatch(VocabularyProgressPolicy.Achievement::achieved));
        assertEquals(7, result.get(0).progress());
        assertEquals(1, result.get(1).progress());
        assertEquals(300, result.get(2).progress());
    }

    @Test void bandSelectionUsesGreatestLearningCountAndLowestBandNeedingReview() {
        var counts = List.of(new VocabularyProgressPolicy.BandCount("A1", 2, 0),
                new VocabularyProgressPolicy.BandCount("A2", 2, 3),
                new VocabularyProgressPolicy.BandCount("B1", 0, 1));
        assertEquals(CefrLevel.A1, VocabularyProgressPolicy.currentBand(counts));
        assertEquals(CefrLevel.A2, VocabularyProgressPolicy.suggestedBand(counts));
        assertNull(VocabularyProgressPolicy.currentBand(List.of(new VocabularyProgressPolicy.BandCount("A1", 0, 0))));
        assertNull(VocabularyProgressPolicy.suggestedBand(List.of()));
    }

    @Test void validatesWeeklyGoalBounds() {
        assertDoesNotThrow(() -> VocabularyProgressPolicy.validateWeeklyTarget(120));
        assertThrows(IllegalArgumentException.class, () -> VocabularyProgressPolicy.validateWeeklyTarget(9));
        assertThrows(IllegalArgumentException.class, () -> VocabularyProgressPolicy.validateWeeklyTarget(1001));
    }
}
