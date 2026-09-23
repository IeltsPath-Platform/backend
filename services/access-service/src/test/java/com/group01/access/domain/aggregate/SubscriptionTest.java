package com.group01.access.domain.aggregate;

import com.group01.access.domain.exception.InsufficientCreditsException;
import com.group01.access.domain.vo.SubscriptionSourceType;
import com.group01.access.domain.vo.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    @Test
    @DisplayName("Should create active subscription with endsAt and human grading credits")
    void shouldCreateActiveSubscription() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        Subscription sub = Subscription.create(userId, planId, SubscriptionSourceType.ACTIVATION_KEY, UUID.randomUUID(), 30, 4);

        assertThat(sub.getUserId()).isEqualTo(userId);
        assertThat(sub.getPlanId()).isEqualTo(planId);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.isActive()).isTrue();
        assertThat(sub.getHumanGradingCreditsTotal()).isEqualTo(4);
        assertThat(sub.getHumanGradingCreditsUsed()).isZero();
        assertThat(sub.getRemainingCredits()).isEqualTo(4);
        assertThat(sub.getEndsAt()).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("Should extend active subscription endsAt and credit quota")
    void shouldExtendSubscription() {
        Subscription sub = Subscription.create(UUID.randomUUID(), UUID.randomUUID(), SubscriptionSourceType.ACTIVATION_KEY, UUID.randomUUID(), 30, 4);
        Instant firstEndsAt = sub.getEndsAt();

        sub.extend(30, 4);

        assertThat(sub.getEndsAt()).isAfter(firstEndsAt.plus(29, ChronoUnit.DAYS));
        assertThat(sub.getHumanGradingCreditsTotal()).isEqualTo(8);
        assertThat(sub.getRemainingCredits()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should consume human grading credit correctly")
    void shouldConsumeHumanGradingCredit() {
        Subscription sub = Subscription.create(UUID.randomUUID(), UUID.randomUUID(), SubscriptionSourceType.ACTIVATION_KEY, UUID.randomUUID(), 30, 2);

        sub.consumeHumanGradingCredit();
        assertThat(sub.getRemainingCredits()).isEqualTo(1);
        assertThat(sub.getHumanGradingCreditsUsed()).isEqualTo(1);

        sub.consumeHumanGradingCredit();
        assertThat(sub.getRemainingCredits()).isZero();
        assertThat(sub.getHumanGradingCreditsUsed()).isEqualTo(2);

        assertThatThrownBy(sub::consumeHumanGradingCredit)
                .isInstanceOf(InsufficientCreditsException.class);
    }

    @Test
    @DisplayName("Should cancel subscription correctly")
    void shouldCancelSubscription() {
        Subscription sub = Subscription.create(UUID.randomUUID(), UUID.randomUUID(), SubscriptionSourceType.ACTIVATION_KEY, UUID.randomUUID(), 30, 4);
        sub.cancel();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(sub.isActive()).isFalse();
        assertThat(sub.getCancelledAt()).isNotNull();
    }
}
