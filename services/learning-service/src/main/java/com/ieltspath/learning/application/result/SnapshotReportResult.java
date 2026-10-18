package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.service.SkillRollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The weekly and monthly views of a learner's progress, read from stored snapshots. A learner with no snapshot for the
 * period gets a null {@code snapshot}, not an error. The band is an estimate, not an exam result.
 */
public final class SnapshotReportResult {
    private SnapshotReportResult() {}

    /** {@code accuracy} is correct over answered, null when nothing was answered. */
    public record SnapshotView(List<SkillRollup.Summary> skills, Map<String, Integer> tierCounts, int answered,
                               int correct, Double accuracy, int activeMinutes, BigDecimal estimatedBand,
                               List<UUID> focusKnowledgePointIds) {}

    /** {@code direction}: UP, DOWN, SAME, NEW (no earlier week to compare) or NONE (no mastery in the block). */
    public record SkillChange(String skill, Double mastery, Double previousMastery, String direction) {}

    public record WeeklyReport(LocalDate weekStart, SnapshotView snapshot, List<SkillChange> changes,
                               SuggestionResult nextWeek) {}

    public record HistoryPoint(LocalDate weekStart, BigDecimal estimatedBand, Double accuracy, int activeMinutes,
                               Map<String, Double> mastery) {}

    public record History(List<HistoryPoint> weeks) {}

    public record MonthlyReport(LocalDate monthStart, SnapshotView snapshot) {}

    /** {@code date} is the projected day the target is reached; when null, {@code reason} says why. */
    public record Projection(LocalDate date, String reason) {}
}
