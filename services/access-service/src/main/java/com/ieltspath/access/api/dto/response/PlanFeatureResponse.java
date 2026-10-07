package com.ieltspath.access.api.dto.response;

import com.ieltspath.access.application.result.PlanFeatureResult;

import java.util.UUID;

public record PlanFeatureResponse(
        UUID id,
        String featureKey,
        boolean enabled,
        Integer limitValue,
        String config
        ) {

    public static PlanFeatureResponse from(PlanFeatureResult r) {
        return new PlanFeatureResponse(r.id(), r.featureKey(), r.enabled(), r.limitValue(), r.config());
    }
}
