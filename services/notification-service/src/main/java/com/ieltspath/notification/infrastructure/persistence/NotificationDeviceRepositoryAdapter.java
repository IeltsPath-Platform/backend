package com.ieltspath.notification.infrastructure.persistence;

import com.ieltspath.notification.domain.repository.NotificationDeviceRepository;
import com.ieltspath.notification.domain.vo.NotificationDevice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Repository
public class NotificationDeviceRepositoryAdapter implements NotificationDeviceRepository {
    private final JdbcTemplate jdbc;

    public NotificationDeviceRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void register(UUID userId, UUID installationId, String provider, String platform, String token, String appVersion) {
        String tokenHash = sha256(token);
        long installationLock = lockKey(provider + ":installation:" + installationId);
        long tokenLock = lockKey(provider + ":token:" + tokenHash);
        long first = Math.min(installationLock, tokenLock);
        long second = Math.max(installationLock, tokenLock);
        jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> null, first);
        if (second != first) jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> null, second);

        jdbc.update("""
                UPDATE notification_devices SET active=false, registration_version=registration_version+1,
                    disabled_at=?, updated_at=?
                WHERE provider=? AND token_hash=? AND active AND installation_id<>?
                """, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), provider, tokenHash, installationId);

        List<NotificationDevice> current = jdbc.query("""
                SELECT id,user_id,installation_id,provider,platform,token,token_hash,app_version,active,
                    registration_version,updated_at FROM notification_devices
                WHERE provider=? AND installation_id=? FOR UPDATE
                """, this::map, provider, installationId);
        Instant now = Instant.now();
        if (current.isEmpty()) {
            jdbc.update("""
                    INSERT INTO notification_devices(id,user_id,installation_id,provider,platform,token,token_hash,
                        app_version,active,registration_version,created_at,updated_at,last_registered_at)
                    VALUES (?,?,?,?,?,?,?,?,true,1,?,?,?)
                    """, UUID.randomUUID(), userId, installationId, provider, platform, token, tokenHash,
                    appVersion, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
            return;
        }

        NotificationDevice old = current.getFirst();
        boolean bindingChanged = !old.userId().equals(userId) || !old.tokenHash().equals(tokenHash)
                || !old.active() || !old.platform().equals(platform);
        jdbc.update("""
                UPDATE notification_devices SET user_id=?, platform=?, token=?, token_hash=?, app_version=?, active=true,
                    registration_version=registration_version+?, disabled_at=NULL, updated_at=?, last_registered_at=?
                WHERE id=?
                """, userId, platform, token, tokenHash, appVersion, bindingChanged ? 1 : 0,
                Timestamp.from(now), Timestamp.from(now), old.id());
    }

    @Override
    @Transactional
    public void disable(UUID userId, UUID installationId) {
        Instant now = Instant.now();
        jdbc.update("""
                UPDATE notification_devices SET active=false, registration_version=registration_version+1,
                    disabled_at=?, updated_at=?
                WHERE user_id=? AND provider='FCM' AND installation_id=? AND active
                """, Timestamp.from(now), Timestamp.from(now), userId, installationId);
    }

    @Override
    public List<NotificationDevice> findActiveByUser(UUID userId) {
        return jdbc.query("""
                SELECT id,user_id,installation_id,provider,platform,token,token_hash,app_version,active,
                    registration_version,updated_at FROM notification_devices WHERE user_id=? AND active
                ORDER BY id
                """, this::map, userId);
    }

    private NotificationDevice map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new NotificationDevice(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("installation_id", UUID.class), rs.getString("provider"), rs.getString("platform"),
                rs.getString("token"), rs.getString("token_hash"), rs.getString("app_version"), rs.getBoolean("active"),
                rs.getLong("registration_version"), rs.getTimestamp("updated_at").toInstant());
    }

    private String sha256(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private long lockKey(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.nio.ByteBuffer.wrap(digest).getLong();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
