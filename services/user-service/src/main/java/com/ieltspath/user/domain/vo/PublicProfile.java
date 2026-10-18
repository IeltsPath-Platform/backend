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

    /**
     * A user without a profile row never chose a public name, so other viewers get a neutral label instead of
     * the account's real name; the owner still sees their own name.
     */
    public String resolvedDisplayName(UUID viewerId) {
        if (displayName != null && !displayName.isBlank()) return displayName;
        if (visibility == null && !id.equals(viewerId)) return "Learner";
        return defaultDisplayName == null || defaultDisplayName.isBlank() ? "Learner" : defaultDisplayName;
    }
}
