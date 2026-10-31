package com.ieltspath.learning.infrastructure.adapter;

import com.ieltspath.learning.domain.aggregate.StudySession;
import com.ieltspath.learning.domain.repository.StudySessionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class StudySessionRepositoryAdapter implements StudySessionRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<StudySession> SESSION = (r, n) -> new StudySession(
            r.getObject("id", UUID.class), r.getObject("user_id", UUID.class), r.getObject("client_session_id", UUID.class),
            r.getString("source_type"), r.getObject("source_id", UUID.class), r.getObject("course_id", UUID.class),
            r.getString("timezone"), r.getLong("epoch"), r.getTimestamp("started_at").toInstant(),
            r.getTimestamp("lease_expires_at").toInstant(), r.getLong("sequence"), r.getLong("watermark_ms"),
            r.getLong("total_accepted_ms"), r.getString("status"));
    public void lockUser(UUID userId) {
        // Its own lock space: study tracking must not wait on, or block, a learner's lesson submissions.
        jdbc.query("SELECT pg_advisory_xact_lock(?)", (r, n) -> 0,
                userId.getMostSignificantBits() ^ userId.getLeastSignificantBits());
    }
    public Optional<String> latestGoalTimezone(UUID userId) {
        return jdbc.query("SELECT timezone FROM study_goal_history WHERE user_id=? AND superseded_at IS NULL "
                + "ORDER BY effective_date DESC, effective_at DESC LIMIT 1", (r, n) -> r.getString(1), userId).stream().findFirst();
    }
    public Optional<String> latestTimezone(UUID userId) {
        return jdbc.query("""
                SELECT timezone FROM (
                    SELECT timezone, started_at AS at FROM study_sessions WHERE user_id=?
                    UNION ALL
                    SELECT timezone, effective_at AS at FROM study_goal_history WHERE user_id=?
                ) used ORDER BY at DESC LIMIT 1
                """, (r, n) -> r.getString(1), userId, userId).stream().findFirst();
    }
    public Optional<StudySession> findByClient(UUID user, UUID client) {
        return jdbc.query("SELECT * FROM study_sessions WHERE user_id=? AND client_session_id=?", SESSION, user, client).stream().findFirst();
    }
    public void lockGoalOwner(UUID goalId) {
        var owners=jdbc.query("SELECT user_id FROM learning_goals WHERE id=?",(r,n)->r.getObject(1,UUID.class),goalId);
        owners.stream().findFirst().ifPresent(this::lockUser);
    }
    public Optional<StudySession> find(UUID user, UUID id) {
        return jdbc.query("SELECT * FROM study_sessions WHERE user_id=? AND id=?", SESSION, user, id).stream().findFirst();
    }
    public Optional<StudySession> findOpen(UUID user) {
        return jdbc.query("SELECT * FROM study_sessions WHERE user_id=? AND status='OPEN'", SESSION, user).stream().findFirst();
    }
    public void save(StudySession s) {
        jdbc.update("""
            INSERT INTO study_sessions(id,user_id,client_session_id,source_type,source_id,course_id,timezone,epoch,
              started_at,lease_expires_at,sequence,watermark_ms,total_accepted_ms,status)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET
              lease_expires_at=EXCLUDED.lease_expires_at,sequence=EXCLUDED.sequence,
              watermark_ms=EXCLUDED.watermark_ms,total_accepted_ms=EXCLUDED.total_accepted_ms,status=EXCLUDED.status
            """, s.id(),s.userId(),s.clientSessionId(),s.sourceType(),s.sourceId(),s.courseId(),s.timezone(),s.epoch(),
                Timestamp.from(s.startedAt()),Timestamp.from(s.leaseExpiresAt()),s.sequence(),s.watermarkMs(),s.totalAcceptedMs(),s.status());
    }
    public Optional<Receipt> receipt(UUID session, long epoch, long sequence) {
        return jdbc.query("SELECT * FROM study_heartbeat_receipts WHERE session_id=? AND epoch=? AND sequence=?",
                (r,n) -> new Receipt(session,epoch,sequence,r.getString("payload_hash"),r.getLong("accepted_ms"),
                        r.getLong("total_accepted_ms"),r.getTimestamp("lease_expires_at").toInstant(),r.getString("status")),
                session,epoch,sequence).stream().findFirst();
    }
    public void record(StudySession s, Receipt receipt, List<StudySession.Segment> segments) {
        jdbc.update("INSERT INTO study_heartbeat_receipts VALUES (?,?,?,?,?,?,?,?)", s.id(),s.epoch(),s.sequence(),
                receipt.payloadHash(),receipt.acceptedMs(),receipt.totalAcceptedMs(),Timestamp.from(receipt.leaseExpiresAt()),receipt.status());
        var rows = new ArrayList<Object[]>();
        record TotalKey(LocalDate date, String timezone, String scope) {}
        var totals = new LinkedHashMap<TotalKey,Long>();
        int index = 0;
        for (var segment : segments) {
            rows.add(new Object[]{s.id(),s.epoch(),s.sequence(),index++,s.userId(),Timestamp.from(segment.start()),
                    Timestamp.from(segment.end()),segment.date(),segment.timezone(),segment.milliseconds()});
            totals.merge(new TotalKey(segment.date(),segment.timezone(),"ALL"),segment.milliseconds(),Long::sum);
            if (s.courseId()!=null) totals.merge(new TotalKey(segment.date(),segment.timezone(),s.courseId().toString()),segment.milliseconds(),Long::sum);
        }
        jdbc.batchUpdate("INSERT INTO study_activity_intervals VALUES (?,?,?,?,?,?,?,?,?,?)",rows);
        var dailyRows = new ArrayList<Object[]>();
        totals.forEach((k,v) -> dailyRows.add(new Object[]{s.userId(),k.date(),k.timezone(),k.scope(),v}));
        jdbc.batchUpdate("""
            INSERT INTO study_daily_totals VALUES (?,?,?,?,?)
            ON CONFLICT(user_id,local_date,scope_key,timezone)
            DO UPDATE SET active_ms=study_daily_totals.active_ms+EXCLUDED.active_ms
            """,dailyRows);
        save(s);
    }
    public Instant trackingStartedAt() {
        return jdbc.queryForObject("SELECT started_at FROM study_tracking_policy WHERE id=1",Timestamp.class).toInstant();
    }
    public List<Daily> daily(UUID user,LocalDate from,LocalDate to,String scope) {
        return jdbc.query("""
            SELECT local_date, CASE WHEN count(DISTINCT timezone)=1 THEN min(timezone) ELSE 'MULTIPLE' END timezone,
              sum(active_ms) active_ms FROM study_daily_totals
            WHERE user_id=? AND local_date BETWEEN ? AND ? AND scope_key=? GROUP BY local_date ORDER BY local_date
            """,(r,n)->new Daily(r.getObject("local_date",LocalDate.class),r.getString("timezone"),r.getLong("active_ms")),user,from,to,scope);
    }
    public List<Goal> goals(UUID user,LocalDate from,LocalDate to) {
        return jdbc.query("""
            (SELECT effective_date,minutes FROM study_goal_history WHERE user_id=? AND effective_date<? AND superseded_at IS NULL
             ORDER BY effective_date DESC LIMIT 1)
            UNION ALL
            (SELECT effective_date,minutes FROM study_goal_history WHERE user_id=? AND effective_date BETWEEN ? AND ? AND superseded_at IS NULL)
            ORDER BY effective_date
            """,(r,n)->new Goal(r.getObject("effective_date",LocalDate.class),r.getObject("minutes",Integer.class)),user,from,user,from,to);
    }
    public LocalDate latestRecordedDate(UUID user, Instant now) {
        return jdbc.queryForObject("""
            SELECT greatest(
              (SELECT max(local_date) FROM study_daily_totals WHERE user_id=?),
              (SELECT max(effective_date) FROM study_goal_history WHERE user_id=? AND effective_at<=? AND superseded_at IS NULL))
            """,LocalDate.class,user,user,Timestamp.from(now));
    }
    public void supersedePendingGoals(UUID user, Instant now) {
        jdbc.update("UPDATE study_goal_history SET superseded_at=? WHERE user_id=? AND effective_at>? AND superseded_at IS NULL",
                Timestamp.from(now),user,Timestamp.from(now));
    }
    public void scheduleGoal(UUID user,UUID goal,Integer minutes,LocalDate date,String zone,Instant at) {
        jdbc.update("""
            INSERT INTO study_goal_history(user_id,effective_date,timezone,effective_at,goal_id,minutes) VALUES (?,?,?,?,?,?)
            ON CONFLICT(user_id,effective_date) DO UPDATE SET timezone=EXCLUDED.timezone,effective_at=EXCLUDED.effective_at,
              goal_id=EXCLUDED.goal_id,minutes=EXCLUDED.minutes,superseded_at=NULL
            """,user,date,zone,Timestamp.from(at),goal,minutes);
    }
}
