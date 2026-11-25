package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.usecase.PublishSectionDraftQuestionsUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
@RequestMapping("/api/content/admin/packages/{packageId}/versions/{versionId}/sections/{sectionId}/questions/publish-drafts")
public class PublishSectionDraftQuestionsController {
    private final PublishSectionDraftQuestionsUseCase publish;
    public PublishSectionDraftQuestionsController(PublishSectionDraftQuestionsUseCase publish) { this.publish = publish; }
    @PostMapping
    public Result execute(@PathVariable UUID packageId, @PathVariable UUID versionId, @PathVariable UUID sectionId) {
        return new Result(publish.execute(packageId, versionId, sectionId));
    }
    public record Result(int publishedCount) {}
}
