package com.ieltspath.access.domain.repository;

import com.ieltspath.access.domain.entity.PointCreditRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PointCreditRequestRepository {
    void lock(String idempotencyKey, UUID userId, LocalDate utcDate);

    Optional<PointCreditRequest> findByIdempotencyKey(String idempotencyKey);

    long sumGrantedSince(UUID userId, Instant startInclusive);

    PointCreditRequest save(PointCreditRequest request);
}
