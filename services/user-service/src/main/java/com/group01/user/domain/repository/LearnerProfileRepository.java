package com.group01.user.domain.repository;

import com.group01.user.domain.aggregate.LearnerProfile;

import java.util.Optional;
import java.util.UUID;

public interface LearnerProfileRepository {
    LearnerProfile save(LearnerProfile profile);
    Optional<LearnerProfile> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}

