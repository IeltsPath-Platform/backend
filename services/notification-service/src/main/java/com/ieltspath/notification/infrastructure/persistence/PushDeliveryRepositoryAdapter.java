package com.ieltspath.notification.infrastructure.persistence;

import com.ieltspath.notification.domain.repository.PushDeliveryRepository;
import com.ieltspath.notification.domain.vo.PushDelivery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class PushDeliveryRepositoryAdapter implements PushDeliveryRepository {
    private final JdbcTemplate jdbc;

    public PushDeliveryRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** One auto-committed statement: rows are claimed with SKIP LOCKED and the lock is gone before anything is sent. */
    @Override
    public List<PushDelivery> claimDue(int limit, Duration lease) {
        return jdbc.query("""
                WITH due AS (
                    SELECT id FROM notification_deliveries WHERE status='PENDING' AND next_attempt_at<=now()
                    ORDER BY next_attempt_at LIMIT ? FOR UPDATE SKIP LOCKED),
                claimed AS (
                    UPDATE notification_deliveries d SET attempts=d.attempts+1, next_attempt_at=now()+make_interval(secs => ?)
                    FROM due WHERE d.id=due.id RETURNING d.id, d.notification_id, d.installation_id, d.attempts)
                SELECT c.id, c.attempts, p.token, n.id AS notification_id, n.event_type, n.post_id, n.comment_id, n.grading_job_id
                FROM claimed c JOIN notifications n ON n.id=c.notification_id
                LEFT JOIN push_devices p ON p.installation_id=c.installation_id AND p.user_id=n.recipient_id
                """, (rs, row) -> new PushDelivery(rs.getObject("id", UUID.class), rs.getInt("attempts"),
                rs.getString("token"), rs.getObject("notification_id", UUID.class), rs.getString("event_type"),
                rs.getObject("post_id", UUID.class), rs.getObject("comment_id", UUID.class),
                rs.getObject("grading_job_id", UUID.class)), limit, lease.toSeconds());
    }

    @Override
    public void markSent(UUID id, String providerMessageId) {
        jdbc.update("UPDATE notification_deliveries SET status='SENT', provider_message_id=?, last_error=NULL, sent_at=now() WHERE id=?",
                providerMessageId, id);
    }

    @Override
    public void retryAt(UUID id, Instant nextAttemptAt, String error) {
        jdbc.update("UPDATE notification_deliveries SET next_attempt_at=?, last_error=? WHERE id=?",
                Timestamp.from(nextAttemptAt), error, id);
    }

    @Override
    public void markFailed(UUID id, String error) {
        jdbc.update("UPDATE notification_deliveries SET status='FAILED', last_error=? WHERE id=?", error, id);
    }
}
