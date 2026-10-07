package com.ieltspath.access.domain.exception;

public class PointsActorMismatchException extends RuntimeException {

    public PointsActorMismatchException() {
        super("Points can only be debited for the authenticated user");
    }
}
