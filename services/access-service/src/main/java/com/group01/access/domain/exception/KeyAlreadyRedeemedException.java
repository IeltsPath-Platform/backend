package com.group01.access.domain.exception;

public class KeyAlreadyRedeemedException extends RuntimeException {

    public KeyAlreadyRedeemedException(String message) {
        super(message);
    }
}
