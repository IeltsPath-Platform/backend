package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.AssessmentResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentResultJpaRepository extends JpaRepository<AssessmentResultJpaEntity, UUID> {
    Optional<AssessmentResultJpaEntity> findTopByAttemptIdOrderByResultVersionDesc(UUID attemptId);
}
