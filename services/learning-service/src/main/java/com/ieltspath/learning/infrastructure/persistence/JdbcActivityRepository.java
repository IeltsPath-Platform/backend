package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.ActivityRepository;
import com.ieltspath.learning.domain.vo.ActivityCounts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Activity counts in UTC. Every interval is written in hours and every week starts at UTC midnight on Monday, so the
 * database's own time zone never changes which day or week a learner falls in.
 */
@Repository
public class JdbcActivityRepository implements ActivityRepository {
    private final JdbcTemplate jdbc;

    public JdbcActivityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** DAU, WAU and MAU for each day in one statement: the correlated counts run in the database. */
    @Override
    public List<ActivityCounts.Daily> daily(Instant fromDay, Instant toDay) {
        return jdbc.query("""
                SELECT d.day,
                  (SELECT count(DISTINCT e.user_id) FROM question_attempt_events e
                   WHERE e.occurred_at >= d.day AND e.occurred_at < d.day + interval '24 hours') AS dau,
                  (SELECT count(DISTINCT e.user_id) FROM question_attempt_events e
                   WHERE e.occurred_at >= d.day - interval '144 hours' AND e.occurred_at < d.day + interval '24 hours') AS wau,
                  (SELECT count(DISTINCT e.user_id) FROM question_attempt_events e
                   WHERE e.occurred_at >= d.day - interval '696 hours' AND e.occurred_at < d.day + interval '24 hours') AS mau
                FROM generate_series(CAST(? AS timestamptz), CAST(? AS timestamptz), interval '24 hours') AS d(day)
                ORDER BY d.day
                """, (rows, index) -> new ActivityCounts.Daily(
                rows.getTimestamp("day").toInstant().atZone(ZoneOffset.UTC).toLocalDate(),
                rows.getLong("dau"), rows.getLong("wau"), rows.getLong("mau")),
                Timestamp.from(fromDay), Timestamp.from(toDay));
    }

    /**
     * Each learner's first-answer week (Monday, UTC), and for weeks 1, 2, 4 and 8 whether they answered in that week.
     * Week N is the 168 hours from N weeks after the cohort week starts.
     */
    @Override
    public List<ActivityCounts.Cohort> cohorts(Instant fromDay, Instant toDay) {
        return jdbc.query("""
                WITH firsts AS (
                    SELECT user_id,
                           date_trunc('week', min(occurred_at) AT TIME ZONE 'UTC') AT TIME ZONE 'UTC' AS cohort
                    FROM question_attempt_events GROUP BY user_id
                )
                SELECT f.cohort AS week_start, count(*) AS size,
                  count(*) FILTER (WHERE EXISTS (SELECT 1 FROM question_attempt_events e WHERE e.user_id = f.user_id
                    AND e.occurred_at >= f.cohort + interval '168 hours' AND e.occurred_at < f.cohort + interval '336 hours')) AS week1,
                  count(*) FILTER (WHERE EXISTS (SELECT 1 FROM question_attempt_events e WHERE e.user_id = f.user_id
                    AND e.occurred_at >= f.cohort + interval '336 hours' AND e.occurred_at < f.cohort + interval '504 hours')) AS week2,
                  count(*) FILTER (WHERE EXISTS (SELECT 1 FROM question_attempt_events e WHERE e.user_id = f.user_id
                    AND e.occurred_at >= f.cohort + interval '672 hours' AND e.occurred_at < f.cohort + interval '840 hours')) AS week4,
                  count(*) FILTER (WHERE EXISTS (SELECT 1 FROM question_attempt_events e WHERE e.user_id = f.user_id
                    AND e.occurred_at >= f.cohort + interval '1344 hours' AND e.occurred_at < f.cohort + interval '1512 hours')) AS week8
                FROM firsts f
                WHERE f.cohort >= ? AND f.cohort < ?
                GROUP BY f.cohort
                ORDER BY f.cohort
                """, (rows, index) -> new ActivityCounts.Cohort(rows.getTimestamp("week_start").toInstant(),
                rows.getLong("size"), rows.getLong("week1"), rows.getLong("week2"), rows.getLong("week4"),
                rows.getLong("week8")),
                Timestamp.from(fromDay), Timestamp.from(toDay));
    }
}
