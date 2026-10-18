package com.ieltspath.user.domain.vo;

import java.math.BigDecimal;
import java.util.UUID;

/** Read model containing only the data needed for the community profile. */
public record PublicProfile(UUID id, String displayName, String defaultDisplayName, String avatarReference,
                            String bio, BigDecimal targetBand, Integer streak, ProfileVisibility visibility) {
    public boolean visibleTo(UUID viewerId) {
        return visibility == null || visibility == ProfileVisibility.PUBLIC
                || visibility == ProfileVisibility.COMMUNITY || id.equals(viewerId);
    }

    public String resolvedDisplayName() {
        if (displayName != null && !displayName.isBlank()) return displayName;
        return defaultDisplayName == null || defaultDisplayName.isBlank() ? "Learner" : defaultDisplayName;
    }
}
