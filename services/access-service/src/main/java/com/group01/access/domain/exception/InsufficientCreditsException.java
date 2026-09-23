package com.group01.access.domain.exception;

import java.util.UUID;

public class InsufficientCreditsException extends RuntimeException {

    public InsufficientCreditsException(UUID subscriptionId, int remainingCredits) {
        super(String.format("Subscription %s has insufficient human grading credits: remaining=%d", subscriptionId, remainingCredits));
    }
}
