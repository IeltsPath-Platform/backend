package com.group01.content.api;

import com.group01.content.api.dto.AccessLevel;

/**
 * Translates the legacy FREE/PREMIUM API field to the resource's entitlement key.
 */
public final class AccessLevelCompatibility {

    private AccessLevelCompatibility() {
    }

    public static String toRequiredFeatureKey(AccessLevel accessLevel, String premiumFeatureKey) {
        return accessLevel == AccessLevel.PREMIUM ? premiumFeatureKey : null;
    }

    public static Boolean toFeatureRequiredFilter(AccessLevel accessLevel) {
        return accessLevel == null ? null : accessLevel == AccessLevel.PREMIUM;
    }

    public static AccessLevel toAccessLevel(String requiredFeatureKey) {
        return requiredFeatureKey == null ? AccessLevel.FREE : AccessLevel.PREMIUM;
    }
}
