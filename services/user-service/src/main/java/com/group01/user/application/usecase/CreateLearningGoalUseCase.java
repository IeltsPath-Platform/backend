package com.group01.user.application.usecase;

import com.group01.user.application.command.CreateLearningGoalCommand;
import com.group01.user.application.result.LearningGoalResult;
import com.group01.user.domain.aggregate.LearningGoal;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.exception.LearningGoalConflictException;
import com.group01.user.domain.repository.LearningGoalRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.GoalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateLearningGoalUseCase {
    private final LearningGoalRepository learningGoalRepository;
    private final UserRepository userRepository;

    @Transactional
    public LearningGoalResult execute(CreateLearningGoalCommand command) {
        if (userRepository.findById(command.userId()).isEmpty()) {
            throw new UserNotFoundException("Không tìm thấy người dùng với id: " + command.userId());
        }

        // Tự động tạm dừng mục tiêu đang active hiện tại (nếu có)
        try {
            // Existing product behavior atomically pauses the current goal before activating its replacement.
            learningGoalRepository.findActiveByUserId(command.userId())
                    .ifPresent(existingActiveGoal -> {
                        existingActiveGoal.pause();
                        learningGoalRepository.save(existingActiveGoal);
                    });

            LearningGoal newGoal = LearningGoal.builder()
                    .id(UUID.randomUUID())
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
