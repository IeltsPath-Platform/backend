package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.AttemptResponseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttemptResponseJpaRepository extends JpaRepository<AttemptResponseJpaEntity, UUID> {
    Optional<AttemptResponseJpaEntity> findByAttemptItemId(UUID attemptItemId);
}
