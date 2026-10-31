package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.PendingCourseUnlockWriter;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconcilePendingCourseUnlocksTest {
    @Mock LearnerCourseUnlockRepository unlocks;
    @Mock AccessClient access;
    @Mock PendingCourseUnlockWriter intentWriter;
    @InjectMocks ReconcilePendingCourseUnlocks reconciler;

    private final UUID user = UUID.randomUUID();

    @Test
    void replaysStoredAmountAndRequestIdBeforeActivating() {
        var row = pending(75);
        UUID ledger = UUID.randomUUID();
        when(unlocks.findPending(user)).thenReturn(List.of(row));
        when(access.debit(user, 75, "COURSE_UNLOCK", row.requestId(), row.requestId().toString(),
                "Course unlock READING")).thenReturn(ledger);

        assertThat(reconciler.reconcile(user)).isTrue();
        verify(unlocks).activate(row.requestId(), ledger);
    }

    @Test
    void unavailableAccessKeepsTheIntentPending() {
        var row = pending(75);
        when(unlocks.findPending(user)).thenReturn(List.of(row));
        when(access.debit(any(), anyInt(), anyString(), any(), anyString(), anyString()))
                .thenThrow(new AccessUnavailableException(null));

        assertThat(reconciler.reconcile(user)).isFalse();
        verify(unlocks, never()).activate(any(), any());
        verify(unlocks, never()).cancel(any());
    }

    @Test
    void insufficientBalanceCancelsTheIntentWithoutActivating() {
        var row = pending(75);
        when(unlocks.findPending(user)).thenReturn(List.of(row));
        when(access.debit(any(), anyInt(), anyString(), any(), anyString(), anyString()))
                .thenThrow(new InsufficientPointsException());

        assertThat(reconciler.reconcile(user)).isTrue();
        verify(intentWriter).cancel(row.requestId());
        verify(unlocks, never()).activate(any(), any());
    }

    private LearnerCourseUnlock pending(int cost) {
        return new LearnerCourseUnlock(UUID.randomUUID(), user, UUID.randomUUID(), LearningSkill.READING,
                cost, UUID.randomUUID(), LearnerCourseUnlock.Status.PENDING, null);
    }
}
