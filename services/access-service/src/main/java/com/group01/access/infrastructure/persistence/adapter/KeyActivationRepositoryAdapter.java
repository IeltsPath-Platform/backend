package com.group01.access.infrastructure.persistence.adapter;

import com.group01.access.domain.entity.KeyActivation;
import com.group01.access.domain.repository.KeyActivationRepository;
import com.group01.access.infrastructure.persistence.entity.KeyActivationJpaEntity;
import com.group01.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.group01.access.infrastructure.persistence.repository.KeyActivationJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KeyActivationRepositoryAdapter implements KeyActivationRepository {

    private final KeyActivationJpaRepository keyActivationJpaRepository;
    private final AccessPersistenceMapper mapper;

    public KeyActivationRepositoryAdapter(KeyActivationJpaRepository keyActivationJpaRepository, AccessPersistenceMapper mapper) {
        this.keyActivationJpaRepository = keyActivationJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<KeyActivation> findById(UUID id) {
        return keyActivationJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<KeyActivation> findByIdempotencyKey(String idempotencyKey) {
        return keyActivationJpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Optional<KeyActivation> findByKeyId(UUID keyId) {
        return keyActivationJpaRepository.findByKeyId(keyId).map(mapper::toDomain);
    }

    @Override
    public List<KeyActivation> findByUserId(UUID userId) {
        return keyActivationJpaRepository.findByUserIdOrderByActivatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public KeyActivation save(KeyActivation keyActivation) {
        KeyActivationJpaEntity entity = mapper.toEntity(keyActivation);
        KeyActivationJpaEntity saved = keyActivationJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
