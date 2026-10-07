package com.ieltspath.access.domain.repository;

import com.ieltspath.access.domain.entity.PointLedgerEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointLedgerRepository {

    Optional<PointLedgerEntry> findByIdempotencyKey(String idempotencyKey);

    List<PointLedgerEntry> findByUserId(UUID userId, int offset, int limit);

    long countByUserId(UUID userId);

    PointLedgerEntry save(PointLedgerEntry entry);
}
