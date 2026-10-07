package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> items, int page, int size, long total) {
    public PageResponse {
        items = List.copyOf(items);
    }

    public static <S, T> PageResponse<T> from(PageResult<S> page, Function<? super S, ? extends T> mapper) {
        return new PageResponse<>(page.items().stream().<T>map(mapper).toList(), page.page(), page.size(), page.total());
    }
}
