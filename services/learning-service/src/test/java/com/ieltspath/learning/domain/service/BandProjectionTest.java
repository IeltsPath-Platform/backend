package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.BandProjection.Projection;
import com.ieltspath.learning.domain.service.BandProjection.WeekBand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BandProjectionTest {
    private static final LocalDate WEEK1 = LocalDate.of(2026, 9, 7);
    private static final BigDecimal TARGET = new BigDecimal("7.5");

    private static WeekBand week(int index, String band) {
        return new WeekBand(WEEK1.plusWeeks(index), band == null ? null : new BigDecimal(band));
    }

    @Test
    void fourWeeksOfSteadyGainProjectTheDateTheTargetIsReached() {
        List<WeekBand> weeks = List.of(week(0, "5.0"), week(1, "5.5"), week(2, "6.0"), week(3, "6.5"));

        Projection projection = BandProjection.project(weeks, TARGET);

        assertThat(projection.reason()).isNull();
        assertThat(projection.date()).as("one half band a week: two more weeks to 7.5")
                .isEqualTo(WEEK1.plusWeeks(3).plusWeeks(2));
    }

    @Test
    void fewerThanFourWeeksIsNotEnoughData() {
        List<WeekBand> weeks = List.of(week(0, "5.0"), week(1, "5.5"), week(2, "6.0"));

        Projection projection = BandProjection.project(weeks, TARGET);

        assertThat(projection.date()).isNull();
        assertThat(projection.reason()).isEqualTo("NOT_ENOUGH_DATA");
    }

    @Test
    void weeksWithoutABandDoNotCount() {
        List<WeekBand> weeks = List.of(week(0, "5.0"), week(1, null), week(2, "6.0"), week(3, "6.5"));

        assertThat(BandProjection.project(weeks, TARGET).reason()).isEqualTo("NOT_ENOUGH_DATA");
    }

    @Test
    void aFallingOrFlatTrendHasNoProgress() {
        List<WeekBand> falling = List.of(week(0, "6.5"), week(1, "6.0"), week(2, "6.0"), week(3, "5.5"));
        List<WeekBand> flat = List.of(week(0, "6.0"), week(1, "6.0"), week(2, "6.0"), week(3, "6.0"));

        assertThat(BandProjection.project(falling, TARGET).reason()).isEqualTo("NO_PROGRESS");
        assertThat(BandProjection.project(flat, TARGET).reason()).isEqualTo("NO_PROGRESS");
    }

    @Test
    void onlyTheFourMostRecentWeeksAreFitted() {
        List<WeekBand> weeks = List.of(week(0, "8.0"), week(1, "5.0"), week(2, "5.5"), week(3, "6.0"), week(4, "6.5"));

        Projection projection = BandProjection.project(weeks, TARGET);

        assertThat(projection.reason()).isNull();
        assertThat(projection.date()).isEqualTo(WEEK1.plusWeeks(4).plusWeeks(2));
    }

    @Test
    void aTargetAlreadyReachedProjectsTheLatestWeek() {
        List<WeekBand> weeks = List.of(week(0, "6.5"), week(1, "7.0"), week(2, "7.5"), week(3, "8.0"));

        Projection projection = BandProjection.project(weeks, TARGET);

        assertThat(projection.date()).isEqualTo(WEEK1.plusWeeks(3));
    }
}
