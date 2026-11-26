# Phase 3 - wallet balance checked against the ledger (access-service)

Rule: SRS DR-41. Size S. Status: proposed. Decision: report only, never repair.

## Context
- `point_wallets(user_id, balance, total_credited, total_debited, row_version)` and
  `point_ledger_entries(user_id, delta, balance_after, idempotency_key, ...)`.
- Both are written together (`CreditPointsWriter` and the debit path) but nothing compares them.
- Existing scheduler pattern: `SubscriptionExpiryNoticeScheduler` with `@Scheduled(fixedDelayString = "${...:PT15M}")`.

## Requirements
1. A scheduled check finds wallets where `balance <> sum(delta)` or `balance <> last balance_after`, and where
   `total_credited - total_debited <> balance`.
2. It logs one WARN per mismatch (user id and the numbers only, no personal data), increments a Micrometer counter, and
   changes nothing.
3. Off by default; interval and row limit are configurable.

## Files
- `application/usecase/ReconcileWalletsUseCase.java` with a port `WalletReconciliationStore` (JDBC query, limit 100).
- `infrastructure/scheduler/WalletReconciliationScheduler.java`.
- Properties `access.points.reconciliation.enabled=false`, `.interval=PT24H`, `.max-rows=100` in `infra/config-server/config-repo/access-service.yaml` and `.env.example` if env based.
- Counter name `points.reconciliation.mismatch`; document it in `docs/system-architecture.md` next to the other metrics.

## Steps
1. Write the SQL with a CTE: sum of deltas and the latest entry per user, joined to `point_wallets`.
2. Use case calls the store, logs, counts; scheduler calls the use case when enabled.
3. Add the metric to the existing Prometheus rules file if the project keeps one for access; otherwise note it in the PR.

## Tests
- Consistent wallet and ledger: no mismatch.
- Wallet whose balance is 5 higher than the ledger sum: one mismatch reported, balance unchanged.
- Disabled by default: the scheduler does nothing.
- Testcontainers run of the SQL on real tables.

## Risks and rollback
- A wallet with ledger entries missing from before the ledger existed would show a false mismatch; run once on a copy
  of production data and record the known-old accounts before enabling it.
- Rollback: set the flag to false or remove the scheduler; no schema change.
