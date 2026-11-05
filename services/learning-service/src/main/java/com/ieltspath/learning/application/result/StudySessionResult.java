package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.aggregate.StudySession;
import java.time.Instant;
import java.util.UUID;

public record StudySessionResult(UUID sessionId,long epoch,Instant serverStartedAt,Instant serverNow,
        Instant leaseExpiresAt,long nextSequence,int heartbeatSeconds,int idleSeconds,int leaseSeconds,
        String status,long totalAcceptedMs) {
    public static StudySessionResult from(StudySession s,Instant now) {
        return new StudySessionResult(s.id(),s.epoch(),s.startedAt(),now,s.leaseExpiresAt(),s.sequence()+1,
                StudySession.HEARTBEAT_SECONDS,StudySession.IDLE_SECONDS,StudySession.LEASE_SECONDS,s.status(),s.totalAcceptedMs());
    }
}
