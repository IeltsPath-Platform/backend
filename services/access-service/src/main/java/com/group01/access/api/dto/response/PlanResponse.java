package com.group01.access.api.dto.response;

import com.group01.access.application.result.PlanFeatureResult;
import com.group01.access.application.result.PlanResult;
import com.group01.access.domain.vo.PlanStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String code,
        String name,
        PlanStatus status,
        List<PlanFeatureResponse> features,
        Instant createdAt,
        Instant updatedAt
        ) {

    public static PlanResponse from(PlanResult r) {
        List<PlanFeatureResponse> features = r.features() != null
                ? r.features().stream().map(PlanFeatureResponse::from).toList()
                : List.of();
        return new PlanResponse(r.id(), r.code(), r.name(), r.status(), features, r.createdAt(), r.updatedAt());
    }
}
