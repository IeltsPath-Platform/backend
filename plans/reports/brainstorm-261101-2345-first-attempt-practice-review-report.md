# Brainstorm: first-attempt Practice review (suggest first, block on repeat)

Date: 2026-11-01. Scope: learning-service + learning-path frontend. Status: design agreed, not implemented.

## Problem

First counted submission of a Practice set below 70% creates a BLOCKING review for every knowledge point (KP) with per-KP correct ratio < 70%, with no minimum evidence. One wrong answer on a KP with a single question (0/1) blocks all later lessons of that skill. Hearts are already lost for the same wrong answers (double penalty). A first attempt on new material is expected to be shaky.

## Requirements

- Expected output: weak-KP rule with minimum evidence; first weak result creates a non-blocking SUGGESTED review with theory when warranted; later weakness or a failed suggested review makes it blocking.
- Acceptance: see "Exact rules" and "Test matrix".
- Out of scope: error-trap reviews (separate flow), DiagnosticFlow (prerequisite probing), assessment/mastery-based reviews (ReviewReevaluation), review basket optional reviews, Writing criterion reviews.
- Constraints: keep one PENDING review per (user, KP); existing blocking reviews unchanged; no data backfill.
- Touchpoints: SubmitPracticeAttemptUseCase, PracticeReviewRule, ReviewItem, ReviewItemRepository (Jdbc), PracticeClearance + PracticePassReason, GetReviewUseCase, SubmitReviewUseCase, PracticeSubmission response (ReviewCreated), FE LessonPractice, ReviewGateBanner, lib/reviewGate, learningAdapters.

## Scout findings that shape the design

1. Two gates today: `REVIEW_REQUIRED` (pending blocking review of the same skill) and `PRACTICE_REQUIRED` (`PracticeClearance`). Making a review non-blocking alone does NOT let the learner continue: a pending PRACTICE review keeps Practice at REQUIRED.
2. Machinery exists: `review_items.blocking`, `trigger_kind`, `promoteToBlocking`, unique pending per (user, KP).
3. Non-blocking (optional) reviews have no theory step (`startWithTheory` throws) and end SKIPPED when failed.
4. `PracticeReviewRule` has no minimum evidence per KP.
5. A stored practice pass reason is sticky (`stored != null` returns PASSED). Confirm in planning.

## Approaches considered

| | Approach | Verdict |
|---|---|---|
| A | Minimum evidence only, keep immediate block | Small, but first-attempt block remains |
| B | Suggest first, block on repeat | **Chosen** |
| C | Tier by severity (block below 40%, suggest 40-70%) | Simpler, but a heavily wrong first attempt still blocks |

B wins because "weak twice" is stronger evidence than "weak once", and it reuses `promoteToBlocking` and the one-pending-per-KP constraint. Cost: PracticeClearance and the theory step.

## Exact rules

Applies only to a counted (first) submission of a set; a retake of a revealed set changes nothing.

1. **Weak KP**: set/skill percent < 70% (existing), and for the KP: wrong answers >= 2 AND kp percent < 70%. Existing filters stay (no pending BLOCKING review of the KP, an unseen alternative set with >= 3 questions exists, Writing excluded, judged per skill).
2. **First weakness**: insert a review with `blocking = false`, `trigger_kind = PRACTICE`. Stage THEORY when kp percent < 40% or the KP was wrong in the lesson's exercises (existing `ReviewRule.initialStage`), else PRACTICE. Theory is now allowed on a non-blocking PRACTICE review.
3. **Weak again** (promote to blocking via `promoteToBlocking`): a later counted submission finds the KP weak by rule 1 while its PENDING non-blocking PRACTICE review exists. This one rule covers "weak again" and "skipped the suggestion, then failed again".
4. **Suggested review failed**: the learner answers the review's set and fails -> the review becomes blocking, stage THEORY (first failed set), status stays PENDING. Not SKIPPED. Passing -> DONE as today.
5. **Practice clearance** (new reasons in `PracticePassReason`): for a counted failed attempt, Practice of that skill is cleared when every PENDING PRACTICE review of the skill is non-blocking (reason `REVIEW_SUGGESTED`), or when the attempt created no review because no KP had enough evidence (reason `NO_WEAK_POINT`). Existing reasons unchanged. Blocking reviews still hold back progress through `REVIEW_REQUIRED`.
6. Response: `ReviewCreated` gains `blocking: boolean` (additive).

## Edge cases

- Same set retaken: not counted, no review, no promotion.
- Same KP weak in two sets in one session: first suggests, second promotes.
- Weak again in a different lesson: promoted review keeps its original lesson; blocks by its skill.
- Pending blocking review exists for the KP: no change (existing filter).
- Pending non-blocking review from another trigger (basket, free practice): **open question** - leave it optional, or promote as today. Proposed: promote only `trigger_kind = PRACTICE`.
- No alternative set available: no review is created; clearance rule 5 `NO_WEAK_POINT` applies, no deadlock.
- Mixed-skill sets: each objective skill judged on its own items (existing).
- Error-trap reviews for the same KP: unique pending constraint makes the later insert a no-op.
- Suggestions pile up: at most one per KP; visible in the review basket. No expiry (YAGNI).
- Learner later passes a set on the KP with a pending suggestion: suggestion stays until done; consider closing it (open question, YAGNI for now).

## Data and migration

- `review_items`: `blocking` and `trigger_kind` exist. `blocking` becomes mutable on failure (UPDATE like `promoteToBlocking`). No backfill; existing blocking reviews unchanged.
- `PracticePassReason` new values: check for a CHECK constraint on the stored column and add a migration if present.
- No content-service change.

## UI impact (frontend)

- Submission result: for a non-blocking review show "Nen on lai: <KP>" with an optional link, not the gate wording.
- `ReviewGateBanner`: must list blocking reviews only (verify `usePendingReviews`).
- Review page: show the theory step for a non-blocking PRACTICE review (today optional reviews skip it).
- Lesson list: next lesson opens when clearance passes via the new reasons; wording of the lock reason unchanged.
- Adapter: map `blocking` on `reviewsCreated` (default true for old payloads).

## Risks

- Learners ignore suggestions and carry gaps forward (intended trade-off; the mastery-based path still catches persistent weakness).
- Clearance change touches a core progression gate; sticky stored reason could hide a later promotion from the Practice gate (the review gate still blocks).
- Larger test matrix than the rule itself.

## Test matrix (first)

PracticeReviewRule: 0/1, 1/2 ignored; 0/2, 1/3, 2/5 weak; already pending blocking skipped. SubmitPracticeAttempt: first weak creates non-blocking review and does not block; second weak promotes; retake does nothing; no weak KP clears Practice. ReviewItem/SubmitReview: failed suggested review becomes blocking with theory. PracticeClearance: new reasons, existing reasons unchanged. Frontend: result card wording, banner shows blocking only, review page shows theory for non-blocking.

## Success metrics

Share of first-attempt failures that end in a blocking review drops; suggestion -> DONE rate; promote rate; drop-off at the next lesson not worse; next-set pass rate on suggested KPs not worse.

## Next steps

Plan with tests first (this changes critical progression logic). Planning confirms: stored pass reason behavior, `PracticePassReason` DB constraint, `usePendingReviews` filtering.

## Unresolved questions

1. Non-blocking pending review from another trigger: leave optional or promote?
2. Close a suggestion automatically when the learner passes a later set on the same KP?
