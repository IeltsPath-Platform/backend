package com.ieltspath.access.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The key batch and distribution columns, and the demo keys the demo-data callback writes. */
@Testcontainers(disabledWithoutDocker = true)
class KeyBatchesMigrationTest {

    private static Flyway flyway(PostgreSQLContainer<?> postgres, MigrationVersion target, boolean demoData) {
        return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration", "classpath:db/callback")
                .placeholders(java.util.Map.of("demoData", String.valueOf(demoData)))
                .target(target).load();
    }

    @Test
    void keysMadeBeforeBatchesKeepWorkingWithoutABatch() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            // No callback here: the demo-data callback only makes sense once the schema is at the latest version.
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").target(MigrationVersion.fromVersion("9")).load().migrate();
            try (Connection connection = connection(postgres); Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO activation_keys (product_id, code_hash, code_hint) "
                        + "SELECT id, 'old-key-hash', 'ABCD' FROM key_products WHERE code = 'POINT_50'");
            }

            flyway(postgres, MigrationVersion.LATEST, false).migrate();

            try (Connection connection = connection(postgres);
                 Statement statement = connection.createStatement();
                 ResultSet row = statement.executeQuery(
                         "SELECT status, batch_code, seq, sale_id, issued_at FROM activation_keys WHERE code_hash = 'old-key-hash'")) {
                assertTrue(row.next());
                assertEquals("ACTIVE", row.getString("status"));
                assertNull(row.getString("batch_code"));
                assertNull(row.getObject("seq"));
                assertNull(row.getObject("sale_id"));
                assertNull(row.getObject("issued_at"));
            }
        }
    }

    @Test
    void theConstraintsHoldABatchTogether() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            flyway(postgres, MigrationVersion.LATEST, false).migrate();
            try (Connection connection = connection(postgres); Statement statement = connection.createStatement()) {
                statement.executeUpdate(batch("B-2611-001", 2));
                // The batch code is B-YYMM-NNN and a batch holds 1 to 1000 keys.
                assertThrows(SQLException.class, () -> statement.executeUpdate(batch("B-261-001", 2)));
                assertThrows(SQLException.class, () -> statement.executeUpdate(batch("B-2611-002", 0)));
                assertThrows(SQLException.class, () -> statement.executeUpdate(batch("B-2611-003", 1001)));

                statement.executeUpdate(key("hash-1", "'B-2611-001'", "1", "'ACTIVE'", "NULL"));
                // A position needs a batch and a batch needs a position; positions are unique inside a batch.
                assertThrows(SQLException.class, () -> statement.executeUpdate(key("hash-2", "'B-2611-001'", "NULL", "'ACTIVE'", "NULL")));
                assertThrows(SQLException.class, () -> statement.executeUpdate(key("hash-3", "NULL", "3", "'ACTIVE'", "NULL")));
                assertThrows(SQLException.class, () -> statement.executeUpdate(key("hash-4", "'B-2611-001'", "1", "'ACTIVE'", "NULL")));
                assertThrows(SQLException.class, () -> statement.executeUpdate(key("hash-5", "'B-2611-001'", "0", "'ACTIVE'", "NULL")));
                // revoked_at only goes with a revoked key.
                assertThrows(SQLException.class, () -> statement.executeUpdate(key("hash-6", "'B-2611-001'", "2", "'ACTIVE'", "now()")));
                statement.executeUpdate(key("hash-7", "'B-2611-001'", "2", "'REVOKED'", "now()"));
            }
        }
    }

    @Test
    void anAllocationRecordsWhoMovedHowManyKeysBetweenWhichHolders() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            flyway(postgres, MigrationVersion.LATEST, false).migrate();
            UUID manager = UUID.randomUUID();
            UUID saleA = UUID.randomUUID();
            UUID saleB = UUID.randomUUID();
            try (Connection connection = connection(postgres); Statement statement = connection.createStatement()) {
                statement.executeUpdate(batch("B-2611-001", 100));
                statement.executeUpdate(allocation("ALLOCATE", null, saleA, 40, manager));
                statement.executeUpdate(allocation("REASSIGN", saleA, saleB, 10, manager));
                statement.executeUpdate(allocation("WITHDRAW", saleB, null, 5, manager));
                // A pool-to-sale move has no source holder, a withdrawal has no target, and a reassignment needs two different sales.
                assertThrows(SQLException.class, () -> statement.executeUpdate(allocation("ALLOCATE", saleA, saleB, 1, manager)));
                assertThrows(SQLException.class, () -> statement.executeUpdate(allocation("ALLOCATE", null, null, 1, manager)));
                assertThrows(SQLException.class, () -> statement.executeUpdate(allocation("WITHDRAW", null, null, 1, manager)));
                assertThrows(SQLException.class, () -> statement.executeUpdate(allocation("WITHDRAW", saleA, saleB, 1, manager)));
                assertThrows(SQLException.class, () -> statement.executeUpdate(allocation("REASSIGN", saleA, saleA, 1, manager)));
                assertThrows(SQLException.class, () -> statement.executeUpdate(allocation("REASSIGN", null, saleB, 1, manager)));
                assertThrows(SQLException.class, () -> statement.executeUpdate(allocation("MOVE", saleA, saleB, 1, manager)));
                // At least one key per allocation, and the batch has to exist.
                assertThrows(SQLException.class, () -> statement.executeUpdate(allocation("ALLOCATE", null, saleA, 0, manager)));
                assertThrows(SQLException.class, () -> statement.executeUpdate(
                        allocation("ALLOCATE", null, saleA, 1, manager).replace("B-2611-001", "B-2611-999")));
            }
            assertEquals(3, count(postgres, "SELECT count(*) FROM key_allocations WHERE allocated_by = '" + manager + "'"));
            assertEquals(40, count(postgres, "SELECT coalesce(sum(quantity), 0) FROM key_allocations WHERE action = 'ALLOCATE' AND to_sale_id = '" + saleA + "'"));
        }
    }

    @Test
    void demoKeysAreWrittenOnlyWhenDemoDataIsOnAndOnlyOnce() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            flyway(postgres, MigrationVersion.LATEST, false).migrate();
            assertEquals(0, count(postgres, "SELECT count(*) FROM activation_keys"));

            flyway(postgres, MigrationVersion.LATEST, true).migrate();
            flyway(postgres, MigrationVersion.LATEST, true).migrate();

            assertEquals(2, count(postgres, "SELECT count(*) FROM key_batches"));
            assertEquals(15, count(postgres, "SELECT count(*) FROM activation_keys"));
            assertEquals(10, count(postgres, "SELECT count(*) FROM activation_keys WHERE batch_code = 'B-2611-001'"));
            assertEquals(1, count(postgres, "SELECT count(*) FROM activation_keys WHERE issued_at IS NOT NULL"));
            // The documented codes are the ones stored, so they can be typed in to activate a key.
            assertEquals(1, count(postgres, "SELECT count(*) FROM activation_keys WHERE code_hash = '" + sha256("IP-DEV5-KEYS-AAA2") + "'"));
            assertEquals(1, count(postgres, "SELECT count(*) FROM activation_keys WHERE code_hash = '" + sha256("IP-DEV9-KEYS-AAA6") + "'"));
            assertEquals(5, count(postgres, "SELECT count(*) FROM activation_keys k JOIN key_products p ON p.id = k.product_id "
                    + "WHERE p.code = 'POINT_100'"));
            // The keys expire at the end of the batch's last day in Vietnam time (17:00 UTC).
            assertEquals(15, count(postgres, "SELECT count(*) FROM activation_keys WHERE expires_at = TIMESTAMPTZ '2027-12-31 17:00:00+00'"));
        }
    }

    private static String batch(String code, int quantity) {
        return "INSERT INTO key_batches (code, label, product_id, quantity) "
                + "SELECT '" + code + "', 'test', id, " + quantity + " FROM key_products WHERE code = 'POINT_50'";
    }

    private static String allocation(String action, UUID from, UUID to, int quantity, UUID by) {
        return "INSERT INTO key_allocations (batch_code, action, from_sale_id, to_sale_id, quantity, allocated_by) VALUES ('B-2611-001', '"
                + action + "', " + (from == null ? "NULL" : "'" + from + "'") + ", " + (to == null ? "NULL" : "'" + to + "'")
                + ", " + quantity + ", '" + by + "')";
    }

    private static String key(String hash, String batchCode, String seq, String status, String revokedAt) {
        return "INSERT INTO activation_keys (product_id, code_hash, status, batch_code, seq, revoked_at) "
                + "SELECT id, '" + hash + UUID.randomUUID() + "', " + status + ", " + batchCode + ", " + seq + ", " + revokedAt
                + " FROM key_products WHERE code = 'POINT_50'";
    }

    private static long count(PostgreSQLContainer<?> postgres, String sql) throws SQLException {
        try (Connection connection = connection(postgres);
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery(sql)) {
            row.next();
            return row.getLong(1);
        }
    }

    private static String sha256(String code) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
    }

    private static Connection connection(PostgreSQLContainer<?> postgres) throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
