package com.ieltspath.access.infrastructure.persistence.adapter;

import com.ieltspath.access.domain.entity.HumanGradingCreditConsumption;
import com.ieltspath.access.domain.repository.HumanGradingCreditConsumptionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcHumanGradingCreditConsumptionRepository implements HumanGradingCreditConsumptionRepository {
    private final JdbcTemplate jdbc;

    public JdbcHumanGradingCreditConsumptionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lockSubmission(UUID submissionId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> null, "human-grading:" + submissionId);
    }

    @Override
    public Optional<HumanGradingCreditConsumption> findBySubmissionId(UUID submissionId) {
        return jdbc.query("""
                SELECT submission_id, user_id, subscription_id, remaining_credits, refunded_at
                FROM human_grading_credit_consumptions WHERE submission_id = ?
                """, (rs, row) -> new HumanGradingCreditConsumption(
                rs.getObject("submission_id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("subscription_id", UUID.class), rs.getInt("remaining_credits"),
                rs.getTimestamp("refunded_at") == null ? null : rs.getTimestamp("refunded_at").toInstant()), submissionId)
                .stream().findFirst();
    }

    @Override
    public void save(HumanGradingCreditConsumption consumption) {
        jdbc.update("""
                INSERT INTO human_grading_credit_consumptions(submission_id,user_id,subscription_id,remaining_credits)
                VALUES (?,?,?,?)
                """, consumption.submissionId(), consumption.userId(), consumption.subscriptionId(),
                consumption.remainingCredits());
    }

    @Override
    public void markRefunded(UUID submissionId, int remainingCredits) {
        jdbc.update("""
                UPDATE human_grading_credit_consumptions
                SET refunded_at = CURRENT_TIMESTAMP, remaining_credits = ?
                WHERE submission_id = ? AND refunded_at IS NULL
                """, remainingCredits, submissionId);
    }

    @Override
    public void markReconsumed(UUID submissionId, UUID subscriptionId, int remainingCredits) {
        jdbc.update("""
                UPDATE human_grading_credit_consumptions
                SET refunded_at = NULL, subscription_id = ?, remaining_credits = ?
                WHERE submission_id = ? AND refunded_at IS NOT NULL
                """, subscriptionId, remainingCredits, submissionId);
    }
}
