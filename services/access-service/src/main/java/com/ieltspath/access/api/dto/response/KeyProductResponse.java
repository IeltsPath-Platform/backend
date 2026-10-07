package com.ieltspath.access.api.dto.response;

import com.ieltspath.access.application.result.KeyProductResult;
import com.ieltspath.access.domain.vo.KeyType;
import com.ieltspath.access.domain.vo.PlanStatus;

import java.time.Instant;
import java.util.UUID;

public record KeyProductResponse(
        UUID id,
        String code,
        String name,
        KeyType keyType,
        Integer pointsAmount,
        UUID planId,
        Integer premiumDays,
        Integer humanGradingCredits,
        PlanStatus status,
        Instant createdAt,
        Instant updatedAt
        ) {

    public static KeyProductResponse from(KeyProductResult r) {
        return new KeyProductResponse(
                r.id(),
                r.code(),
                r.name(),
                r.keyType(),
                r.pointsAmount(),
                r.planId(),
                r.premiumDays(),
                r.humanGradingCredits(),
                r.status(),
                r.createdAt(),
                r.updatedAt()
        );
    }
}
