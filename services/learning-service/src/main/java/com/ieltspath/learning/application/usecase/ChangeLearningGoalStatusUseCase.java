package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.ChangeLearningGoalStatusCommand;
import com.ieltspath.learning.application.result.LearningGoalResult;
import com.ieltspath.learning.domain.aggregate.LearningGoal;
import com.ieltspath.learning.domain.exception.LearningGoalNotFoundException;
import com.ieltspath.learning.domain.exception.LearningGoalConflictException;
import com.ieltspath.learning.domain.repository.LearningGoalRepository;
import com.ieltspath.learning.domain.vo.GoalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
public class ChangeLearningGoalStatusUseCase {
    private final LearningGoalRepository learningGoalRepository;
    private final ScheduleStudyGoalUseCase studyGoals;

    @Transactional
    public LearningGoalResult execute(ChangeLearningGoalStatusCommand command) {
        if (command.userId() != null) studyGoals.lock(command.userId());
        else studyGoals.lockGoalOwner(command.goalId());
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
            studyGoals.execute(learningGoalRepository.findActiveByUserId(goal.getUserId()).orElse(saved));
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

