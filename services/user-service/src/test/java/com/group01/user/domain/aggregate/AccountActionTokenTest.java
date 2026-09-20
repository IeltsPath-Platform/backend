package com.group01.user.domain.aggregate;

import com.group01.user.domain.vo.ActionTokenPurpose;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountActionTokenTest {

    @Test
    void use_marksTokenAsUsed() {
        AccountActionToken token = AccountActionToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .purpose(ActionTokenPurpose.PASSWORD_RESET)
                .tokenHash("hash123")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        assertThat(token.isValid()).isTrue();
        assertThat(token.isUsed()).isFalse();

        token.use();

        assertThat(token.isUsed()).isTrue();
        assertThat(token.isValid()).isFalse();
        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void use_throwsWhenAlreadyUsed() {
        AccountActionToken token = AccountActionToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .purpose(ActionTokenPurpose.EMAIL_VERIFICATION)
                .tokenHash("hash123")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .usedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        assertThatThrownBy(token::use)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã được sử dụng");
    }

    @Test
    void use_throwsWhenExpired() {
        AccountActionToken token = AccountActionToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .purpose(ActionTokenPurpose.PASSWORD_RESET)
                .tokenHash("hash123")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        assertThat(token.isExpired()).isTrue();
        assertThatThrownBy(token::use)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã hết hạn");
    }
}

