package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.HumanReviewOutcomeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcHumanReviewOutcomeRepository implements HumanReviewOutcomeRepository {
    private final JdbcTemplate jdbc;

    public JdbcHumanReviewOutcomeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertIfAbsent(UUID reviewSubmissionId, UUID userId, UUID submissionId, String studyPointsJson) {
        return jdbc.update("""
                INSERT INTO human_review_outcomes (review_submission_id, user_id, submission_id, study_points)
                VALUES (?, ?, ?, CAST(? AS jsonb))
                ON CONFLICT DO NOTHING
                """, reviewSubmissionId, userId, submissionId, studyPointsJson) == 1;
    }

    @Override
    public Optional<String> studyPointsOf(UUID submissionId) {
        return jdbc.query("SELECT study_points::text FROM human_review_outcomes WHERE submission_id = ?",
                (row, index) -> row.getString(1), submissionId).stream().findFirst();
    }
}
