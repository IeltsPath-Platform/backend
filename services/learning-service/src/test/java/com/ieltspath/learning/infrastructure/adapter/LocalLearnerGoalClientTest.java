package com.ieltspath.learning.infrastructure.adapter;

import com.ieltspath.learning.domain.aggregate.LearningGoal;
import com.ieltspath.learning.domain.exception.LearningGoalInvariantViolationException;
import com.ieltspath.learning.domain.repository.LearningGoalRepository;
import com.ieltspath.learning.domain.vo.GoalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The report reads the learner's goal from Learning's own store; a goal is optional context and never blocks a report. */
class LocalLearnerGoalClientTest {
    private final UUID user = UUID.randomUUID();
    private final LearningGoalRepository goals = mock(LearningGoalRepository.class);
    private final LocalLearnerGoalClient client = new LocalLearnerGoalClient(goals);

    @Test
    void returnsTheTargetBandAndExamDateOfTheActiveGoal() {
        var goal = LearningGoal.builder().id(UUID.randomUUID()).userId(user).targetBand(new BigDecimal("7.5"))
                .examDate(LocalDate.parse("2027-03-01")).status(GoalStatus.ACTIVE).build();
        when(goals.findActiveByUserId(user)).thenReturn(Optional.of(goal));

        var found = client.goal(user).orElseThrow();

        assertEquals(new BigDecimal("7.5"), found.targetBand());
        assertEquals(LocalDate.parse("2027-03-01"), found.examDate());
    }

    @Test
    void aLearnerWithoutAnActiveGoalHasNone() {
        when(goals.findActiveByUserId(user)).thenReturn(Optional.empty());

        assertTrue(client.goal(user).isEmpty());
    }

    @Test
    void aStoreProblemMeansNoGoalInsteadOfAFailedReport() {
        when(goals.findActiveByUserId(user)).thenThrow(new LearningGoalInvariantViolationException("two active goals"));

        assertTrue(client.goal(user).isEmpty());
    }
}
