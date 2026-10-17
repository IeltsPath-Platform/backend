package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.Hearts;
import com.ieltspath.learning.domain.repository.HeartsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcHeartsRepository implements HeartsRepository {
    private final JdbcTemplate jdbc;

    public JdbcHeartsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Hearts> find(UUID userId) {
        List<Hearts> found = jdbc.query("SELECT hearts, max_hearts, last_regen_at FROM learner_hearts WHERE user_id = ?",
                (rows, index) -> Hearts.restore(userId, rows.getInt("hearts"), rows.getInt("max_hearts"),
                        rows.getTimestamp("last_regen_at").toInstant()), userId);
        return found.stream().findFirst();
    }

    @Override
    public void save(Hearts hearts) {
        jdbc.update("""
                INSERT INTO learner_hearts (user_id, hearts, max_hearts, last_regen_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET hearts = EXCLUDED.hearts, max_hearts = EXCLUDED.max_hearts,
                    last_regen_at = EXCLUDED.last_regen_at, updated_at = CURRENT_TIMESTAMP
                """, hearts.userId(), hearts.hearts(), hearts.max(), Timestamp.from(hearts.lastRegenAt()));
    }
}
