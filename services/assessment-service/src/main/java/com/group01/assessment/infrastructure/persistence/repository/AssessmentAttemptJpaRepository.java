package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.AssessmentAttemptJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentAttemptJpaRepository extends JpaRepository<AssessmentAttemptJpaEntity, UUID> {
    Optional<AssessmentAttemptJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    List<AssessmentAttemptJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
