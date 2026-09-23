package com.group01.access.infrastructure.persistence.adapter;

import com.group01.access.domain.aggregate.KeyProduct;
import com.group01.access.domain.repository.KeyProductRepository;
import com.group01.access.infrastructure.persistence.entity.KeyProductJpaEntity;
import com.group01.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.group01.access.infrastructure.persistence.repository.KeyProductJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KeyProductRepositoryAdapter implements KeyProductRepository {

    private final KeyProductJpaRepository keyProductJpaRepository;
    private final AccessPersistenceMapper mapper;

    public KeyProductRepositoryAdapter(KeyProductJpaRepository keyProductJpaRepository, AccessPersistenceMapper mapper) {
        this.keyProductJpaRepository = keyProductJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<KeyProduct> findById(UUID id) {
        return keyProductJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<KeyProduct> findByCode(String code) {
        return keyProductJpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<KeyProduct> findAll() {
        return keyProductJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public KeyProduct save(KeyProduct keyProduct) {
        KeyProductJpaEntity entity = mapper.toEntity(keyProduct);
        KeyProductJpaEntity saved = keyProductJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
