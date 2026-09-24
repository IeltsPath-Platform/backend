package com.group01.learningsupport.api.dto.response;

import com.group01.learningsupport.application.result.StreakResult;

import java.time.LocalDate;
import java.util.UUID;

public record StreakResponse(UUID userId, int currentDays, int longestDays, LocalDate lastQualifiedDate,
                             String timezone) {
    public static StreakResponse from(StreakResult result) {
        return new StreakResponse(result.userId(), result.currentDays(), result.longestDays(),
                result.lastQualifiedDate(), result.timezone());
    }
}
