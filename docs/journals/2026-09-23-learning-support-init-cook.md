# Learning-support-service init

Date: 2026-09-23

## What landed

Initialized `learning-support-service` against Database V5 and the approved plan: Flyway V1 for eight business tables plus empty `outbox_events`, Compose `learning-support-db` on `127.0.0.1:5433`, tracking and personal-library CRUD under `/api/learning-support`, owner scoping via JWT subject with 404 for other users' rows, and no RBAC.

## Hard parts

Assigned composite keys on `flashcard_deck_items` made Spring Data `saveAndFlush` merge on the second insert, wiping `added_at` and surfacing SQLSTATE `23502` as 400. Add now checks existence for an immediate 409 and uses `EntityManager.persist` so a race still maps `23505` to conflict.

Local Maven needed JDK 21 (`ms-21.0.7`) and `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`. Docker is not installed, so schema and `@DataJpaTest` upsert tests stay skipped.

## Indexes

List and membership paths got explicit indexes in V1: progress/segments by user+time, notes/decks/cards by `user_id, status, updated_at DESC`, deck items by `(deck_id, sort_order, flashcard_id)` and `(flashcard_id)`.

## Still open

Re-run `LearningSupportSchemaTest` and `VideoProgressUpsertTest` once Docker is available. Controllers still return domain aggregates rather than separate response DTOs.
