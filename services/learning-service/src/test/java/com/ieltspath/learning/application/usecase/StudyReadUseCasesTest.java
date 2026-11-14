package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.query.PageQuery;
import com.ieltspath.learning.application.service.LearnerTimezones;
import com.ieltspath.learning.domain.aggregate.LearningActivity;
import com.ieltspath.learning.domain.exception.InvalidDataException;
import com.ieltspath.learning.domain.repository.ActivityRepository;
import com.ieltspath.learning.domain.repository.LearningActivityRepository;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.service.StreakRules;
import com.ieltspath.learning.domain.vo.ActivityCounts;
import com.ieltspath.learning.domain.vo.OwnedPage;
import com.ieltspath.learning.domain.vo.StreakState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Reads of study data: streaks of many learners, admin activity statistics and a learner's activity list. */
class StudyReadUseCasesTest {

    // ---- streaks of many learners ------------------------------------------------------------------------------

    private static StreakState streak(int days, LocalDate lastDay) {
        StreakState state = StreakState.empty();
        for (int i = days - 1; i >= 0; i--) state = StreakRules.recordAnswer(state, lastDay.minusDays(i)).state();
        return state;
    }

    @Test
    void streaksOfManyLearnersAreReadInTheirOwnDayAndLearnersWithoutAStreakAreLeftOut() {
        StreakRepository streaks = mock(StreakRepository.class);
        LearnerTimezones timezones = mock(LearnerTimezones.class);
        UUID active = UUID.randomUUID(), lapsed = UUID.randomUUID(), none = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(streaks.findAll(Set.of(active, lapsed, none))).thenReturn(Map.of(
                active, streak(4, today), lapsed, streak(6, today.minusDays(3))));
        when(timezones.zonesOf(Set.of(active, lapsed))).thenReturn(Map.of(active, ZoneOffset.UTC, lapsed, ZoneOffset.UTC));

        Map<UUID, Integer> result = new GetStreaksUseCase(streaks, timezones).execute(Set.of(active, lapsed, none));

        assertThat(result).containsOnly(Map.entry(active, 4), Map.entry(lapsed, 0));
        verify(streaks, never()).save(any(), any());
    }

    @Test
    void anEmptyListOfLearnersReadsNothing() {
        StreakRepository streaks = mock(StreakRepository.class);
        LearnerTimezones timezones = mock(LearnerTimezones.class);
        assertThat(new GetStreaksUseCase(streaks, timezones).execute(List.of())).isEmpty();
        verifyNoInteractions(streaks, timezones);
    }

    // ---- admin activity statistics -----------------------------------------------------------------------------

    private final ActivityRepository activity = mock(ActivityRepository.class);
    private final GetActivityStatsUseCase stats = new GetActivityStatsUseCase(activity);

    private static int status(Throwable error) { return ((LearningRequestException) error).getStatus(); }
    private static String code(Throwable error) { return ((LearningRequestException) error).getCode(); }

    @Test
    void aRangeEndingBeforeItStartsIsRefusedBeforeAnyQuery() {
        LocalDate day = LocalDate.parse("2026-10-15");
        assertThatThrownBy(() -> stats.execute(day, day.minusDays(1)))
                .satisfies(error -> {
                    assertThat(status(error)).isEqualTo(400);
                    assertThat(code(error)).isEqualTo("INVALID_RANGE");
                });
        verifyNoInteractions(activity);
    }

    @Test
    void aRangeMayCoverAtMost180Days() {
        LocalDate from = LocalDate.parse("2026-01-01");
        assertThat(stats.execute(from, from.plusDays(179)).from()).isEqualTo(from);
        assertThatThrownBy(() -> stats.execute(from, from.plusDays(180)))
                .satisfies(error -> {
                    assertThat(status(error)).isEqualTo(400);
                    assertThat(code(error)).isEqualTo("RANGE_TOO_LONG");
                });
    }

    @Test
    void retentionIsEmptyWhileItsWeekRunsAndForAnEmptyCohort() {
        Instant now = Instant.now();
        Instant lastWeek = now.minus(7, ChronoUnit.DAYS);
        Instant tenWeeksAgo = now.minus(70, ChronoUnit.DAYS);
        when(activity.cohorts(any(), any())).thenReturn(List.of(
                new ActivityCounts.Cohort(lastWeek, 10, 5, 0, 0, 0),
                new ActivityCounts.Cohort(tenWeeksAgo, 10, 5, 4, 2, 1),
                new ActivityCounts.Cohort(tenWeeksAgo, 0, 0, 0, 0, 0)));

        var result = stats.execute(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"));

        var running = result.retention().get(0);
        assertThat(running.week1()).isNull();
        assertThat(running.week8()).isNull();
        var ended = result.retention().get(1);
        assertThat(ended.week1()).isEqualTo(0.5);
        assertThat(ended.week2()).isEqualTo(0.4);
        assertThat(ended.week4()).isEqualTo(0.2);
        assertThat(ended.week8()).isEqualTo(0.1);
        var empty = result.retention().get(2);
        assertThat(empty.week1()).isNull();
    }

    // ---- a learner's activity list -----------------------------------------------------------------------------

    @Test
    void activitiesAreListedPageByPageForTheirOwner() {
        LearningActivityRepository repository = mock(LearningActivityRepository.class);
        UUID user = UUID.randomUUID();
        var item = LearningActivity.create(user, "WATCH", "VIDEO", UUID.randomUUID(), Instant.parse("2026-10-15T00:00:00Z"), 30);
        when(repository.findByUserId(user, 1, 20)).thenReturn(new OwnedPage<>(List.of(item), 21));

        var page = new ListLearningActivitiesUseCase(repository).execute(user, new PageQuery(1, 20));

        assertThat(page.items()).singleElement().satisfies(found -> assertThat(found.id()).isEqualTo(item.getId()));
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.total()).isEqualTo(21);
    }

    @Test
    void aPageSizeAboveOneHundredIsRefused() {
        assertThatThrownBy(() -> new PageQuery(0, 101))
                .isInstanceOf(InvalidDataException.class).hasMessage("Phân trang không hợp lệ");
        assertThatThrownBy(() -> new PageQuery(-1, 20)).isInstanceOf(InvalidDataException.class);
        assertThat(new PageQuery(0, 100).size()).isEqualTo(100);
    }
}
