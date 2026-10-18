# Notification backend

Depends on [Community backend](phase-01-community-backend.md) passing verification.

- [x] Add isolated notification database, Flyway schema, REST ownership and pagination.
- [x] Consume versioned events idempotently with retry/DLQ and ACK after commit.
- [x] Push STOMP notifications after commit; persist even if delivery fails.
- [x] Authenticate Gateway handshake, forward internal JWT, constrain origins/subscriptions.
- [x] Test REST security, persistence and Gateway; validate Compose without reading `.env`.

Files: `services/notification-service/**`, `infra/api-gateway/**`, config-repo, init-databases.sql, docker-compose.yml, contracts.
Validation: module Maven tests and `docker compose --env-file <temporary-test-config> config --quiet`.
Risk: duplicate delivery and subscription IDOR; enforce event uniqueness and authenticated user destinations. Disable relay/consumer for rollback without deleting persisted rows.
