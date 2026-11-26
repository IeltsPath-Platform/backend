# Phase 5 - status changes follow the lifecycle (access, assessment)

Rule: SRS DR-43. Size M. Status: proposed.

## Context
- Statuses are enums with `CHECK` lists in SQL. Nothing stops a code path moving an `EXPIRED` subscription back to `ACTIVE`.
- Enums: access `SubscriptionStatus`, `KeyStatus` (`domain/vo`); assessment `GradingJobStatus`
  (`QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED`), the human review status enum and `SubmissionStatus`.
- Allowed moves come from SRS section 3.2.

## Requirements
1. Each enum has `canMoveTo(next)`; the domain method that changes the status calls it and throws the existing
   invalid-state exception when the move is not allowed.
2. The allowed moves are written once, in the enum, and tested as a table.
3. No schema change; the `CHECK` lists stay.

## Allowed moves (from SRS 3.2)
- Subscription: ACTIVE to EXPIRED, ACTIVE to CANCELLED. EXPIRED and CANCELLED are final.
- Activation key (`KeyStatus`): ACTIVE to REDEEMED, REVOKED, EXPIRED. The three are final.
- Grading job: QUEUED to PROCESSING, CANCELLED; PROCESSING to COMPLETED, FAILED, CANCELLED, and back to QUEUED for a retry if the code does that today (confirm). COMPLETED, FAILED, CANCELLED final.
- Human review: QUEUED to ASSIGNED to IN_REVIEW to COMPLETED; a queued review may be re-assigned (confirm in `ExaminerAssignmentService`).
- Submission: DRAFT to SUBMITTED.

## Files
- The enums above, plus the aggregates that change them: `Subscription` (`extend`, cancel, new `expire`), `ActivationKey` (`redeem`, `revoke`, the expiry at line ~120), the grading job and human review domain classes.
- Table-driven tests next to each enum.

## Steps
1. For each enum, read every place that assigns the status (grep `setStatus`, `status =`, SQL `UPDATE ... SET status`). Record
   moves the code really makes, including retries. If the code makes a move that SRS 3.2 forbids, decide with the owner
   whether the code or the SRS is right before writing `canMoveTo`.
2. Add `canMoveTo` and use it. JDBC `UPDATE ... SET status` paths that bypass the aggregate get a `WHERE status IN (allowed from)` guard instead.
3. Land the Subscription part first because phase 1 needs `expire()`.

## Tests
- For every pair of statuses of each enum, assert allowed or refused (the table is the specification).
- Existing use case tests still pass; add one test per refused move showing the invalid-state error.

## Risks and rollback
- A legitimate retry path that goes backwards is refused in production; step 1 exists to find it.
- Rollback: revert the PR; no data change.
