---
title: 'Lesson theory-only, practice-gated unlock'
description: >-
  Bài học chỉ còn lý thuyết + summary; Practice thành mục riêng trên lộ trình,
  đạt Practice là xong bài và mở bài sau; bài ôn lặp đến khi đạt
status: completed
priority: P1
branch: feat/lesson-resources
tags:
  - learning-service
  - frontend
  - lesson-gate
  - practice
  - review
  - tdd
blockedBy: []
blocks: []
supersedes:
  - 261009-1417-practice-gated-lesson-unlock
created: '2026-10-14T15:37:24.305Z'
createdBy: 'ck:plan'
source: skill
---

# Lesson theory-only, practice-gated unlock

## Overview

Hiện tại: trang bài học có 4 tab (Slide, Summary, Tài liệu, **Luyện tập**). Practice chỉ mở sau khi bấm "Hoàn thành
bài". Bài sau mở khi bài trước COMPLETED, **không xét Practice**: L1 "Cần luyện thêm" mà L2 vẫn mở. Bài ôn trượt 2 set
thì bị SKIPPED (cho qua). Practice được tính PASSED khi **một** bài ôn bất kỳ của skill đã xong.

Sau plan này:

```text
Lộ trình chặng:  L1 (Slide · Summary · Tài liệu)   ── mở ngay khi tới lượt
                 Luyện tập L1                       ── mở cùng L1
                     │ mỗi skill bắt buộc: 1 bộ đạt ≥70% lần đầu
                     │   hoặc mọi bài ôn sinh ra đã xong (lặp đến khi đạt)
                     │   hoặc đã làm hết mọi bộ
                     ▼ Practice PASSED  ⇒  L1 tự COMPLETED  ⇒  L2 mở
                 L2 ...
Bài không có Practice bắt buộc (chỉ Writing / không có bộ): giữ nút "Hoàn thành bài".
```

Quyết định đã chốt (người dùng, 2026-10-14):
- Bài học chỉ còn lý thuyết (Slide, Tài liệu) + Lesson Summary; bỏ tab Luyện tập. Practice của bài giữ nguyên (bộ đề,
  chấm, bài ôn).
- Practice là **mục riêng trên lộ trình**, ngay dưới bài của nó, chạy ở trang riêng `/learn/lessons/:id/practice`.
- "Xong Practice" = luật `PracticeClearance` hiện có: mỗi skill bắt buộc (Reading, Listening) đạt một lần; Writing tùy
  chọn.
- Practice **mở ngay cùng bài học**, không chờ "Hoàn thành bài".
- **Bỏ nút "Hoàn thành bài"**: Practice PASSED thì bài tự hoàn thành. Bài không có Practice bắt buộc vẫn dùng nút.
- Thanh "x/y bài" đếm bài xong = đã hoàn thành **và** Practice đạt. Với luật mới hai điều này trùng nhau nên
  `completedLessonCount` giữ cách đếm theo `completed_at`.
- Bài ôn **lặp đến khi đạt**: chỉ lần trượt đầu bắt đọc lại summary; SKIPPED chỉ khi Content hết bộ chưa lộ.
- Plan `261009-1417-practice-gated-lesson-unlock` (chưa làm) được **gộp vào plan này** và đánh dấu cancelled.

Khác plan 09/10: không thêm mã lỗi `PRACTICE_REQUIRED` cho cổng bài, không mở rộng `REVIEW_REQUIRED` theo bài trước.
Vì `completed_at` giờ chỉ được ghi khi Practice đạt, cổng hiện có "các bài trước COMPLETED" đã đủ chặn. `LESSON_LOCKED`
giữ nguyên, và lộ trình cho học viên thấy cần làm gì.

Chế độ `--tdd`: mỗi phase backend viết/sửa test cho hành vi mới trước, thấy đỏ, rồi sửa code.

## Phân công

- **Codex viết code** theo [codex-brief.md](./codex-brief.md): chỉ file đó, chỉ code main + migration + frontend, không
  test, không README.
- **Claude kiểm**: sửa/viết test theo các phase (1–3, test migration ở phase 5), chạy toàn bộ test backend và frontend,
  README, đóng plan cũ, chạy thử app. Phase file là nguồn cho phần kiểm.
- Brief chốt thêm 2 điểm so với phase gốc: `PracticeClearance` xét `NO_PRACTICE` **trước** `LOCKED` (để lộ trình không
  hiện ô Luyện tập cho bài chỉ có Writing khi bài còn khóa); mock frontend không sửa (mock không có bộ Practice).

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Review repeats until passed](./phase-01-review-repeats-until-passed.md) | Completed |
| 2 | [Practice clearance without completion lock](./phase-02-practice-clearance-without-completion-lock.md) | Completed |
| 3 | [Practice pass completes the lesson](./phase-03-practice-pass-completes-the-lesson.md) | Completed |
| 4 | [Frontend practice on the trail](./phase-04-frontend-practice-on-the-trail.md) | Completed |
| 5 | [Docs data reset and verification](./phase-05-docs-data-reset-and-verification.md) | Completed |

