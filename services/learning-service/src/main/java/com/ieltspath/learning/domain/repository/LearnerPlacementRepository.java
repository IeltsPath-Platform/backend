package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.LearnerPlacement;

import java.util.Optional;
import java.util.UUID;

public interface LearnerPlacementRepository {
    Optional<LearnerPlacement> find(UUID userId);
    void save(LearnerPlacement placement);
}
