package com.group01.learningsupport.infrastructure.persistence.repository;

import com.group01.learningsupport.infrastructure.persistence.entity.StreakJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StreakJpaRepository extends JpaRepository<StreakJpaEntity, UUID> {
}
