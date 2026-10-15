package com.ieltspath.user.application.result;

import com.ieltspath.user.domain.repository.StudySessionRepository.Receipt;
import java.time.Instant;
import java.util.UUID;

public record StudyHeartbeatResult(UUID sessionId,long epoch,long sequence,long acceptedMs,
        long totalAcceptedMs,Instant leaseExpiresAt,String status) {
    public static StudyHeartbeatResult from(Receipt r) {
        return new StudyHeartbeatResult(r.sessionId(),r.epoch(),r.sequence(),r.acceptedMs(),r.totalAcceptedMs(),r.leaseExpiresAt(),r.status());
    }
}
