package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.result.LearningGoalResult;
import com.ieltspath.learning.domain.aggregate.LearningGoal;
import com.ieltspath.learning.domain.exception.LearningGoalNotFoundException;
import com.ieltspath.learning.domain.repository.LearningGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetActiveLearningGoalUseCase {
    private final LearningGoalRepository learningGoalRepository;

    @Transactional(readOnly = true)
    public LearningGoalResult execute(UUID userId) {
        LearningGoal goal = learningGoalRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new LearningGoalNotFoundException("Không tìm thấy mục tiêu học tập đang hoạt động"));
        return toResult(goal);
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

