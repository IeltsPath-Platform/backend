package com.group01.access.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivationKeyCodeTest {

    @Test
    @DisplayName("Should generate valid SHA-256 hash and 4-char hint from raw key")
    void shouldGenerateValidHashAndHint() {
        String rawKey = "IP-ABCD-1234-EFGH-9999";
        ActivationKeyCode code = ActivationKeyCode.fromRawKey(rawKey);

        assertThat(code.rawKey()).isEqualTo(rawKey);
        assertThat(code.codeHash()).hasSize(64); // SHA-256 hex string length
        assertThat(code.codeHint()).isEqualTo("9999");
    }

    @Test
    @DisplayName("Should trim and uppercase raw key for consistent hashing")
    void shouldNormalizeRawKey() {
        ActivationKeyCode code1 = ActivationKeyCode.fromRawKey("  ip-abcd-1234  ");
        ActivationKeyCode code2 = ActivationKeyCode.fromRawKey("IP-ABCD-1234");

        assertThat(code1.codeHash()).isEqualTo(code2.codeHash());
        assertThat(code1.codeHint()).isEqualTo("1234");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when raw key is blank")
    void shouldThrowWhenRawKeyIsBlank() {
        assertThatThrownBy(() -> ActivationKeyCode.fromRawKey(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActivationKeyCode.fromRawKey("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActivationKeyCode.fromRawKey(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
