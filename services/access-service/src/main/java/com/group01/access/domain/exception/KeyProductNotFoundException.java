package com.group01.access.domain.exception;

import java.util.UUID;

public class KeyProductNotFoundException extends RuntimeException {

    public KeyProductNotFoundException(UUID id) {
        super("Key product not found with id: " + id);
    }

    public KeyProductNotFoundException(String code) {
        super("Key product not found with code: " + code);
    }
}
