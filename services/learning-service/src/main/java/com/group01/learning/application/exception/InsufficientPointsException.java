package com.group01.learning.application.exception;

/** Access Service refused a debit because the learner's balance is too low. */
public class InsufficientPointsException extends RuntimeException {
    public InsufficientPointsException() {
        super("Insufficient points");
    }
}