Thứ tự: 1 và 2 độc lập (khác file), 3 dựa trên 2, 4 dựa trên contract của 1–3, 5 sau cùng.

## Acceptance criteria

- Trang bài học chỉ có 3 tab: Slide bài giảng, Lesson Summary, Tài liệu bài khoá. Không còn tab Luyện tập.
- Lộ trình chặng: dưới mỗi bài có Practice bắt buộc là một ô "Luyện tập", mở cùng bài, dẫn tới `/learn/lessons/:id/practice`.
- L1 mở, chưa làm Practice → mở được Practice L1 ngay. `GET /topics/{id}/lessons`: L1 `AVAILABLE`, practice `REQUIRED`,
  L2 `LOCKED`. Mở L2 → `403 LESSON_LOCKED`.
- Practice L1 đạt lần đầu → L1 `COMPLETED` (không bấm gì), L2 `AVAILABLE`, chặng "1/n bài".
- Practice L1 trượt → bài ôn PENDING → L1 chưa COMPLETED, L2 khóa. Bài ôn trượt lần 1 → THEORY; trượt lần 2, 3 → vẫn
  PENDING ở PRACTICE; đạt → DONE → Practice PASSED → L1 COMPLETED → L2 mở.
- Hai bài ôn cùng skill, một DONE một PENDING → Practice `REQUIRED`.
- Hết bộ chưa lộ → bài ôn SKIPPED → Practice PASSED → L1 COMPLETED (không ai kẹt).
- Bài không có Practice bắt buộc: trang bài có nút "Hoàn thành bài"; bài có Practice bắt buộc: không có nút, và
  `POST /lessons/{id}/complete` trả `409 PRACTICE_REQUIRED`.
- `GET /reviews/{id}` trả `maxFailedSets: null`; FE không hiện "lần x/2".
- Cổng thi chặng, mở chặng/khóa học không đổi hành vi.
- `mvn -q -pl services/learning-service -am test` xanh (trừ `AnswerSpecGraderTest` thiếu file vectors có sẵn từ trước);
  frontend `typecheck`, `lint`, `build` xanh.

## Dependencies

- Plan `261014-0050-lesson-resources-replace-blocks` (đã implement, chưa commit) là nền: trang bài 4 tab, Practice nhúng
  tab. Plan này sửa tiếp trên cùng nhánh `feat/lesson-resources` (backend và frontend).
- Plan `261009-1417-practice-gated-lesson-unlock`: superseded. Phase 5 đổi `status: cancelled` và ghi lý do.
- Mobile ngoài phạm vi.
- SRS/FDS (repo docs): người dùng tự cập nhật luồng bài học, Practice và bài ôn. Không chạy script build tài liệu.
- Nội dung: seed có KP chỉ có 1 bộ PRACTICE_SET → trượt ở KP đó không sinh bài ôn; học viên qua bằng một bộ khác đạt
  lần đầu hoặc làm hết bộ (`ALL_SETS_ATTEMPTED`). Cần ≥ 3–4 bộ/KP để luật lặp đến khi đạt có tác dụng.

## Thay đổi sau khi làm (2026-10-14)

- Danh sách bài của chặng (`TopicLessonsView`, dùng ở thẻ chặng và `TopicPage`) **không** hiện ô Luyện tập; chỉ liệt kê
  bài học. Practice vào từ trang bài học (nút "Luyện tập bài này" ở cột phải, link ở panel hoàn thành) và từ trang
  riêng `/learn/lessons/:id/practice`. Thay thế quyết định "mục riêng trên lộ trình" và phần "Ô Luyện tập trên lộ
  trình" của phase 4.

## Open questions

None.

## Validation Log

### Session 1 — 2026-10-14
**Trigger:** người dùng chọn `/ck:plan validate` sau khi tạo plan.
**Questions asked:** 4

#### Verification Results
- Tier: Full (5 phase; Fact Checker, Flow Tracer, Scope Auditor, Contract Verifier)
- Claims checked: 34
- Verified: 29 | Failed: 3 | Unverified: 2
- Failures:
  1. [Flow Tracer] Phase 3 đưa `GetPracticeAttemptUseCase` qua `LessonAccess.authorize`; use case này `readOnly`, không
     lock (`GetPracticeAttemptUseCase.java` L35), còn `authorize` có thể gọi `refreshTopics` (ghi) và yêu cầu lock
     (`LessonAccess.java` L24-27, L77-84). → sửa: chỉ kiểm sở hữu.
  2. [Scope Auditor] `LessonAccess.isOpen` "cùng điều kiện như authorize" sẽ kéo `topic()` refresh vào
     `GetLessonPracticeSetsUseCase` (`readOnly`). → sửa: `isOpen` chỉ đọc, không refresh.
  3. [Fact Checker] Phase 5 "mẫu `*MigrationTest` sẵn có" — learning-service không có; chỉ content-service có. → sửa:
     test mới theo mẫu content-service.
