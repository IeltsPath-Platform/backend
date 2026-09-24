---
date: 2026-09-24
kind: planning-journal
scope: assessment to ai-learning main flow blockers (no LLM)
---

# Main-flow blocker plan (no LLM)

## Context

The Phase 1 feedback flow code exists, and its Python end-to-end tests pass on local PostgreSQL. The flow still cannot run for real, for four reasons:

- `ai_learning_db` has no base DDL and no migration runner.
- Events for learners without a path are retried and then end in the DLQ.
- Nothing calls finalize.
- Compose has no AI Learning runtime.

LLM/model configuration was evaluated and deferred: the main flow imports no LLM or provider module.

## What happened

Wrote `plans/260924-2135-main-flow-blockers/` (`--fast --tdd`, HOLD scope) with four phases:

1. V5 baseline `V0_1__` run by Flyway.
2. Pending-result inbox guarded by a `(user, goal)` advisory lock and applied inside the path-creation transaction.
3. EXAMINER/ADMIN grading endpoints.
4. Minimal compose, the Docker-dependent tests, and a real end-to-end runbook through the Gateway.

## Decisions

- **No `assessment-db` in compose.** Assessment config defaults to local PostgreSQL, like User and Content, so a container would only add env overrides without isolating the flow. This reverses an earlier approved item; the user confirmed it after seeing the new evidence.
- **Grader details always overwrite `overallBand`.** The learner endpoint is kept, and it can pre-create a DRAFT with a self-declared band that the grader would otherwise finalize unchanged.
- **Grading access.** EXAMINER is not limited to assigned results in this round (no `human_reviews` assignment model).
- **Inbox retention.** Inbox rows are kept until the path is created; there is no cleanup yet.
- **Role setup.** The first ADMIN is a dev-only SQL grant. Other roles use the existing `PUT /api/users/{id}/roles`.

## Validation and limits

This was planning only; no code changed. Docker Desktop is installed (CLI 29.1.3) but not running, so the Docker-dependent tests remain NOT EXECUTED.

Recorded risks:

- The AI Learning image installs DeepTutor's full LLM/RAG dependency stack.
- Windows firewall may block `host.docker.internal`.
- Docker Engine 29 may not work with Testcontainers 1.21.4.
- A local `content_db` that applied the pre-rename V2 migration fails Flyway validation.

## Next

Run the post-plan gate (red-team recommended for auth and data integrity), then cook with `--tdd`.
