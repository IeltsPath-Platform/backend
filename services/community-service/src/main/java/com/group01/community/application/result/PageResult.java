package com.group01.community.application.result;

import com.group01.community.domain.vo.CommunityPage;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public PageResult {
        content = List.copyOf(content);
    }

    public static <S, T> PageResult<T> from(
            CommunityPage<S> page,
            int pageNumber,
            int pageSize,
            Function<S, T> mapper
    ) {
        int totalPages = page.totalElements() == 0
                ? 0
                : (int) ((page.totalElements() + pageSize - 1) / pageSize);
        return new PageResult<>(
                page.items().stream().map(mapper).toList(),
                pageNumber,
                pageSize,
                page.totalElements(),
                totalPages
        );
    }
}
