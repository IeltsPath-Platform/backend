package com.ieltspath.library.domain.progress;

import com.ieltspath.library.domain.vo.CefrLevel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class VocabularyProgressPolicy {
    private VocabularyProgressPolicy() {}

    public static boolean isLearned(int reviewCount, int leitnerBox) {
        return reviewCount > 0 || leitnerBox >= 2;
    }

    public static WeekWindow weekWindow(LocalDate today, ZoneId timezone) {
        LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new WeekWindow(start, start.plusDays(7), timezone);
    }

    /** Current streak includes today, or yesterday when today has not yet had a review. */
    public static int reviewStreak(Set<LocalDate> reviewDays, LocalDate today) {
        if (reviewDays == null || reviewDays.isEmpty()) return 0;
        LocalDate cursor = reviewDays.contains(today) ? today : today.minusDays(1);
        if (!reviewDays.contains(cursor)) return 0;
        int days = 0;
        while (reviewDays.contains(cursor)) {
            days++;
            cursor = cursor.minusDays(1);
        }
        return days;
    }

    public static List<Achievement> achievements(int reviewStreak, long completedTopics, long masteredWords) {
        return List.of(
                new Achievement("REVIEW_STREAK_7", reviewStreak >= 7, Math.min(reviewStreak, 7), 7),
                new Achievement("TOPIC_COMPLETED", completedTopics >= 1, Math.min(completedTopics, 1), 1),
                new Achievement("MASTERED_300", masteredWords >= 300, Math.min(masteredWords, 300), 300));
    }

    public static String topicStatus(long total, long learned) {
        if (learned == 0) return "NOT_STARTED";
        if (learned >= total) return "COMPLETED";
        return "IN_PROGRESS";
    }

    public static void validateWeeklyTarget(int target) {
        if (target < 10 || target > 1000) throw new IllegalArgumentException("targetWords must be between 10 and 1000");
    }

    public static CefrLevel currentBand(List<BandCount> bands) {
        return bands.stream().max(Comparator.comparingLong(BandCount::learning)
                        .thenComparingInt(band -> -CefrLevel.valueOf(band.cefr()).ordinal()))
                .filter(band -> band.learning() > 0)
                .map(band -> CefrLevel.valueOf(band.cefr())).orElse(null);
    }

    public static CefrLevel suggestedBand(List<BandCount> bands) {
        return bands.stream().filter(band -> band.needsReview() > 0)
                .min(Comparator.comparingInt(band -> CefrLevel.valueOf(band.cefr()).ordinal()))
                .map(band -> CefrLevel.valueOf(band.cefr())).orElse(null);
    }

    public record WeekWindow(LocalDate start, LocalDate endExclusive, ZoneId timezone) {}
    public record Achievement(String code, boolean achieved, long progress, long target) {}
    public record BandCount(String cefr, long learning, long needsReview) {}
}
