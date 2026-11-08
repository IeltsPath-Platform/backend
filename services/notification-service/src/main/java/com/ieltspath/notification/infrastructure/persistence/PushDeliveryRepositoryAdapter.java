package com.ieltspath.notification.infrastructure.persistence;

import com.ieltspath.notification.domain.repository.PushDeliveryRepository;
import com.ieltspath.notification.domain.vo.Notification;
import com.ieltspath.notification.domain.vo.PushDeliveryJob;
import com.ieltspath.notification.domain.vo.PushSendSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class PushDeliveryRepositoryAdapter implements PushDeliveryRepository {
    private static final int HOUSEKEEPING_BATCH_SIZE = 100;
    private final JdbcTemplate jdbc;

    public PushDeliveryRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int enqueueFor(Notification notification) {
        List<DeviceSnapshot> devices = jdbc.query("""
                SELECT id,registration_version FROM notification_devices
                WHERE user_id=? AND active AND provider='FCM' AND platform='ANDROID' ORDER BY id
                """, (rs, row) -> new DeviceSnapshot(rs.getObject("id", UUID.class), rs.getLong("registration_version")),
                notification.recipientId());
        if (devices.isEmpty()) return 0;
        Instant enqueuedAt = Instant.now();
        Timestamp created = Timestamp.from(enqueuedAt);
        int[][] inserted = jdbc.batchUpdate("""
                INSERT INTO notification_push_deliveries(id,notification_id,recipient_id,device_id,registration_version,
                    status,attempt_count,next_attempt_at,expires_at,created_at,updated_at)
                VALUES (?,?,?,?,?,'PENDING',0,?,?,?,?) ON CONFLICT (notification_id,device_id,registration_version) DO NOTHING
                """, devices, devices.size(), (ps, device) -> {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, notification.id());
                    ps.setObject(3, notification.recipientId());
                    ps.setObject(4, device.id());
                    ps.setLong(5, device.registrationVersion());
                    ps.setTimestamp(6, created);
                    ps.setTimestamp(7, Timestamp.from(enqueuedAt.plusSeconds(24 * 60 * 60)));
                    ps.setTimestamp(8, created);
                    ps.setTimestamp(9, created);
                });
        int total = 0;
        for (int[] batch : inserted) for (int count : batch) if (count > 0) total += count;
        return total;
    }

    @Override
    @Transactional
    public List<PushDeliveryJob> claimDue(Instant now, int batchSize, Instant leaseUntil, int maxAttempts) {
        jdbc.update("""
                UPDATE notification_push_deliveries SET status='CANCELLED', cancellation_reason='EXPIRED',
                    claim_id=NULL, lease_until=NULL, updated_at=?
                WHERE id IN (SELECT id FROM notification_push_deliveries
                    WHERE expires_at<=? AND status IN ('PENDING','RETRY','IN_FLIGHT')
                    ORDER BY expires_at,id LIMIT ?)
                """, Timestamp.from(now), Timestamp.from(now), HOUSEKEEPING_BATCH_SIZE);
        jdbc.update("""
                UPDATE notification_push_deliveries SET status='PARKED', last_error_code='ATTEMPTS_EXHAUSTED',
                    claim_id=NULL, lease_until=NULL, updated_at=?
                WHERE id IN (SELECT id FROM notification_push_deliveries
                    WHERE attempt_count>=? AND status IN ('PENDING','RETRY') AND expires_at>?
                    ORDER BY attempt_count,expires_at,id LIMIT ?)
                """, Timestamp.from(now), maxAttempts, Timestamp.from(now), HOUSEKEEPING_BATCH_SIZE);
        jdbc.update("""
                UPDATE notification_push_deliveries SET status='PARKED', last_error_code='ATTEMPTS_EXHAUSTED',
                    claim_id=NULL, lease_until=NULL, updated_at=?
                WHERE id IN (SELECT id FROM notification_push_deliveries
                    WHERE attempt_count>=? AND status='IN_FLIGHT' AND lease_until<=? AND expires_at>?
                    ORDER BY lease_until,id LIMIT ?)
                """, Timestamp.from(now), maxAttempts, Timestamp.from(now), Timestamp.from(now), HOUSEKEEPING_BATCH_SIZE);
        jdbc.update("""
                UPDATE notification_push_deliveries SET status='RETRY', claim_id=NULL, lease_until=NULL,
                    next_attempt_at=?, updated_at=?
                WHERE id IN (SELECT id FROM notification_push_deliveries
                    WHERE status='IN_FLIGHT' AND attempt_count<? AND lease_until<=? AND expires_at>?
                    ORDER BY lease_until,id LIMIT ?)
                """, Timestamp.from(now), Timestamp.from(now), maxAttempts, Timestamp.from(now),
                Timestamp.from(now), HOUSEKEEPING_BATCH_SIZE);
        return jdbc.query("""
                WITH due AS (
                    SELECT id FROM notification_push_deliveries
                    WHERE status IN ('PENDING','RETRY') AND next_attempt_at<=? AND expires_at>?
                    ORDER BY next_attempt_at,id FOR UPDATE SKIP LOCKED LIMIT ?
                )
                UPDATE notification_push_deliveries d SET status='IN_FLIGHT', attempt_count=d.attempt_count+1,
                    claim_id=gen_random_uuid(), lease_until=?, updated_at=?
                FROM due WHERE d.id=due.id
                RETURNING d.id,d.notification_id,d.recipient_id,d.device_id,d.registration_version,d.attempt_count,d.claim_id,d.expires_at
                """, (rs, row) -> new PushDeliveryJob(rs.getObject("id", UUID.class),
                rs.getObject("notification_id", UUID.class), rs.getObject("recipient_id", UUID.class),
                rs.getObject("device_id", UUID.class), rs.getLong("registration_version"),
                rs.getInt("attempt_count"), rs.getObject("claim_id", UUID.class), rs.getTimestamp("expires_at").toInstant()),
                Timestamp.from(now), Timestamp.from(now), batchSize, Timestamp.from(leaseUntil), Timestamp.from(now));
    }

    @Override
    public PushSendSnapshot loadCurrentBinding(PushDeliveryJob job) {
        List<PushSendSnapshot> snapshots = jdbc.query("""
                SELECT d.id AS delivery_id,d.notification_id,d.recipient_id,d.device_id,d.registration_version,
                    d.attempt_count,d.claim_id,d.expires_at,n.actor_id,n.event_type,n.post_id,n.comment_id,n.grading_job_id,
                    n.created_at,n.read_at,device.token,device.token_hash
                FROM notification_push_deliveries d
                JOIN notification_devices device ON device.id=d.device_id
                JOIN notifications n ON n.id=d.notification_id
                WHERE d.id=? AND d.claim_id=? AND d.status='IN_FLIGHT' AND device.active
                    AND device.user_id=d.recipient_id AND device.registration_version=d.registration_version
                """, (rs, row) -> {
            Notification notification = new Notification(rs.getObject("notification_id", UUID.class),
                    rs.getObject("actor_id", UUID.class), rs.getObject("recipient_id", UUID.class),
                    rs.getString("event_type"), rs.getObject("post_id", UUID.class),
                    rs.getObject("comment_id", UUID.class), rs.getObject("grading_job_id", UUID.class),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant());
            PushDeliveryJob current = new PushDeliveryJob(rs.getObject("delivery_id", UUID.class),
                    rs.getObject("notification_id", UUID.class), rs.getObject("recipient_id", UUID.class),
                    rs.getObject("device_id", UUID.class), rs.getLong("registration_version"),
                    rs.getInt("attempt_count"), rs.getObject("claim_id", UUID.class), rs.getTimestamp("expires_at").toInstant());
            return new PushSendSnapshot(current, notification, rs.getString("token"), rs.getString("token_hash"));
        }, job.id(), job.claimId());
        return snapshots.stream().findFirst().orElse(null);
    }

    @Override
    public boolean markSent(PushDeliveryJob job, Instant now, String providerMessageId) {
        return jdbc.update("""
                UPDATE notification_push_deliveries SET status='SENT', sent_at=?, provider_message_id=?,
                    claim_id=NULL, lease_until=NULL, updated_at=? WHERE id=? AND status='IN_FLIGHT' AND claim_id=?
                    AND EXISTS (SELECT 1 FROM notification_devices device WHERE device.id=notification_push_deliveries.device_id
                        AND device.user_id=notification_push_deliveries.recipient_id AND device.registration_version=notification_push_deliveries.registration_version
                        AND device.active)
                """, Timestamp.from(now), providerMessageId, Timestamp.from(now), job.id(), job.claimId()) > 0;
    }

    @Override
    public boolean scheduleRetry(PushDeliveryJob job, Instant now, Instant nextAttemptAt, String errorCode) {
        return jdbc.update("""
                UPDATE notification_push_deliveries SET status='RETRY', next_attempt_at=?, last_error_code=?,
                    claim_id=NULL, lease_until=NULL, updated_at=?
                WHERE id=? AND status='IN_FLIGHT' AND claim_id=?
                """, Timestamp.from(nextAttemptAt), errorCode, Timestamp.from(now), job.id(), job.claimId()) > 0;
    }

    @Override
    public boolean park(PushDeliveryJob job, Instant now, String errorCode) {
        return jdbc.update("""
                UPDATE notification_push_deliveries SET status='PARKED', last_error_code=?,
                    claim_id=NULL, lease_until=NULL, updated_at=?
                WHERE id=? AND status='IN_FLIGHT' AND claim_id=?
                """, errorCode, Timestamp.from(now), job.id(), job.claimId()) > 0;
    }

    @Override
    public boolean cancel(PushDeliveryJob job, Instant now, String reason) {
        return jdbc.update("""
                UPDATE notification_push_deliveries SET status='CANCELLED', cancellation_reason=?,
                    claim_id=NULL, lease_until=NULL, updated_at=?
                WHERE id=? AND status='IN_FLIGHT' AND claim_id=?
                """, reason, Timestamp.from(now), job.id(), job.claimId()) > 0;
    }

    @Override
    @Transactional
    public boolean disableInvalidBindingAndCancel(PushDeliveryJob job, String tokenHash, Instant now) {
        jdbc.update("""
                UPDATE notification_devices SET active=false, registration_version=registration_version+1,
                    disabled_at=?, updated_at=? WHERE id=? AND user_id=? AND registration_version=?
                    AND token_hash=? AND active
                """, Timestamp.from(now), Timestamp.from(now), job.deviceId(), job.recipientId(),
                job.registrationVersion(), tokenHash);
        return cancel(job, now, "UNREGISTERED");
    }

    @Override
    public long countPending() {
        Long result = jdbc.queryForObject("SELECT count(*) FROM notification_push_deliveries WHERE status IN ('PENDING','RETRY','IN_FLIGHT')",
                Long.class);
        return result == null ? 0 : result;
    }

    @Override
    public Instant oldestPendingAt() {
        List<Timestamp> result = jdbc.query("SELECT min(created_at) FROM notification_push_deliveries WHERE status IN ('PENDING','RETRY','IN_FLIGHT')",
                (rs, row) -> rs.getTimestamp(1));
        return result.isEmpty() || result.getFirst() == null ? null : result.getFirst().toInstant();
    }

    private record DeviceSnapshot(UUID id, long registrationVersion) { }
}
