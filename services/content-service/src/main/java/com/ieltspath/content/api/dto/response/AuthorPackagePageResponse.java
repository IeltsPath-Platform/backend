package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.usecase.ListAuthorPackagesUseCase;

import java.util.List;

public record AuthorPackagePageResponse(
        List<AuthorPackageSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AuthorPackagePageResponse from(ListAuthorPackagesUseCase.Page page) {
        return new AuthorPackagePageResponse(page.items().stream().map(AuthorPackageSummaryResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
