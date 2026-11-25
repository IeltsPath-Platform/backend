package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.port.PackageAttemptStatsReader;
import com.ieltspath.assessment.application.usecase.GetPackageAttemptStatsUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Attempt counts and average band per test, behind the author screens of the practice library and placement tests. */
@RestController
@RequestMapping("/api/assessments/admin")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class PackageStatsController {
    private final GetPackageAttemptStatsUseCase stats;

    public PackageStatsController(GetPackageAttemptStatsUseCase stats) {
        this.stats = stats;
    }

    @GetMapping("/package-stats")
    public List<PackageAttemptStatsReader.PackageStats> packageStats(
            @RequestParam(name = "packageIds", required = false) List<UUID> packageIds) {
        return stats.execute(packageIds);
    }
}
