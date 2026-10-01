package com.group01.learningsupport.application.result;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> items, int page, int size, long total) {
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(items.stream().<R>map(mapper).toList(), page, size, total);
    }
}
