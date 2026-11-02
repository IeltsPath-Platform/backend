---
title: "First-attempt suggested review"
description: "A first counted Practice failure suggests a review instead of blocking; it blocks only when the same knowledge point is weak again"
status: pending
priority: P2
branch: "main"
tags: [learning-service, review, practice, frontend, tdd]
blockedBy: [261009-1417-practice-gated-lesson-unlock]
blocks: []
created: "2026-11-01T16:52:25.971Z"
createdBy: "ck:plan"
source: skill
---

# First-attempt suggested review

Source: [brainstorm report](../reports/brainstorm-261101-2345-first-attempt-practice-review-report.md). Mode: `--tdd` (each phase writes its failing tests first).

## Overview

Today the first counted submission of a Practice set below 70% creates a BLOCKING review for every knowledge point (KP) under 70%, even a KP with one question. This plan changes it to:

1. A KP counts as weak only with at least 2 wrong answers and under 70% (the set or skill must still be under 70%).
2. The first weakness creates a **non-blocking suggested review** (`trigger_kind = PRACTICE`, `blocking = false`), with the theory step when the KP scored under 40% or was wrong in the lesson.
3. A later counted submission that finds the same KP weak while its suggestion is still pending promotes it to blocking.
4. A suggested review whose set is failed becomes blocking at the theory stage, not SKIPPED.
5. `PracticeClearance` clears a failed first attempt when the skill has no pending blocking review: one reason, `ATTEMPT_NOT_BLOCKED`, computed on every read and never stored, so the learner continues.

Two gates exist today and both must be handled: `REVIEW_REQUIRED` (pending blocking review of the skill) and `PRACTICE_REQUIRED` (`PracticeClearance`). Changing only the review flag leaves the second gate shut.

## Implementation handoff

Codex implements; Claude reviews. Start with [codex-handoff.md](./codex-handoff.md): branches
(`feat/first-attempt-suggested-review` from `feat/course-skill-board` in `backend/` and `frontend/`), work packages A-D, boundaries,
commands and report format. Each phase file holds the exact code changes, the tests to write first (with code) and the
existing tests to change by line, so no design choice is left to the implementer.

Decisions fixed for implementation (in addition to the logs below):
- Weak KP: `wrong >= 2` and KP percent `< 70%`; set or skill percent still `< 70%`; an unseen spare set must exist.
- First weakness: non-blocking review, trigger `PRACTICE`, stage from `ReviewRule.initialStage`.
- Promotion: KP weak again while a pending non-blocking review with trigger `PRACTICE` or `ERROR_PATTERN` exists;
  the trigger is kept; promoted reviews appear in `reviewsCreated` with `blocking: true`.
- A failed set on a non-blocking `PRACTICE` review makes it blocking at the theory stage; other optional reviews keep
  ending `SKIPPED`.
- Practice clearance: `ATTEMPT_NOT_BLOCKED`, derived and never stored; also counts as practice-cleared for the topic test.
- Frontend: suggestion notice "Nên ôn lại ... Bạn vẫn học tiếp được." with "Ôn ngay"; set label "Nên ôn lại"; soft
  status text; review page wording unchanged.

## Phases

| Phase | Name | Depends on | Status |
|-------|------|------------|--------|
| 1 | [Weak KP rule with minimum evidence](./phase-01-weak-kp-rule-with-minimum-evidence.md) | - | Pending |
| 2 | [Suggested review model](./phase-02-suggested-review-model.md) | 1 | Pending |
| 3 | [Submit flow: suggest and promote](./phase-03-submit-flow-suggest-and-promote.md) | 1, 2 | Pending |
| 4 | [Practice clearance (derived, no migration)](./phase-04-practice-clearance-and-migration.md) | 2 | Pending |
| 5 | [Frontend suggestion UX](./phase-05-frontend-suggestion-ux.md) | 3, 4 | Pending |
| 6 | [Verification and docs](./phase-06-verification-and-docs.md) | 1-5 | Pending |

Phases 3 and 4 ship together: after phase 3 alone a failed first attempt leaves Practice REQUIRED with only a suggestion pending, so the learner would be stuck on the lesson list.

## Confirmed facts (checked in code)

