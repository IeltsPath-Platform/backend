package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.ExaminerDashboardStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class JdbcExaminerDashboardStore implements ExaminerDashboardStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcExaminerDashboardStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Counts counts(UUID examinerId, Instant reminderBefore, Instant overdueBefore, Instant todayStart, Instant weekStart) {
        return jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE j.status='QUEUED' AND hr.draft_payload IS NULL) AS queued,
                       count(*) FILTER (WHERE j.status='PROCESSING'
                                          OR (j.status='QUEUED' AND hr.draft_payload IS NOT NULL)) AS processing,
                       count(*) FILTER (WHERE j.status IN ('QUEUED','PROCESSING') AND j.requested_at<=:reminderBefore
                                          AND j.requested_at>:overdueBefore) AS due_soon,
                       count(*) FILTER (WHERE j.status IN ('QUEUED','PROCESSING') AND j.requested_at<=:overdueBefore) AS overdue,
                       count(*) FILTER (WHERE j.status='COMPLETED' AND j.completed_at>=:todayStart) AS completed_today,
                       count(*) FILTER (WHERE j.status='COMPLETED' AND j.completed_at>=:weekStart) AS completed_week
                FROM grading_jobs j
                LEFT JOIN human_reviews hr ON hr.grading_job_id=j.id
                WHERE j.grading_mode='HUMAN' AND COALESCE(j.reason,'')<>'QA_SAMPLE' AND j.requested_examiner_id=:id
                """, new MapSqlParameterSource("id", examinerId)
                .addValue("reminderBefore", Timestamp.from(reminderBefore))
                .addValue("overdueBefore", Timestamp.from(overdueBefore))
                .addValue("todayStart", Timestamp.from(todayStart))
                .addValue("weekStart", Timestamp.from(weekStart)),
                (row, index) -> new Counts(row.getInt("queued"), row.getInt("processing"), row.getInt("due_soon"), row.getInt("overdue"),
                        row.getInt("completed_today"), row.getInt("completed_week")));
    }
}
