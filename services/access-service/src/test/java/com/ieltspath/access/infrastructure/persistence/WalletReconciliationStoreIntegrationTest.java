package com.ieltspath.access.infrastructure.persistence;

import com.ieltspath.access.infrastructure.persistence.adapter.JdbcWalletReconciliationStore;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class WalletReconciliationStoreIntegrationTest {
    @Test
    void sqlFindsOnlyInconsistentWalletsAndDoesNotChangeTheirBalances() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").load().migrate();
            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
                    postgres.getUsername(), postgres.getPassword()));
            UUID consistentUser = UUID.randomUUID();
            UUID mismatchedUser = UUID.randomUUID();
            insertWalletAndLedger(jdbc, consistentUser, 10, 10);
            insertWalletAndLedger(jdbc, mismatchedUser, 15, 10);

            var mismatches = new JdbcWalletReconciliationStore(jdbc).findMismatches(100);

            assertThat(mismatches).hasSize(1);
            assertThat(mismatches.getFirst().userId()).isEqualTo(mismatchedUser);
            assertThat(mismatches.getFirst().balance()).isEqualTo(15);
            assertThat(mismatches.getFirst().ledgerDeltaSum()).isEqualTo(10);
            assertThat(jdbc.queryForObject("SELECT balance FROM point_wallets WHERE user_id = ?", Long.class,
                    mismatchedUser)).isEqualTo(15L);
        }
    }

    private static void insertWalletAndLedger(JdbcTemplate jdbc, UUID userId, long balance, long ledgerBalance) {
        jdbc.update("INSERT INTO point_wallets (user_id, balance, total_credited, total_debited) VALUES (?, ?, 10, 0)",
                userId, balance);
        jdbc.update("""
                INSERT INTO point_ledger_entries (user_id, delta, balance_after, transaction_type, reference_type,
                                                  reference_id, idempotency_key)
                VALUES (?, 10, ?, 'ADMIN_ADJUSTMENT', 'TEST', ?, ?)
                """, userId, ledgerBalance, UUID.randomUUID(), "test:" + userId);
    }
}
