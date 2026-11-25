package com.ieltspath.user.domain.result;

import com.ieltspath.user.domain.aggregate.User;

import java.util.List;

public record UserPage(List<User> content, int page, int size, long totalElements) {
    public UserPage {
        content = List.copyOf(content);
    }

    public int totalPages() {
        return totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
    }
}