- The stored practice pass reason is sticky: `PracticeProgress.persistPassed` calls `LessonPracticePassRepository.insertIfAbsent` ("stored rows are never overwritten") and `PracticeClearance.derive` returns PASSED whenever a stored reason exists. Red Team decision: the new reason is **derived, never stored**, because the previous code reads stored reasons with `PracticePassReason.valueOf` (`JdbcLessonPracticePassRepository.java:23`) and would throw after a rollback.
- `lesson_practice_passes.reason` has a CHECK on four values (V4). Because the new reason is never stored, there is no migration and no CHECK change; V41 stays unused by this plan.
- `JdbcPracticeAttemptRepository.findForTopic` loads only counted attempts that passed (`passed = TRUE OR cardinality(passed_skills) > 0`). The clearance rule cannot see a failed counted attempt today; phase 4 adds it.
- `usePendingReviews` / `ReviewGateBanner` keep only reviews with `blocking !== false`, so suggestions never raise the banner. But `LessonPractice.submit` builds `ReviewRef` without `blocking`, which counts as blocking: it must pass the flag.
- The review page already renders the theory step by `stage`, independent of `blocking`; only the header says "Bài ôn tự chọn" for any non-blocking review.
- `ReviewReevaluation` (assessment path) calls `promoteToBlocking` for every candidate, which also flips a pending suggestion. Accepted: a second, independent signal.

## Cross-plan

`261009-1417-practice-gated-lesson-unlock` (phases 1-3 done, phase 4 open) defines the rules this plan amends: its acceptance criteria say a failed Practice always yields a blocking review (`REVIEW_REQUIRED`) and that one DONE plus one PENDING review keeps Practice `REQUIRED`. Finish its phase 4 first; this plan then records the amended criteria in phase 6.

## Acceptance criteria

- A set under 70% where no KP has 2 wrong answers creates no review and clears Practice (`ATTEMPT_NOT_BLOCKED`); the next lesson opens.
- A KP with 2+ wrong and under 70% creates a non-blocking review; the next lesson opens (`ATTEMPT_NOT_BLOCKED`); no banner; the result card says to review.
- The same KP weak in another counted set while the suggestion is pending turns it blocking (`REVIEW_REQUIRED` on the next lesson of that skill).
- Doing the suggested review and failing its set makes it blocking at the theory stage; passing it makes it DONE.
- Retaking a revealed set creates and promotes nothing.
- Existing blocking reviews, assessment and error-trap reviews behave as before.
- A suggested review appears in the review basket as an optional review, with no duplicate row for the KP.
- A pending `ERROR_PATTERN` review on a KP that turns weak in a counted set becomes blocking.
- Rolling the code back leaves no data the previous code cannot read: nothing new is stored.
- Backend tests (learning-service) and the touched frontend tests pass.

## Out of scope

Error-trap reviews, `DiagnosticFlow`, `ReviewReevaluation`, basket and free-practice optional reviews, Writing criterion reviews, content-service, auto-closing a suggestion when a later set on the same KP is passed.

## Unresolved questions

None. The three earlier questions were settled in Validation Session 1 (below).

## Validation Log

### Session 1 - 2026-11-01
**Trigger:** `/ck:plan validate` after the plan was written from the brainstorm report.
**Questions asked:** 7

> Superseded in part by the Red Team Review below: question 3 (now `PRACTICE` and `ERROR_PATTERN` are promoted), the rationale of question 4 (rollback needed the stored reason removed, so phase 4 no longer has a migration), and the two new pass reasons (merged into one derived reason).

#### Verification Results
- **Tier:** Full (6 phases)
- **Claims checked:** 32
- **Verified:** 30 | **Failed:** 2 | **Unverified:** 0 (1 ambiguous claim resolved during the pass)

Failures:
1. [Fact Checker] Phase 2 said the repository tests sit "beside the existing review repository tests"; `find` shows no test for `JdbcReviewItemRepository`. Only `ReviewItemTest` exists (`domain/aggregate`).
2. [Fact Checker] Phase 4 said to update a `PracticePassReason` union in the frontend; `learningAdapters.ts` and `types/learningPath.ts` type it as `string | null` and no union exists.

