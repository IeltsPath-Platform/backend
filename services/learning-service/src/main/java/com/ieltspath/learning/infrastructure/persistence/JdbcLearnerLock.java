package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.LearnerLock;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/** A transaction-scoped PostgreSQL advisory lock per learner. */
@Component
public class JdbcLearnerLock implements LearnerLock {
    private final JdbcTemplate jdbc;

    public JdbcLearnerLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lock(UUID userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Learning writes require a transaction");
        }
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
                statement.setString(1, userId.toString());
                statement.execute();
            }
            return null;
        });
    }
}
