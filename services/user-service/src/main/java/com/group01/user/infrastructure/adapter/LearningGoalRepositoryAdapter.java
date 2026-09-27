package com.group01.user.infrastructure.adapter;

import com.group01.user.domain.aggregate.LearningGoal;
import com.group01.user.domain.exception.LearningGoalInvariantViolationException;
import com.group01.user.domain.repository.LearningGoalRepository;
import com.group01.user.domain.vo.GoalStatus;
import com.group01.user.infrastructure.persistence.entity.LearningGoalJpaEntity;
import com.group01.user.infrastructure.persistence.mapper.LearningGoalMapper;
import com.group01.user.infrastructure.persistence.repository.LearningGoalJpaRepository;
import com.group01.user.infrastructure.persistence.repository.UserJpaRepository;
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
    private final UserJpaRepository userJpaRepository;

    @Override
    public LearningGoal save(LearningGoal goal) {
        LearningGoalJpaEntity entity = learningGoalMapper.toEntity(goal);
        if (entity.getUser() == null && goal.getUserId() != null) {
            entity.setUser(userJpaRepository.getReferenceById(goal.getUserId()));
        }
        return learningGoalMapper.toDomain(learningGoalJpaRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<LearningGoal> findById(UUID id) {
        return learningGoalJpaRepository.findById(id).map(learningGoalMapper::toDomain);
    }

    @Override
    public List<LearningGoal> findByUserId(UUID userId) {
        return learningGoalJpaRepository.findByUser_Id(userId).stream()
                .map(learningGoalMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<LearningGoal> findActiveByUserId(UUID userId) {
        List<LearningGoalJpaEntity> activeGoals = learningGoalJpaRepository
                .findAllByUser_IdAndStatusOrderByCreatedAtDescIdDesc(userId, GoalStatus.ACTIVE);
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

