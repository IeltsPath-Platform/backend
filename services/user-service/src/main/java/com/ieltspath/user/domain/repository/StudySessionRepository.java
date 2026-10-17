package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.StudySession;
import java.time.*;
import java.util.*;

public interface StudySessionRepository {
    record Receipt(UUID sessionId, long epoch, long sequence, String payloadHash, long acceptedMs,
                   long totalAcceptedMs, Instant leaseExpiresAt, String status) {}
    record Daily(LocalDate date, String timezone, long milliseconds) {}
    record Goal(LocalDate effectiveDate, Integer minutes) {}
    void lockUser(UUID userId);
    void lockGoalOwner(UUID goalId);
    Optional<StudySession> findByClient(UUID userId, UUID clientSessionId);
    Optional<StudySession> find(UUID userId, UUID sessionId);
    Optional<StudySession> findOpen(UUID userId);
    void save(StudySession session);
    Optional<Receipt> receipt(UUID sessionId, long epoch, long sequence);
    void record(StudySession session, Receipt receipt, List<StudySession.Segment> segments);
    Instant trackingStartedAt();
    List<Daily> daily(UUID userId, LocalDate from, LocalDate to, String scope);
    List<Goal> goals(UUID userId, LocalDate from, LocalDate to);
    LocalDate latestRecordedDate(UUID userId, Instant now);
    void supersedePendingGoals(UUID userId, Instant now);
    void scheduleGoal(UUID userId, UUID goalId, Integer minutes, LocalDate date, String timezone, Instant effectiveAt);
}
