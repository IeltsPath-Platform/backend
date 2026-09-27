# Code Review: daily tutor turn quota (phase 1)

Date: 2026-09-27. Branch `feat/ai-learning-service`, uncommitted working tree. Advisory only; no code, test or doc edits.

## Scope
- New: `app/usage/__init__.py`, `app/usage/quota.py`, `migrations/V9__llm_daily_usage.sql`,
  `tests/test_daily_quota_postgres.py`, `tests/test_tutor_quota_api_postgres.py`
- Modified: `app/config.py`, `app/api/dependencies.py`, `app/api/tutor.py`, `app/api/dto/tutor.py`,
  `app/tutor/memory.py`, `main.py`, two test files, service README, `docs/contracts/tutor-sse-v1.md`,
  `.sdd/database/DATABASE_V5.md`
- About 208 changed lines plus about 450 new lines. Blast-radius code I read: `engine.py`, `tutor_sse.py`,
  `session_store.py`, `llm/client.py`, gateway CORS and routes, `docker-compose.yml`, the E2E harness.

## Verification performed
- The suite ran against a throwaway `postgres:15-alpine` container, using `python -B -m unittest` with
  `PYTHONDONTWRITEBYTECODE=1`. The local `.venv` has no pytest. No `.pyc` was written, and I stopped the container afterwards.
  - New and modified modules (quota store, quota API, memory, migrations): **36 tests OK**.
  - Every `tests/test_*.py` module: **295 ran**. The only non-passes come from the environment: 2 import errors in
    `test_mastery_interactions`/`test_mastery_scheduler` (`import pytest`, not installed in the venv) and 1 skip
    (`AI_LEARNING_TEST_AMQP_URL` not set). Nothing is attributable to this change.
- Scratch probes (outside the repo):
  - If `consume()` raises `UndefinedTable`: HTTP 500, and the turn row becomes `failed/internal_error`, so the slot is released.
  - If `refund()` raises `OperationalError`: `turn.failed/llm_error` is still streamed, `used` stays at 1, and the log
    holds only the error type.
  - `Settings` rejects `-1`, `""` (empty int) and a blank timezone; `0` is accepted.
  - PostgreSQL accepts `UTC+7` and `+07` with POSIX sign inversion, which gives a reset at 07:00Z. It also accepts `ICT`,
    `Asia/Saigon` and lowercase names. A leading space is rejected.
- `git diff --check`: clean. No linter or type checker is configured for the service. One new line is 121 chars
  (`quota.py:89`), within the repo norm (the longest existing line is 141).

## Critical
None.

## High
None.

## Medium

### M1. The `llm_error` refund also covers calls the provider already billed (depends on a user decision; not a code defect)
- Where: `app/api/tutor.py:45,63-65`, together with the unchanged `app/tutor/engine.py:115-117` and `app/llm/client.py:144-154`.
- The engine sets `failure = "llm_error"` before every `await chat(...)` in all 6 rounds. The client maps these to
  `LlmApiError`:
  - `httpx.HTTPError`, which includes the **read timeout** (`AI_LEARNING_LLM_TIMEOUT_SECONDS`, default 20). The provider
    may still finish and bill the generation.
  - 200 responses with a malformed body.
  - Any failure in rounds 2 to 6, after rounds that already succeeded and were billed.
- All of these are refunded. Failure scenario: a learner who can reliably provoke long generations that time out, or a
  provider that times out intermittently, gets billed calls that never count against the cap. That is the cost leak this
  feature exists to close.
- The user decided "refund on `llm_error`/`llm_not_configured`". I am not reversing that. The gap is that the engine's
  `llm_error` is broader than the decision's stated intent ("a provider/system failure, not the learner's"). Options:
  - A: keep as is and accept the leak.
  - B: in `_refund_system_failures`, skip the refund when any `tool.called` or `assistant.message` event came before
    `turn.failed`, because that proves at least one round was billed. This works in the wrapper, so `TutorEngine` stays unchanged.
  - C: B, plus an operator log or metric of refund counts so abuse is visible.
- Needs a user decision before any change.