Contract facts recorded (counts): `PracticeReviewRule.derive` 2 call sites (`SubmitPracticeAttemptUseCase.reviewNeeds`) + `PracticeReviewRuleTest`; `ReviewFact` 1 main site (`PracticeProgress:95`) and many test sites using the 1- and 2-argument constructors; `AttemptFact` 1 main site (`PracticeProgress:90`); `PracticeReviewState` 2 main sites (`JdbcReviewItemRepository` lines 71 and 95); `ReviewCreated` exists twice (`domain/vo/PracticeSubmission`, `api/dto/response/PracticeSubmissionResponse`) with 2 main constructors and 1 test use (`MultiSkillLessonIntegrationTest:180`); `FirstPass` / `TopicAttempts` are used by 3 files (`PracticeAttemptRepository`, `JdbcPracticeAttemptRepository`, `PracticeProgress`); `blocking()` has 10 consumers (diagnostics, quests, report, basket, review page, submit review, reevaluation).

Flow facts: `refreshPassForLesson` runs at the end of the practice submit, after the review insert, so the stored reason sees the new suggestion; `promoteToBlocking` overwrites `trigger_kind`, so the new promote method keeps `PRACTICE`; `ReviewReevaluation` promotes any pending non-blocking review of a candidate KP, including a suggestion (accepted); the first-pass loader returns only passed attempts, so failed attempts need a new fact (phase 4). Scope: `ReviewItem` is loaded per request, so a mutable `blocking` is safe; the stored pass reason is process-wide data and sticky, as documented.

#### Questions & Answers

1. **[Tradeoffs]** Phase 2 says to write repository tests beside the existing review repository tests, but none exist for `JdbcReviewItemRepository`. How to fix?
   - Options: Create a new `JdbcReviewItemRepositoryIntegrationTest` | Test only through use-case integration tests
   - **Answer:** Create a new `JdbcReviewItemRepositoryIntegrationTest`
   - **Rationale:** the new promote SQL and the `blocking` persistence need direct coverage.
2. **[Assumptions]** Phase 4 says to update a frontend `PracticePassReason` union, but the frontend types it as `string | null`. How to fix?
   - Options: Drop the item | Introduce a union type
   - **Answer:** Drop the item
   - **Rationale:** the frontend only compares with `NO_PRACTICE`; two new string values need no type change.
