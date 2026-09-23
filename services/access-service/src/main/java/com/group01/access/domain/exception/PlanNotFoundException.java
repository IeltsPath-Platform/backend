package com.group01.access.domain.exception;

import java.util.UUID;

public class PlanNotFoundException extends RuntimeException {

    public PlanNotFoundException(UUID id) {
        super("Plan not found with id: " + id);
    }

    public PlanNotFoundException(String code) {
        super("Plan not found with code: " + code);
    }
}
