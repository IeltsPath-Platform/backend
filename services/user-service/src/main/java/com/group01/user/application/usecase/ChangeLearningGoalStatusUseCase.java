package com.group01.user.application.usecase;

import com.group01.user.application.command.ChangeLearningGoalStatusCommand;
import com.group01.user.application.result.LearningGoalResult;
import com.group01.user.domain.aggregate.LearningGoal;
import com.group01.user.domain.exception.LearningGoalNotFoundException;
import com.group01.user.domain.exception.LearningGoalConflictException;
import com.group01.user.domain.repository.LearningGoalRepository;
import com.group01.user.domain.vo.GoalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
public class ChangeLearningGoalStatusUseCase {
    private final LearningGoalRepository learningGoalRepository;

    @Transactional
    public LearningGoalResult execute(ChangeLearningGoalStatusCommand command) {
        LearningGoal goal = learningGoalRepository.findById(command.goalId())
                .orElseThrow(() -> new LearningGoalNotFoundException("Không tìm thấy mục tiêu học tập với id: " + command.goalId()));

        if (command.userId() != null && !goal.getUserId().equals(command.userId())) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa mục tiêu học tập này");
        }

        if (command.status() == null || command.status().isBlank()) {
            throw new IllegalArgumentException("Trạng thái mục tiêu không được để trống");
        }

        GoalStatus targetStatus;
        try {
            targetStatus = GoalStatus.valueOf(command.status().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + command.status());
        }

        try {
            switch (targetStatus) {
                case ACHIEVED -> goal.complete();
                case ABANDONED -> goal.abandon();
                case PAUSED -> goal.pause();
                case ACTIVE -> {
                    learningGoalRepository.findActiveByUserId(goal.getUserId())
                            .filter(other -> !other.getId().equals(goal.getId()))
                            .ifPresent(other -> {
                                other.pause();
                                learningGoalRepository.save(other);
                            });
                    goal.resume();
                }
            }

            LearningGoal saved = learningGoalRepository.save(goal);
            return toResult(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new LearningGoalConflictException("An active learning goal already exists for this user");
        }
    }

    private LearningGoalResult toResult(LearningGoal goal) {
        return new LearningGoalResult(
                goal.getId(),
                goal.getUserId(),
                goal.getTargetBand(),
                goal.getExamDate(),
                goal.getAvailableMinutesPerDay(),
                goal.getStatus().name(),
                goal.getStartedAt(),
                goal.getEndedAt(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}

