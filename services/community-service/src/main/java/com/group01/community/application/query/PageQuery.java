package com.group01.community.application.query;

import com.group01.community.domain.exception.CommunityException;

public record PageQuery(int page, int size) {
    public PageQuery {
        if (page < 0) {
            throw new CommunityException("Page must be non-negative");
        }
        if (size < 1 || size > 100) {
            throw new CommunityException("Size must be between 1 and 100");
        }
    }
}
