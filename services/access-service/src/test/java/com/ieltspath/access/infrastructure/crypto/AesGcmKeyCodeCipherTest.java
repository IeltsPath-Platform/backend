package com.ieltspath.access.infrastructure.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmKeyCodeCipherTest {

    private static final String SECRET = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    private static final String CODE = "IP-ABCD-2345-WXYZ";

    @Test
    @DisplayName("A stored code decrypts back to the code it came from")
    void roundTripReturnsTheSameCode() {
        AesGcmKeyCodeCipher cipher = new AesGcmKeyCodeCipher(SECRET);

        assertThat(cipher.available()).isTrue();
        assertThat(cipher.decrypt(cipher.encrypt(CODE))).isEqualTo(CODE);
    }

    @Test
    @DisplayName("The same code encrypts differently every time, and the stored text never contains the plain code")
    void eachEncryptionUsesANewNonce() {
        AesGcmKeyCodeCipher cipher = new AesGcmKeyCodeCipher(SECRET);

        String first = cipher.encrypt(CODE);
        String second = cipher.encrypt(CODE);

        assertThat(first).isNotEqualTo(second);
        assertThat(first).doesNotContain("ABCD").doesNotContain(CODE);
    }

    @Test
    @DisplayName("A stored code changed by one character is refused, not decrypted to garbage")
    void tamperedCiphertextIsRefused() {
        AesGcmKeyCodeCipher cipher = new AesGcmKeyCodeCipher(SECRET);
        byte[] raw = Base64.getDecoder().decode(cipher.encrypt(CODE));
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A code encrypted under another secret cannot be read")
    void otherSecretCannotDecrypt() {
        String otherSecret = Base64.getEncoder().encodeToString("ffffffffffffffffffffffffffffffff".getBytes());
        String stored = new AesGcmKeyCodeCipher(otherSecret).encrypt(CODE);

        assertThatThrownBy(() -> new AesGcmKeyCodeCipher(SECRET).decrypt(stored)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("With no secret the cipher is unavailable and says so, instead of failing at startup")
    void missingSecretLeavesCipherUnavailable() {
        AesGcmKeyCodeCipher cipher = new AesGcmKeyCodeCipher("");

        assertThat(cipher.available()).isFalse();
        assertThatThrownBy(() -> cipher.encrypt(CODE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A secret that is not Base64 of exactly 32 bytes is treated as missing")
    void malformedSecretLeavesCipherUnavailable() {
        assertThat(new AesGcmKeyCodeCipher("not-base64!!").available()).isFalse();
        assertThat(new AesGcmKeyCodeCipher(Base64.getEncoder().encodeToString(new byte[16])).available()).isFalse();
    }
}
