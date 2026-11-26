package com.ieltspath.access.application.usecase;

import com.ieltspath.access.domain.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpireLapsedSubscriptionsUseCaseTest {
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final ExpireLapsedSubscriptionsUseCase useCase = new ExpireLapsedSubscriptionsUseCase(subscriptions);

    @Test
    void expiresLapsedRowsAtTheProvidedInstant() {
        Instant now = Instant.parse("2026-11-26T12:00:00Z");
        when(subscriptions.expireAllLapsed(now)).thenReturn(3);

        assertThat(useCase.execute(now)).isEqualTo(3);

        verify(subscriptions).expireAllLapsed(now);
    }
}
