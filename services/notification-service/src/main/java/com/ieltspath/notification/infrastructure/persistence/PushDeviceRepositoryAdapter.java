package com.ieltspath.notification.infrastructure.persistence;

import com.ieltspath.notification.domain.repository.PushDeviceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class PushDeviceRepositoryAdapter implements PushDeviceRepository {
    private final JdbcTemplate jdbc;

    public PushDeviceRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void register(UUID userId, String installationId, String platform, String token, Instant now) {
        jdbc.update("DELETE FROM push_devices WHERE token=? AND installation_id<>?", token, installationId);
        jdbc.update("""
                INSERT INTO push_devices(installation_id,user_id,platform,token,created_at,updated_at) VALUES (?,?,?,?,?,?)
                ON CONFLICT (installation_id) DO UPDATE
                SET user_id=EXCLUDED.user_id, platform=EXCLUDED.platform, token=EXCLUDED.token, updated_at=EXCLUDED.updated_at
                """, installationId, userId, platform, token, Timestamp.from(now), Timestamp.from(now));
    }

    @Override
    public boolean revoke(UUID userId, String installationId) {
        return jdbc.update("DELETE FROM push_devices WHERE installation_id=? AND user_id=?", installationId, userId) > 0;
    }

    @Override
    public void deleteByToken(String token) {
        jdbc.update("DELETE FROM push_devices WHERE token=?", token);
    }
}
