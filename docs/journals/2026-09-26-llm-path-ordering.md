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

Documentation was prepared while implementation was in progress. Test, Docker,
Gateway E2E, and review results are pending evidence from the implementation
controller; no passing gate is claimed here. Update this section and synchronize
all plan phases after those results are available. Verification reports belong in
`plans/260925-1547-llm-path-ordering/reports/` and must omit credentials, tokens, full
prompts, and full payloads.
