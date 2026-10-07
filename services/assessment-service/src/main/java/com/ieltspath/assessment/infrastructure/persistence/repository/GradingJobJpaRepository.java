package com.ieltspath.assessment.infrastructure.persistence.repository;

import com.ieltspath.assessment.infrastructure.persistence.entity.GradingJobJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface GradingJobJpaRepository extends JpaRepository<GradingJobJpaEntity, UUID> {}
