package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.usecase.ListQuestionsUseCase;

import java.util.List;

public record QuestionPageResponse(
        List<QuestionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static QuestionPageResponse from(ListQuestionsUseCase.Page page) {
        return new QuestionPageResponse(page.items().stream().map(QuestionResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
