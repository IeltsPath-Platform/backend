package com.ieltspath.learning.application.result;

import java.time.*;
import java.util.List;

public record StudyTimeStatisticsResult(String availability, Instant generatedAt, LocalDate dataFrom,
        String timezonePolicy,String scope,List<Day> days) {
    public record Day(LocalDate date,String timezone,Long actualSeconds,Long plannedSeconds,String status) {}
}
