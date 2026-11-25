package com.ieltspath.access.application.port;

/**
 * Keeps the full code of an activation key so a sale can look at it again and a manager can export a batch. Only the
 * hash is compared at activation, so a key still works when its code cannot be decrypted.
 */
public interface KeyCodeCipher {

    /** False when no secret is configured: keys can still be activated but no batch can be made. */
    boolean available();

    /** The encrypted form to store; never contains the plain code. */
    String encrypt(String code);

    /** The plain code, or throws {@link IllegalStateException} when the text was altered or the secret is not the one used. */
    String decrypt(String stored);
}
