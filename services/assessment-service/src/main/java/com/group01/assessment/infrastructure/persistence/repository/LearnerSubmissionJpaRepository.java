package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.LearnerSubmissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerSubmissionJpaRepository extends JpaRepository<LearnerSubmissionJpaEntity, UUID> {
    Optional<LearnerSubmissionJpaEntity> findByUserIdAndSubmissionKey(UUID userId, String submissionKey);
}
