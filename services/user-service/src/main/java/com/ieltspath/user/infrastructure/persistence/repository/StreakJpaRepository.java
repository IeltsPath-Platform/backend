package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.infrastructure.persistence.entity.StreakJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StreakJpaRepository extends JpaRepository<StreakJpaEntity, UUID> {
}