### M2. docker-compose does not pass the three new variables, so the limits cannot be tuned in the only defined deployment
- Where: `docker-compose.yml:125-138` (`ai-learning-api.environment`).
- Functionally this is harmless: pydantic defaults apply (50/10/`Asia/Ho_Chi_Minh`) and startup is unaffected. But the
  image does not ship `.env` (see `.dockerignore`), and the root `.env` only feeds compose interpolation. Two consequences:
  1. The plan's Risk mitigation ("tune via env, no code deploy") does not hold for the compose deployment. Changing a limit
     means editing `docker-compose.yml`.
  2. Phase 2 E2E logs in with fixed accounts (`tutor_e2e.py:141-142`) against the persistent `ai-learning-db`.
     Repeated same-day runs accumulate `tutor_turn` usage for those learners, and there is no knob to raise the limit or
     set it to 0 for them.
- Fix, in phase 2 or as a follow-up, using **non-empty** defaults. An empty string fails `int` validation, which I verified.
  ```yaml
  AI_LEARNING_TUTOR_TURNS_PER_DAY: ${AI_LEARNING_TUTOR_TURNS_PER_DAY:-50}
  AI_LEARNING_MEMORY_SUMMARIES_PER_DAY: ${AI_LEARNING_MEMORY_SUMMARIES_PER_DAY:-10}
  AI_LEARNING_QUOTA_TIMEZONE: ${AI_LEARNING_QUOTA_TIMEZONE:-Asia/Ho_Chi_Minh}
  ```

## Low

### L1. The startup zone check accepts POSIX offsets with inverted sign
- Where: `app/usage/quota.py:62-65`.
- `AI_LEARNING_QUOTA_TIMEZONE=UTC+7` (or `+07`) passes `check_timezone()`, but PostgreSQL reads it as UTC-7. Verified:
  the reset lands at 07:00Z (14:00 in Viet Nam), so the day boundary is silently 14 hours off.
- Fix: in `check_timezone`, also require `SELECT 1 FROM pg_timezone_names WHERE name = %s` (run once at startup), or
  reject values that contain no `/` and are not `UTC`.

### L2. Startup verifies the zone but not the V9 table
- Where: `main.py:44-45`, `quota.py:62-65`.
- The plan's Risk section said startup should call `usage()` once. The implementation runs only the clock query. If V9
  is missing (manual deploy, or a failed migrate that is bypassed), startup passes and every turn returns a generic 500,
  because `UndefinedTable` is a `ProgrammingError`, not the mapped `OperationalError`. Verified: 500 and
  `failed/internal_error`.
- Fix: in `check_timezone`, also run `SELECT 1 FROM llm_daily_usage LIMIT 0`, or call `usage()` with a nil UUID.

### L3. Error branches are only checked by my probes, not by repo tests
- Where: `tests/test_tutor_quota_api_postgres.py`, `tests/`.
- No repo test covers:
  - `consume()` raising, then the slot is released (`tutor.py:206-211`);
  - `refund()` raising, then `turn.failed` is still delivered (`tutor.py:64-67`);
  - `Settings` rejecting negative limits or a blank zone. The criterion "negative limits rejected" is tested only at the
    store level, and only for `consume`.
- All three behave correctly in my probes. Add small tests so a regression is caught.

### L4. `Retry-After` is not a CORS-exposed header
- Where: `infra/api-gateway/.../SecurityConfig.java:153` exposes only `Authorization` and `Content-Type`.
- A browser SPA on another origin cannot read `Retry-After`. The body's `resetsAt` carries the same information. Either
  add `Retry-After` to the exposed headers, or state in the contract doc that browsers should use `resetsAt`.

### L5. Each refused request still inserts and closes a `turns` row (follows from decision Q2)
- Where: `tutor.py:199-214`.
- After the cap is hit, each retry performs `get_session`, `begin_turn` (INSERT), `consume` (upsert plus select) and
  `finish_turn`: about 5 new DB connections and one permanent `failed/quota_exceeded` row. A client retry loop grows
  `turns` without bound.
- Q2 is a user decision, so this is informational. An optional mitigation is a read-only `usage()` pre-check before
  `open_turn` that returns 429 without opening a turn. That trades away the "closed as quota_exceeded" row for pre-checked
  refusals, so it needs the user's call.

