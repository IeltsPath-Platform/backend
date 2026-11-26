package com.ieltspath.library.infrastructure.scheduler;

import com.ieltspath.library.infrastructure.persistence.OutboxPurgeRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OutboxPurgeJobTest {
    @Test
    void disabledJobDoesNothing() {
        OutboxPurgeRepository repository = mock(OutboxPurgeRepository.class);
        new OutboxPurgeJob(repository, new SimpleMeterRegistry(), false, 30, 10, 2).purge();
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnsafeLimits() {
        OutboxPurgeRepository repository = mock(OutboxPurgeRepository.class);
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        assertThrows(IllegalArgumentException.class, () -> new OutboxPurgeJob(repository, meters, true, 1, 10, 2));
        assertThrows(IllegalArgumentException.class, () -> new OutboxPurgeJob(repository, meters, true, 30, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> new OutboxPurgeJob(repository, meters, true, 30, 10, 0));
    }

    @Test
    void stopsAtTheConfiguredBatchLimitAndCountsDeletedRows() {
        OutboxPurgeRepository repository = mock(OutboxPurgeRepository.class);
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        when(repository.deletePublishedBefore(any(), eq(10))).thenReturn(10);

        new OutboxPurgeJob(repository, meters, true, 30, 10, 2).purge();

        verify(repository, org.mockito.Mockito.times(2)).deletePublishedBefore(any(), eq(10));
        assertEquals(20, meters.get("outbox.purge.deleted").tag("service", "library").counter().count());
    }
}
