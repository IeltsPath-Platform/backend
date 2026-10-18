package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.BadgeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcBadgeRepository implements BadgeRepository {
    private static final String COLUMNS = "code, name, description, reward_points, active";

    private final JdbcTemplate jdbc;

    public JdbcBadgeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Badge> catalog() {
        return jdbc.query("SELECT " + COLUMNS + " FROM badges ORDER BY code", JdbcBadgeRepository::toBadge);
    }

    @Override
    public Optional<Badge> find(String code) {
        return jdbc.query("SELECT " + COLUMNS + " FROM badges WHERE code = ?", JdbcBadgeRepository::toBadge, code)
                .stream().findFirst();
    }

    @Override
    public boolean setActive(String code, boolean active) {
        return jdbc.update("UPDATE badges SET active = ? WHERE code = ?", active, code) > 0;
    }

    @Override
    public Map<String, Instant> awarded(UUID userId) {
        Map<String, Instant> held = new HashMap<>();
        jdbc.query("SELECT badge_code, awarded_at FROM learner_badges WHERE user_id = ?",
                rows -> { held.put(rows.getString("badge_code"), rows.getTimestamp("awarded_at").toInstant()); },
                userId);
        return held;
    }

    @Override
    public List<Badge> award(UUID userId, Collection<String> codes, Instant now) {
        if (codes.isEmpty()) return List.of();
        // One statement for the whole family: the primary key makes a badge already held a no-op.
        List<String> fresh = jdbc.query("""
                INSERT INTO learner_badges (user_id, badge_code, awarded_at)
                SELECT ?, b.code, ? FROM badges b WHERE b.code = ANY (CAST(? AS varchar[])) AND b.active
                ON CONFLICT (user_id, badge_code) DO NOTHING
                RETURNING badge_code
                """, (rows, index) -> rows.getString("badge_code"), userId, Timestamp.from(now),
                codes.toArray(new String[0]));
        if (fresh.isEmpty()) return List.of();
        return jdbc.query("SELECT " + COLUMNS + " FROM badges WHERE code = ANY (CAST(? AS varchar[])) ORDER BY code",
                JdbcBadgeRepository::toBadge, (Object) fresh.toArray(new String[0]));
    }

    @Override
    public int maintainedPoints(UUID userId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM kp_mastery_profiles WHERE user_id = ? AND tier = 'MAINTAIN'", Integer.class,
                userId);
        return count == null ? 0 : count;
    }

    @Override
    public int completedQuests(UUID userId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM daily_quests WHERE user_id = ? AND completed_at IS NOT NULL", Integer.class,
                userId);
        return count == null ? 0 : count;
    }

    private static Badge toBadge(ResultSet rows, int index) throws SQLException {
        return new Badge(rows.getString("code"), rows.getString("name"), rows.getString("description"),
                rows.getInt("reward_points"), rows.getBoolean("active"));
    }
}
