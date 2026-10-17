package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.LeagueRepository;
import com.ieltspath.learning.domain.service.LeagueRules;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLeagueRepository implements LeagueRepository {
    private final JdbcTemplate jdbc;

    public JdbcLeagueRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Integer> tierOf(UUID userId) {
        return jdbc.query("SELECT tier FROM learner_league WHERE user_id = ?", (rows, index) -> rows.getInt("tier"),
                userId).stream().findFirst();
    }

    @Override
    public boolean addXp(UUID userId, LocalDate week, int xp, Instant now) {
        return jdbc.update("UPDATE league_members SET weekly_xp = weekly_xp + ?, xp_updated_at = ? "
                + "WHERE week_start = ? AND user_id = ?", xp, Timestamp.from(now), Date.valueOf(week), userId) > 0;
    }

    @Override
    public void join(UUID userId, LocalDate week, int tier, int xp, Instant now) {
        // One lock per week and tier, released at commit: groups of that tier fill one learner at a time.
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext(?))", rows -> { }, "league:" + week + ":" + tier);
        jdbc.update("INSERT INTO league_weeks (week_start) VALUES (?) ON CONFLICT (week_start) DO NOTHING",
                Date.valueOf(week));
        UUID group = jdbc.query("""
                SELECT g.id FROM league_groups g
                WHERE g.week_start = ? AND g.tier = ?
                  AND (SELECT count(*) FROM league_members m WHERE m.group_id = g.id) < ?
                ORDER BY g.created_at LIMIT 1
                """, (rows, index) -> rows.getObject("id", UUID.class), Date.valueOf(week), tier,
                LeagueRules.GROUP_SIZE).stream().findFirst().orElse(null);
        if (group == null) {
            group = UUID.randomUUID();
            jdbc.update("INSERT INTO league_groups (id, week_start, tier) VALUES (?, ?, ?)", group, Date.valueOf(week),
                    tier);
        }
        jdbc.update("INSERT INTO league_members (week_start, user_id, group_id, tier, weekly_xp, xp_updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)", Date.valueOf(week), userId, group, tier, xp, Timestamp.from(now));
    }

    @Override
    public List<Member> table(UUID userId, LocalDate week) {
        return jdbc.query("""
                SELECT user_id, weekly_xp,
                       row_number() OVER (ORDER BY weekly_xp DESC, xp_updated_at ASC, user_id) AS rnk
                FROM league_members
                WHERE group_id = (SELECT group_id FROM league_members WHERE week_start = ? AND user_id = ?)
                ORDER BY rnk
                """, (rows, index) -> new Member(rows.getObject("user_id", UUID.class), rows.getInt("weekly_xp"),
                rows.getInt("rnk")), Date.valueOf(week), userId);
    }

    @Override
    public Optional<Integer> weekTier(UUID userId, LocalDate week) {
        return jdbc.query("SELECT tier FROM league_members WHERE week_start = ? AND user_id = ?",
                (rows, index) -> rows.getInt("tier"), Date.valueOf(week), userId).stream().findFirst();
    }

    @Override
    public List<HistoryRow> history(UUID userId, int weeks) {
        return jdbc.query("""
                SELECT m.week_start, m.tier, m.weekly_xp, m.final_rank, m.outcome
                FROM league_members m JOIN league_weeks w ON w.week_start = m.week_start
                WHERE m.user_id = ? AND w.closed_at IS NOT NULL
                ORDER BY m.week_start DESC LIMIT ?
                """, (rows, index) -> new HistoryRow(rows.getDate("week_start").toLocalDate(), rows.getInt("tier"),
                rows.getInt("weekly_xp"), (Integer) rows.getObject("final_rank"), rows.getString("outcome")),
                userId, weeks);
    }

    @Override
    public List<LocalDate> unclosedWeeksBefore(LocalDate current) {
        return jdbc.query("SELECT week_start FROM league_weeks WHERE closed_at IS NULL AND week_start < ? "
                + "ORDER BY week_start", (rows, index) -> rows.getDate("week_start").toLocalDate(),
                Date.valueOf(current));
    }

    @Override
    public int closeWeek(LocalDate week, int promoteTop, int demoteFrom, Instant now) {
        // Claiming the week first makes a second run a no-op even if two instances race.
        int claimed = jdbc.update("UPDATE league_weeks SET closed_at = ? WHERE week_start = ? AND closed_at IS NULL",
                Timestamp.from(now), Date.valueOf(week));
        if (claimed == 0) return -1;
        int ranked = jdbc.update("""
                WITH ranked AS (
                    SELECT user_id,
                           row_number() OVER (PARTITION BY group_id
                               ORDER BY weekly_xp DESC, xp_updated_at ASC, user_id) AS rnk,
                           count(*) OVER (PARTITION BY group_id) AS size
                    FROM league_members WHERE week_start = ?
                )
                UPDATE league_members m SET final_rank = r.rnk,
                    outcome = CASE WHEN r.rnk <= ? THEN 'PROMOTED'
                                   WHEN r.size >= ? AND r.rnk >= ? THEN 'DEMOTED'
                                   ELSE 'STAYED' END
                FROM ranked r WHERE m.week_start = ? AND m.user_id = r.user_id
                """, Date.valueOf(week), promoteTop, demoteFrom, demoteFrom, Date.valueOf(week));
        jdbc.update("""
                INSERT INTO learner_league (user_id, tier)
                SELECT user_id, CASE outcome WHEN 'PROMOTED' THEN LEAST(7, tier + 1)
                                             WHEN 'DEMOTED' THEN GREATEST(1, tier - 1) ELSE tier END
                FROM league_members WHERE week_start = ?
                ON CONFLICT (user_id) DO UPDATE SET tier = EXCLUDED.tier, updated_at = CURRENT_TIMESTAMP
                """, Date.valueOf(week));
        return ranked;
    }
}
