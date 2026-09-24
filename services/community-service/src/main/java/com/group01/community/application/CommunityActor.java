package com.group01.community.application;

import java.util.UUID;

/**
 * Authenticated capabilities mapped from the verified JWT at the API boundary.
 */
public record CommunityActor(UUID userId, boolean administrator) {
    public CommunityActor {
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated user id is required");
        }
    }
}
