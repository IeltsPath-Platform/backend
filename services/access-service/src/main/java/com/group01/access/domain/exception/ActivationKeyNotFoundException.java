package com.group01.access.domain.exception;

import java.util.UUID;

public class ActivationKeyNotFoundException extends RuntimeException {

    public ActivationKeyNotFoundException(UUID id) {
        super("Activation key not found with id: " + id);
    }

    public ActivationKeyNotFoundException(String message, boolean isCustomMessage) {
        super(message);
    }
}
