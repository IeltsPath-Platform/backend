package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
}
