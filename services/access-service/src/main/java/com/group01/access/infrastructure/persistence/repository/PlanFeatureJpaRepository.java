package com.group01.access.infrastructure.persistence.repository;

import com.group01.access.infrastructure.persistence.entity.PlanFeatureJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanFeatureJpaRepository extends JpaRepository<PlanFeatureJpaEntity, UUID> {

    List<PlanFeatureJpaEntity> findByPlanId(UUID planId);
}
