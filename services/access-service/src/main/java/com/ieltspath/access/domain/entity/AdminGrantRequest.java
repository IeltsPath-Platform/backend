package com.ieltspath.access.domain.entity;

import java.util.UUID;

public record AdminGrantRequest(String idempotencyKey, String requestHash, UUID subscriptionId) {}
