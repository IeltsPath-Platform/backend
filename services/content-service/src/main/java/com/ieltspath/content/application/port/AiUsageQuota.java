package com.ieltspath.content.application.port;

import com.ieltspath.content.domain.vo.AiUsageKind;

import java.time.LocalDate;
import java.util.UUID;

public interface AiUsageQuota {
    /** Counts one use if the day's total is below {@code dailyLimit}. Atomic: concurrent requests cannot overshoot. */
    boolean tryConsume(UUID userId, LocalDate day, AiUsageKind kind, int dailyLimit);

    /** Gives back one use, for a draft the system could not produce. Never goes below zero. */
    void refund(UUID userId, LocalDate day, AiUsageKind kind);
}
