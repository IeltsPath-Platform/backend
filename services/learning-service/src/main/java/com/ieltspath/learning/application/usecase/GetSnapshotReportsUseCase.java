package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.SnapshotReportResult.History;
import com.ieltspath.learning.application.result.SnapshotReportResult.HistoryPoint;
import com.ieltspath.learning.application.result.SnapshotReportResult.MonthlyReport;
import com.ieltspath.learning.application.result.SnapshotReportResult.SkillChange;
import com.ieltspath.learning.application.result.SnapshotReportResult.SnapshotView;
import com.ieltspath.learning.application.result.SnapshotReportResult.WeeklyReport;
import com.ieltspath.learning.application.service.SnapshotService;
import com.ieltspath.learning.domain.repository.SnapshotRepository;
import com.ieltspath.learning.domain.repository.SnapshotRepository.Period;
import com.ieltspath.learning.domain.repository.SnapshotRepository.Snapshot;
import com.ieltspath.learning.domain.service.SkillRollup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The weekly report with its change from the week before, the weekly history, and the monthly report. */
@Service
public class GetSnapshotReportsUseCase {
    public static final int MIN_HISTORY_WEEKS = 4;
    public static final int MAX_HISTORY_WEEKS = 12;
    /** A skill whose mastery moved less than this between two weeks is reported as unchanged. */
    private static final double SAME_BELOW = 0.02;

    private final SnapshotRepository snapshots;
    private final SnapshotService snapshotService;
    private final GetSuggestionsUseCase suggestions;

    public GetSnapshotReportsUseCase(SnapshotRepository snapshots, SnapshotService snapshotService,
                                     GetSuggestionsUseCase suggestions) {
        this.snapshots = snapshots;
        this.snapshotService = snapshotService;
        this.suggestions = suggestions;
    }

    /** {@code weekStart} is a Monday; without one, the week before the current one. */
    @Transactional(readOnly = true)
    public WeeklyReport weekly(UUID userId, LocalDate weekStart) {
        LocalDate start = weekStart == null ? snapshotService.lastWeekStart() : weekStart;
        if (start.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new LearningRequestException(400, "INVALID_WEEK_START", "weekStart must be a Monday");
        }
        Optional<Snapshot> current = snapshots.find(Period.WEEK, userId, start);
        Optional<Snapshot> previous = snapshots.find(Period.WEEK, userId, start.minusWeeks(1));
        return new WeeklyReport(start, current.map(GetSnapshotReportsUseCase::view).orElse(null),
                current.map(snapshot -> changes(snapshot, previous.orElse(null))).orElse(List.of()),
                suggestions.execute(userId));
    }

    @Transactional(readOnly = true)
    public History history(UUID userId, int weeks) {
        if (weeks < MIN_HISTORY_WEEKS || weeks > MAX_HISTORY_WEEKS) {
            throw new LearningRequestException(400, "INVALID_WEEKS",
                    "weeks must be between " + MIN_HISTORY_WEEKS + " and " + MAX_HISTORY_WEEKS);
        }
        LocalDate last = snapshotService.lastWeekStart();
        List<HistoryPoint> points = snapshots.range(Period.WEEK, userId, last.minusWeeks(weeks - 1L), last).stream()
                .map(snapshot -> {
                    Map<String, Double> mastery = new LinkedHashMap<>();
                    for (SkillRollup.Summary skill : snapshot.skills()) mastery.put(skill.skill(), skill.mastery());
                    return new HistoryPoint(snapshot.start(), snapshot.estimatedBand(), accuracy(snapshot),
                            snapshot.activeMinutes(), mastery);
                }).toList();
        return new History(points);
    }

    /** {@code monthStart} is the first of a month; without one, the month before the current one. */
    @Transactional(readOnly = true)
    public MonthlyReport monthly(UUID userId, LocalDate monthStart) {
        LocalDate start = monthStart == null ? snapshotService.lastMonthStart() : monthStart;
        if (start.getDayOfMonth() != 1) {
            throw new LearningRequestException(400, "INVALID_MONTH_START", "monthStart must be the first of a month");
        }
        return new MonthlyReport(start, snapshots.find(Period.MONTH, userId, start)
                .map(GetSnapshotReportsUseCase::view).orElse(null));
    }

    static SnapshotView view(Snapshot snapshot) {
        return new SnapshotView(snapshot.skills(), snapshot.tierCounts(), snapshot.answered(), snapshot.correct(),
                accuracy(snapshot), snapshot.activeMinutes(), snapshot.estimatedBand(),
                snapshot.focusKnowledgePointIds());
    }

    private static Double accuracy(Snapshot snapshot) {
        return snapshot.answered() == 0 ? null : (double) snapshot.correct() / snapshot.answered();
    }

    private static List<SkillChange> changes(Snapshot current, Snapshot previous) {
        Map<String, Double> before = new LinkedHashMap<>();
        if (previous != null) for (SkillRollup.Summary skill : previous.skills()) before.put(skill.skill(), skill.mastery());
        List<SkillChange> changes = new ArrayList<>();
        for (SkillRollup.Summary skill : current.skills()) {
            Double now = skill.mastery();
            Double then = before.get(skill.skill());
            String direction;
            if (now == null) direction = "NONE";
            else if (previous == null || then == null) direction = "NEW";
            else if (Math.abs(now - then) < SAME_BELOW) direction = "SAME";
            else direction = now > then ? "UP" : "DOWN";
            changes.add(new SkillChange(skill.skill(), now, then, direction));
        }
        return changes;
    }
}
