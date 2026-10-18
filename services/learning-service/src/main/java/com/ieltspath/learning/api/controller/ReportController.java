package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.LearnerReportResult;
import com.ieltspath.learning.application.result.SnapshotReportResult;
import com.ieltspath.learning.application.usecase.GetLearnerReportUseCase;
import com.ieltspath.learning.application.usecase.GetSnapshotReportsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
public class ReportController {
    private final CurrentUserProvider currentUser;
    private final GetLearnerReportUseCase report;
    private final GetSnapshotReportsUseCase snapshots;

    public ReportController(CurrentUserProvider currentUser, GetLearnerReportUseCase report,
                            GetSnapshotReportsUseCase snapshots) {
        this.currentUser = currentUser;
        this.report = report;
        this.snapshots = snapshots;
    }

    /** The learner's own week: the stored snapshot, the change per skill from the week before, and what to do next. */
    @GetMapping("/report/weekly")
    @PreAuthorize("hasRole('CUSTOMER')")
    public SnapshotReportResult.WeeklyReport weekly(
            @RequestParam(name = "weekStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart) {
        return snapshots.weekly(currentUser.requireUserId(), weekStart);
    }

    /** Mastery, accuracy and active minutes per week for the last 4 to 12 weeks. */
    @GetMapping("/report/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public SnapshotReportResult.History history(@RequestParam(name = "weeks", defaultValue = "4") int weeks) {
        return snapshots.history(currentUser.requireUserId(), weeks);
    }

    @GetMapping("/report/monthly")
    @PreAuthorize("hasRole('CUSTOMER')")
    public SnapshotReportResult.MonthlyReport monthly(
            @RequestParam(name = "monthStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate monthStart) {
        return snapshots.monthly(currentUser.requireUserId(), monthStart);
    }

    /** The learner's own report; the learner is the verified caller, never a request parameter. */
    @GetMapping("/report")
    @PreAuthorize("hasRole('CUSTOMER')")
    public LearnerReportResult mine() {
        return report.execute(currentUser.requireUserId());
    }

    @GetMapping("/admin/learners/{userId}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXAMINER')")
    public LearnerReportResult learner(@PathVariable("userId") UUID userId) {
        return report.execute(userId);
    }
}
