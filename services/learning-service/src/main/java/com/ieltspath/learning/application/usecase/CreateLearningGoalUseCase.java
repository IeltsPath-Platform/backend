package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.CreateLearningGoalCommand;
import com.ieltspath.learning.application.result.LearningGoalResult;
import com.ieltspath.learning.domain.aggregate.LearningGoal;
import com.ieltspath.learning.domain.exception.LearningGoalConflictException;
import com.ieltspath.learning.domain.repository.LearningGoalRepository;
import com.ieltspath.learning.domain.vo.GoalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateLearningGoalUseCase {
    private final LearningGoalRepository learningGoalRepository;
    private final ScheduleStudyGoalUseCase studyGoals;

    @Transactional
    public LearningGoalResult execute(CreateLearningGoalCommand command) {
        studyGoals.lock(command.userId());

        // Tự động tạm dừng mục tiêu đang active hiện tại (nếu có)
        try {
            // Existing product behavior atomically pauses the current goal before activating its replacement.
            learningGoalRepository.findActiveByUserId(command.userId())
                    .ifPresent(existingActiveGoal -> {
                        existingActiveGoal.pause();
                        learningGoalRepository.save(existingActiveGoal);
                    });

            // No id: the JPA entity generates it. A preassigned id makes Spring Data merge an entity
            // that does not exist yet, which Hibernate rejects as a stale update.
            LearningGoal newGoal = LearningGoal.builder()
                    .userId(command.userId())
                    .targetBand(command.targetBand())
                    .examDate(command.examDate())
                    .availableMinutesPerDay(command.availableMinutesPerDay())
                    .status(GoalStatus.ACTIVE)
                    .startedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            newGoal.updateGoal(command.targetBand(), command.examDate(), command.availableMinutesPerDay());

            LearningGoal saved = learningGoalRepository.save(newGoal);
            studyGoals.execute(saved);
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
