package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.LearnerPlacement;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.vo.BandLevel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLearnerPlacementRepository implements LearnerPlacementRepository {
    private final JdbcTemplate jdbc;

    public JdbcLearnerPlacementRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<LearnerPlacement> find(UUID userId) {
        return jdbc.query("""
                SELECT user_id, band, attempt_id, completed_at, updated_at
                FROM learner_placements WHERE user_id = ?
                """, (row, index) -> LearnerPlacement.restore(row.getObject("user_id", UUID.class),
                new BandLevel(row.getBigDecimal("band")), row.getObject("attempt_id", UUID.class),
                row.getTimestamp("completed_at").toInstant(), row.getTimestamp("updated_at").toInstant()),
                userId).stream().findFirst();
    }

    @Override
    public void save(LearnerPlacement placement) {
        jdbc.update("""
                INSERT INTO learner_placements (user_id, band, attempt_id, completed_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET band = EXCLUDED.band, attempt_id = EXCLUDED.attempt_id,
                completed_at = EXCLUDED.completed_at, updated_at = EXCLUDED.updated_at
                """, placement.userId(), placement.band().value(), placement.attemptId(),
                Timestamp.from(placement.completedAt()), Timestamp.from(placement.updatedAt()));
    }
}
