package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.LearningActivity;
import com.ieltspath.user.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface LearningActivityRepository {
    LearningActivity save(LearningActivity activity);

    OwnedPage<LearningActivity> findByUserId(UUID userId, int page, int size);

    Optional<LearningActivity> findByIdAndUserId(UUID id, UUID userId);

    void delete(LearningActivity activity);
}
