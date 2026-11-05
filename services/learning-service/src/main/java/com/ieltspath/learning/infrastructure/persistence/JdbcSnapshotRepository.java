package com.ieltspath.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.repository.SnapshotRepository;
import com.ieltspath.learning.domain.service.SkillRollup;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSnapshotRepository implements SnapshotRepository {
    /** An answer open longer than this adds only this much to a learner's active time. */
    private static final int ANSWER_CAP_MS = 10 * 60 * 1000;
    private static final TypeReference<List<SkillRollup.Summary>> SKILLS = new TypeReference<>() {};
    private static final TypeReference<Map<String, Integer>> TIERS = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final ObjectMapper json;

    public JdbcSnapshotRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named, ObjectMapper json) {
        this.jdbc = jdbc;
        this.named = named;
        this.json = json;
    }

    @Override
    public List<UUID> usersActiveSince(Instant since) {
        return jdbc.query("SELECT DISTINCT user_id FROM question_attempt_events WHERE occurred_at >= ? ORDER BY user_id",
                (rows, index) -> rows.getObject("user_id", UUID.class), Timestamp.from(since));
    }

    @Override
    public Map<UUID, Activity> activity(Collection<UUID> userIds, Instant from, Instant to) {
        Map<UUID, Activity> result = new HashMap<>();
        if (userIds.isEmpty()) return result;
        named.query("""
                SELECT user_id, count(*) AS answered, count(*) FILTER (WHERE correct) AS correct,
                       COALESCE(SUM(LEAST(duration_ms, :cap)), 0) AS active_ms
                FROM question_attempt_events
                WHERE user_id IN (:ids) AND occurred_at >= :from AND occurred_at < :to
                GROUP BY user_id
                """, new MapSqlParameterSource().addValue("cap", ANSWER_CAP_MS).addValue("ids", userIds)
                .addValue("from", Timestamp.from(from)).addValue("to", Timestamp.from(to)), rows -> {
            result.put(rows.getObject("user_id", UUID.class), new Activity(rows.getInt("answered"),
                    rows.getInt("correct"), rows.getLong("active_ms")));
        });
        return result;
    }

    @Override
    public Map<UUID, List<ProfileRow>> profiles(Collection<UUID> userIds) {
        Map<UUID, List<ProfileRow>> result = new HashMap<>();
        if (userIds.isEmpty()) return result;
        named.query("""
                SELECT user_id, knowledge_point_id, tier, mastery, evidence_count
                FROM kp_mastery_profiles m JOIN knowledge_point_catalog c ON c.kp_id = m.knowledge_point_id
                WHERE user_id IN (:ids) AND c.parent_kp_id IS NULL
                """, new MapSqlParameterSource("ids", userIds), rows -> {
            result.computeIfAbsent(rows.getObject("user_id", UUID.class), key -> new ArrayList<>())
                    .add(new ProfileRow(rows.getObject("knowledge_point_id", UUID.class),
                            MasteryTier.valueOf(rows.getString("tier")), rows.getDouble("mastery"),
                            rows.getInt("evidence_count")));
        });
        return result;
    }

    @Override
    public Map<UUID, BigDecimal> placementBands(Collection<UUID> userIds) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        if (userIds.isEmpty()) return result;
        named.query("SELECT user_id, band FROM learner_placements WHERE user_id IN (:ids)",
                new MapSqlParameterSource("ids", userIds),
                rows -> { result.put(rows.getObject("user_id", UUID.class), rows.getBigDecimal("band")); });
        return result;
    }

    @Override
    public void save(Period period, List<Snapshot> snapshots) {
        if (snapshots.isEmpty()) return;
        String table = table(period);
        String column = column(period);
        jdbc.batchUpdate("INSERT INTO " + table + " (user_id, " + column + ", skills, tier_counts, answered, correct, "
                + "active_minutes, focus_kp_ids, estimated_band) VALUES (?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (user_id, " + column + ") DO UPDATE SET skills = EXCLUDED.skills, "
                + "tier_counts = EXCLUDED.tier_counts, answered = EXCLUDED.answered, correct = EXCLUDED.correct, "
                + "active_minutes = EXCLUDED.active_minutes, focus_kp_ids = EXCLUDED.focus_kp_ids, "
                + "estimated_band = EXCLUDED.estimated_band", snapshots, snapshots.size(), (statement, snapshot) -> {
            statement.setObject(1, snapshot.userId());
            statement.setDate(2, Date.valueOf(snapshot.start()));
            statement.setString(3, write(snapshot.skills()));
            statement.setString(4, write(snapshot.tierCounts()));
            statement.setInt(5, snapshot.answered());
            statement.setInt(6, snapshot.correct());
            statement.setInt(7, snapshot.activeMinutes());
            statement.setArray(8, statement.getConnection().createArrayOf("uuid",
                    snapshot.focusKnowledgePointIds().toArray()));
            statement.setBigDecimal(9, snapshot.estimatedBand());
        });
    }

    @Override
    public Optional<Snapshot> find(Period period, UUID userId, LocalDate start) {
        return jdbc.query(select(period) + " WHERE user_id = ? AND " + column(period) + " = ?", this::toSnapshot,
                userId, Date.valueOf(start)).stream().findFirst();
    }

    @Override
    public List<Snapshot> range(Period period, UUID userId, LocalDate from, LocalDate to) {
        return jdbc.query(select(period) + " WHERE user_id = ? AND " + column(period) + " BETWEEN ? AND ? ORDER BY "
                + column(period), this::toSnapshot, userId, Date.valueOf(from), Date.valueOf(to));
    }

    @Override
    public int purgeBefore(Period period, LocalDate cutoff) {
        return jdbc.update("DELETE FROM " + table(period) + " WHERE " + column(period) + " < ?", Date.valueOf(cutoff));
    }

    private String select(Period period) {
        return "SELECT user_id, " + column(period) + " AS start, skills::text AS skills, tier_counts::text AS tiers, "
                + "answered, correct, active_minutes, focus_kp_ids, estimated_band FROM " + table(period);
    }

    private Snapshot toSnapshot(ResultSet rows, int index) throws SQLException {
        Array focus = rows.getArray("focus_kp_ids");
        List<UUID> focusIds = new ArrayList<>();
        if (focus != null) for (Object id : (Object[]) focus.getArray()) focusIds.add((UUID) id);
        try {
            return new Snapshot(rows.getObject("user_id", UUID.class), rows.getDate("start").toLocalDate(),
                    json.readValue(rows.getString("skills"), SKILLS), json.readValue(rows.getString("tiers"), TIERS),
                    rows.getInt("answered"), rows.getInt("correct"), rows.getInt("active_minutes"), focusIds,
                    rows.getBigDecimal("estimated_band"));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Stored snapshot is not readable", failure);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Snapshot cannot be written", failure);
        }
    }

    // Table and column names come from this closed enum, never from a request.
    private static String table(Period period) {
        return period == Period.WEEK ? "learner_weekly_snapshots" : "learner_monthly_snapshots";
    }

    private static String column(Period period) {
        return period == Period.WEEK ? "week_start" : "month_start";
    }
}
