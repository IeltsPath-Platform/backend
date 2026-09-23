package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.AttemptSectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttemptSectionJpaRepository extends JpaRepository<AttemptSectionJpaEntity, UUID> {
    List<AttemptSectionJpaEntity> findByAttemptIdOrderBySortOrderAsc(UUID attemptId);
    Optional<AttemptSectionJpaEntity> findById(UUID id);
}
