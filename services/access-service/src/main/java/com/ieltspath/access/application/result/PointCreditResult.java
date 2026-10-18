package com.ieltspath.access.application.result;

import java.util.UUID;

public record PointCreditResult(int granted, boolean capped, long balance, UUID transactionId) {}
