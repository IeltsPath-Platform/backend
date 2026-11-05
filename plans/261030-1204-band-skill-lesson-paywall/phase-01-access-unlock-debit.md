---
phase: 1
title: "Access unlock debit"
status: pending
priority: P1
effort: "2h"
dependencies: []
---

# Phase 1: Access unlock debit

## Overview

Let learning-service spend Points for a course unlock. Access only gains a new ledger transaction type; wallet, keys and endpoints stay.

## Requirements

- Functional: `referenceType = "COURSE_UNLOCK"` on the existing internal debit maps to a new `COURSE_UNLOCK_DEBIT` ledger type.
- Non-functional: idempotent by key (existing behaviour), balance never negative, existing types unaffected.

## Tests first (red)

In `backend/services/access-service/src/test/java/com/ieltspath/access/`:
- `DebitPointsUseCaseTest`: COURSE_UNLOCK debit writes a `COURSE_UNLOCK_DEBIT` entry; retry with same key and same amount returns the original entry; insufficient balance throws; unknown reference type still maps to `AI_GRADING_DEBIT`.
- Persistence test: ledger accepts `COURSE_UNLOCK_DEBIT` after the migration (mirror the existing test for `HEART_REFILL_DEBIT`).

## Related Code Files

- Create: `access-service/src/main/resources/db/migration/V5__course_unlock_debit.sql` - drop and re-add `chk_point_ledger_entries_transaction_type` with all current values (see V4) plus `COURSE_UNLOCK_DEBIT`.
- Modify: `access/domain/vo/PointTransactionType.java`, `access/application/usecase/DebitPointsUseCase.java` (switch near line 72), `access/application/usecase/PointDebitWriter.java` (switch near line 66).

## Implementation Steps

1. Write the failing tests above.
2. Add the enum value and the two `case "COURSE_UNLOCK"` branches.
3. Add V5 migration keeping every existing value in the CHECK list.
4. Run access-service tests.

## Success Criteria

- [ ] Tests above green; existing access tests green.
- [ ] Migration applies on a DB that already has ledger rows.

## Risk Assessment

- Dropping a CHECK constraint: copy the full value list from V4 so no existing row is rejected. Rollback: restore V4 list (only safe while no `COURSE_UNLOCK_DEBIT` rows exist).
