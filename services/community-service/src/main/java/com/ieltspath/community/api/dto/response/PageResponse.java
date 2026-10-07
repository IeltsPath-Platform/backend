package com.ieltspath.community.api.dto.response;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, String sort) {
    public PageResponse {
        content = List.copyOf(content);
    }
}
