package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.QuestRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcQuestRepository implements QuestRepository {
    private static final String COLUMNS = "id, user_id, local_date, type, target_kp_id, required, progress, "
            + "completed_at, xp_reward, points_reward";

    private final JdbcTemplate jdbc;

    public JdbcQuestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Quest> find(UUID userId, LocalDate date) {
        return jdbc.query("SELECT " + COLUMNS + " FROM daily_quests WHERE user_id = ? AND local_date = ? "
                + "ORDER BY CASE type WHEN 'PRACTICE' THEN 1 WHEN 'WEAKNESS' THEN 2 WHEN 'PRACTICE_PLUS' THEN 2 "
                + "ELSE 3 END", JdbcQuestRepository::toQuest, userId, Date.valueOf(date));
    }

    @Override
    public void insertIfAbsent(UUID userId, LocalDate date, List<NewQuest> quests) {
        if (quests.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO daily_quests (id, user_id, local_date, type, target_kp_id, required, xp_reward,
                    points_reward)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, local_date, type) DO NOTHING
                """, quests, quests.size(), (statement, quest) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setDate(3, Date.valueOf(date));
            statement.setString(4, quest.type());
            statement.setObject(5, quest.targetKnowledgePointId());
            statement.setInt(6, quest.required());
            statement.setInt(7, quest.xpReward());
            statement.setInt(8, quest.pointsReward());
        });
    }

    @Override
    public Optional<Quest> addProgress(UUID userId, LocalDate date, String type, int amount, Instant now) {
        if (amount <= 0) return Optional.empty();
        // Only an unfinished quest is touched, so a returned row that is now complete was completed by this call.
        return jdbc.query("UPDATE daily_quests SET progress = LEAST(required, progress + ?), "
                + "completed_at = CASE WHEN progress + ? >= required THEN CAST(? AS timestamptz) ELSE NULL END "
                + "WHERE user_id = ? AND local_date = ? AND type = ? AND completed_at IS NULL RETURNING " + COLUMNS,
                JdbcQuestRepository::toQuest, amount, amount, Timestamp.from(now), userId, Date.valueOf(date), type)
                .stream().filter(Quest::completed).findFirst();
    }

    private static Quest toQuest(ResultSet rows, int index) throws SQLException {
        Timestamp completed = rows.getTimestamp("completed_at");
        return new Quest(rows.getObject("id", UUID.class), rows.getObject("user_id", UUID.class),
                rows.getDate("local_date").toLocalDate(), rows.getString("type"),
                rows.getObject("target_kp_id", UUID.class), rows.getInt("required"), rows.getInt("progress"),
                completed == null ? null : completed.toInstant(), rows.getInt("xp_reward"),
                rows.getInt("points_reward"));
    }
}
