package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.LearnerReportResult;
import com.ieltspath.learning.application.usecase.GetLearnerReportUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
public class ReportController {
    private final CurrentUserProvider currentUser;
    private final GetLearnerReportUseCase report;

    public ReportController(CurrentUserProvider currentUser, GetLearnerReportUseCase report) {
        this.currentUser = currentUser;
        this.report = report;
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
