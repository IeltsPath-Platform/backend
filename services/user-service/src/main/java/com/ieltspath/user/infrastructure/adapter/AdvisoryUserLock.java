package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.UserLock;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** PostgreSQL transaction-scoped advisory lock keyed by the user id. */
@Component
@RequiredArgsConstructor
public class AdvisoryUserLock implements UserLock {
    private final JdbcTemplate jdbc;

    /** The study-time code locks the same user with this key, so both kinds of work exclude each other. */
    static long key(UUID userId) {
        return userId.getMostSignificantBits() ^ userId.getLeastSignificantBits();
    }

    @Override
    public void lock(UUID userId) {
        jdbc.query("SELECT pg_advisory_xact_lock(?)", (r, n) -> 0, key(userId));
    }
}
