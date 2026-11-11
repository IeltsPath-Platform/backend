package com.ieltspath.notification.application;

import com.ieltspath.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRetentionUseCaseTest {
    @Test
    void appliesNinetyDayReadAndOneHundredEightyDayUnreadCutoffs() {
        NotificationRepository repository = mock(NotificationRepository.class);
        Instant now = Instant.parse("2026-11-10T12:00:00Z");
        when(repository.deleteExpiredBatch(now.minusSeconds(90L * 86400), now.minusSeconds(180L * 86400), 25)).thenReturn(2);

        assertEquals(2, new NotificationRetentionUseCase(repository).deleteExpiredBatch(now, 25));
        verify(repository).deleteExpiredBatch(now.minusSeconds(90L * 86400), now.minusSeconds(180L * 86400), 25);
    }

    @Test
    void rejectsAnUnboundedBatchSize() {
        NotificationRetentionUseCase useCase = new NotificationRetentionUseCase(mock(NotificationRepository.class));
        assertThrows(IllegalArgumentException.class, () -> useCase.deleteExpiredBatch(Instant.now(), 10_001));
    }
}
