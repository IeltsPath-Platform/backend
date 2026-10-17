package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcRewardGrantRepository implements RewardGrantRepository {
    private final JdbcTemplate jdbc;

    public JdbcRewardGrantRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enqueue(UUID userId, String reason, String referenceId, int amount) {
        jdbc.update("""
                INSERT INTO reward_grants (id, user_id, amount, reason, reference_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id, reason, reference_id) DO NOTHING
                """, UUID.randomUUID(), userId, amount, reason, referenceId);
    }
}
