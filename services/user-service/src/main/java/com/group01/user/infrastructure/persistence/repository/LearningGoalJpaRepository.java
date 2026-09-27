package com.group01.user.infrastructure.persistence.repository;

import com.group01.user.domain.vo.GoalStatus;
import com.group01.user.infrastructure.persistence.entity.LearningGoalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningGoalJpaRepository extends JpaRepository<LearningGoalJpaEntity, UUID> {
    List<LearningGoalJpaEntity> findByUser_Id(UUID userId);
    List<LearningGoalJpaEntity> findAllByUser_IdAndStatusOrderByCreatedAtDescIdDesc(UUID userId, GoalStatus status);
}

