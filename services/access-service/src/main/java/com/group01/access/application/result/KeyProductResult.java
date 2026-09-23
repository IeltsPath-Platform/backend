package com.group01.access.application.result;

import com.group01.access.domain.vo.KeyType;
import com.group01.access.domain.vo.PlanStatus;

import java.time.Instant;
import java.util.UUID;

public record KeyProductResult(
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

}
