package com.ieltspath.content.infrastructure.scheduling;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Holds a PostgreSQL session advisory lock for the duration of a scheduled job. */
@Component
public class PostgresAdvisoryLock {
    private final DataSource dataSource;

    public PostgresAdvisoryLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean runIfAcquired(long key, Runnable action) {
        try (Connection connection = dataSource.getConnection()) {
            if (!lock(connection, "SELECT pg_try_advisory_lock(?)", key)) return false;
            try {
                action.run();
                return true;
            } finally {
                lock(connection, "SELECT pg_advisory_unlock(?)", key);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to coordinate scheduled publication", ex);
        }
    }

    private static boolean lock(Connection connection, String sql, long key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }
}
