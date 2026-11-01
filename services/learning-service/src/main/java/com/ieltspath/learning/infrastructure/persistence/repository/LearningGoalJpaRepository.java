package com.ieltspath.learning.infrastructure.persistence.repository;

import com.ieltspath.learning.domain.vo.GoalStatus;
import com.ieltspath.learning.infrastructure.persistence.entity.LearningGoalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningGoalJpaRepository extends JpaRepository<LearningGoalJpaEntity, UUID> {
    List<LearningGoalJpaEntity> findByUserId(UUID userId);
    List<LearningGoalJpaEntity> findAllByUserIdAndStatusOrderByCreatedAtDescIdDesc(UUID userId, GoalStatus status);
}

