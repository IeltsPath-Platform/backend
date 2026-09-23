package com.group01.access.domain.aggregate;

import com.group01.access.domain.exception.InsufficientPointsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointWalletTest {

    @Test
    @DisplayName("Should create wallet with zero balance")
    void shouldCreateWalletWithZeroBalance() {
        UUID userId = UUID.randomUUID();
        PointWallet wallet = PointWallet.create(userId);

        assertThat(wallet.getUserId()).isEqualTo(userId);
        assertThat(wallet.getBalance()).isZero();
        assertThat(wallet.getTotalCredited()).isZero();
        assertThat(wallet.getTotalDebited()).isZero();
    }

    @Test
    @DisplayName("Should credit points correctly")
    void shouldCreditPoints() {
        PointWallet wallet = PointWallet.create(UUID.randomUUID());
        wallet.credit(50);

        assertThat(wallet.getBalance()).isEqualTo(50);
        assertThat(wallet.getTotalCredited()).isEqualTo(50);
        assertThat(wallet.getTotalDebited()).isZero();

        wallet.credit(100);
        assertThat(wallet.getBalance()).isEqualTo(150);
        assertThat(wallet.getTotalCredited()).isEqualTo(150);
    }

    @Test
    @DisplayName("Should debit points correctly when balance is sufficient")
    void shouldDebitPoints() {
        PointWallet wallet = PointWallet.create(UUID.randomUUID());
        wallet.credit(50);

        wallet.debit(20);
        assertThat(wallet.getBalance()).isEqualTo(30);
        assertThat(wallet.getTotalDebited()).isEqualTo(20);
        assertThat(wallet.getTotalCredited()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should throw InsufficientPointsException when debiting more than balance")
    void shouldThrowWhenDebitExceedsBalance() {
        PointWallet wallet = PointWallet.create(UUID.randomUUID());
        wallet.credit(10);

        assertThatThrownBy(() -> wallet.debit(15))
                .isInstanceOf(InsufficientPointsException.class);
    }

    @Test
    @DisplayName("Should reject invalid amounts for credit and debit")
    void shouldRejectInvalidAmounts() {
        PointWallet wallet = PointWallet.create(UUID.randomUUID());

        assertThatThrownBy(() -> wallet.credit(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.credit(-5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(-5)).isInstanceOf(IllegalArgumentException.class);
    }
}
