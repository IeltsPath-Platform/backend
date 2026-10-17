package com.ieltspath.access.api.dto.response;

import com.ieltspath.access.application.result.PointCreditResult;

import java.util.UUID;

public record CreditPointsResponse(int granted, boolean capped, long balance, UUID transactionId) {
    public static CreditPointsResponse from(PointCreditResult result) {
        return new CreditPointsResponse(result.granted(), result.capped(), result.balance(), result.transactionId());
    }
}
