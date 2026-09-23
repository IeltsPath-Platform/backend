package com.group01.access.domain.aggregate;

import com.group01.access.domain.exception.InvalidKeyOperationException;
import com.group01.access.domain.exception.KeyAlreadyRedeemedException;
import com.group01.access.domain.exception.KeyExpiredException;
import com.group01.access.domain.exception.KeyRevokedException;
import com.group01.access.domain.vo.KeyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivationKeyTest {

    @Test
    @DisplayName("Should create active key that is redeemable")
    void shouldCreateActiveKey() {
        ActivationKey key = ActivationKey.create(UUID.randomUUID(), "hash123", "9999", Instant.now().plus(10, ChronoUnit.DAYS), UUID.randomUUID());

        assertThat(key.getStatus()).isEqualTo(KeyStatus.ACTIVE);
        assertThat(key.isRedeemable()).isTrue();
        assertThat(key.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should successfully redeem active key")
    void shouldRedeemKey() {
        ActivationKey key = ActivationKey.create(UUID.randomUUID(), "hash123", "9999", null, null);
        key.redeem();

        assertThat(key.getStatus()).isEqualTo(KeyStatus.REDEEMED);
        assertThat(key.getRedeemedAt()).isNotNull();
        assertThat(key.isRedeemable()).isFalse();

        // Second redemption must throw
        assertThatThrownBy(key::redeem)
                .isInstanceOf(KeyAlreadyRedeemedException.class);
    }

    @Test
    @DisplayName("Should throw KeyExpiredException when redeeming expired key")
    void shouldThrowWhenRedeemingExpiredKey() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        ActivationKey key = ActivationKey.create(UUID.randomUUID(), "hash123", "9999", past, null);

        assertThat(key.isExpired()).isTrue();
        assertThat(key.isRedeemable()).isFalse();

        assertThatThrownBy(key::redeem)
                .isInstanceOf(KeyExpiredException.class);
    }

    @Test
    @DisplayName("Should revoke active key and block redemption")
    void shouldRevokeKey() {
        ActivationKey key = ActivationKey.create(UUID.randomUUID(), "hash123", "9999", null, null);
        key.revoke();

        assertThat(key.getStatus()).isEqualTo(KeyStatus.REVOKED);
        assertThat(key.isRedeemable()).isFalse();

        assertThatThrownBy(key::redeem)
                .isInstanceOf(KeyRevokedException.class);
    }

    @Test
    @DisplayName("Should throw when trying to revoke already redeemed key")
    void shouldThrowWhenRevokingRedeemedKey() {
        ActivationKey key = ActivationKey.create(UUID.randomUUID(), "hash123", "9999", null, null);
        key.redeem();

        assertThatThrownBy(key::revoke)
                .isInstanceOf(InvalidKeyOperationException.class);
    }
}