### L6. A memory summary unit is not given back when `complete` fails
- Where: `memory.py:195-210`.
- During a provider outage, every completed turn with 8 or more pending messages spends one of the 10 daily summaries and
  fails. Memory then stalls until the next day, even after the provider recovers. This is consistent with "count LLM
  calls" and the spec does not require a refund here. It is noted only so the behavior is known.

### L7. No log line for a quota refusal
- Where: `tutor.py:212-214`.
- The engine logs every other turn outcome with session and turn ids. Suggest
  `logger.info("Tutor turn refused session=%s turn=%s reason=quota", ...)`, with ids only and no content.

### L8. `/usage` reads in two separate transactions
- Where: `tutor.py:77-78`.
- Right at midnight, the two counts can come from different days, and `resetsAt` comes from the turns read only. One
  query that joins both kinds against a single `clock` would be exact. This is a nit.

### L9. Cancellation window after `open_turn`
- Where: `tutor.py:206-213`.
- `except Exception` does not cover `asyncio.CancelledError`. The change adds new await points between claiming the slot
  and starting the self-closing engine generator. Only a shutdown can cancel there, and `recover_interrupted_turns`
  frees the slot on the next start, so this is acceptable as is.

### L10. `used` can exceed `limit`
- When an operator lowers a limit mid-day, `used` in the 429 payload and in `/usage` can be greater than `limit`.
  Frontends should clamp the display.

### L11. `llm_daily_usage` has no retention
- It grows by up to 2 rows per learner per day. Consider a periodic `DELETE ... WHERE usage_date < current_date - 90` later.

## Verdicts

### (a) Acceptance criteria: MET
| Criterion | Evidence |
| --- | --- |
| 429 before SSE and before any LLM call | `tutor.py:205-214` returns `JSONResponse` before `engine.run`. The test checks `chat.requests == 2` and JSON content-type. |
| `limit`, `resetsAt`, `Retry-After` | `_quota_exceeded` at `tutor.py:49-55`. The test checks all three, with a 5 s tolerance. |
| Turn closed as `failed/quota_exceeded` | `tutor.py:213`. The test queries the `turns` row. `failure_code` has no CHECK constraint (V5). |
| Atomic under concurrency | Single guarded upsert (`quota.py:72-83`). 20 threads with limit 10 give exactly 10 allowed. See (e). |
| 404/409 cost nothing | `consume` runs after `open_turn`. The test covers both, with `used == 0`. |
| Refund only `llm_error`/`llm_not_configured`, on the consumed day | `_REFUNDED_FAILURES` plus `allowance.usage_date` (`tutor.py:45,217`). The test covers llm_error=0, not_configured=0, too_many_rounds=1. The refund cannot go below 0. See M1 on scope. |
| `/usage` contract | `TutorUsageResponse` with camelCase. The test covers values, 401, and learner isolation. |
| Memory over the limit is skipped and the cursor is kept | `memory.py:195-200` runs after the 8-message gate and before `complete`. The test checks `complete` is not called, the record is unchanged, and the exact log line appears. |
| 0 = unlimited, still counted | The `WHERE %(limit)s = 0 OR ...` guard. Store test. |
| Negative limits rejected | `config.py:39-44` (checked by my probe; no repo test, see L3). The store also rejects them. |

### (b) Regressions: NONE FOUND
- `run_turn` happy path: only the `_refund_system_failures` wrapper was added. It passes events through unchanged, and
  its `aclosing` is a no-op when the generator is already exhausted. The engine is unchanged.
- The turn slot is released in every new branch: when `consume` raises (verified), and on refusal (`quota_exceeded`).
  The only uncovered case is cancellation (L9, mitigated by startup recovery).
- `stream_events` still starts the producer task eagerly in `run_turn`. The refund runs inside that task, after the
  engine's `finish_turn`, so the slot is already free.
- Memory: `quota=None` keeps the old behavior, and existing tests pass. Production adds one DB round trip per eligible
  summary. A `consume` DB error propagates to `schedule`'s best-effort handler.
- Other routes: `get_session` also resolves `get_tutor_engine`, which now builds a `DailyQuotaStore` with no I/O. No
  behavior change.
- Lifespan: the zone check runs after `recover_interrupted_turns`, and a failure blocks startup as intended.
  `TestClient` without `with` does not run the lifespan, so other API tests are unaffected. The full suite confirms this.

