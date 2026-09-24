package com.group01.learningsupport.application.result;

import com.group01.learningsupport.domain.aggregate.Streak;

import java.time.LocalDate;
import java.util.UUID;

public record StreakResult(UUID userId, int currentDays, int longestDays, LocalDate lastQualifiedDate,
                           String timezone) {
    public static StreakResult from(Streak streak) {
        return new StreakResult(streak.getUserId(), streak.getCurrentDays(), streak.getLongestDays(),
                streak.getLastQualifiedDate(), streak.getTimezone());
    }
}
