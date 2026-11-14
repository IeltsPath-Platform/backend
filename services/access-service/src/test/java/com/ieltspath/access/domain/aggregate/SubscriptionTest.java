package com.ieltspath.access.domain.aggregate;

import com.ieltspath.access.domain.exception.InsufficientCreditsException;
import com.ieltspath.access.domain.vo.SubscriptionSourceType;
import com.ieltspath.access.domain.vo.SubscriptionStatus;
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

    @Test
    @DisplayName("A cancelled subscription cannot use a credit")
    void shouldRefuseCreditOnCancelledSubscription() {
        Subscription sub = Subscription.create(UUID.randomUUID(), UUID.randomUUID(), SubscriptionSourceType.ACTIVATION_KEY, UUID.randomUUID(), 30, 4);
        sub.cancel();

        assertThatThrownBy(sub::consumeHumanGradingCredit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot consume credit on inactive subscription");
        assertThat(sub.getRemainingCredits()).isEqualTo(4);
    }

    @Test
    @DisplayName("Extending an ended subscription counts from now")
    void shouldExtendEndedSubscriptionFromNow() {
        Instant now = Instant.now();
        Subscription sub = new Subscription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SubscriptionStatus.ACTIVE,
                now.minus(35, ChronoUnit.DAYS), now.minus(5, ChronoUnit.DAYS), SubscriptionSourceType.ACTIVATION_KEY,
                UUID.randomUUID(), 0, 0, null, 0, null, null);

        sub.extend(30, 0);

        assertThat(sub.getEndsAt()).isAfter(now.plus(29, ChronoUnit.DAYS)).isBefore(now.plus(31, ChronoUnit.DAYS));
        assertThat(sub.isActive()).isTrue();
    }

    @Test
    @DisplayName("A credit cannot be refunded when none was used")
    void shouldRefuseRefundWithoutUsedCredit() {
        Subscription sub = Subscription.create(UUID.randomUUID(), UUID.randomUUID(), SubscriptionSourceType.ACTIVATION_KEY, UUID.randomUUID(), 30, 2);

        assertThatThrownBy(sub::refundHumanGradingCredit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No consumed human grading credit to refund");
        assertThat(sub.getHumanGradingCreditsUsed()).isZero();
    }
}
