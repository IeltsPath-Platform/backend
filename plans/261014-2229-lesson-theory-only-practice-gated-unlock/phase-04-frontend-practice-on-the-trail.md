---
phase: 4
title: Frontend practice on the trail
status: completed
priority: P1
dependencies:
  - 1
  - 2
  - 3
---

# Phase 4: Frontend practice on the trail

## Overview

Repo `frontend` (nhánh `feat/lesson-resources`). Trang bài học chỉ còn lý thuyết. Practice có trang riêng và một ô
riêng trên lộ trình chặng, ngay dưới bài của nó. Nút "Hoàn thành bài" chỉ còn ở bài không có Practice bắt buộc. Bỏ
chỗ gán cứng giới hạn số lần trượt bài ôn.

## Requirements

**Trang bài học** (`features/learning-path/pages/LessonPage.tsx`)
- `TABS` còn 3: `slides`, `summary`, `documents`. Bỏ `practice` khỏi `TABS`, `parseTab`, `LessonTab` (`~types/lesson`),
  và bỏ nhánh render `LessonPractice` (L168).
- Không còn nút "Hoàn thành bài" khi bài có Practice bắt buộc. Thay bằng lời nhắc + link "Luyện tập bài này"
  → `/learn/lessons/:id/practice`. Nút chỉ hiện khi bài chưa COMPLETED và `practicePassReason === 'NO_PRACTICE'`.
- Lỗi `409 PRACTICE_REQUIRED` từ `completeLesson` (đường dự phòng) thì dẫn tới trang Practice.
- `practicePath` (L130) đổi từ `?view=practice` sang trang riêng.

**Trang Practice riêng**
- `App.tsx`: route `lessons/:lessonId/practice` render trang mới thay cho `LessonPracticeRedirect` (L125-129, xóa
  hàm này cùng comment "The Practice is a tab of the lesson page").
- Trang mới (`pages/LessonPracticePage.tsx`) bọc `components/LessonPractice` (đã có, nhận `lessonId`), kèm tiêu đề bài
  và link quay lại bài học / chặng, theo layout `LearnLayout` / `LearnTrail` như các trang `/learn` khác.
- Link cũ `?view=practice` (bookmark): `parseTab` gặp `practice` thì chuyển hướng sang trang Practice thay vì rơi về
  tab Slide.
- Thêm helper `lessonPracticePath(lessonId)` vào `lib/routes.ts`; mọi chỗ dựng URL Practice dùng helper này.

**Ô Luyện tập trên lộ trình** (`components/TopicLessonsPanel.tsx`)
- Sau mỗi `LessonTile` có `practicePassReason !== 'NO_PRACTICE'` là một `PracticeTile`:
  - `practiceStatus` `LOCKED` → khóa ("Mở khi tới bài này");
  - `REQUIRED` → "Cần đạt để mở bài sau", link tới trang Practice;
  - `PASSED` → đã đạt, link "Luyện thêm".
- `needsPractice` (L12) đổi: bài có Practice bắt buộc chưa đạt thì `status !== 'COMPLETED'` và
  `practiceStatus === 'REQUIRED'` (trước: COMPLETED + REQUIRED, trạng thái này không còn tồn tại).
- "Tiếp theo": bài đầu tiên chưa COMPLETED và không LOCKED. Ô Luyện tập của bài đó cũng là bước đang làm.
- `TopicLessonsView` được dùng ở `TopicPage` trực tiếp và ở `TopicListPage` qua `TopicLessonsPanel` (thẻ chặng), nên cả
  hai nơi đều có ô Luyện tập.
- Bài ôn PENDING của bài đó chặn Practice (backend `REVIEW_REQUIRED`, giữ luật hiện có). Khi đó ô Luyện tập và trang
  Practice dẫn thẳng vào bài ôn đang chờ ("Làm bài ôn để luyện tiếp"), không hiện danh sách bộ bị khóa mà không giải
  thích. <!-- Updated: Validation Session 1 - làm bài ôn trước khi thử bộ khác -->
- Dòng "N bài học · khoảng X phút" giữ đếm theo bài (không đếm ô Luyện tập).

**Bài ôn**
- `api/learningAdapters.ts` L369: bỏ `dto.maxFailedSets ?? 2`. `maxFailedSets: number | null`; `null` nghĩa là không
  giới hạn, và UI không hiện "lần x/y" (L376-377, L403 và chỗ render tương ứng).
- Chữ hướng dẫn bài ôn: chỉ lần trượt đầu bắt đọc summary; các lần sau vào thẳng bộ mới.

