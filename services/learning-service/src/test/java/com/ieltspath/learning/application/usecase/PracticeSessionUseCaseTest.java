package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import com.ieltspath.learning.domain.repository.PracticeSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PracticeSessionUseCaseTest {
    private final UUID user = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();
    private final PracticeSessionRepository repository = mock(PracticeSessionRepository.class);
    private final LearnerLock lock = mock(LearnerLock.class);
    private final TransactionTemplate transactions = new TransactionTemplate() {
        @Override public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(mock(TransactionStatus.class));
        }
    };
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC);
    private final OpenPracticeSessionUseCase useCase = new OpenPracticeSessionUseCase(repository, lock, transactions, clock);

    @Test
    void opensNewPracticeSessionAfterLockingLearner() {
        var result = useCase.execute(user, "WRITING", requestId);
        var saved = org.mockito.ArgumentCaptor.forClass(com.ieltspath.learning.domain.aggregate.PracticeSession.class);
        verify(lock).lock(user);
        verify(repository).findByRequest(user, requestId);
        verify(repository).save(saved.capture());
        assertEquals(result.id(), saved.getValue().id());
        assertEquals("WRITING", result.kind());
        assertEquals(clock.instant().plusSeconds(6 * 3600), result.expiresAt());
    }

    @Test
    void sameRequestReturnsSameSessionWithoutCreatingAnother() {
        var existing = com.ieltspath.learning.domain.aggregate.PracticeSession.start(user, "SPEAKING", requestId, clock.instant());
        when(repository.findByRequest(user, requestId)).thenReturn(Optional.of(existing));
        var result = useCase.execute(user, "SPEAKING", requestId);
        assertEquals(existing.id(), result.id());
        verify(repository, never()).save(any());
    }

    @Test
    void sameRequestWithDifferentKindConflicts() {
        var existing = com.ieltspath.learning.domain.aggregate.PracticeSession.start(user, "SPEAKING", requestId, clock.instant());
        when(repository.findByRequest(user, requestId)).thenReturn(Optional.of(existing));
        assertEquals("PAYLOAD_CONFLICT", assertThrows(StudyTrackingException.class,
                () -> useCase.execute(user, "WRITING", requestId)).getCode());
        verify(repository, never()).save(any());
    }
}
