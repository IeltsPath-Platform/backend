package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.ChangeLearningGoalStatusCommand;
import com.ieltspath.learning.application.command.CreateLearningGoalCommand;
import com.ieltspath.learning.application.result.LearningGoalResult;
import com.ieltspath.learning.domain.aggregate.LearningGoal;
import com.ieltspath.learning.domain.exception.LearningGoalConflictException;
import com.ieltspath.learning.domain.exception.LearningGoalNotFoundException;
import com.ieltspath.learning.domain.repository.LearningGoalRepository;
import com.ieltspath.learning.domain.vo.GoalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningGoalUseCaseTest {
    @Mock private LearningGoalRepository learningGoalRepository;
    @Mock private ScheduleStudyGoalUseCase studyGoals;

    private GetActiveLearningGoalUseCase getActiveLearningGoalUseCase;
    private GetLearningGoalsByUserIdUseCase getLearningGoalsByUserIdUseCase;
    private CreateLearningGoalUseCase createLearningGoalUseCase;
    private ChangeLearningGoalStatusUseCase changeLearningGoalStatusUseCase;

    @BeforeEach
    void setUp() {
        getActiveLearningGoalUseCase = new GetActiveLearningGoalUseCase(learningGoalRepository);
        getLearningGoalsByUserIdUseCase = new GetLearningGoalsByUserIdUseCase(learningGoalRepository);
        createLearningGoalUseCase = new CreateLearningGoalUseCase(learningGoalRepository, studyGoals);
        changeLearningGoalStatusUseCase = new ChangeLearningGoalStatusUseCase(learningGoalRepository, studyGoals);
    }

    @Test
    void getActiveLearningGoalReturnsActiveGoal() {
        UUID userId = UUID.randomUUID();
        LearningGoal goal = LearningGoal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .targetBand(BigDecimal.valueOf(7.5))
                .examDate(LocalDate.now().plusMonths(2))
                .availableMinutesPerDay(45)
                .status(GoalStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(learningGoalRepository.findActiveByUserId(userId)).thenReturn(Optional.of(goal));

        LearningGoalResult result = getActiveLearningGoalUseCase.execute(userId);
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(7.5), result.targetBand());
        assertEquals("ACTIVE", result.status());
    }

    @Test
    void getActiveLearningGoalThrowsWhenNotFound() {
        UUID userId = UUID.randomUUID();
        when(learningGoalRepository.findActiveByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(LearningGoalNotFoundException.class, () -> getActiveLearningGoalUseCase.execute(userId));
    }

    @Test
    void createLearningGoalPausesExistingActiveGoal() {
        UUID userId = UUID.randomUUID();
        LearningGoal oldGoal = LearningGoal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .targetBand(BigDecimal.valueOf(6.5))
                .status(GoalStatus.ACTIVE)
                .build();

        when(learningGoalRepository.findActiveByUserId(userId)).thenReturn(Optional.of(oldGoal));
        when(learningGoalRepository.save(any(LearningGoal.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateLearningGoalCommand cmd = new CreateLearningGoalCommand(
                userId,
                BigDecimal.valueOf(7.5),
                LocalDate.now().plusMonths(3),
                60
        );

        LearningGoalResult result = createLearningGoalUseCase.execute(cmd);
        assertEquals(GoalStatus.PAUSED, oldGoal.getStatus());
        assertEquals("ACTIVE", result.status());
        assertEquals(BigDecimal.valueOf(7.5), result.targetBand());
    }

    @Test
    void changeLearningGoalStatusChangesStatusSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        LearningGoal goal = LearningGoal.builder()
                .id(goalId)
                .userId(userId)
                .targetBand(BigDecimal.valueOf(7.0))
                .status(GoalStatus.ACTIVE)
                .build();

        when(learningGoalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(learningGoalRepository.save(any(LearningGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangeLearningGoalStatusCommand cmd = new ChangeLearningGoalStatusCommand(goalId, userId, "ACHIEVED");
        LearningGoalResult result = changeLearningGoalStatusUseCase.execute(cmd);

        assertEquals("ACHIEVED", result.status());
    }

    @Test
    void changeLearningGoalStatusThrowsWhenUserMismatch() {
        UUID ownerId = UUID.randomUUID();
        UUID anotherUser = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        LearningGoal goal = LearningGoal.builder()
                .id(goalId)
                .userId(ownerId)
                .targetBand(BigDecimal.valueOf(7.0))
                .status(GoalStatus.ACTIVE)
                .build();

        when(learningGoalRepository.findById(goalId)).thenReturn(Optional.of(goal));

        ChangeLearningGoalStatusCommand cmd = new ChangeLearningGoalStatusCommand(goalId, anotherUser, "PAUSED");
        assertThrows(AccessDeniedException.class, () -> changeLearningGoalStatusUseCase.execute(cmd));
    }

    private static LearningGoal goal(UUID userId, GoalStatus status) {
        return LearningGoal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .targetBand(BigDecimal.valueOf(7.0))
                .availableMinutesPerDay(60)
                .status(status)
                .build();
    }

    @Test
    void aFirstGoalIsCreatedActiveAndScheduled() {
        UUID userId = UUID.randomUUID();
        when(learningGoalRepository.findActiveByUserId(userId)).thenReturn(Optional.empty());
        when(learningGoalRepository.save(any(LearningGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        LearningGoalResult result = createLearningGoalUseCase.execute(
                new CreateLearningGoalCommand(userId, BigDecimal.valueOf(6.5), LocalDate.now().plusMonths(2), 60));

        assertEquals("ACTIVE", result.status());
        assertEquals(BigDecimal.valueOf(6.5), result.targetBand());
        ArgumentCaptor<LearningGoal> scheduled = ArgumentCaptor.forClass(LearningGoal.class);
        verify(studyGoals).execute(scheduled.capture());
        assertEquals(60, scheduled.getValue().getAvailableMinutesPerDay());
    }

    @Test
    void anUnknownOrBlankStatusIsRefused() {
        UUID userId = UUID.randomUUID();
        LearningGoal goal = goal(userId, GoalStatus.ACTIVE);
        when(learningGoalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        var unknown = assertThrows(IllegalArgumentException.class, () -> changeLearningGoalStatusUseCase.execute(
                new ChangeLearningGoalStatusCommand(goal.getId(), userId, "DONE")));
        assertEquals("Trạng thái không hợp lệ: DONE", unknown.getMessage());
        var blank = assertThrows(IllegalArgumentException.class, () -> changeLearningGoalStatusUseCase.execute(
                new ChangeLearningGoalStatusCommand(goal.getId(), userId, " ")));
        assertEquals("Trạng thái mục tiêu không được để trống", blank.getMessage());
        assertEquals(GoalStatus.ACTIVE, goal.getStatus());
        verify(learningGoalRepository, never()).save(any());
    }

    @Test
    void changingTheStatusOfAGoalThatDoesNotExistIsNotFound() {
        UUID goalId = UUID.randomUUID();
        when(learningGoalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThrows(LearningGoalNotFoundException.class, () -> changeLearningGoalStatusUseCase.execute(
                new ChangeLearningGoalStatusCommand(goalId, UUID.randomUUID(), "PAUSED")));
        verify(learningGoalRepository, never()).save(any());
    }

    @Test
    void resumingAGoalPausesTheOtherActiveGoal() {
        UUID userId = UUID.randomUUID();
        LearningGoal paused = goal(userId, GoalStatus.PAUSED);
        LearningGoal active = goal(userId, GoalStatus.ACTIVE);
        when(learningGoalRepository.findById(paused.getId())).thenReturn(Optional.of(paused));
        when(learningGoalRepository.findActiveByUserId(userId)).thenReturn(Optional.of(active));
        when(learningGoalRepository.save(any(LearningGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        LearningGoalResult result = changeLearningGoalStatusUseCase.execute(
                new ChangeLearningGoalStatusCommand(paused.getId(), userId, "ACTIVE"));

        assertEquals("ACTIVE", result.status());
        assertEquals(GoalStatus.PAUSED, active.getStatus());
        verify(learningGoalRepository).save(active);
        verify(learningGoalRepository).save(paused);
    }

    @Test
    void aStatusIsReadWithoutCaseOrSurroundingSpaces() {
        UUID userId = UUID.randomUUID();
        LearningGoal goal = goal(userId, GoalStatus.ACTIVE);
        when(learningGoalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(learningGoalRepository.save(any(LearningGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        LearningGoalResult result = changeLearningGoalStatusUseCase.execute(
                new ChangeLearningGoalStatusCommand(goal.getId(), userId, " abandoned "));

        assertEquals("ABANDONED", result.status());
    }

    @Test
    void anAdminChangeLocksTheGoalOwnerAndSkipsTheOwnerCheck() {
        LearningGoal goal = goal(UUID.randomUUID(), GoalStatus.ACTIVE);
        when(learningGoalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(learningGoalRepository.save(any(LearningGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        LearningGoalResult result = changeLearningGoalStatusUseCase.execute(
                new ChangeLearningGoalStatusCommand(goal.getId(), null, "PAUSED"));

        assertEquals("PAUSED", result.status());
        verify(studyGoals).lockGoalOwner(goal.getId());
        verify(studyGoals, never()).lock(any());
    }

    @Test
    void theGoalHistoryOfALearnerListsEveryGoal() {
        UUID userId = UUID.randomUUID();
        LearningGoal old = goal(userId, GoalStatus.ABANDONED);
        LearningGoal current = goal(userId, GoalStatus.ACTIVE);
        when(learningGoalRepository.findByUserId(userId)).thenReturn(List.of(old, current));

        List<LearningGoalResult> history = getLearningGoalsByUserIdUseCase.execute(userId);

        assertEquals(List.of("ABANDONED", "ACTIVE"), history.stream().map(LearningGoalResult::status).toList());
        assertEquals(List.of(old.getId(), current.getId()), history.stream().map(LearningGoalResult::id).toList());
    }

    @Test
    void aSecondActiveGoalHittingTheUniqueConstraintIsAConflict() {
        UUID userId = UUID.randomUUID();
        when(learningGoalRepository.findActiveByUserId(userId)).thenReturn(Optional.empty());
        when(learningGoalRepository.save(any(LearningGoal.class)))
                .thenThrow(new DataIntegrityViolationException("unique active goal"));

        var conflict = assertThrows(LearningGoalConflictException.class, () -> createLearningGoalUseCase.execute(
                new CreateLearningGoalCommand(userId, BigDecimal.valueOf(7.0), null, 60)));
        assertEquals("An active learning goal already exists for this user", conflict.getMessage());
        verify(studyGoals, never()).execute(any());
    }
}

