package com.group01.access.infrastructure.persistence.adapter;

import com.group01.access.domain.aggregate.Plan;
import com.group01.access.domain.repository.PlanRepository;
import com.group01.access.infrastructure.persistence.entity.PlanJpaEntity;
import com.group01.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.group01.access.infrastructure.persistence.repository.PlanJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PlanRepositoryAdapter implements PlanRepository {

    private final PlanJpaRepository planJpaRepository;
    private final AccessPersistenceMapper mapper;

    public PlanRepositoryAdapter(PlanJpaRepository planJpaRepository, AccessPersistenceMapper mapper) {
        this.planJpaRepository = planJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Plan> findById(UUID id) {
        return planJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Plan> findByCode(String code) {
        return planJpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Plan> findAll() {
        return planJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Plan save(Plan plan) {
        PlanJpaEntity entity = mapper.toEntity(plan);
        PlanJpaEntity saved = planJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
