# Account anonymization follow-up

Status: proposed; design captured in [account deletion design](../../docs/account-deletion-design.md).

## Phases

1. Confirm legal retention, staff-account policy, community content policy, and email reuse; see [phase 1](phase-01-account-anonymization.md).
2. Implement User Service deletion request, tombstone, credential revocation, outbox event, and status reporting.
3. Implement idempotent consumers and private-content/object-storage deletion in Assessment, Learning, Library, Community, Access, and Notification.
4. Add cross-service replay, partial-failure, authorization, and data-retention tests; deploy in dry-run/observe mode before enabling deletion.

## Acceptance criteria

- Deleted accounts cannot authenticate through password, OAuth, refresh, reset, or verification flows.
- Each consumer handles an event once effectively, including duplicate delivery and retry after partial failure.
- Private user content and storage objects are removed; retained history is detached or pseudonymous under an approved policy.
- Ledger entries stay balanced, community author rendering is stable, and deletion progress is visible to support without restoring identity.
- Retention and audit rules are approved and documented before production execution.

## Dependencies

Resolve every open decision in `docs/account-deletion-design.md` before implementation. This follow-up is intentionally separate from the current retention work.
