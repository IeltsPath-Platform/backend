package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.exception.StudyTrackingException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Only server-anchored, recent intervals from the current lease may contribute time. */
public record StudySession(UUID id, UUID userId, UUID clientSessionId, String sourceType, UUID sourceId,
        UUID courseId, String timezone, long epoch, Instant startedAt, Instant leaseExpiresAt,
        long sequence, long watermarkMs, long totalAcceptedMs, String status) {
    public static final int HEARTBEAT_SECONDS = 30, IDLE_SECONDS = 120, LEASE_SECONDS = 90;
    public record Interval(long startOffsetMs, long endOffsetMs) {
        public Interval {
            if (startOffsetMs < 0 || endOffsetMs <= startOffsetMs)
                throw new IllegalArgumentException("Invalid active interval");
        }
    }
    public record Segment(Instant start, Instant end, LocalDate date, String timezone, long milliseconds) {
        public Segment {
            if (milliseconds <= 0) throw new IllegalArgumentException("Segment milliseconds must be positive");
        }
    }
    public record Acceptance(StudySession session, List<Segment> segments, long acceptedMs) {}

    /** PostgreSQL timestamptz stores microsecond precision; normalize before values enter domain state. */
    public static Instant databasePrecision(Instant value) {
        return Objects.requireNonNull(value).truncatedTo(ChronoUnit.MICROS);
    }

    public static StudySession start(UUID user, UUID client, String sourceType, UUID sourceId,
                                     UUID courseId, String timezone, Instant now) {
        Objects.requireNonNull(user); Objects.requireNonNull(client); Objects.requireNonNull(sourceId);
        if (!Set.of("LESSON", "PRACTICE", "REVIEW", "ASSESSMENT").contains(sourceType))
            throw new IllegalArgumentException("Unsupported study source");
        ZoneId.of(timezone);
        Instant preciseNow = databasePrecision(now);
        return new StudySession(UUID.randomUUID(), user, client, sourceType, sourceId, courseId,
                timezone, 1, preciseNow, preciseNow.plusSeconds(LEASE_SECONDS), 0, 0, 0, "OPEN");
    }

    public Acceptance accept(long suppliedEpoch, long suppliedSequence, List<Interval> intervals,
                             Instant now, boolean close) {
        now = databasePrecision(now);
        if (suppliedEpoch != epoch) throw new StudyTrackingException("STALE_EPOCH");
        if (!status.equals("OPEN")) throw new StudyTrackingException("SESSION_CLOSED");
        if (!now.isBefore(leaseExpiresAt)) throw new StudyTrackingException("LEASE_EXPIRED");
        if (suppliedSequence != sequence + 1) throw new StudyTrackingException("INVALID_SEQUENCE");
        if (intervals == null || intervals.size() > 16) throw new IllegalArgumentException("At most 16 intervals are allowed");
        long elapsed = Duration.between(startedAt, now).toMillis();
        long previousEnd = -1, nextWatermark = watermarkMs, accepted = 0;
        var segments = new ArrayList<Segment>();
        ZoneId zone = ZoneId.of(timezone);
        for (Interval interval : intervals) {
            if (interval == null || interval.startOffsetMs() < previousEnd || interval.endOffsetMs() > elapsed
                    || interval.startOffsetMs() < Math.max(0, elapsed - LEASE_SECONDS * 1000L))
                throw new IllegalArgumentException("Intervals must be ordered, recent and no later than server time");
            previousEnd = interval.endOffsetMs();
            long from = Math.max(watermarkMs, interval.startOffsetMs());
            long to = interval.endOffsetMs();
            nextWatermark = Math.max(nextWatermark, to);
            if (to <= from) continue;
            accepted += to - from;
            long allocated = 0;
            Instant cursor = startedAt.plusMillis(from), end = startedAt.plusMillis(to);
            while (cursor.isBefore(end)) {
                LocalDate date = cursor.atZone(zone).toLocalDate();
                Instant midnight = date.plusDays(1).atStartOfDay(zone).toInstant();
                Instant segmentEnd = midnight.isBefore(end) ? midnight : end;
                long segmentMs = segmentEnd.equals(end)
                        ? to - from - allocated
                        : Duration.between(cursor, segmentEnd).toMillis();
                if (segmentMs > 0) segments.add(new Segment(cursor, segmentEnd, date, timezone, segmentMs));
                allocated += segmentMs;
                cursor = segmentEnd;
            }
        }
        StudySession next = new StudySession(id, userId, clientSessionId, sourceType, sourceId, courseId,
                timezone, epoch, startedAt, close ? now : now.plusSeconds(LEASE_SECONDS), suppliedSequence,
                nextWatermark, totalAcceptedMs + accepted, close ? "CLOSED" : "OPEN");
        return new Acceptance(next, List.copyOf(segments), accepted);
    }

    public StudySession closeAt(Instant now) {
        now = databasePrecision(now);
        return new StudySession(id, userId, clientSessionId, sourceType, sourceId, courseId, timezone,
                epoch, startedAt, now, sequence, watermarkMs, totalAcceptedMs, "CLOSED");
    }
}
