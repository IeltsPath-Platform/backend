package com.ieltspath.access.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Closing a deleted account's wallet goes through the ledger, once, so balance and ledger still agree. */
@Testcontainers(disabledWithoutDocker = true)
class AccountDeletionWalletIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void writesTheRemainingBalanceOffOnceAndLeavesOtherWalletsAlone() {
        UUID deleted = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        jdbc.update("INSERT INTO point_wallets (user_id, balance, total_credited) VALUES (?, 50, 50), (?, 30, 30)", deleted, other);
        var store = new JdbcDeletedUserStore(jdbc);

        store.applyAccountDeletion(deleted);
        store.applyAccountDeletion(deleted);

        assertThat(jdbc.queryForMap("SELECT balance, total_debited FROM point_wallets WHERE user_id = ?", deleted))
                .containsEntry("balance", 0L).containsEntry("total_debited", 50L);
        assertThat(jdbc.queryForList("SELECT delta FROM point_ledger_entries WHERE user_id = ?", Long.class, deleted))
                .containsExactly(-50L);
        assertThat(jdbc.queryForObject("SELECT balance FROM point_wallets WHERE user_id = ?", Long.class, other)).isEqualTo(30L);
    }
}
