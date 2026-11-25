package com.ieltspath.access.application.service;

import com.ieltspath.access.application.exception.KeyDistributionException;
import com.ieltspath.access.application.port.KeyCodeCipher;
import org.springframework.stereotype.Component;

/** Turns a stored encrypted code back into the plain code, with the refusals the screens know how to show. */
@Component
public class KeyCodeReader {
    private final KeyCodeCipher cipher;

    public KeyCodeReader(KeyCodeCipher cipher) {
        this.cipher = cipher;
    }

    /** For a code that must be shown: no stored code and an unreadable one are different answers. */
    public String read(String stored) {
        if (stored == null || stored.isBlank()) throw KeyDistributionException.codeNotStored();
        if (!cipher.available()) throw KeyDistributionException.cipherUnavailable();
        try {
            return cipher.decrypt(stored);
        } catch (IllegalStateException unreadable) {
            throw KeyDistributionException.codeUnreadable();
        }
    }

    /** For an export row: a code that cannot be shown leaves its cell empty instead of failing the whole batch. */
    public String readOrBlank(String stored) {
        if (stored == null || stored.isBlank() || !cipher.available()) return "";
        try {
            return cipher.decrypt(stored);
        } catch (IllegalStateException unreadable) {
            return "";
        }
    }

    public void requireAvailable() {
        if (!cipher.available()) throw KeyDistributionException.cipherUnavailable();
    }

    /** The code as screens list it: only the last four characters are real. */
    public static String mask(String hint) {
        return "IP-****-****-" + (hint == null ? "****" : hint);
    }
}
