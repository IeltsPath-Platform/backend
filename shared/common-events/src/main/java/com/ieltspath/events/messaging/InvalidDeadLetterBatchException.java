package com.ieltspath.events.messaging;

/** A replay asked for a batch size outside the allowed range. */
public class InvalidDeadLetterBatchException extends RuntimeException {
    public InvalidDeadLetterBatchException(int max) {
        super("max must be between 1 and " + DeadLetterReplayer.MAX_PER_CALL + ", got " + max);
    }
}