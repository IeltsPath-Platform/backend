# Implementation and verification report

Date: 2026-11-08  
Branch: `feat/fcm-token-mobile`

## Implemented

- Owner-scoped Android FCM device registration and deletion APIs with token hashing, idempotent registration, and registration-version fencing.
- Durable PostgreSQL push deliveries written with notifications across all six existing notification sources; both enqueue and worker flags default off.
- Firebase Admin sender and leased worker with bounded retries, expiry, invalid-token handling, metrics, and calls outside database transactions.
- Bounded, indexed delivery housekeeping; stale registration claims cannot be finalized as `SENT`.
- Push contract, operations guide, architecture updates, and database migrations V5–V7.

## Verification

- `mvn -pl services/notification-service -am -DskipTests compile` — passed.
- `mvn -pl services/notification-service -am -DskipTests test-compile` — passed.
- Focused tests for device API/security, worker behavior, PostgreSQL registration/queue behavior, migration, and two Spring application contexts — 27 tests passed, 0 failures/errors. Testcontainers used PostgreSQL and RabbitMQ.
- Re-ran the Docker-backed PostgreSQL tests on `feat/fcm-token-mobile`: `mvn -pl services/notification-service -am -Dtest=NotificationDeviceRepositoryIntegrationTest,NotificationMigrationTest -Dsurefire.failIfNoSpecifiedTests=false test` — 8 tests passed, 0 failures/errors/skips. Docker Desktop 29.6.1 started PostgreSQL 15.19 containers and Flyway applied migrations V1–V7.
- `mvn -q -pl services/notification-service -am test` — passed after the constructor fix: 616 suites, 3,229 tests, 0 failures, 0 errors, 4 skipped.

## Remaining validation and rollout gates

- Android build, token refresh/logout integration, notification tap/refetch behavior, and real FCM delivery need the Android app and Firebase project credentials; none were available in this backend workspace.
- Confirm database token encryption/access controls and add device revocation to the account-deletion flow before production; no account-deletion hook was found in this service.
- Phase 4 still needs targeted cases for every notification source's delivery atomicity, expiry/attempt exhaustion, stale invalid-token response, registration/delete races, flag combinations, and Firebase configuration/error behavior.
- No shared-environment migration, live push, or deployment was run.
