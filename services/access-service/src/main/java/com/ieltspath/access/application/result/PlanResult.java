package com.ieltspath.access.application.result;

import com.ieltspath.access.domain.vo.PlanStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanResult(
        UUID id,
        String code,
        String name,
        PlanStatus status,
        List<PlanFeatureResult> features,
        Instant createdAt,
        Instant updatedAt
        ) {

}
