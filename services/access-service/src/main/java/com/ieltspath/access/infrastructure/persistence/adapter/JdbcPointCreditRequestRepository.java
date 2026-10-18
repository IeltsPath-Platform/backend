package com.ieltspath.access.infrastructure.persistence.adapter;

import com.ieltspath.access.application.result.PointCreditResult;
import com.ieltspath.access.domain.entity.PointCreditRequest;
import com.ieltspath.access.domain.repository.PointCreditRequestRepository;
import com.ieltspath.access.domain.vo.LearningRewardReason;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPointCreditRequestRepository implements PointCreditRequestRepository {
    private final JdbcTemplate jdbc;

    public JdbcPointCreditRequestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lock(String idempotencyKey, UUID userId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext('point-credit-key'), hashtext(?))", rs -> {}, idempotencyKey);
        // One lock per learner: the limit is a rolling window, so credits of one learner must always be counted in turn.
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext('point-credit-user'), hashtext(?))", rs -> {},
                userId.toString());
    }

    @Override
    public Optional<PointCreditRequest> findByIdempotencyKey(String idempotencyKey) {
        List<PointCreditRequest> rows = jdbc.query("""
                SELECT idempotency_key, user_id, amount, reason, reference_id, granted, capped, balance, transaction_id
                FROM point_credit_requests WHERE idempotency_key = ?
                """, (rs, rowNum) -> new PointCreditRequest(
                rs.getString("idempotency_key"),
                rs.getObject("user_id", UUID.class),
                rs.getInt("amount"),
                LearningRewardReason.valueOf(rs.getString("reason")),
                rs.getString("reference_id"),
                rs.getInt("granted"),
                rs.getBoolean("capped"),
                rs.getLong("balance"),
                rs.getObject("transaction_id", UUID.class)
        ), idempotencyKey);
        return rows.stream().findFirst();
    }

    @Override
    public long sumGrantedSince(UUID userId, Instant startInclusive) {
        Long total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(delta), 0)
                FROM point_ledger_entries
                WHERE user_id = ? AND transaction_type = 'LEARNING_REWARD' AND reference_type <> 'BADGE'
                  AND created_at >= ?
                """, Long.class, userId, Timestamp.from(startInclusive));
        return total == null ? 0 : total;
    }

    @Override
    public PointCreditRequest save(PointCreditRequest request) {
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO point_credit_requests
                        (idempotency_key, user_id, amount, reason, reference_id, granted, capped, balance, transaction_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """);
            statement.setString(1, request.idempotencyKey());
            statement.setObject(2, request.userId());
            statement.setInt(3, request.amount());
            statement.setString(4, request.reason().name());
            statement.setString(5, request.referenceId());
            statement.setInt(6, request.granted());
            statement.setBoolean(7, request.capped());
            statement.setLong(8, request.balance());
            statement.setObject(9, request.transactionId());
            return statement;
        });
        return request;
    }
}
