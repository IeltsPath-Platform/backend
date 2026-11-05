package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.service.ExaminerDashboardService;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The examiner's own numbers for the dashboard: queue by state, today's intake against the daily limit, finished work. */
@RestController
@RequestMapping("/api/assessments/examiners/me")
@PreAuthorize("hasRole('EXAMINER')")
public class ExaminerDashboardController {
    private final CurrentUserProvider currentUser;
    private final ExaminerDashboardService dashboard;

    public ExaminerDashboardController(CurrentUserProvider currentUser, ExaminerDashboardService dashboard) {
        this.currentUser = currentUser;
        this.dashboard = dashboard;
    }

    @GetMapping("/summary")
    public ExaminerDashboardService.Summary summary() {
        return dashboard.summary(currentUser.requireUserId());
    }
}
