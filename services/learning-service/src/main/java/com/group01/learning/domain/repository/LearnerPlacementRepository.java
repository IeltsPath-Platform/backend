package com.group01.learning.domain.repository;

import com.group01.learning.domain.aggregate.LearnerPlacement;

import java.util.Optional;
import java.util.UUID;

public interface LearnerPlacementRepository {
    Optional<LearnerPlacement> find(UUID userId);
    void save(LearnerPlacement placement);
}
