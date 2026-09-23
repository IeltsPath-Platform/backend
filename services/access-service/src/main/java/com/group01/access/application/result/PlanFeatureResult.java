package com.group01.access.application.result;

import java.util.UUID;

public record PlanFeatureResult(
        UUID id,
        String featureKey,
        boolean enabled,
        Integer limitValue,
        String config
        ) {

}
