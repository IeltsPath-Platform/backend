---
phase: 3
title: "Learning unlock purchase"
status: done
priority: P1
effort: "7h"
dependencies: [1, 2]
---

# Phase 3: Learning unlock purchase

## Overview

Persist what a learner unlocked and let them buy a skill or a band combo with Points, without ever losing Points if a step fails midway.

## Requirements

- Functional: `POST /api/learning/courses/{courseId}/unlocks` body `{ "target": "READING" | "LISTENING" | "WRITING" | "SPEAKING" | "COMBO" }`, header `Idempotency-Key` (UUID). `GET /api/learning/courses` also returns per course: `skillPrices` (map skill -> Points, absent = not for sale; prices differ per skill and per course), `comboUnlockPoints`, `unlockedSkills[]`, `comboUnlocked`, `comboPayablePoints` (after deduction), `availableSkills[]`, `premium`.
- Price rules (server-side only): skill = the price of that skill in the course's `skillPrices` (content-service, edited by R&D); combo = max(0, `comboUnlockPoints` - sum(`point_cost` of this learner's ACTIVE skill unlocks in the course)), where `point_cost` is what the learner actually paid, not the current price. Null price = `NOT_FOR_SALE` (409). Target already owned, or any target when combo owned = `ALREADY_UNLOCKED` (409), no debit. Skill not in the course's `skills[]` = `SKILL_NOT_AVAILABLE` (409), so Speaking cannot be sold before it has lessons.
- Durable intent: the purchase is recorded as a PENDING row (target, price, request id) in its own committed transaction BEFORE the debit; the row becomes ACTIVE after the debit succeeds. A PENDING row is completed by replaying the debit with the stored request id and stored amount (Access replays only an identical debit, so the stored amount keeps a retry valid even if prices change).
- Non-functional: no double debit; no Points lost if the process dies after the debit; per-user lock via `LearnerLock` (`JdbcLearnerLock`); insufficient Points maps to the existing `InsufficientPointsException` response.

## Tests first (red)

In `learning-service` tests:
- Domain: unlock pricing (a different price per skill; combo with 0, 1, 3 owned skills bought at different prices; floor at 0; null price; PENDING rows excluded from deduction).
- Price edited by R&D after a purchase: the ACTIVE unlock stays and keeps its paid `point_cost`; the next purchase uses the new price; the combo deduction uses the old paid amounts.
- Use case with fake `AccessClient`/content:
  - success writes PENDING, debits once with reference type `COURSE_UNLOCK`, ends ACTIVE with ledger id;
  - same `Idempotency-Key` twice debits once;
  - crash after debit (fake throws before marking ACTIVE): a new request, or `GET /courses`, completes the PENDING row with the stored request id and amount, balance dropped exactly once;
  - price changed between attempts: reconcile still uses the stored amount and succeeds;
  - insufficient points: PENDING row cancelled, no ACTIVE row, no debit;
  - Access unavailable: PENDING row kept, request fails with 503, later reconcile completes it;
  - combo owned blocks skill purchase; concurrent buys serialise.
- Persistence: unique `(user, course, coalesce(skill,'ALL'))`; ownership queries count ACTIVE only.
- Controller/security: user token required, courseId from path, 400 on bad target.

## Related Code Files

- Create: `learning-service/src/main/resources/db/migration/V38__learner_course_unlocks.sql` (`id, user_id, course_id, skill NULL=combo, point_cost, request_id UNIQUE, status PENDING|ACTIVE|CANCELLED, ledger_entry_id NULL, created_at, activated_at`; unique `(user_id, course_id, COALESCE(skill,'ALL'))` over non-cancelled rows).
- Create: domain aggregate/VO + repository (`LearnerCourseUnlock`, `LearnerCourseUnlockRepository`), JPA entity/adapter, `UnlockPricing` (pure), `UnlockCourseUseCase`, `ReconcilePendingUnlocks`, request/response DTOs.
- Modify: `api/controller/CourseController.java` (new POST), `application/usecase/ListCoursesUseCase.java` (reconcile PENDING first, then new fields) + `CourseResult`/`CourseResponse`, `api/exception/GlobalExceptionHandler.java` (409 codes if no existing exception fits).

## Implementation Steps

1. Write failing tests (pricing, then use case incl. crash cases, then API).
2. Migration and persistence.
3. `UnlockCourseUseCase`: lock user; reconcile any PENDING row of this learner; check ownership and availability; compute price; commit PENDING row (separate transaction); if price > 0 `access.debit(userId, price, "COURSE_UNLOCK", requestId, requestId.toString(), description)`; mark ACTIVE with ledger id; return state.
4. `ReconcilePendingUnlocks`: for each PENDING row, replay the debit with stored values; on success ACTIVE, on `InsufficientPointsException` CANCELLED, on `AccessUnavailableException` leave PENDING.
5. Extend course list; `premium` from `AccessClient.premium()` (only here, once per request; failure degrades to `premium = false` for display, never fails the list).

## Success Criteria

- [x] Tests cover the crash-before-activation replay, retry with the same request key, insufficient balance cancellation, Access outage, migration constraints and course-list unlock fields.
- [x] Access debit uses the stored amount and request id, so replay cannot create another charge.
- [x] Course list shows owned skills, combo deduction and Premium status.

## Risk Assessment

- Replaying a debit whose original did not happen charges at reconcile time; if the balance is then too low the row is cancelled and the learner sees the normal insufficient-points path.
- A learner who bought a skill at a high price and later sees a cheaper price pays no refund (refunds are out of scope); R&D should lower prices sparingly. The combo deduction keeps what was paid, so total spend on a band never exceeds the larger of the combo price and the sum already paid.
- Cancelled rows accumulate; they are tiny and keep an audit trail. No cleanup job in scope.

## Completion

- Added V38, the `learner_course_unlocks` repository, a `REQUIRES_NEW` intent writer, debit reconciliation, course unlock endpoint, course-list purchase fields and graceful Premium lookup fallback.
- Focused unit/API tests: 16 passed. Persistence and V38 Testcontainers tests: 3 passed. Combined focused runs: 21 passed, 0 failures/errors/skips.
- Full learning-service reactor: 1,019 tests passed, 0 failures/errors/skips. Surefire waited 30 seconds for broker-listener shutdown, then reported BUILD SUCCESS.
- Implementation commit is recorded in the acceptance report.
