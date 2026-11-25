package com.ieltspath.user.api.dto.response;

import java.util.List;

public record UserPageResponse(List<UserResponse> content, int page, int size, long totalElements, int totalPages) {
    public UserPageResponse {
        content = List.copyOf(content);
    }
}
