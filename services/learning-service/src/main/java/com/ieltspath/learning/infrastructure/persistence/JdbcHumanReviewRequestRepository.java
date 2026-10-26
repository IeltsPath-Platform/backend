package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcHumanReviewRequestRepository implements HumanReviewRequestRepository {
    private final JdbcTemplate jdbc;

    public JdbcHumanReviewRequestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> findReviewId(UUID submissionId) {
        return jdbc.queryForList("SELECT review_submission_id FROM human_review_requests WHERE submission_id = ?",
                UUID.class, submissionId).stream().findFirst();
    }

    @Override
    public boolean insert(UUID submissionId, UUID userId, UUID reviewSubmissionId) {
        return jdbc.update("""
                INSERT INTO human_review_requests (submission_id, user_id, review_submission_id) VALUES (?, ?, ?)
                ON CONFLICT (submission_id) DO NOTHING
                """, submissionId, userId, reviewSubmissionId) == 1;
    }

    @Override
    public List<Requested> findByUser(UUID userId, int limit, int offset) {
        return jdbc.query("""
                SELECT r.submission_id, r.review_submission_id, r.requested_at, s.source, s.word_count,
                       s.prompt_snapshot->>'task' AS task, s.prompt_snapshot->>'stem' AS stem
                FROM human_review_requests r JOIN lesson_writing_submissions s ON s.id = r.submission_id
                WHERE r.user_id = ?
                ORDER BY r.requested_at DESC, r.submission_id
                LIMIT ? OFFSET ?
                """, (row, index) -> new Requested(row.getObject("submission_id", UUID.class),
                row.getObject("review_submission_id", UUID.class), row.getString("source"), row.getString("task"),
                row.getString("stem"), row.getInt("word_count"), row.getTimestamp("requested_at").toInstant()),
                userId, limit, offset);
    }
}
