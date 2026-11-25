package com.ieltspath.access.application.usecase;

import com.ieltspath.access.domain.aggregate.Subscription;
import com.ieltspath.access.domain.repository.SubscriptionRepository;
import com.ieltspath.access.domain.vo.SubscriptionSourceType;
import com.ieltspath.access.domain.vo.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IsUserPremiumUseCaseTest {
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final IsUserPremiumUseCase useCase = new IsUserPremiumUseCase(subscriptions);

    @Test
    void activeSubscriptionIsPremium() {
        UUID userId = UUID.randomUUID();
        when(subscriptions.findActiveByUserId(userId)).thenReturn(Optional.of(subscription(userId, null)));

        assertTrue(useCase.execute(userId));
    }

    @Test
    void absentOrExpiredSubscriptionIsNotPremium() {
        UUID userId = UUID.randomUUID();
        when(subscriptions.findActiveByUserId(userId)).thenReturn(Optional.empty());
        assertFalse(useCase.execute(userId));

        when(subscriptions.findActiveByUserId(userId)).thenReturn(Optional.of(subscription(userId, Instant.now().minusSeconds(1))));
        assertFalse(useCase.execute(userId));
    }

    private static Subscription subscription(UUID userId, Instant endsAt) {
        return new Subscription(UUID.randomUUID(), userId, UUID.randomUUID(), SubscriptionStatus.ACTIVE,
                Instant.now().minusSeconds(10), endsAt, SubscriptionSourceType.ADMIN, null,
                0, 0, null, 0, Instant.now(), Instant.now());
    }
}
