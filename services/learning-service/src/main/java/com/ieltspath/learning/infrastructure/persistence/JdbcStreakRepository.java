package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.vo.StreakState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcStreakRepository implements StreakRepository {
    private final JdbcTemplate jdbc;

    public JdbcStreakRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<StreakState> find(UUID userId) {
        return jdbc.query("""
                SELECT current_days, longest_days, last_active_date, streak_start, evaluated_through,
                       freeze_available, freeze_eligible_from, frozen_run_end, freeze_run, lost_streak_days
                FROM learner_streaks WHERE user_id = ?
                """, JdbcStreakRepository::toState, userId).stream().findFirst();
    }

    @Override
    public Map<UUID, StreakState> findAll(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<UUID, StreakState> states = new LinkedHashMap<>();
        jdbc.query(connection -> {
            var statement = connection.prepareStatement("""
                    SELECT user_id, current_days, longest_days, last_active_date, streak_start, evaluated_through,
                           freeze_available, freeze_eligible_from, frozen_run_end, freeze_run, lost_streak_days
                    FROM learner_streaks WHERE user_id = ANY (?)
                    """);
            statement.setArray(1, connection.createArrayOf("uuid", userIds.stream().map(UUID::toString).toArray()));
            return statement;
        }, rows -> {
            states.put(rows.getObject("user_id", UUID.class), toState(rows, 0));
        });
        return states;
    }

    @Override
    public void save(UUID userId, StreakState state) {
        jdbc.update("""
                INSERT INTO learner_streaks (user_id, current_days, longest_days, last_active_date, streak_start,
                    evaluated_through, freeze_available, freeze_eligible_from, frozen_run_end, freeze_run,
                    lost_streak_days)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET current_days = EXCLUDED.current_days,
                    longest_days = EXCLUDED.longest_days, last_active_date = EXCLUDED.last_active_date,
                    streak_start = EXCLUDED.streak_start, evaluated_through = EXCLUDED.evaluated_through,
                    freeze_available = EXCLUDED.freeze_available, freeze_eligible_from = EXCLUDED.freeze_eligible_from,
                    frozen_run_end = EXCLUDED.frozen_run_end, freeze_run = EXCLUDED.freeze_run,
                    lost_streak_days = EXCLUDED.lost_streak_days, updated_at = CURRENT_TIMESTAMP
                """, userId, state.currentDays(), state.longestDays(), day(state.lastActive()),
                day(state.streakStart()), day(state.evaluatedThrough()), state.freezeAvailable(),
                day(state.freezeEligibleFrom()), day(state.frozenRunEnd()), state.freezeRun(),
                state.lostStreakDays());
    }

    private static StreakState toState(ResultSet rows, int index) throws SQLException {
        return new StreakState(rows.getInt("current_days"), rows.getInt("longest_days"),
                localDay(rows, "last_active_date"), localDay(rows, "streak_start"),
                localDay(rows, "evaluated_through"), rows.getBoolean("freeze_available"),
                localDay(rows, "freeze_eligible_from"), localDay(rows, "frozen_run_end"),
                rows.getInt("freeze_run"), rows.getInt("lost_streak_days"));
    }

    private static LocalDate localDay(ResultSet rows, String column) throws SQLException {
        Date value = rows.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static Date day(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }
}
