package com.ieltspath.user.api.dto.response;

import com.ieltspath.user.application.result.UserDisplayNameResult;

import java.util.List;
import java.util.UUID;

public record UserDisplayNamesResponse(List<Item> items) {
    public record Item(UUID id, String displayName) {}

    public static UserDisplayNamesResponse from(List<UserDisplayNameResult> results) {
        return new UserDisplayNamesResponse(results.stream()
                .map(item -> new Item(item.id(), item.displayName())).toList());
    }
}
