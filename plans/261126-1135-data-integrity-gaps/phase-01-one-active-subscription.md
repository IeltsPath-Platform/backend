# Phase 1 - one Active subscription per user (access-service)

Rules: SRS DR-39 and DR-48. Size M. Status: proposed.

## Context
- `Subscription` aggregate and table `subscriptions` (status ACTIVE, EXPIRED, CANCELLED; `starts_at`, `ends_at`).
- Writers: `ActivateKeyUseCase` (about lines 143-154) and `GrantSubscriptionUseCase` (about lines 89-104). Both call
  `subscriptionRepository.findActiveByUserId`, then `extend(...)` or `Subscription.create(...)`.
- `SubscriptionJpaRepository.findActiveByUserId` is `status = 'ACTIVE' AND (ends_at IS NULL OR ends_at > now)`.
- Nothing sets a lapsed subscription to `EXPIRED`. `SubscriptionExpiryNoticeScheduler` only sends notices.
- Existing indexes: `(user_id)` and `(user_id, status)`; no uniqueness.

## Requirements
1. At most one row with `status = 'ACTIVE'` per user, enforced by the database.
2. A subscription whose `ends_at` has passed becomes `EXPIRED` (the lifecycle in SRS 3.2 says so; today it is only hidden).
3. A key activation or an admin grant on a user with an Active subscription adds days and credits to it (already true).
4. Two concurrent activations for one user end with one Active row and no error shown to the learner.

## Files
- New migration `services/access-service/src/main/resources/db/migration/V15__one_active_subscription_per_user.sql` (take the next free version).
- `SubscriptionJpaRepository` / `SubscriptionRepository` and adapter: add `expireLapsed(userId, now)` and `expireAllLapsed(now)`.
- `ActivateKeyUseCase`, `GrantSubscriptionUseCase`: call `expireLapsed` first inside the same transaction; on
  `DataIntegrityViolationException` for the new index, reload the active row and extend it (one retry).
- New scheduler `SubscriptionLapseScheduler` (pattern of `SubscriptionExpiryNoticeScheduler`, property
  `access.subscription-lapse.scan-interval`, default PT15M) that calls `expireAllLapsed(now)`.
- `Subscription`: add `expire()` guarded by phase 5's `canMoveTo`.

## Steps
1. Pre-check, run on production data and paste the result in the PR:
   `SELECT user_id, count(*) FROM subscriptions WHERE status='ACTIVE' GROUP BY user_id HAVING count(*) > 1;`
   and the count of rows with `status='ACTIVE' AND ends_at <= now()`.
2. Migration, in this order: set lapsed rows to EXPIRED; for any user still with several ACTIVE rows keep the one with
   the latest `ends_at` and set the others to CANCELLED with `cancelled_at = now()`; then
   `CREATE UNIQUE INDEX uq_subscriptions_one_active ON subscriptions (user_id) WHERE status = 'ACTIVE';`.
3. Implement `expireLapsed`, the retry, and the scheduler.
4. Contract note: `docs/contracts` for access entitlement stays the same (the read already ignores lapsed rows).

## Tests
- Unit: activation on a user whose only subscription lapsed creates a new Active one and the old one is EXPIRED.
- Unit: activation on an Active subscription extends it (days and credits), no new row.
- Testcontainers: two parallel activations for one user leave exactly one ACTIVE row.
- Migration test: data with two ACTIVE rows and one lapsed row ends valid and keeps the latest.
- Scheduler test: a lapsed row is set to EXPIRED, an unexpired one is not.

## Risks and rollback
- The migration changes statuses of existing rows; keep the pre-check output so it can be reviewed.
- Entitlement reads must keep working while rows flip to EXPIRED (they already filter by date).
- Rollback: `DROP INDEX uq_subscriptions_one_active;` status changes are not reverted (they were true already).

