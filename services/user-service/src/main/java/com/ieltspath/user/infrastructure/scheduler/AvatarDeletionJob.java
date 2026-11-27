package com.ieltspath.user.infrastructure.scheduler;

import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.user.application.port.AvatarStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Removes the avatar objects of deleted accounts. Rows are claimed with a lease under {@code FOR UPDATE SKIP LOCKED},
 * so several instances never work on the same object, and storage is called outside any transaction. A row stays
 * until storage confirms the object is gone: failures are retried with a delay that doubles up to
 * {@code max-backoff-minutes}, and from {@code alert-after-attempts} on every failure is logged as an error.
 */
@Slf4j
@Component
public class AvatarDeletionJob {
    private final JdbcTemplate jdbc;
    private final ObjectProvider<AvatarStoragePort> storageProvider;
    private final long leaseSeconds;
    private final int alertAfterAttempts;
    private final long maxBackoffMinutes;

    public AvatarDeletionJob(JdbcTemplate jdbc,
                             ObjectProvider<AvatarStoragePort> storageProvider,
                             @Value("${user.avatar-deletion.lease-seconds:300}") long leaseSeconds,
                             @Value("${user.avatar-deletion.alert-after-attempts:10}") int alertAfterAttempts,
                             @Value("${user.avatar-deletion.max-backoff-minutes:360}") long maxBackoffMinutes) {
        this.jdbc = jdbc;
        this.storageProvider = storageProvider;
        this.leaseSeconds = leaseSeconds;
        this.alertAfterAttempts = alertAfterAttempts;
        this.maxBackoffMinutes = maxBackoffMinutes;
    }

    record Claim(String objectKey, UUID userId, int attempts) { }

    @Scheduled(fixedDelayString = "${user.avatar-deletion.poll-delay-ms:30000}")
    public void run() {
        AvatarStoragePort storage = storageProvider.getIfAvailable();
        if (storage == null) return;
        for (Claim claim : claim(20)) {
            try {
                storage.delete(claim.objectKey());
                done(claim);
            } catch (StorageException failure) {
                if (failure.reason() == StorageException.Reason.NOT_FOUND) confirmGone(storage, claim);
                else failed(claim, failure.reason().name());
            } catch (RuntimeException failure) {
                failed(claim, failure.getClass().getSimpleName());
            }
        }
    }

    /**
     * A 404 also comes back for a wrong bucket or key, so it only counts as deleted when an existence check, which
     * first confirms the bucket is reachable, says the object is absent.
     */
    private void confirmGone(AvatarStoragePort storage, Claim claim) {
        try {
            if (storage.exists(claim.objectKey())) failed(claim, "STILL_PRESENT");
            else done(claim);
        } catch (StorageException failure) {
            failed(claim, failure.reason().name());
        } catch (RuntimeException failure) {
            failed(claim, failure.getClass().getSimpleName());
        }
    }

    List<Claim> claim(int limit) {
        return jdbc.query("""
                UPDATE pending_avatar_deletions
                SET status = 'PROCESSING', claimed_at = CURRENT_TIMESTAMP, attempts = attempts + 1
                WHERE object_key IN (
                    SELECT object_key FROM pending_avatar_deletions
                    WHERE (status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP)
                       OR (status = 'PROCESSING' AND claimed_at < CURRENT_TIMESTAMP - make_interval(secs => ?))
                    ORDER BY next_attempt_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                RETURNING object_key, user_id, attempts
                """, (rs, row) -> new Claim(rs.getString("object_key"), UUID.fromString(rs.getString("user_id")),
                rs.getInt("attempts")), leaseSeconds, limit);
    }

    private void done(Claim claim) {
        jdbc.update("DELETE FROM pending_avatar_deletions WHERE object_key = ?", claim.objectKey());
    }

    private void failed(Claim claim, String errorCode) {
        long delayMinutes = Math.min(maxBackoffMinutes, 1L << Math.min(claim.attempts(), 20));
        jdbc.update("""
                UPDATE pending_avatar_deletions
                SET status = 'PENDING', claimed_at = NULL, last_error = ?,
                    next_attempt_at = CURRENT_TIMESTAMP + make_interval(mins => ?)
                WHERE object_key = ?
                """, errorCode, (int) delayMinutes, claim.objectKey());
        if (claim.attempts() >= alertAfterAttempts) {
            log.error("Avatar deletion still failing: userId={}, attempts={}, errorCode={}", claim.userId(), claim.attempts(), errorCode);
        } else {
            log.warn("Avatar deletion will be retried: userId={}, attempts={}, errorCode={}", claim.userId(), claim.attempts(), errorCode);
        }
    }
}
