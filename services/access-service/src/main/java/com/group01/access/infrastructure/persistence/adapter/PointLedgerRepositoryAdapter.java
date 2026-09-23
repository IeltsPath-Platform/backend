package com.group01.access.infrastructure.persistence.adapter;

import com.group01.access.domain.entity.PointLedgerEntry;
import com.group01.access.domain.repository.PointLedgerRepository;
import com.group01.access.infrastructure.persistence.entity.PointLedgerEntryJpaEntity;
import com.group01.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.group01.access.infrastructure.persistence.repository.PointLedgerJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PointLedgerRepositoryAdapter implements PointLedgerRepository {

    private final PointLedgerJpaRepository pointLedgerJpaRepository;
    private final AccessPersistenceMapper mapper;

    public PointLedgerRepositoryAdapter(PointLedgerJpaRepository pointLedgerJpaRepository, AccessPersistenceMapper mapper) {
        this.pointLedgerJpaRepository = pointLedgerJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<PointLedgerEntry> findByIdempotencyKey(String idempotencyKey) {
        return pointLedgerJpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<PointLedgerEntry> findByUserId(UUID userId, int offset, int limit) {
        int page = offset / Math.max(limit, 1);
        Pageable pageable = PageRequest.of(page, limit);
        return pointLedgerJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(UUID userId) {
        return pointLedgerJpaRepository.countByUserId(userId);
    }

    @Override
    public PointLedgerEntry save(PointLedgerEntry entry) {
        PointLedgerEntryJpaEntity entity = mapper.toEntity(entry);
        PointLedgerEntryJpaEntity saved = pointLedgerJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
