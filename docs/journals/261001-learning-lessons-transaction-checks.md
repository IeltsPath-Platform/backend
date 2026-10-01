# Lesson transactions: replay, evidence, and binding failures

**Date**: 2026-10-01 17:24 (Asia/Saigon)
**Severity**: Medium
**Component**: learning-service lesson submission and mastery
**Status**: Resolved

## What Happened

The Java lesson slice implemented the [approved scope](../../plans/261001-1228-learning-service-java/phase-02-bai-hoc-va-mastery.md): gated lessons, submission replay, mastery, and review insertion. The first MVC run failed because controller path variables relied on unavailable compiler parameter metadata. Explicit names fixed MVC. A reversed-catalog test reproduced a deadlock against stale compiled code; forced recompilation cleared it. The full regression passed with no failures, errors, or skipped tests, including real PostgreSQL checks.

## The Brutal Truth

This is frustrating because correct math sat behind a broken HTTP entry point. Passing unit checks did not prove a learner could submit anything. Then source and bytecode disagreed, making a fixed concurrency defect fail again. That wastes debugging effort and undermines trust in the check being run.

## Technical Details

- `MasteryCalculator` retains the DeepTutor v1.6.9 Apache-2.0 source comment. Lan's original results `0.728571 / 0.875 / 0.487179` match the rounded `0.729 / 0.875 / 0.487`; thresholds were not changed.
- `LearnLessonUseCase.submit` takes `pg_advisory_xact_lock(hashtext(user_id))` inside one transaction, validates the owner of a saved `requestId`, and returns its stored response before current gates.
- Only the first submission of a block appends `kp_evidence`. Later attempts may pass the block without manufacturing more mastery evidence.
- `JdbcLearningProgressStore.findMastery` orders by evidence `ordinal`, fetches the latest five per KP, and retains the full evidence count. Lesson completion inserts eligible pending reviews in the same transaction.

## What We Tried

Explicit `@PathVariable("id")` and `@PathVariable("blockId")` fixed MVC binding. Review prompted UUID-sorted catalog upserts. The reversed-order test still deadlocked; `javap` confirmed target bytecode lacked the sorted batch already present in source. Forced recompilation made that test pass without weakening it.

## Root Cause Analysis

We omitted binding names in a build without reflection parameter names. Different users also update shared catalog rows in opposite Content order: their advisory locks differ, so row locks need stable ordering. The test then exercised stale bytecode, not the source fix.

## Decisions

Keep original mastery math and first-attempt evidence semantics. Replay saved responses before gates so newly inserted reviews cannot invalidate a successful retry. Sort shared catalog writes by KP UUID rather than trusting Content order. Reject broader compiler changes because local explicit bindings fix the affected controller without widening scope.

## Lessons Learned

Test HTTP binding, inspect shared rows before trusting per-user locks, and verify compiled artifacts when source and failures disagree. Preserve counts while bounding history reads; never alter expected math to make a port pass.

## Next Steps

- Implementer: archive the final checks in the [verification report](../../plans/261001-1228-learning-service-java/reports/learning-lessons-verification.md) at handoff.
- Reviewer: require replay, first-attempt evidence, and opposite-order catalog concurrency checks before accepting future submission changes.
- Documentation owner: update root architecture documentation in the already scheduled documentation phase.