**Lỗi và mã cũ**
- Xóa ánh xạ `PRACTICE_LOCKED` ở `api/apiError.ts` L53 và `api/httpLearningApi.ts` L70.
- `components/PageState.tsx` L167 (`LESSON_LOCKED`): giữ, đổi câu thành "Bài này mở khi bạn đạt phần Luyện tập của bài
  trước."

**Mock API** (`features/learning-path/api/mock/*`, `src/mocks/learning-path/*`)
- `mockProgress` / `mockLessonHandlers` theo luật mới (Practice mở cùng bài, đạt Practice thì bài COMPLETED, complete
  bị từ chối khi bài có Practice bắt buộc), để chế độ mock và `tests/learning-path.test.mjs` khớp backend.

## Related Code Files

(trong `frontend/src/features/learning-path` trừ khi ghi khác)

- Create: `pages/LessonPracticePage.tsx`
- Modify: `pages/LessonPage.tsx`, `components/TopicLessonsPanel.tsx`, `components/PageState.tsx`,
  `components/LessonPractice.tsx` (nếu cần prop điều hướng sau khi đạt), `lib/routes.ts`, `api/learningAdapters.ts`,
  `api/apiError.ts`, `api/httpLearningApi.ts`, `api/mock/*`, `frontend/src/app/App.tsx`, `frontend/src/types/lesson.ts`,
  CSS lộ trình (`learning-path` stylesheet chứa `.lp-ltile`) cho ô Luyện tập
- Test: `frontend/tests/learning-path.test.mjs`

## Implementation Steps

1. Route + `lessonPracticePath` + `LessonPracticePage`; xóa `LessonPracticeRedirect`.
2. `LessonPage`: 3 tab, chuyển hướng `?view=practice`, nút hoàn thành có điều kiện, xử lý `PRACTICE_REQUIRED`.
3. `TopicLessonsPanel`: `PracticeTile`, `needsPractice`, "Tiếp theo"; CSS ô mới theo token sẵn có của `.lp-ltile`.
4. Bài ôn: `maxFailedSets` nullable, bỏ hiển thị giới hạn.
5. Mã lỗi: xóa `PRACTICE_LOCKED`, sửa câu `LESSON_LOCKED`.
6. Mock theo luật mới; cập nhật `tests/learning-path.test.mjs`.
7. `npm run typecheck && npm run lint && npm run build`. `package.json` không có script `test` và repo không có CI:
   xác định lệnh chạy `tests/learning-path.test.mjs` (file dùng `node:test`; kiểm loader TS/alias cần thiết, ví dụ
   `node --test` hoặc `npx tsx --test`), chạy trước khi sửa để có mốc, giữ nó xanh sau khi sửa. Nếu không chạy được
   vì lý do có sẵn từ trước thì ghi rõ lệnh đã thử và lỗi trong báo cáo, không tự thêm runner mới.
   <!-- Updated: Validation Session 1 - tìm cách chạy test FE có sẵn -->
8. Chạy app với backend thật (dùng skill `run`) và đi luồng: L1 → Practice trượt → bài ôn → đạt → L1 xong → L2 mở.

## Success Criteria

- [ ] `/learn/lessons/:id` có đúng 3 tab; `?view=practice` chuyển sang trang Practice.
- [ ] Lộ trình hiện ô Luyện tập dưới mỗi bài có Practice bắt buộc, trạng thái đúng theo `practiceStatus`.
- [ ] Không còn nút "Hoàn thành bài" ở bài có Practice bắt buộc.
- [ ] `grep -rn "maxFailedSets ?? \|PRACTICE_LOCKED" frontend/src` rỗng.
- [ ] typecheck, lint, build xanh; `tests/learning-path.test.mjs` xanh (hoặc lý do không chạy được, ghi rõ); đã chạy
      thử luồng trên trình duyệt.
- [ ] Bài ôn PENDING: ô Luyện tập/trang Practice dẫn vào bài ôn.

## Risk Assessment

- Thẻ chặng ở `TopicListPage` sẽ dài gấp đôi khi thêm ô Luyện tập. Nếu rối mắt, có thể gộp ô Luyện tập thành một hàng
  phụ của ô bài; giữ quyết định "mục riêng trên lộ trình" và hỏi lại người dùng nếu đổi cách trình bày.
- Mobile ngoài phạm vi; app mobile có thể còn gọi luồng cũ (`complete` trước Practice) và sẽ nhận 409.
