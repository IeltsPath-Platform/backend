package com.group01.access.infrastructure.persistence.repository;

import com.group01.access.infrastructure.persistence.entity.PlanJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {

    @EntityGraph(attributePaths = "features")
    Optional<PlanJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = "features")
    Optional<PlanJpaEntity> findByCode(String code);

    @EntityGraph(attributePaths = "features")
    List<PlanJpaEntity> findAll();
}
