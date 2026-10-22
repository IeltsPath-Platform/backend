package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.result.ContentOverviewResult;
import com.ieltspath.content.application.usecase.GetContentOverviewUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content/admin/overview")
@PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'EXAMINER')")
public class AdminOverviewController {
    private final GetContentOverviewUseCase overview;

    public AdminOverviewController(GetContentOverviewUseCase overview) {
        this.overview = overview;
    }

    @GetMapping
    public ContentOverviewResult get() {
        return overview.execute();
    }
}
