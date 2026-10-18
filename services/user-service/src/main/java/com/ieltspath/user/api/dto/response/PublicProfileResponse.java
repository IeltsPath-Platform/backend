package com.ieltspath.user.api.dto.response;

import com.ieltspath.user.application.result.PublicProfileResult;
import java.math.BigDecimal;
import java.util.UUID;

public record PublicProfileResponse(UUID id, String displayName, String avatarUrl, String bio,
                                    BigDecimal targetBand, int streak) {
    public static PublicProfileResponse from(PublicProfileResult result) {
        return new PublicProfileResponse(result.id(), result.displayName(), result.avatarUrl(), result.bio(),
                result.targetBand(), result.streak());
    }
}
