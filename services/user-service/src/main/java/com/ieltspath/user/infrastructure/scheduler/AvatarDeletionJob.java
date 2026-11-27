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
 * so several instances never work on the same object, and storage is called outside any transaction. A failed object
 * is tried again after the lease; after the last attempt it is marked FAILED and logged for an operator.
 */
@Slf4j
@Component
public class AvatarDeletionJob {
    private final JdbcTemplate jdbc;
    private final ObjectProvider<AvatarStoragePort> storageProvider;
    private final long leaseSeconds;
    private final int maxAttempts;

    public AvatarDeletionJob(JdbcTemplate jdbc,
                             ObjectProvider<AvatarStoragePort> storageProvider,
                             @Value("${user.avatar-deletion.lease-seconds:300}") long leaseSeconds,
                             @Value("${user.avatar-deletion.max-attempts:10}") int maxAttempts) {
        this.jdbc = jdbc;
        this.storageProvider = storageProvider;
        this.leaseSeconds = leaseSeconds;
        this.maxAttempts = maxAttempts;
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
                if (failure.reason() == StorageException.Reason.NOT_FOUND) done(claim);
                else failed(claim, failure.reason().name());
            } catch (RuntimeException failure) {
                failed(claim, failure.getClass().getSimpleName());
            }
        }
    }

    List<Claim> claim(int limit) {
        return jdbc.query("""
                UPDATE pending_avatar_deletions
                SET status = 'PROCESSING', claimed_at = CURRENT_TIMESTAMP, attempts = attempts + 1
                WHERE object_key IN (
                    SELECT object_key FROM pending_avatar_deletions
                    WHERE (status = 'PENDING'
                           OR (status = 'PROCESSING' AND claimed_at < CURRENT_TIMESTAMP - make_interval(secs => ?)))
                      AND attempts < ?
                    ORDER BY created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                RETURNING object_key, user_id, attempts
                """, (rs, row) -> new Claim(rs.getString("object_key"), UUID.fromString(rs.getString("user_id")),
                rs.getInt("attempts")), leaseSeconds, maxAttempts, limit);
    }

    private void done(Claim claim) {
        jdbc.update("DELETE FROM pending_avatar_deletions WHERE object_key = ?", claim.objectKey());
    }

    private void failed(Claim claim, String errorCode) {
        boolean last = claim.attempts() >= maxAttempts;
        jdbc.update("UPDATE pending_avatar_deletions SET status = ?, last_error = ? WHERE object_key = ?",
                last ? "FAILED" : "PROCESSING", errorCode, claim.objectKey());
        if (last) {
            log.error("Avatar deletion gave up: userId={}, attempts={}, errorCode={}", claim.userId(), claim.attempts(), errorCode);
        } else {
            log.warn("Avatar deletion will be retried: userId={}, attempts={}, errorCode={}", claim.userId(), claim.attempts(), errorCode);
        }
    }
}
