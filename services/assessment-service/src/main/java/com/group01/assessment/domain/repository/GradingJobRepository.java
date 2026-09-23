package com.group01.assessment.domain.repository;

import com.group01.assessment.domain.entity.GradingJob;

import java.util.Optional;
import java.util.UUID;

public interface GradingJobRepository {
    GradingJob save(GradingJob job);
    Optional<GradingJob> findById(UUID id);
}
