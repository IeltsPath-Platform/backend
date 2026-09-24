---
date: 2026-09-24
kind: implementation-journal
scope: ai-learning-service Stage A
---

# AI-learning Stage A implementation

## Context

Continued Phase 1 implementation from the existing specification. Scope is Stage A path creation and learning progress APIs, preserving the upstream DeepTutor v1.6.9 boundary. The accepted baseline is commit `da856ad67075d49b483150ac44ebca710cb5f266`; the fork remote remains unresolved and does not block this work.

## What happened

Implemented the synchronous PostgreSQL `LearningStore` adapter and transaction joining needed to keep DeepTutor path creation and curriculum replacement atomic. Added User Service and Content Service clients, curriculum assembly, JWT-owned path APIs, and allowlist response DTOs. Knowledge points are ordered by `createdAt` ascending, as accepted, with UUIDs used for stable identifiers. The one-path-per-learning-goal uniqueness constraint remains a manual SQL migration artifact because this service has no migration runner.

## Decisions

- Keep DeepTutor v1.6.9 source and transaction boundary unchanged; use a synchronous adapter.
- Accept `createdAt` order for knowledge points instead of adding a Content Service sort-order schema/API change.
- Keep the uniqueness migration explicit and manual; do not add RabbitMQ or assessment ingestion to Stage A.

## Validation and limits

Compile and `git diff --check` passed. Runtime tests could not run in this environment because required Python packages are unavailable and no PostgreSQL test database is configured. Docker is unavailable as well, so database-backed behavior remains unverified here.

## Next

Run the focused runtime and PostgreSQL-backed tests once the service dependencies and test database are available; retain the manual uniqueness SQL as a deployment prerequisite.
