package com.ieltspath.access.infrastructure.persistence.adapter;

import com.ieltspath.access.domain.aggregate.PointWallet;
import com.ieltspath.access.domain.repository.PointWalletRepository;
import com.ieltspath.access.infrastructure.persistence.entity.PointWalletJpaEntity;
import com.ieltspath.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.ieltspath.access.infrastructure.persistence.repository.PointWalletJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PointWalletRepositoryAdapter implements PointWalletRepository {

    private final PointWalletJpaRepository pointWalletJpaRepository;
    private final AccessPersistenceMapper mapper;

    public PointWalletRepositoryAdapter(PointWalletJpaRepository pointWalletJpaRepository, AccessPersistenceMapper mapper) {
        this.pointWalletJpaRepository = pointWalletJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<PointWallet> findByUserId(UUID userId) {
        return pointWalletJpaRepository.findById(userId).map(mapper::toDomain);
    }

    @Override
    public PointWallet save(PointWallet pointWallet) {
        PointWalletJpaEntity entity = mapper.toEntity(pointWallet);
        PointWalletJpaEntity saved = pointWalletJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
