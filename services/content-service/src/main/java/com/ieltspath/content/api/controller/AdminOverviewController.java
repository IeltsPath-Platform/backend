package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.result.ContentOverviewResult;
import com.ieltspath.content.application.result.ContentOverviewResult.QuestionLabel;
import com.ieltspath.content.application.usecase.GetContentOverviewUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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

    /** The start of the stem of each question version asked for, so a list of statistics can name its questions. */
    @GetMapping("/question-labels")
    public List<QuestionLabel> questionLabels(@RequestParam("ids") List<UUID> ids) {
        return overview.questionLabels(ids);
    }
}