3. **[Scope]** A KP turns weak while a non-blocking review from another trigger (basket, free practice) is pending. Handling?
   - Options: Promote only PRACTICE suggestions | Promote any non-blocking review (today's behavior) | Do nothing
   - **Answer:** Promote only PRACTICE suggestions
   - **Rationale:** a review the learner picked stays optional; the unique pending index forbids a second row.
4. **[Risks]** The change edits a core progression gate. A switch to turn the new rule off?
   - Options: No, one change set | Config `learning.practice.suggest-first`, default on
   - **Answer:** No, one change set
   - **Rationale:** YAGNI; V41 only widens a CHECK, so rollback is a revert.
5. **[Scope]** A suggested review will appear in the review basket as an optional review. Handling?
   - Options: Show it in the basket | Hide it until promoted
   - **Answer:** Show it in the basket
   - **Rationale:** no code needed; add one scenario to phase 6 for duplicates.
6. **[Scope]** Auto-close a suggestion when the learner later passes a set on the same KP?
   - Options: No, it stays until done | Yes, close it as DONE
   - **Answer:** No, it stays until done
   - **Rationale:** YAGNI; one pending review per KP at most.
7. **[Architecture]** Review page label for a non-blocking review.
   - Options: Keep "Bài ôn tự chọn", no backend change | "Bài ôn gợi ý" for PRACTICE-triggered (needs `triggerKind` in the response)
   - **Answer:** Keep "Bài ôn tự chọn", no backend change
   - **Rationale:** avoids a new response field; the label changes when the review becomes blocking.

#### Confirmed Decisions
- Repository coverage: new `JdbcReviewItemRepositoryIntegrationTest`.
- No frontend union type; no feature flag.
- Promote only `PRACTICE` suggestions; basket optional reviews untouched.
- Suggestions show in the basket; they are never auto-closed.
- Review page wording unchanged.

#### Action Items
- [x] Phase 2: name the new repository test and the two `PracticeReviewState` sites.
- [x] Phase 4: remove the frontend union item; list the `FirstPass` consumers; define "failed for a skill".
- [x] Phase 3: list both `ReviewCreated` records and the test use; keep a 3-argument convenience constructor.
- [x] Phase 5: drop the review page wording change.
- [x] Phase 6: name the contract file; add the basket scenario; record no feature flag.

#### Impact on Phases
- Phase 1: test file exists; two call sites noted.
- Phase 2: new repository test; `PracticeReviewState` sites.
- Phase 3: `ReviewCreated` records; basket decision.
- Phase 4: no frontend type change; consumers resolved; per-skill failure rule.
- Phase 5: no review page change.
- Phase 6: contract file; basket scenario; no flag.

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01 to phase-06
- Decision deltas checked: 7
- Reconciled stale references: 5 (phase 1 test file and call sites, phase 2 repository test, phase 4 frontend union and consumers, phase 5 review page wording, phase 6 contract file)
- Unresolved contradictions: 0

## Red Team Review

### Session 1 - 2026-11-01
Lenses: Security Adversary, Assumption Destroyer, Failure Mode Analyst, Scope & Complexity Critic, run inline by the controller (no reviewer subagents). Every finding below carries `file:line` evidence; findings without it were not accepted.

**Findings:** 6 raised (2 High, 2 Medium, 2 Low), 6 more rejected on evidence. Accepted: 6. Rejected: 6.

| # | Severity | Finding | Evidence | Disposition |
|---|----------|---------|----------|-------------|
| 1 | High | Rollback is unsafe if the new pass reasons are stored: the previous code parses stored reasons with `valueOf` and throws | `JdbcLessonPracticePassRepository.java:23`, `PracticeProgress.persistPassed:73` | Accept: derive the reason, never store it, drop V41 |
| 2 | High | Practice gate becomes skippable: submitting any failing set clears Practice; only hearts bound it | `PracticeClearance.java:64-73`, `SubmitPracticeAttemptUseCase.java:126` | Accept as the cost of the soft gate; add a pass-through metric (phase 6) |
| 3 | Medium | A pending `ERROR_PATTERN` review on the KP blocks both creating a suggestion and promoting, so the KP never escalates | `ErrorTrapReviews.java:111`, `JdbcReviewItemRepository.java:105-111` | Accept: promote `PRACTICE` and `ERROR_PATTERN`, keep user-picked tiers optional |
| 4 | Medium | Two new reasons (`REVIEW_SUGGESTED`, `NO_WEAK_POINT`) are indistinguishable to every consumer | `lib/lessonPractice.ts:5`; no other service reads the reason | Accept: one reason `ATTEMPT_NOT_BLOCKED` |
| 5 | Low | Basket sends `reason = "PRACTICE"` for a suggestion; the frontend type lists four values | `GetReviewBasketUseCase.java` (reason from `triggerKind`), `types/learningPath.ts:635`, `masteryGuideAdapters.ts:47` | Accept: add `PRACTICE` to both types (phase 5) |
| 6 | Low | Practice status text says "Đã đạt phần luyện tập" even when the learner failed | `LessonPractice.tsx:55` | Accept: separate text for the derived reason (phase 5) |

Rejected on evidence: `passed_skills` null on failed attempts (`V9__practice_skills.sql:4` NOT NULL default, `JdbcPracticeAttemptRepository.java:204`); concurrent submits (`SubmitPracticeAttemptUseCase.java:118` takes the learner lock); other services reading the reason or `reviewsCreated` (none found); error-trap reviews blocking at first submission (`ErrorTrapReviews.java:111` inserts `blocking=false`); re-escalating after a suggestion was finished and the KP fails again (the review loop worked; YAGNI); duplicate nudge in the suggestions strip (`GetSuggestionsUseCase.java:84-86`, cosmetic). Also confirmed true: the basket lists pending non-blocking reviews and starting one returns the existing review (`GetReviewBasketUseCase.java`, `StartBasketReviewUseCase.java:30-31`).

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01 to phase-06
- Decision deltas checked: 6 (one derived reason, no migration, promote `PRACTICE` + `ERROR_PATTERN`, pass-through metric, basket type, status text)
- Reconciled stale references: 7 (overview item 5, phase table title, sticky and CHECK facts, two acceptance criteria, phase 2 promote triggers, phase 3 promote triggers, phase 4 rewritten, phase 6 rollback and contract wording)
- Historical lines kept on purpose in the Validation Log (Q3 answer, Q4 rationale), marked as superseded at the top of that session
- Unresolved contradictions: 0
