---
phase: 5
title: "Frontend paywall and admin prices"
status: pending
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
  - Skills missing from `availableSkills[]` (Speaking today) show "sắp ra mắt" and have no unlock button.
  - A purchase that fails with 503 or a lost response is not shown as failed money: refresh the course list (the server reconciles pending purchases) and show the resulting state.
  - `mapTopicSummary` stops forcing Premium topics to `LOCKED`; use `premium` from the course list.
  - Admin `CourseForm`: two price inputs (skill, combo), no hint text.
- Non-functional: pass an `Idempotency-Key` per purchase attempt and reuse it on retry; handle 403 `PAYWALL`, 409 codes and insufficient points; keyboard accessible dialog.

## Tests first (red)

- Adapter tests: DTO -> `CourseSummary` unlock fields; lesson summary paywall flag; Premium topic no longer always `LOCKED`.
- Component tests: `UnlockDialog` shows prices and net combo price, disables unlock when thiếu Point and shows shortfall, calls API once on double click, reuses key on retry; lesson list renders the two lock reasons differently.
- Admin: `CourseForm` validates positive integers and combo >= skill, sends both fields, null when empty.

## Related Code Files

- Create: `features/learning-path/components/UnlockDialog.tsx`. No client-side price math: show `skillUnlockPoints`, `comboPayablePoints` and balance exactly as the server returns them.
- Modify: `types/learningPath.ts`, `features/learning-path/api/learningAdapters.ts`, `httpLearningApi.ts`, `learningApi.ts`, mock API, `components/TopicLessonsPanel.tsx`, `pages/CourseListPage.tsx`, `pages/TopicListPage.tsx`, `pages/TopicPage.tsx`, `api/apiError.ts`/`errorHint.ts` (new codes), CSS in `learning-path.css`.
- Modify (admin): `features/admin/courses/types.ts`, `components/CourseForm.tsx`, `api/courseApi.ts`, related tests.
- Reuse: Point balance source from `features/point-management` or the existing balance call.

## Implementation Steps

1. Write failing adapter, component and form tests.
2. Types, adapters, API client, mock.
3. `UnlockDialog` and wire it from the locked lesson row and the skill header.
4. Update lock rendering and Premium handling.
5. Admin price inputs.
6. Check in browser: free learner, learner with Points, Premium learner, thiếu Point.

## Success Criteria

- [ ] New and touched FE tests green; lint and `tsc` clean.
- [ ] Learner can unlock a skill and a combo end to end against the real API.
- [ ] Locked and progress-locked lessons look and read differently.

## Risk Assessment

- Cover-description plan edits the same files: start after it merges to avoid conflicts.
- UI trusting its own price math: always show the server's `comboPayablePoints`.
