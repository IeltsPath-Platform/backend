package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.LearningActivity;
import com.ieltspath.learning.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface LearningActivityRepository {
    LearningActivity save(LearningActivity activity);

    OwnedPage<LearningActivity> findByUserId(UUID userId, int page, int size);

    Optional<LearningActivity> findByIdAndUserId(UUID id, UUID userId);

    void delete(LearningActivity activity);
}
