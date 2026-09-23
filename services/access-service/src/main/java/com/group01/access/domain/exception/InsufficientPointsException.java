package com.group01.access.domain.exception;

import java.util.UUID;

public class InsufficientPointsException extends RuntimeException {

    public InsufficientPointsException(UUID userId, long currentBalance, long requiredPoints) {
        super(String.format("User %s has insufficient points: balance=%d, required=%d", userId, currentBalance, requiredPoints));
    }
}