- Unverified: lệnh chạy test frontend (không có script `test`, không CI) → phase 4 xác định khi làm; câu
  "`TopicLessonsView` dùng ở `TopicListPage`" → đúng gián tiếp qua `TopicLessonsPanel` (L38), đã sửa câu chữ.
- Contract Verifier: `PracticeAccess` có 5 caller (`require`: Start L38, Submit L74, SubmitEssay L107, GetAttempt L35;
  `completed`: GetLessonPracticeSets L48). `LessonProgress.complete`: 1 caller (`CompleteLessonUseCase` L41).
  `refreshPassForLesson`: 4 caller (SubmitPracticeAttempt L132, SubmitReview L119, GetReview L78, CompleteLesson L43).
  Mã `PRACTICE_LOCKED`: backend `PracticeAccess` L28, test `PracticeIntegrationTest` L114, README L162; frontend
  `apiError.ts` L53, `httpLearningApi.ts` L70.

#### Questions & Answers

1. **[Architecture]** Phase 3: xem lại kết quả một attempt Practice đã nộp (`GetPracticeAttemptUseCase`, chạy
   read-only, không giữ lock) có cần qua cổng bài học không?
   - Options: Chỉ kiểm quyền sở hữu | Qua cổng, không refresh
   - **Answer:** Chỉ kiểm quyền sở hữu
   - **Rationale:** tránh ghi trong transaction read-only; học viên luôn xem lại được bài đã làm.
2. **[Risk]** Phase 5: migration V11 xoá `completed_at` của các bài đã bấm "Hoàn thành" mà Practice chưa đạt. Làm vậy
   không?
   - Options: Có, chạy V11 | Không, reset DB demo | Không làm gì
   - **Answer:** Có, chạy V11
   - **Rationale:** dữ liệu cũ khớp nghĩa mới ngay khi deploy, không mất dữ liệu demo khác.
3. **[Assumptions]** Khi Practice L1 trượt và sinh bài ôn PENDING, học viên có được thử bộ Practice khác của L1 trước
   khi làm xong bài ôn không?
   - Options: Không, làm bài ôn trước | Có, cho thử bộ khác
   - **Answer:** Không, làm bài ôn trước
   - **Rationale:** giữ luật `REVIEW_REQUIRED` hiện có; FE phải dẫn vào bài ôn từ ô Luyện tập/trang Practice.
4. **[Scope]** Phase 4: frontend không có script `test` và không có CI. Kiểm frontend bằng gì?
   - Options: Tìm cách chạy test có sẵn | Chỉ typecheck/lint/build + chạy thử
   - **Answer:** Tìm cách chạy test có sẵn
   - **Rationale:** giữ `tests/learning-path.test.mjs` làm lưới an toàn cho mock và trang.

#### Confirmed Decisions
- Đọc attempt: chỉ kiểm sở hữu, không cổng.
- `LessonAccess.isOpen`: chỉ đọc, không refresh curriculum.
- V11 chạy, test bằng `LessonCompletionMigrationIntegrationTest` (Flyway `target("10")` rồi migrate).
- Bài ôn PENDING chặn Practice cùng bài; FE dẫn vào bài ôn.
- Test FE: xác định lệnh chạy `tests/learning-path.test.mjs`, giữ xanh.

#### Impact on Phases
- Phase 2: sửa lý do `open = true` của `refreshPassForLesson`.
- Phase 3: 3 caller ghi qua `authorize`; `GetPracticeAttemptUseCase` chỉ kiểm sở hữu; `isOpen` read-only; thêm 3 test
  (REVIEW_REQUIRED cho Practice, đọc attempt khi bài khóa, practice-sets read-only); rủi ro transaction read-only.
- Phase 4: dẫn vào bài ôn khi PENDING; câu chữ `TopicLessonsView`/`TopicLessonsPanel`; lệnh chạy test FE.
- Phase 5: tên file và mẫu test migration; lệnh test FE.

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01 … phase-05 (grep `GetPracticeAttempt`, `MigrationTest`, `test frontend`,
  `cùng điều kiện như`, `PRACTICE_LOCKED`, `mọi caller`, `isOpen`, `read-only`).
- Decision deltas checked: 5
- Reconciled stale references: 6 (phase 2 L30; phase 3 requirements, architecture, steps, risk; phase 4 runner;
  phase 5 test file)
- Unresolved contradictions: 0
