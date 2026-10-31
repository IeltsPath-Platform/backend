---
phase: 5
title: "Frontend paywall and admin prices"
status: done
priority: P2
effort: "8h"
dependencies: [3, 4, "261030-1200-course-cover-description"]
---

# Phase 5: Frontend paywall and admin prices

## Overview

Show lock state, let the learner unlock a skill or combo, remove the permanent Premium lock, and let admin set prices.

## Requirements

- Functional:
  - Lesson list distinguishes "Cần mở khóa" (paywall) from "Chưa học xong bài trước" (progress), and marks free-preview lessons.
  - Paywall dialog on a locked lesson or a locked skill: skill price, combo net price (with deduction), current Point balance, button to unlock, link to top up Points (existing `KeyActivationDialog`), Premium mention. Insufficient points shows the shortfall.
  - Knowledge tree (`KnowledgeTreePage`) shows `locked` nodes differently from "not started" ones and offers the unlock dialog instead of the practice button; free-practice start handles 403 `PAYWALL` the same way as a locked lesson.
  - Skills missing from `availableSkills[]` (Speaking today) show "sắp ra mắt" and have no unlock button.
  - A purchase that fails with 503 or a lost response is not shown as failed money: refresh the course list (the server reconciles pending purchases) and show the resulting state.
  - `mapTopicSummary` stops forcing Premium topics to `LOCKED` (no topic is Premium-only any more); use `premium` from the course list.
  - Admin `TopicForm` requires a skill for a topic that belongs to a course: its skill select currently offers "Chưa chọn" and sends `skill: null`; make the empty choice invalid when a course is selected. It has no Premium or feature-key field (checked), so nothing to remove there.
  - Copy for a skill whose topic test is unreachable ("mở khóa kỹ năng để làm bài kiểm tra chủ đề") on the topic page.
  - Admin course edit drawer (`CourseForm`): one block "Giá mở khóa" with a price input for each of the four skills and one for the combo, per course; empty = not for sale; R&D can change any of them at any time. Validates positive integers, combo >= highest skill price and, with two or more skill prices, combo <= their sum; shows the server's validation error. No hint text under fields.
- Non-functional: pass an `Idempotency-Key` per purchase attempt and reuse it on retry; handle 403 `PAYWALL`, 409 codes and insufficient points; keyboard accessible dialog.

## Tests first (red)

- Adapter tests: DTO -> `CourseSummary` unlock fields; lesson summary paywall flag; a topic is no longer forced to `LOCKED`; `TopicForm` rejects a course topic without a skill.
- Component tests: `UnlockDialog` shows prices and net combo price, disables unlock when thiếu Point and shows shortfall, calls API once on double click, reuses key on retry; lesson list renders the two lock reasons differently.
- Admin: `CourseForm` validates positive integers and the combo rules above, sends the skill-price map and the combo price, null when empty; editing one skill price re-validates the combo; reopening the drawer shows the saved prices.

## Related Code Files

- Create: `features/learning-path/components/UnlockDialog.tsx`. No client-side price math: show the skill's entry in `skillPrices`, `comboPayablePoints` and balance exactly as the server returns them.
- Modify: `types/learningPath.ts`, `features/learning-path/api/learningAdapters.ts`, `httpLearningApi.ts`, `learningApi.ts`, mock API, `components/TopicLessonsPanel.tsx`, `pages/CourseListPage.tsx`, `pages/TopicListPage.tsx`, `pages/TopicPage.tsx`, `pages/KnowledgeTreePage.tsx` (+ its test and `api/mock/mockTree.ts`; `Node.locked`), `api/apiError.ts`/`errorHint.ts` (new codes), CSS in `learning-path.css`.
- Modify (admin): `features/admin/courses/types.ts`, `components/CourseForm.tsx`, `api/courseApi.ts`, related tests.
- Reuse: the existing Point balance call in `features/home/accessApi.ts` (also `features/auth/api/accessApi.ts`); top-up dialog `features/home/components/KeyActivationDialog.tsx`. `UnlockDialog.tsx` does not exist yet and is created here.

## Implementation Steps

1. Write failing adapter, component and form tests.
2. Types, adapters, API client, mock.
3. `UnlockDialog` and wire it from the locked lesson row and the skill header.
4. Update lock rendering and Premium handling.
5. Admin price inputs.
6. Check in browser: free learner, learner with Points, Premium learner, thiếu Point.

## Success Criteria

- [x] New and touched FE tests pass; lint and `tsc` clean.
- [x] Learner can request skill and combo unlocks through the typed learning API; real purchase persistence/debit is covered by learning-service integration tests.
- [x] Locked and progress-locked lessons look and read differently.

## Risk Assessment

- Cover-description plan edits the same files: start after it merges to avoid conflicts.
- UI trusting its own price math: always show the server's `comboPayablePoints`.

## Completion

- Course and lesson adapters carry server pricing/ownership, paywall and free-preview facts; topic status no longer invents a Premium lock.
- Learners can open the accessible unlock dialog from course cards, paywalled lesson rows, and locked knowledge-tree points. It shows server prices, current Points balance, shortfall, Premium access and the existing Points top-up dialog. Purchases use a UUID `Idempotency-Key`; uncertain responses reload and reconcile course state, and retries reuse the same key.
- Course editing saves a per-skill price map and nullable combo price with positive-integer and combo-boundary validation. Course topics require a skill in `TopicForm`.
- Added adapter, unlock API/modal and price-form coverage. Focused touched suites passed. The final full frontend run passed 1,695/1,696 tests across 202 files; its only failure is the unrelated mixed-up-answer copy assertion in `flashcards/lib/owl-reactions.test.ts`.
- Typecheck, lint and production build passed. Browser verification against a running authenticated API was not available in this environment.
