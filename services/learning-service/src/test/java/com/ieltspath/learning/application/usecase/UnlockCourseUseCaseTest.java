package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.TopicResult;
import com.ieltspath.learning.application.port.PendingCourseUnlockWriter;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnlockCourseUseCaseTest {
    @Mock com.ieltspath.learning.application.port.LearnerLock lock;
    @Mock RefreshLearningTopicsUseCase refresh;
    @Mock LearnerCourseUnlockRepository unlocks;
    @Mock PendingCourseUnlockWriter writer;
    @Mock ReconcilePendingCourseUnlocks reconciler;
    @InjectMocks UnlockCourseUseCase useCase;

    private final UUID user = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();
    private final UUID ledgerId = UUID.randomUUID();

    @Test
    void rejectsAnUnknownTargetAsBadRequestBeforeLockingOrCallingContent() {
        assertThatThrownBy(() -> useCase.execute(user, courseId, "MATH", requestId))
                .isInstanceOf(LearningRequestException.class).extracting("status").isEqualTo(400);
        verifyNoInteractions(lock, refresh, unlocks, writer, reconciler);
    }

    @Test
    void purchaseCommitsPendingBeforeTheIdempotentDebitAndActivatesIt() {
        var pending = new LearnerCourseUnlock(UUID.randomUUID(), user, courseId, LearningSkill.READING,
                125, requestId, LearnerCourseUnlock.Status.PENDING, null);
        var active = new LearnerCourseUnlock(pending.id(), user, courseId, LearningSkill.READING,
                125, requestId, LearnerCourseUnlock.Status.ACTIVE, ledgerId);
        when(unlocks.findByRequestId(requestId)).thenReturn(Optional.empty(), Optional.empty(), Optional.of(active));
        when(refresh.execute(user)).thenReturn(List.of(topic()));
        when(unlocks.findActive(user, courseId)).thenReturn(List.of());
        when(writer.createPending(user, courseId, LearningSkill.READING, 125, requestId)).thenReturn(pending);
        when(reconciler.debit(pending)).thenReturn(ledgerId);

        var result = useCase.execute(user, courseId, "READING", requestId);
        var repeated = useCase.execute(user, courseId, "READING", requestId);

        assertThat(result.pointCost()).isEqualTo(125);
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(repeated).isEqualTo(result);
        verify(writer).createPending(user, courseId, LearningSkill.READING, 125, requestId);
        verify(reconciler, times(1)).debit(pending);
        verify(unlocks, times(1)).activate(requestId, ledgerId);
    }

    @Test
    void accessFailureLeavesTheCommittedIntentPendingForReplay() {
        var pending = new LearnerCourseUnlock(UUID.randomUUID(), user, courseId, LearningSkill.READING,
                125, requestId, LearnerCourseUnlock.Status.PENDING, null);
        when(unlocks.findByRequestId(requestId)).thenReturn(Optional.empty(), Optional.empty());
        when(refresh.execute(user)).thenReturn(List.of(topic()));
        when(unlocks.findActive(user, courseId)).thenReturn(List.of());
        when(writer.createPending(user, courseId, LearningSkill.READING, 125, requestId)).thenReturn(pending);
        when(reconciler.debit(pending)).thenThrow(new AccessUnavailableException(null));

        assertThatThrownBy(() -> useCase.execute(user, courseId, "READING", requestId))
                .isInstanceOf(AccessUnavailableException.class);
        verify(writer).createPending(user, courseId, LearningSkill.READING, 125, requestId);
        verify(unlocks, never()).activate(any(), any());
        verify(writer, never()).cancel(any());
    }

    @Test
    void retryAfterDebitButBeforeActivationReplaysTheSameRequestInsteadOfCreatingAnotherIntent() {
        var pending = new LearnerCourseUnlock(UUID.randomUUID(), user, courseId, LearningSkill.READING,
                125, requestId, LearnerCourseUnlock.Status.PENDING, null);
        var active = new LearnerCourseUnlock(pending.id(), user, courseId, LearningSkill.READING,
                125, requestId, LearnerCourseUnlock.Status.ACTIVE, ledgerId);
        when(unlocks.findByRequestId(requestId)).thenReturn(Optional.empty(), Optional.empty(), Optional.of(active),
                Optional.of(active));
        when(refresh.execute(user)).thenReturn(List.of(topic()));
        when(unlocks.findActive(user, courseId)).thenReturn(List.of());
        when(writer.createPending(user, courseId, LearningSkill.READING, 125, requestId)).thenReturn(pending);
        when(reconciler.debit(pending)).thenReturn(ledgerId);
        doThrow(new RuntimeException("simulated process failure before activation commit"))
                .when(unlocks).activate(requestId, ledgerId);

        assertThatThrownBy(() -> useCase.execute(user, courseId, "READING", requestId))
                .hasMessageContaining("simulated process failure");
        clearInvocations(unlocks);

        var retried = useCase.execute(user, courseId, "READING", requestId);

        assertThat(retried.status()).isEqualTo("ACTIVE");
        verify(writer).createPending(user, courseId, LearningSkill.READING, 125, requestId);
        verify(reconciler, times(2)).reconcile(user);
        verify(reconciler).debit(pending);
    }

    @Test
    void insufficientPointsCancelsThePendingIntentAndKeepsTheExisting402Exception() {
        var pending = new LearnerCourseUnlock(UUID.randomUUID(), user, courseId, LearningSkill.READING,
                125, requestId, LearnerCourseUnlock.Status.PENDING, null);
        when(unlocks.findByRequestId(requestId)).thenReturn(Optional.empty(), Optional.empty());
        when(refresh.execute(user)).thenReturn(List.of(topic()));
        when(unlocks.findActive(user, courseId)).thenReturn(List.of());
        when(writer.createPending(user, courseId, LearningSkill.READING, 125, requestId)).thenReturn(pending);
        when(reconciler.debit(pending)).thenThrow(new InsufficientPointsException());

        assertThatThrownBy(() -> useCase.execute(user, courseId, "READING", requestId))
                .isInstanceOf(InsufficientPointsException.class);
        verify(writer).cancel(requestId);
        verify(unlocks, never()).activate(any(), any());
    }

    private TopicResult topic() {
        var course = new TopicResult.Course(courseId, "IELTS_6_0", "IELTS 6.0", new BigDecimal("6.0"), null,
                null, Map.of(LearningSkill.READING, 125), 400,
                Set.of(LearningSkill.READING, LearningSkill.LISTENING));
        return new TopicResult(UUID.randomUUID(), "R1", "Reading", 1,
                com.ieltspath.learning.domain.vo.TopicStatus.IN_PROGRESS, 0, null, LearningSkill.READING,
                true, course, false);
    }
}
