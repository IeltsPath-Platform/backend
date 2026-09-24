package com.group01.community.domain.vo;

import java.util.List;

/**
 * Repository page data without HTTP or Spring Data types.
 */
public record CommunityPage<T>(List<T> items, long totalElements) {
    public CommunityPage {
        items = List.copyOf(items);
    }
}
