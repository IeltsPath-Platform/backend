package com.ieltspath.learning.infrastructure.adapter;

import com.ieltspath.learning.domain.aggregate.LearningGoal;
import com.ieltspath.learning.domain.exception.LearningGoalInvariantViolationException;
import com.ieltspath.learning.domain.repository.LearningGoalRepository;
import com.ieltspath.learning.domain.vo.GoalStatus;
import com.ieltspath.learning.infrastructure.persistence.entity.LearningGoalJpaEntity;
import com.ieltspath.learning.infrastructure.persistence.mapper.LearningGoalMapper;
import com.ieltspath.learning.infrastructure.persistence.repository.LearningGoalJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LearningGoalRepositoryAdapter implements LearningGoalRepository {
    private final LearningGoalJpaRepository learningGoalJpaRepository;
    private final LearningGoalMapper learningGoalMapper;

    @Override
    public LearningGoal save(LearningGoal goal) {
        LearningGoalJpaEntity entity = learningGoalMapper.toEntity(goal);
        return learningGoalMapper.toDomain(learningGoalJpaRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<LearningGoal> findById(UUID id) {
        return learningGoalJpaRepository.findById(id).map(learningGoalMapper::toDomain);
    }

    @Override
    public List<LearningGoal> findByUserId(UUID userId) {
        return learningGoalJpaRepository.findByUserId(userId).stream()
                .map(learningGoalMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<LearningGoal> findActiveByUserId(UUID userId) {
        List<LearningGoalJpaEntity> activeGoals = learningGoalJpaRepository
                .findAllByUserIdAndStatusOrderByCreatedAtDescIdDesc(userId, GoalStatus.ACTIVE);
        if (activeGoals.size() > 1) {
            throw new LearningGoalInvariantViolationException(
                    "Multiple active learning goals found for user " + userId
            );
        }
        return activeGoals.stream().findFirst().map(learningGoalMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        learningGoalJpaRepository.deleteById(id);
    }
}

