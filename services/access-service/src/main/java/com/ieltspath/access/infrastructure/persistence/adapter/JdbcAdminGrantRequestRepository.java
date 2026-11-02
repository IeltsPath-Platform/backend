package com.ieltspath.access.infrastructure.persistence.adapter;

import com.ieltspath.access.domain.entity.AdminGrantRequest;
import com.ieltspath.access.domain.repository.AdminGrantRequestRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcAdminGrantRequestRepository implements AdminGrantRequestRepository {
    private final JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    public JdbcAdminGrantRequestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean reserve(String idempotencyKey, String requestHash, UUID subscriptionId) {
        entityManager.flush();
        return jdbc.update("""
                INSERT INTO admin_grant_requests (idempotency_key, request_hash, subscription_id)
                VALUES (?, ?, ?) ON CONFLICT (idempotency_key) DO NOTHING
                """, idempotencyKey, requestHash, subscriptionId) == 1;
    }

    @Override
    public void lock(String idempotencyKey) {
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtext(?))", idempotencyKey);
    }

    @Override
    public Optional<AdminGrantRequest> find(String idempotencyKey) {
        return jdbc.query("""
                SELECT idempotency_key, request_hash, subscription_id
                FROM admin_grant_requests WHERE idempotency_key = ?
                """, (rs, row) -> new AdminGrantRequest(rs.getString("idempotency_key"),
                rs.getString("request_hash"), rs.getObject("subscription_id", UUID.class)), idempotencyKey)
                .stream().findFirst();
    }
}
