---
phase: 5
title: "Frontend suggestion UX"
status: pending
priority: P2
effort: "3h"
dependencies: [3, 4]
---

# Phase 5: Frontend suggestion UX

Work package C. Repo: `frontend/`. Paths under `frontend/src/`.

## Overview

A created review that does not block shows as a suggestion, never as the gate; a set waiting only for a suggestion is
labelled "Nên ôn lại"; a lesson cleared softly says so; the basket reason type accepts `PRACTICE`.

## Changes

### 1. Types

- `types/learningPath.ts`:
  - `PracticeReviewCreated` (line ~469) gains `blocking: boolean`.
  - The basket optional-item `reason` union (line ~635) becomes
    `'CONSOLIDATE' | 'DUE' | 'WRITING_CRITERION' | 'ERROR_PATTERN' | 'PRACTICE'`.
- `features/learning-path/api/masteryGuideAdapters.ts` line ~47: same union change.
- `features/learning-path/api/learningAdapters.ts`:
  - DTO type (line ~351): `reviewsCreated?: Array<{ reviewId: string; knowledgePointId: string; stage?: string; blocking?: boolean }>`.
  - Mapper (line ~794): add `blocking: review.blocking ?? true,` (older payloads meant blocking).

### 2. `features/learning-path/lib/reviewGate.ts`

Add:

```ts
/** The reviews a Practice submission created, as gate entries; the store keeps only the blocking ones. */
export function reviewRefsFromSubmission(created: PracticeReviewCreated[], lessonId: string): ReviewRef[] {
  return created.map((review) => ({
    reviewId: review.reviewId,
    lessonId,
    knowledgePointCode: review.knowledgePointId.slice(0, 8),
    knowledgePointTitle: 'Bài ôn từ luyện thêm',
    blocking: review.blocking,
  }))
}
```

(import the `PracticeReviewCreated` type from `~types/learningPath`).

### 3. New `features/learning-path/components/PracticeReviewNotice.tsx`

```tsx
import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { PracticeReviewCreated } from '~types/learningPath'
import type { ReviewNavigationState } from '../lib/routes'

/** What a Practice submission created: a blocking review stops the learner, a suggestion only invites a review. */
export function PracticeReviewNotice({ created, reviewOrigin }: {
  created: PracticeReviewCreated[]
  reviewOrigin?: ReviewNavigationState['reviewOrigin']
}) {
  const blocking = created.find((review) => review.blocking)
  const first = blocking ?? created[0]
  if (!first) return null
  const state = reviewOrigin ? { reviewOrigin } : undefined
  return blocking ? (
    <div className="lp-done lp-done--review" role="status">
      <p>Đã tạo bài ôn bắt buộc sau lần luyện này.</p>
      <Button asChild className="lp-btn lp-btn--accent lp-btn--cta">
        <Link to={`/learn/reviews/${first.reviewId}`} state={state}>Làm bài ôn<ArrowRight aria-hidden="true" /></Link>
      </Button>
    </div>
  ) : (
    <div className="lp-done lp-done--review" role="status">
      <p>Nên ôn lại {created.length} điểm kiến thức còn yếu. Bạn vẫn học tiếp được.</p>
      <Button asChild className="lp-btn">
        <Link to={`/learn/reviews/${first.reviewId}`} state={state}>Ôn ngay<ArrowRight aria-hidden="true" /></Link>
      </Button>
    </div>
  )
}
```

If the type of `reviewOrigin` passed in `LessonPractice` is named differently, use that type; behavior and strings stay.

### 4. `features/learning-path/components/LessonPractice.tsx`

- `submit` (lines ~138-144): replace the inline `setPendingReviews(outcome.reviewsCreated.map(...))` with
  `setPendingReviews(reviewRefsFromSubmission(outcome.reviewsCreated, catalog.lessonId))`.
- Result card (lines ~237-246): replace the `{submission?.reviewsCreated[0] ? (...) : null}` block with
  `{submission ? <PracticeReviewNotice created={submission.reviewsCreated} reviewOrigin={reviewOrigin} /> : null}`.
- Set label: change `itemMeta(item, waitsForReview: boolean)` to
  `itemMeta(item, review: 'BLOCKING' | 'SUGGESTED' | 'NONE')`:

```ts
function itemMeta(item: PracticeSetItem, review: 'BLOCKING' | 'SUGGESTED' | 'NONE'): ItemMeta | undefined {
  if (item.status !== 'ATTEMPTED') return ITEM_META[item.status]
  if (review === 'BLOCKING') return { label: 'Cần làm bài ôn', tone: 'accent', icon: ShieldAlert }
  if (review === 'SUGGESTED') return { label: 'Nên ôn lại', tone: 'accent', icon: CircleDot }
  return { label: 'Đã xong', tone: 'success', icon: CheckCircle2 }
}
```
  and at the call site (line ~303):

```ts
const setReviews = reviewsBySet.get(item.packageId) ?? []
const meta = itemMeta(item, setReviews.some((review) => review.blocking !== false) ? 'BLOCKING'
  : setReviews.length > 0 ? 'SUGGESTED' : 'NONE')
```
- Status text (line ~287): add
  `const SOFT_PASS_TEXT = 'Bài tiếp theo đã mở. Nên làm các bài ôn gợi ý để củng cố điểm còn yếu.'` next to
  `PRACTICE_STATUS_TEXT`, and render
  `{catalog.practiceStatus === 'PASSED' && catalog.practicePassReason === 'ATTEMPT_NOT_BLOCKED' ? SOFT_PASS_TEXT : (PRACTICE_STATUS_TEXT[catalog.practiceStatus] ?? catalog.practiceStatus)}`.

## Tests first

1. New `features/learning-path/components/PracticeReviewNotice.test.tsx` (render inside `MemoryRouter`):
   - non-blocking only: shows "Nên ôn lại 2 điểm kiến thức còn yếu. Bạn vẫn học tiếp được." and a link "Ôn ngay" to
     `/learn/reviews/<first id>`;
   - one blocking among them: shows "Đã tạo bài ôn bắt buộc sau lần luyện này." and "Làm bài ôn" linking the blocking one;
   - empty list: renders nothing.
2. `features/learning-path/lib/reviewGate.test.tsx` (exists), add: `reviewRefsFromSubmission` carries `blocking`;
   after `setPendingReviews(reviewRefsFromSubmission([nonBlocking, blocking], 'l1'))` the pending store holds only the
   blocking one.
3. `features/learning-path/components/LessonPractice.reviews.test.tsx` (exists), add:
   - `getLessonPracticeSets` resolves `{ lessonId: 'lesson-2', practiceStatus: 'PASSED', practicePassReason: 'ATTEMPT_NOT_BLOCKED', items: [] }`:
     the soft text is shown;
   - an `ATTEMPTED` set whose only review (from `getPendingReviews`, with `sourcePackageId` equal to the set and
     `blocking: false`) shows "Nên ôn lại"; the same with `blocking: true` shows "Cần làm bài ôn".
4. `features/learning-path/api/learningAdapters` mapping: a payload without `blocking` maps to `true`, with
   `blocking: false` maps to `false` (add to an existing adapter test file or a new
   `learningAdapters.practiceSubmission.test.ts`).

## Verify

From `frontend/`: `npx tsc -b --pretty false`, `npx eslint src/features/learning-path`,
`npx vitest run src/features/learning-path`, `node --test tests/learning-path.test.mjs`.

## Success Criteria

- [ ] All tests above pass; typecheck, lint and suites green.
