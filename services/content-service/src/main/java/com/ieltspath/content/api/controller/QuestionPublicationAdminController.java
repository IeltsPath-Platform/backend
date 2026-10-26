package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.response.QuestionDetailResponse;
import com.ieltspath.content.application.usecase.ArchiveQuestionVersionUseCase;
import com.ieltspath.content.application.usecase.PublishQuestionVersionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/content/admin/questions/{questionId}/versions/{versionId}")
@RequiredArgsConstructor
public class QuestionPublicationAdminController {
    private final PublishQuestionVersionUseCase publishQuestionVersionUseCase;
    private final ArchiveQuestionVersionUseCase archiveQuestionVersionUseCase;

    @PostMapping("/publish")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public QuestionDetailResponse publish(@PathVariable("questionId") UUID questionId,
                                          @PathVariable("versionId") UUID versionId) {
        return QuestionDetailResponse.from(publishQuestionVersionUseCase.execute(questionId, versionId));
    }

    @PostMapping("/archive")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public QuestionDetailResponse archive(@PathVariable("questionId") UUID questionId,
                                          @PathVariable("versionId") UUID versionId) {
        return QuestionDetailResponse.from(archiveQuestionVersionUseCase.execute(questionId, versionId));
    }
}
