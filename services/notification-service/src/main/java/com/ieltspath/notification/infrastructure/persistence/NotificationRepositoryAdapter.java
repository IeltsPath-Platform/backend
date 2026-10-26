package com.ieltspath.notification.infrastructure.persistence;

import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {
    private final JdbcTemplate jdbc;

    public NotificationRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean insertIfAbsent(UUID sourceEventId, Notification n) {
        return jdbc.update("""
                INSERT INTO notifications(id,source_event_id,recipient_id,actor_id,event_type,post_id,comment_id,grading_job_id,created_at)
                VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING
                """, n.id(), sourceEventId, n.recipientId(), n.actorId(), n.eventType(), n.postId(), n.commentId(),
                n.gradingJobId(), Timestamp.from(n.createdAt())) > 0;
    }

    @Override
    public List<Notification> findByRecipient(UUID recipientId, int size, long offset) {
        return jdbc.query("""
                SELECT id,actor_id,recipient_id,event_type,post_id,comment_id,grading_job_id,created_at,read_at
                FROM notifications WHERE recipient_id=? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> new Notification(rs.getObject("id", UUID.class), rs.getObject("actor_id", UUID.class),
                rs.getObject("recipient_id", UUID.class), rs.getString("event_type"), rs.getObject("post_id", UUID.class),
                rs.getObject("comment_id", UUID.class), rs.getObject("grading_job_id", UUID.class),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant()),
                recipientId, size, offset);
    }

    @Override
    public long countUnread(UUID recipientId) {
        Long count = jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=? AND read_at IS NULL",
                Long.class, recipientId);
        return count == null ? 0 : count;
    }

    @Override
    public boolean markRead(UUID id, UUID recipientId, Instant readAt) {
        return jdbc.update("UPDATE notifications SET read_at=COALESCE(read_at,?) WHERE id=? AND recipient_id=?",
                Timestamp.from(readAt), id, recipientId) > 0;
    }

    @Override
    public int markAllRead(UUID recipientId, Instant readAt) {
        return jdbc.update("UPDATE notifications SET read_at=? WHERE recipient_id=? AND read_at IS NULL",
                Timestamp.from(readAt), recipientId);
    }
}
