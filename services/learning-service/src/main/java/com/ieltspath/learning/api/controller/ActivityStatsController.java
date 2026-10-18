package com.ieltspath.learning.api.controller;

import com.ieltspath.learning.application.usecase.GetActivityStatsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Learner activity and retention for admins. Counts are of learners who answered; logins live in user-service. */
@RestController
@RequestMapping("/api/learning/admin/activity-stats")
@PreAuthorize("hasRole('ADMIN')")
public class ActivityStatsController {
    private final GetActivityStatsUseCase stats;

    public ActivityStatsController(GetActivityStatsUseCase stats) {
        this.stats = stats;
    }

    @GetMapping
    public GetActivityStatsUseCase.Result stats(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return stats.execute(from, to);
    }
}
