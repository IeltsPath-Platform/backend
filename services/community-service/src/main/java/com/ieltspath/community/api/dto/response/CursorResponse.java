package com.ieltspath.community.api.dto.response;

import java.util.List;

public record CursorResponse<T>(List<T> content, String nextCursor, boolean hasNext, int size) {
    public CursorResponse {
        content = List.copyOf(content);
    }
}
