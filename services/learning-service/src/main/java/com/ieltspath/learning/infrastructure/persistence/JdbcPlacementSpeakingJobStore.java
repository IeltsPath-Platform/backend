package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.PlacementSpeakingJobStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcPlacementSpeakingJobStore implements PlacementSpeakingJobStore {
    private final JdbcTemplate jdbc;

    public JdbcPlacementSpeakingJobStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public boolean enqueue(UUID eventId, UUID attemptId, UUID userId, String payload, String correlationId, String traceContext) {
        return jdbc.update("""
                INSERT INTO placement_speaking_jobs(event_id,attempt_id,user_id,payload,status,correlation_id,trace_context)
                VALUES (?, ?, ?, CAST(? AS JSONB), 'PENDING', ?, ?)
                ON CONFLICT DO NOTHING
                """, eventId, attemptId, userId, payload, correlationId, traceContext) == 1;
    }

    @Override
    @Transactional
    public List<Job> claimDue(int limit, int maxAttempts, Instant now, Instant staleBefore) {
        return jdbc.query("""
                WITH candidates AS (
                    SELECT event_id FROM placement_speaking_jobs
                    WHERE attempts < ? AND ((status='PENDING' AND next_attempt_at <= ?)
                         OR (status='PROCESSING' AND claimed_at < ?))
                    ORDER BY next_attempt_at, created_at LIMIT ? FOR UPDATE SKIP LOCKED
                )
                UPDATE placement_speaking_jobs j SET status='PROCESSING',attempts=j.attempts+1,
                    claimed_at=?,updated_at=? FROM candidates c WHERE j.event_id=c.event_id
                RETURNING j.event_id,j.attempt_id,j.user_id,j.payload::text,j.attempts,j.correlation_id,j.trace_context
                """, (rs, row) -> new Job(rs.getObject("event_id", UUID.class),rs.getObject("attempt_id", UUID.class),
                rs.getObject("user_id", UUID.class),rs.getString("payload"),rs.getInt("attempts"),
                rs.getString("correlation_id"),rs.getString("trace_context")), maxAttempts,Timestamp.from(now),
                Timestamp.from(staleBefore),limit,Timestamp.from(now),Timestamp.from(now));
    }

    @Override
    @Transactional
    public void retry(UUID eventId, String failureCode, Instant nextAttempt) {
        jdbc.update("""
                UPDATE placement_speaking_jobs SET status='PENDING',failure_code=?,claimed_at=NULL,
                    next_attempt_at=?,updated_at=CURRENT_TIMESTAMP WHERE event_id=? AND status='PROCESSING'
                """, failureCode, Timestamp.from(nextAttempt), eventId);
    }

    @Override
    @Transactional
    public void complete(UUID eventId, String status, String failureCode, String result, OutboxEvent event) {
        int updated = jdbc.update("""
                UPDATE placement_speaking_jobs SET status=?,failure_code=?,result=CAST(? AS JSONB),completed_at=?,claimed_at=NULL,
                    updated_at=CURRENT_TIMESTAMP WHERE event_id=? AND status='PROCESSING'
                """, status, failureCode, result, Timestamp.from(event.createdAt()), eventId);
        if (updated == 0) return;
        jdbc.update("""
                INSERT INTO placement_speaking_outbox(id,event_type,payload,correlation_id,trace_context,created_at)
                VALUES (?,?,CAST(? AS JSONB),?,?,?)
                """, event.id(),event.eventType(),event.payload(),event.correlationId(),event.traceContext(),
                Timestamp.from(event.createdAt()));
    }

    @Override
    public java.util.Optional<Stored> findOwned(UUID attemptId, UUID userId) {
        return jdbc.query("SELECT status,result::text FROM placement_speaking_jobs WHERE attempt_id=? AND user_id=?",
                (rs, row) -> new Stored(rs.getString("status"), rs.getString("result")), attemptId, userId)
                .stream().findFirst();
    }

    @Override
    @Transactional
    public List<OutboxEvent> claimOutbox(int limit) {
        return jdbc.query("""
                SELECT id,event_type,payload::text,created_at,correlation_id,trace_context
                FROM placement_speaking_outbox WHERE published_at IS NULL
                ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED
                """, (rs, row) -> new OutboxEvent(rs.getObject("id",UUID.class),rs.getString("event_type"),
                rs.getString("payload"),rs.getTimestamp("created_at").toInstant(),rs.getString("correlation_id"),
                rs.getString("trace_context")), limit);
    }

    @Override
    @Transactional
    public void markPublished(UUID id) {
        jdbc.update("UPDATE placement_speaking_outbox SET published_at=CURRENT_TIMESTAMP,retry_count=retry_count+1 WHERE id=?", id);
    }

    @Override
    @Transactional
    public void recordPublishFailure(UUID id, String message) {
        String kept = message == null ? "unknown" : message.substring(0,Math.min(2000,message.length()));
        jdbc.update("UPDATE placement_speaking_outbox SET retry_count=retry_count+1,last_error=? WHERE id=?", kept,id);
    }
}
