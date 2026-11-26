package com.ieltspath.access.infrastructure.persistence.adapter;

import com.ieltspath.access.application.port.WalletReconciliationStore;
import com.ieltspath.access.application.result.WalletMismatch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcWalletReconciliationStore implements WalletReconciliationStore {
    private static final String MISMATCH_QUERY = """
            WITH ledger_totals AS (
                SELECT user_id, SUM(delta) AS delta_sum
                FROM point_ledger_entries
                GROUP BY user_id
            ), latest_entries AS (
                SELECT DISTINCT ON (user_id) user_id, balance_after
                FROM point_ledger_entries
                ORDER BY user_id, created_at DESC, id DESC
            )
            SELECT wallet.user_id, wallet.balance,
                   COALESCE(ledger.delta_sum, 0) AS ledger_delta_sum,
                   COALESCE(latest.balance_after, 0) AS latest_ledger_balance,
                   wallet.total_credited, wallet.total_debited
            FROM point_wallets wallet
            LEFT JOIN ledger_totals ledger ON ledger.user_id = wallet.user_id
            LEFT JOIN latest_entries latest ON latest.user_id = wallet.user_id
            WHERE wallet.balance <> COALESCE(ledger.delta_sum, 0)
               OR wallet.balance <> COALESCE(latest.balance_after, 0)
               OR wallet.balance <> wallet.total_credited - wallet.total_debited
            ORDER BY wallet.user_id
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcWalletReconciliationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<WalletMismatch> findMismatches(int limit) {
        if (limit < 1) throw new IllegalArgumentException("Wallet reconciliation row limit must be positive");
        return jdbcTemplate.query(MISMATCH_QUERY, (row, index) -> new WalletMismatch(
                row.getObject("user_id", java.util.UUID.class),
                row.getLong("balance"),
                row.getLong("ledger_delta_sum"),
                row.getLong("latest_ledger_balance"),
                row.getLong("total_credited"),
                row.getLong("total_debited")), limit);
    }
}