### (c) Public contracts: DOCUMENTED and ADDITIVE, with two gaps
- The env vars are documented in the README "Daily limits" section and all have defaults. The endpoint and 429 appear in
  the contract doc, the README and OpenAPI (`responses={429: QuotaExceededResponse}`). V9 is in the README migration table
  and `DATABASE_V5.md` §7.20. The example `resetsAt` `2026-09-27T17:00:00Z` is correct for midnight in Viet Nam.
- Backward compatible: new table only, new optional constructor parameters, a new route, and a new status on an existing
  route (older clients see a non-200 JSON error, which is the same shape as their existing `detail` errors).
- No unintended changes: the diff is confined to the listed files.
- Gaps: compose passthrough (M2) and the CORS-exposed header (L4).

### (d) Patterns: FOLLOWED
- The store copies `LearnerMemoryStore`: `closing(psycopg2.connect)`, `with connection` for writes, calls through
  `asyncio.to_thread`, `__all__` at the bottom.
- DTOs extend `ApiResponse` (`populate_by_name`) with camelCase aliases. Logs carry only `error_type` or a reason, never
  content. `# noqa: BLE001 - reason` matches the engine.
- Tests follow the unittest style with `PostgresSchema`, and the settings cache is cleared as in `test_tutor_api_postgres.py`.
- Nit: `_user_id` is duplicated from `memory.py` (a private one-liner, acceptable).

### (e) SQL and lint: CORRECT
- **Day and reset**: `(now() AT TIME ZONE tz)::date` gives the local date. `date_trunc('day', local) + interval '1 day'`
  gives the next local midnight as wall-clock time, and `AT TIME ZONE tz` turns it back into a `timestamptz`. Python then
  normalizes it to UTC (`quota.py:47`). A single `now()` (transaction-stable) feeds both values, and `clock` is
  referenced twice, so it is materialized. The day does not depend on the session `TimeZone`. Parameters are bound; the
  f-string interpolates only the constant `_CLOCK`, so there is no injection.
- **Upsert under READ COMMITTED**:
  - A first insert always succeeds with `used=1` (the limit is 0 or at least 1).
  - Concurrent first inserts: the loser waits on the speculative insertion, then takes the `DO UPDATE` path.
  - `DO UPDATE ... WHERE` is evaluated against the latest committed row version, with the row locked, so updates
    serialize and `used` can never pass the limit.
  - When the `WHERE` is false, the row stays locked but unchanged, and `RETURNING` yields nothing.
- **Data-modifying CTE with `LEFT JOIN counted ON true`**: the CTE runs exactly once. The main query always returns one
  row, with `used` NULL when the request was refused.
- **Refused path**: the follow-up `SELECT` runs in the same transaction with a fresh READ COMMITTED snapshot and the row
  lock still held, so it reads the current value.
- **Refund**: bounded by `used > 0` and targets the consumed `usage_date`, so it stays correct across midnight.
- **Lint**: no unused imports found, `git diff --check` is clean, and there is no configured linter.

## Recommended actions (priority order)
1. Decide M1 (option A, B or C). If B, implement it in the wrapper only.
2. M2: add the compose passthrough with non-empty defaults (can go with phase 2).
3. L1 and L2: harden `check_timezone` to check the zone against `pg_timezone_names` and probe the table.
4. L3: add the three missing tests.
5. L4 and L7: expose `Retry-After` or document `resetsAt` for browsers; add the refusal log line.

## Unresolved questions
- M1: should `llm_error` refunds exclude turns where a round already succeeded, or where the error was a client timeout?
- L5: is one permanent `turns` row per refused request acceptable, or should a read-only pre-check refuse without opening a turn?
- M2: should the compose change land in this commit or in phase 2? The plan's file list omitted compose.

Status: DONE_WITH_CONCERNS
Summary: Every phase-1 acceptance criterion is met, the SQL is correct under concurrency, and the full suite shows no
regressions (the only non-passes are environmental). Nothing is Critical or High.
Concerns/Blockers: M1 (the `llm_error` refund also covers billed timeouts and later-round failures; needs a user
decision) and M2 (compose does not pass the new env vars, so limits cannot be tuned without editing compose).
