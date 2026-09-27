---
date: 2026-09-26
kind: implementation-journal
scope: one-time LLM path ordering through DeepTutor
---

# LLM path ordering

## Context

The accepted plan adds Gemini ordering when a goal's path is first created. Content
owns curriculum membership and metadata; DeepTutor remains the only adaptive
engine. The LLM may propose a permutation, which the service validates before
persisting it.

## What happened

The implementation is being built around DeepTutor's existing model catalog,
`complete()` call, structured JSON handling, and token ledger. The operator guide
now describes guarded catalog setup, cache refresh, six fallback reasons, the
outbound data fields, and five E2E cases using an OpenAI-compatible stub. Existing
private catalogs are never replaced by the documented copy commands.

A source check showed that `docker compose exec ... id` describes an exec process,
not necessarily the application after the root entrypoint drops privileges. The
runbook therefore checks the application's `/proc/1/status` and write access as
`appuser`.

## Reflection

The key consistency issue was refresh: the earlier README said order follows
Content on every refresh, which would erase the LLM's stored order. The explicit
user clarification resolves this while retaining Content metadata updates and
retirement/restoration of points. Verification must separate a documented recipe
from evidence that its cases were actually executed.

## Decisions

| Decision | Rationale and impact |
| --- | --- |
| Preserve existing module and point order during refresh; append new points to their module | User explicitly confirmed this on 2026-09-26. The LLM ordering remains fixed while the curriculum can grow. New modules append after existing modules. |
| Reuse DeepTutor's catalog and LLM layer | No provider client, runtime dependency, key/model environment variable, or upstream submodule change is needed. |
| Fall back to the complete Content order on an unusable proposal or LLM failure | Path creation remains available; `path.ordered` records the source and reason. |
| Send only learning context and curriculum metadata | Learner identity, goal identity, tokens and assessment answers stay out of the ordering request. |
| Use temporary catalogs and stubs for automated verification | No real key or real Gemini request is required; private local configuration remains outside tracked artifacts. |

## Validation and next steps

Verified on 2026-09-26 against `13dcdd8` plus `d5d3837`. The second commit keeps `*.sh` files LF, so the container
entrypoint survives a Windows checkout with `core.autocrlf`.

- Python suite with PostgreSQL and RabbitMQ: 179 passed, 0 skipped.
- Java suite (`mvn test`): 317 tests, 0 failures, 0 skipped.
- Gateway E2E with the OpenAI-compatible stub: all five cases passed.
  - `reverse`: `source=llm`.
  - `unknown_kp`: `invalid_ordering`.
  - `slow`: `llm_timeout`, after 20 s.
  - `error`: `llm_error`.
  - No catalog: `llm_not_configured`.
- Placement parked before the path still tested out. A later result updated mastery without changing the order.
  A refresh appended the new point without calling the LLM again.
- The container runs API and consumer as uid 10001 and writes the root-owned bind mount.
  `deeptutor config show` masks the key.

Evidence: `plans/260925-1547-llm-path-ordering/reports/e2e-verification-260926-llm-path-ordering-report.md`.

Next: the team runs the real-key check from the AI Learning README. The follow-ups listed in the plan are still
open.
