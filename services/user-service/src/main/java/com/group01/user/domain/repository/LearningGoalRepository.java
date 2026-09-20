package com.group01.user.domain.repository;

import com.group01.user.domain.aggregate.LearningGoal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningGoalRepository {
    LearningGoal save(LearningGoal goal);
    Optional<LearningGoal> findById(UUID id);
    List<LearningGoal> findByUserId(UUID userId);
    Optional<LearningGoal> findActiveByUserId(UUID userId);
    void deleteById(UUID id);
}

