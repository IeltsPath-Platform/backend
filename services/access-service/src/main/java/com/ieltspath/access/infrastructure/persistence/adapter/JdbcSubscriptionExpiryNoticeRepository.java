package com.ieltspath.access.infrastructure.persistence.adapter;

import com.ieltspath.access.domain.repository.SubscriptionExpiryNoticeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcSubscriptionExpiryNoticeRepository implements SubscriptionExpiryNoticeRepository {
    private static final int SCAN_LIMIT = 500;

    private final JdbcTemplate jdbc;

    public JdbcSubscriptionExpiryNoticeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<DueSubscription> findActiveEndingBefore(Instant cutoff) {
        return jdbc.query("""
                SELECT s.id, s.user_id, s.ends_at FROM subscriptions s
                WHERE s.status = 'ACTIVE' AND s.ends_at IS NOT NULL AND s.ends_at <= ?
                  AND NOT EXISTS (SELECT 1 FROM subscription_expiry_notices n
                                  WHERE n.subscription_id = s.id AND n.stage = 'EXPIRED')
                ORDER BY s.ends_at
                LIMIT ?
                """, (rs, row) -> new DueSubscription(rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class), rs.getTimestamp("ends_at").toInstant()),
                Timestamp.from(cutoff), SCAN_LIMIT);
    }

    @Override
    public boolean claim(UUID subscriptionId, String stage, Instant claimedAt) {
        return jdbc.update("""
                INSERT INTO subscription_expiry_notices (subscription_id, stage, sent_at) VALUES (?, ?, ?)
                ON CONFLICT (subscription_id, stage) DO NOTHING
                """, subscriptionId, stage, Timestamp.from(claimedAt)) == 1;
    }
}
