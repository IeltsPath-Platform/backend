package com.ieltspath.notification.infrastructure.persistence;

import com.ieltspath.notification.application.LearningMailRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLearningMailRepository implements LearningMailRepository {
    private static final String LEARNING = "learning";

    private final JdbcTemplate jdbc;

    public JdbcLearningMailRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsertContact(UUID userId, String email, String displayName, Instant occurredAt) {
        jdbc.update("""
                INSERT INTO user_contacts (user_id, email, display_name, occurred_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET email = EXCLUDED.email, display_name = EXCLUDED.display_name,
                    occurred_at = EXCLUDED.occurred_at
                WHERE user_contacts.occurred_at < EXCLUDED.occurred_at
                """, userId, email, displayName, Timestamp.from(occurredAt));
    }

    @Override
    public Optional<Contact> findContact(UUID userId) {
        return jdbc.query("SELECT email, display_name FROM user_contacts WHERE user_id = ?",
                (rs, row) -> new Contact(rs.getString("email"), rs.getString("display_name")), userId)
                .stream().findFirst();
    }

    @Override
    public boolean learningEnabled(UUID userId) {
        return jdbc.query("SELECT enabled FROM mail_preferences WHERE user_id = ? AND category = ?",
                (rs, row) -> rs.getBoolean("enabled"), userId, LEARNING).stream().findFirst().orElse(true);
    }

    @Override
    public void setLearningEnabled(UUID userId, boolean enabled, Instant changedAt) {
        jdbc.update("""
                INSERT INTO mail_preferences (user_id, category, enabled, updated_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, category) DO UPDATE SET enabled = EXCLUDED.enabled, updated_at = EXCLUDED.updated_at
                """, userId, LEARNING, enabled, Timestamp.from(changedAt));
    }

    @Override
    public void addPendingExaminerJob(UUID gradingJobId, UUID examinerId, Instant createdAt) {
        jdbc.update("""
                INSERT INTO examiner_job_digest (grading_job_id, examiner_id, created_at) VALUES (?, ?, ?)
                ON CONFLICT (grading_job_id) DO NOTHING
                """, gradingJobId, examinerId, Timestamp.from(createdAt));
    }

    @Override
    public List<UUID> examinersWithPendingJobs() {
        return jdbc.queryForList("SELECT DISTINCT examiner_id FROM examiner_job_digest", UUID.class);
    }

    @Override
    public List<UUID> claimPendingExaminerJobs(UUID examinerId) {
        return jdbc.queryForList("DELETE FROM examiner_job_digest WHERE examiner_id = ? RETURNING grading_job_id",
                UUID.class, examinerId);
    }
}
