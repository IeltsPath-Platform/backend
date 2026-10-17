package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.result.InternalLearningGoalResult;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.LearningGoalRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetInternalLearningGoalUseCase {
    private final UserRepository users;
    private final LearningGoalRepository learningGoals;

    @Transactional(readOnly = true)
    public InternalLearningGoalResult execute(UUID userId) {
        if (users.findById(userId).isEmpty()) {
            throw new UserNotFoundException("User not found: " + userId);
        }
        return learningGoals.findActiveByUserId(userId)
                .map(goal -> new InternalLearningGoalResult(goal.getTargetBand(), goal.getExamDate()))
                .orElseGet(() -> new InternalLearningGoalResult(null, null));
    }
}
