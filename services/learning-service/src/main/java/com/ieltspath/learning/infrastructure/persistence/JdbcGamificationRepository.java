package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.GamificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcGamificationRepository implements GamificationRepository {
    private final JdbcTemplate jdbc;

    public JdbcGamificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> timezone(UUID userId) {
        return jdbc.query("SELECT timezone FROM learner_settings WHERE user_id = ?",
                (rows, index) -> rows.getString("timezone"), userId).stream().findFirst();
    }

    @Override
    public void saveTimezone(UUID userId, String timezone) {
        jdbc.update("""
                INSERT INTO learner_settings (user_id, timezone) VALUES (?, ?)
                ON CONFLICT (user_id) DO UPDATE SET timezone = EXCLUDED.timezone, updated_at = CURRENT_TIMESTAMP
                """, userId, timezone);
    }

    @Override
    public long countedAnswerXpOn(UUID userId, LocalDate day) {
        Long total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(amount), 0) FROM xp_ledger
                WHERE user_id = ? AND local_date = ? AND counted AND source = 'QUESTION'
                """, Long.class, userId, Date.valueOf(day));
        return total == null ? 0 : total;
    }

    @Override
    public boolean[] insertLedger(UUID userId, List<LedgerEntry> entries) {
        if (entries.isEmpty()) return new boolean[0];
        int[][] batches = jdbc.batchUpdate("""
                INSERT INTO xp_ledger (id, user_id, amount, source, source_ref, local_date, counted)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, source, source_ref) DO NOTHING
                """, entries, entries.size(), (statement, entry) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setInt(3, entry.amount());
            statement.setString(4, entry.source());
            statement.setString(5, entry.sourceRef());
            statement.setDate(6, Date.valueOf(entry.localDate()));
            statement.setBoolean(7, entry.counted());
        });
        // One batch holds every row, so the first batch's counts line up with the entries.
        int[] inserted = batches.length == 0 ? new int[0] : batches[0];
        boolean[] fresh = new boolean[inserted.length];
        for (int index = 0; index < inserted.length; index++) fresh[index] = inserted[index] == 1;
        return fresh;
    }

    @Override
    public int totalXp(UUID userId) {
        return jdbc.query("SELECT total_xp FROM learner_xp WHERE user_id = ?",
                (rows, index) -> rows.getInt("total_xp"), userId).stream().findFirst().orElse(0);
    }

    @Override
    public void saveTotals(UUID userId, int totalXp, int level) {
        jdbc.update("""
                INSERT INTO learner_xp (user_id, total_xp, level) VALUES (?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET total_xp = EXCLUDED.total_xp, level = EXCLUDED.level,
                    updated_at = CURRENT_TIMESTAMP
                """, userId, totalXp, level);
    }
}
