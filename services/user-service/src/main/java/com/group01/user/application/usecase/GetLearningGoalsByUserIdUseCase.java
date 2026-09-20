package com.group01.user.application.usecase;

import com.group01.user.application.result.LearningGoalResult;
import com.group01.user.domain.aggregate.LearningGoal;
import com.group01.user.domain.repository.LearningGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetLearningGoalsByUserIdUseCase {
    private final LearningGoalRepository learningGoalRepository;

    @Transactional(readOnly = true)
    public List<LearningGoalResult> execute(UUID userId) {
        return learningGoalRepository.findByUserId(userId).stream()
                .map(this::toResult)
                .toList();
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

