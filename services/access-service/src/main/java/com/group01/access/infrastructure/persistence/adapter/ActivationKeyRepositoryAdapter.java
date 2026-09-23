package com.group01.access.infrastructure.persistence.adapter;

import com.group01.access.domain.aggregate.ActivationKey;
import com.group01.access.domain.repository.ActivationKeyRepository;
import com.group01.access.domain.vo.KeyStatus;
import com.group01.access.infrastructure.persistence.entity.ActivationKeyJpaEntity;
import com.group01.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.group01.access.infrastructure.persistence.repository.ActivationKeyJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ActivationKeyRepositoryAdapter implements ActivationKeyRepository {

    private final ActivationKeyJpaRepository activationKeyJpaRepository;
    private final AccessPersistenceMapper mapper;

    public ActivationKeyRepositoryAdapter(ActivationKeyJpaRepository activationKeyJpaRepository, AccessPersistenceMapper mapper) {
        this.activationKeyJpaRepository = activationKeyJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ActivationKey> findById(UUID id) {
        return activationKeyJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ActivationKey> findByCodeHash(String codeHash) {
        return activationKeyJpaRepository.findByCodeHash(codeHash).map(mapper::toDomain);
    }

    @Override
    public List<ActivationKey> findAll(int offset, int limit) {
        int page = offset / Math.max(limit, 1);
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return activationKeyJpaRepository.findAll(pageable).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ActivationKey> findByStatus(KeyStatus status, int offset, int limit) {
        int page = offset / Math.max(limit, 1);
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return activationKeyJpaRepository.findByStatus(status, pageable).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ActivationKey> findByProductId(UUID productId, int offset, int limit) {
        int page = offset / Math.max(limit, 1);
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return activationKeyJpaRepository.findByProductId(productId, pageable).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ActivationKey save(ActivationKey activationKey) {
        ActivationKeyJpaEntity entity = mapper.toEntity(activationKey);
        ActivationKeyJpaEntity saved = activationKeyJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ActivationKey> saveAll(List<ActivationKey> activationKeys) {
        List<ActivationKeyJpaEntity> entities = activationKeys.stream().map(mapper::toEntity).toList();
        List<ActivationKeyJpaEntity> saved = activationKeyJpaRepository.saveAll(entities);
        return saved.stream().map(mapper::toDomain).toList();
    }
}
