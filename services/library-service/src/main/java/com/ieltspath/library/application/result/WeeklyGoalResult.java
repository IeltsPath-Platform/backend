package com.ieltspath.library.application.result;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;

public record WeeklyGoalResult(int target, long progress, long remaining, List<Day> days) {
    public record Day(LocalDate date, DayOfWeek weekday, boolean achieved) {}
}
