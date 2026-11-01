package com.ieltspath.learning.infrastructure.adapter;

import com.ieltspath.learning.application.port.LearnerGoalClient;
import com.ieltspath.learning.domain.repository.LearningGoalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** The learner's active goal from Learning's own store; any failure means no goal. */
@Component
public class LocalLearnerGoalClient implements LearnerGoalClient {
    private static final Logger log = LoggerFactory.getLogger(LocalLearnerGoalClient.class);

    private final LearningGoalRepository goals;

    public LocalLearnerGoalClient(LearningGoalRepository goals) {
        this.goals = goals;
    }

    @Override
    public Optional<Goal> goal(UUID userId) {
        try {
            return goals.findActiveByUserId(userId).map(goal -> new Goal(goal.getTargetBand(), goal.getExamDate()));
        } catch (RuntimeException failure) {
            log.warn("Learning goal unavailable: userId={}", userId, failure);
            return Optional.empty();
        }
    }
}
