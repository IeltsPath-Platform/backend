package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.repository.ActivityRepository;
import com.ieltspath.learning.domain.vo.ActivityCounts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Activity for admins: daily, weekly and monthly active learners, and week-by-week retention of each weekly cohort. A
 * retention ratio is left empty while its week has not ended yet, so it is never shown as a false zero.
 */
@Service
public class GetActivityStatsUseCase {
    /** The longest range a single request may ask for, in days, both ends included. */
    public static final int MAX_DAYS = 180;

    private final ActivityRepository activity;
    private final Clock clock = Clock.systemUTC();

    public GetActivityStatsUseCase(ActivityRepository activity) {
        this.activity = activity;
    }

    public record Result(LocalDate from, LocalDate to, List<Day> daily, List<Cohort> retention) {}

    public record Day(LocalDate day, long dau, long wau, long mau) {}

    /** Retention ratios 0 to 1 for weeks 1, 2, 4 and 8; null while that week has not ended. */
    public record Cohort(Instant weekStart, long size, Double week1, Double week2, Double week4, Double week8) {}

    @Transactional(readOnly = true)
    public Result execute(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new LearningRequestException(400, "INVALID_RANGE", "to must not be before from");
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new LearningRequestException(400, "RANGE_TOO_LONG", "A range may cover at most 180 days");
        }
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now = clock.instant();
        List<Day> daily = activity.daily(start, end).stream()
                .map(day -> new Day(day.day(), day.dau(), day.wau(), day.mau())).toList();
        List<Cohort> retention = activity.cohorts(start, end).stream()
                .map(cohort -> cohortView(cohort, now)).toList();
        return new Result(from, to, daily, retention);
    }

    private static Cohort cohortView(ActivityCounts.Cohort cohort, Instant now) {
        return new Cohort(cohort.weekStart(), cohort.size(), ratio(cohort, cohort.week1(), 1, now),
                ratio(cohort, cohort.week2(), 2, now), ratio(cohort, cohort.week4(), 4, now),
                ratio(cohort, cohort.week8(), 8, now));
    }

    /** The share of the cohort active in week {@code weeks}, or null while that week is still running. */
    private static Double ratio(ActivityCounts.Cohort cohort, long active, int weeks, Instant now) {
        Instant weekEnd = cohort.weekStart().plus(7L * (weeks + 1), ChronoUnit.DAYS);
        if (weekEnd.isAfter(now) || cohort.size() == 0) return null;
        return (double) active / cohort.size();
    }
}
