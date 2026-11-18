---
phase: 4
title: "Frontend pool picker and retake"
status: pending
priority: P1
dependencies: [2, 3]
effort: "L"
---

# Phase 4: Frontend pool picker and retake

## Overview
The learner picks a pool before the test, can retake from several places, can cancel an in-progress retake, sees a warning when a pool's ceiling is below their current band, sees a "try a higher pool" hint when the band reached the ceiling, and sees an honest "not graded" state for skills that were left out. Authors choose a pool when creating a placement package and see, on the existing Packages page, each package's pool and how many exams each pool has published. Work happens in the `frontend` repo.

## Requirements
- Functional:
  - New step "choose pool" with three cards (0-3, 3-5, 5-9), each with a short description and the exam count; a pool with no exam is disabled with "Chưa có đề"; hint "Không chắc thì chọn 3-5". When the learner has a current band and a pool's ceiling is below it, that card shows a warning ("Kho này tối đa X, thấp hơn band hiện tại Y; kết quả mới sẽ thay thế").
  - The attempt is created only when the learner confirms "Bắt đầu làm bài" after picking the pool, not when the pool card is clicked. If creating the attempt fails, the frontend calls release on the assignment and shows the error.
  - First-time flow: survey, pool, confirm, test, result. Retake flow: skip the survey, go to pool, confirm, test, result.
  - Resume rule: if the latest placement attempt is `IN_PROGRESS`, show it with the buttons "Tiếp tục" and "Hủy lượt thi này" (calls the cancel route); after cancel the page falls back to the pool picker (the page already treats `CANCELLED` as unusable).
  - Retake entry points: the result page, plus a persistent "Làm lại bài test đầu vào" link wherever a placement exists: the course list header area (next to the existing placement spotlight, which today shows only without a placement) and the overview. Each shows the retakes left in the last 24 hours and, at the limit, when the next one opens (`nextRetakeAt`).
  - Result page: confirmation that the new result replaces the old one; limit state; when `reachedCeiling` show the higher-pool hint with a button that opens the picker; skills missing from `skills` (Speaking or ungraded Writing) render as "Chưa chấm", never as band 0 and never as an error; when `partial` explain that part of the test could not be graded.
  - The recommended course on the result page is shown only after Learning reports the new band: poll `getPlacementOptions()` (reuse the page's existing polling constants) until `currentBand` equals the result's overall band, then fetch courses.
  - Admin package form: pool select, required and visible only for type `PLACEMENT_TEST`, sent in the body of `POST /api/content/admin/packages`.
  - Admin Packages page (`PackagesPage.tsx`): a "Kho" column showing the pool label for placement packages (empty for other types); a pool filter select shown only when the type filter is `PLACEMENT_TEST`, sent as `placementPool`; and, when the type filter is `PLACEMENT_TEST`, a summary strip with the three pools and the number of **published** exams in each (a pool below 2 is highlighted with "Cần thêm đề: lượt thi lại sẽ lặp đề"; 0 says "Chưa có đề, học viên không chọn được kho này"). The counts come from a separate fetch of `listPackages({ type: 'PLACEMENT_TEST' })` that ignores the status, pool, course, topic and text filters, so they do not change with them; count only `status === 'PUBLISHED'`.
- Non-functional: mobile-first, semantic HTML and keyboard focus, WCAG AA; loading, error, empty and limit states for every async step; shadcn primitives in `src/components/ui`, `lucide-react` icons, existing tokens (AGENTS.md); no new dependency.

## Architecture
- API layer (`src/features/learning-path/api`): replace `getPlacementTest()` by `getPlacementOptions()`, `assignPlacementTest(pool)` and `releasePlacementAssignment(id)` in `learningApi.ts`, `httpLearningApi.ts` and `mock/mockLearningApi.ts`; add `cancelAttempt(id)` for the cancel route; new types `PlacementPool`, `PlacementOptions`, `PlacementAssignment` in `src/types/learningPath.ts`; add error code `PLACEMENT_LIMIT_REACHED`; drop the `PLACEMENT_ALREADY_DONE` redirect.
- `PlacementPage.tsx`: `View` gains `'pool'`. `viewFor` stays based on the latest attempt (in progress resumes, submitted shows the result, cancelled or expired falls back to the picker or survey). A learner who already has a placement and opens the page lands on their latest result with the retake button instead of being redirected to `/learn`. `startTest` runs only on the explicit confirm and uses the assignment.
- New component `placement/PlacementPoolPicker.tsx`; changes in `PlacementResultView.tsx` and `ReportSkillPanels.tsx`; entry-point links in `pages/CourseListPage.tsx` (placement spotlight area) and the overview module (`features/overview`, `overviewViewModel.ts` shows the placement item only without a band today).
- Admin: `features/admin/packages/components/PackageCreateForm.tsx`, `packages/types.ts` and `packages/api/packageApi.ts` (`createPackage` posts to the admin base path; `listPackages` sends the `placementPool` filter) carry `placementPool`; `packages/pages/PackagesPage.tsx` gets the column, the filter and a small `PlacementPoolSummary` component (new file `packages/components/PlacementPoolSummary.tsx`, reusing the pool labels from `placement/placementPools.ts`). No new page and no new route: it is a view on the existing Packages page.

## Related Code Files
- Create: `placement/PlacementPoolPicker.tsx` (+ test), `placement/placementPools.ts` (labels and descriptions).
- Modify: `pages/PlacementPage.tsx`, `placement/PlacementResultView.tsx`, `placement/ReportSkillPanels.tsx`, `placement/placement.css`, `pages/CourseListPage.tsx`, overview module files, `api/learningApi.ts`, `api/httpLearningApi.ts`, `api/mock/mockLearningApi.ts`, `api/apiError.ts`, `src/types/learningPath.ts`, `features/admin/packages/components/PackageCreateForm.tsx`, `features/admin/packages/pages/PackagesPage.tsx`, `packages/types.ts`, `packages/api/packageApi.ts` and their tests (`packageApi.test.ts` asserts the admin path), plus existing tests that mock `getPlacementTest`.

## Implementation Steps (tests first)
1. **Red:** `PlacementPoolPicker` tests: three cards render with counts; empty pool disabled; selecting only selects (no attempt yet); the ceiling warning appears for a pool below the current band; keyboard operable.
2. **Red:** `PlacementPage` tests: first-time flow reaches the picker after the survey; retake from the result skips the survey; the attempt is created only on confirm; a failed attempt creation releases the assignment; limit error shows the limit state; learner with a placement and no attempt sees the latest result with the retake button (no redirect); an `IN_PROGRESS` attempt shows resume and cancel, and cancel returns to the picker.
3. **Red:** result view tests: `reachedCeiling` shows the hint (and not for the highest pool); missing Speaking or ungraded Writing shows "Chưa chấm"; `partial` explanation; retake button shows remaining count, the confirmation and the `nextRetakeAt` state; the recommended course waits for the new band.
4. **Red:** entry-point tests: the course list and overview show "Làm lại" when a placement exists and the existing spotlight behaviour is unchanged otherwise.
5. **Red:** admin form test: pool select appears and is required only for `PLACEMENT_TEST`, and is sent in the body posted to `/api/content/admin/packages`.
6. **Red:** admin Packages page tests: the "Kho" column shows the label for a placement package; the pool filter appears only with type `PLACEMENT_TEST` and sends `placementPool`; the summary strip counts only published exams per pool, flags a pool with fewer than 2 and one with 0, and does not change when the status or pool filter changes; the strip is absent for other types; `packageApi.test.ts` asserts the `placementPool` query parameter.
7. **Green:** implement API layer, page flow, picker, result changes, entry points, admin form and Packages page view, mock API.
8. Run `npm run typecheck`, `npm run lint`, `npm run build` (AGENTS.md); run the Vitest suites for the touched files. Note: on the author's machine Vitest currently fails to start any suite ("failed to find the current suite"); fix the runner or run the tests in CI, and say so in the PR if they could not run.
9. Check in the browser: both flows, cancel, limit state, mobile width, keyboard only; the admin Packages page with the placement filter and the pool summary.

## Success Criteria
- [ ] New tests failed before and pass after (or the runner limitation is stated explicitly).
- [ ] `typecheck`, `lint`, `build` pass.
- [ ] No reference left to `getPlacementTest` or `PLACEMENT_ALREADY_DONE`.
- [ ] An author sees the pool of every placement package and the published exam count per pool, with a warning below 2, without leaving the Packages page.
- [ ] A learner can never be stuck on an empty test; a retake entry exists wherever a result is shown.
- [ ] Both flows verified in the browser including the limit state.

## Risk Assessment
- The page used to decide everything from the latest attempt; adding a user-chosen `pool` view must not break resuming an in-progress attempt. Covered by test 2.
- The cancel route is new in Assessment (phase 2); the frontend must not ship before it exists.
- Existing tests that mock the old API call will fail to compile; update them in the same change.
- Old browser tabs still running the previous bundle will get an error from the removed route after the release; the error state tells them to reload.
