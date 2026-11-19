---
phase: 4
title: "Frontend: UI chẩn đoán và giỏ ôn"
status: pending
items: [13, "hiển thị 4, 5, 7, 8, 9"]
repo: frontend
---

# Phase 4: Frontend, UI chẩn đoán và giỏ ôn

Làm trong repo `frontend` (quy tắc: `frontend/AGENTS.md`; định hướng bằng graphify trước khi đọc nhiều file).

<!-- Updated: Red Team Session 1 - trường blocking khi nộp, skip cần ≥1 probe, attemptNumber + mock, nhãn UNBLOCKED, KP name đã có -->

## Ngữ cảnh

- `src/features/learning-path/pages/ReviewPage.tsx`:
  - chỉ hiện bộ câu hỏi khi `review.set` có giá trị (dòng 97, 225);
  - không đọc `state`, `diagnosticSessionId`, `waitingOnReviewIds`;
  - gỡ cổng chặn chỉ khi status khác `PENDING` (131);
  - tự gửi theory check ở stage `THEORY` (101–108).
- `src/features/learning-path/api/learningAdapters.ts`:
  - DTO nộp bài ôn có `reviewStatus`, `stage`, `failedSets`, `gamification` (230–234);
  - `maxFailedSets` được dùng để chặn trần `attemptNumber`/`maxAttempts` (671–680).
- `src/features/learning-path/api/mock/mockLessonHandlers.ts:152`: mock đặt `maxFailedSets: pack.length`.
- `src/features/learning-path/pages/ReviewBasketPage.tsx`: bốn nhóm `required`, `consolidate`, `due`, `weak`.
- Backend:
  - `GET /api/learning/diagnostics/{sessionId}` (đã trả mã và tên KP của từng probe, `GetDiagnosticUseCase.java:33-38`);
  - `GET .../probes/{probeId}` và `POST .../probes/{probeId}/submissions`;
  - mới: `POST .../{sessionId}/skip` (phase 2).
  
  Contract `backend/docs/contracts/diagnostics-v1.md`.

## Mục 13: UI chẩn đoán

**Trước:** trượt set thứ 2 → backend trả bài ôn `state = DIAGNOSING`, không set → trang chỉ có tiêu đề và lý thuyết, không
nút. Bài học sau bị `REVIEW_REQUIRED`. Học viên kẹt.

**Sau:**

- Adapter đọc `state`, `diagnosticSessionId`, `waitingOnReviewIds`, `triggerKind`, `theoryScope` vào `ReviewDetail`.
- `ReviewPage`, `state = DIAGNOSING`: khối "Kiểm tra kiến thức nền".
  - Gọi `GET /diagnostics/{sessionId}`, liệt kê probe: tên KP và trạng thái (chưa làm, đạt, chưa đạt, không có câu hỏi,
    đã bỏ qua).
  - Mỗi probe `OPEN` có nút "Làm bài" → trang probe mới.
  - Nút "Bỏ qua kiểm tra": **chỉ bật khi đã làm ít nhất một probe**; kèm dòng giải thích ngắn trên khối (không phải chú
    thích dưới trường). Bấm → hộp xác nhận → `POST .../skip` → tải lại bài ôn. `409 DIAGNOSIS_PROBE_REQUIRED` → báo lỗi
    và tải lại.
- Trang probe mới (ví dụ `/learn/reviews/:reviewId/diagnostics/:sessionId/probes/:probeId`):
  - dùng lại `ExerciseBlock`, `PassageBlock`, `AudioBlock`;
  - nộp → hiện kết quả và giải thích → nút "Quay lại bài ôn";
  - `409 PROBE_CLOSED` → tải lại;
  - gửi lại cùng `requestId` khi mạng lỗi (backend trả phản hồi đã lưu).
- `state = WAITING_ON_PREREQUISITES`: khối "Cần ôn trước" liệt kê bài ôn trong `waitingOnReviewIds`, mỗi mục có link tới
  `/reviews/:id`.
- `diagnosticSessionId = null` (phiên không có probe) → không hiện khối chẩn đoán.
- Có trạng thái chờ, lỗi, rỗng; điều hướng bằng bàn phím được (`frontend/AGENTS.md` §4).

## Hiển thị cho mục 4

**Trước:**
- Màn hình hiện "Số bộ chưa đạt: 2/" vì `maxFailedSets` là null.
- Khi `SKIPPED` hiện "Bạn chưa đạt sau null bộ".
- FE không biết bài ôn vừa hạ cấp, vì response không có `blocking` và status vẫn `PENDING`.

**Sau:**

- DTO nộp bài ôn đọc trường mới `blocking` (`null` với phản hồi replay cũ).
- Kết quả nộp có `blocking === false` trong khi bài ôn trước đó đang chặn:
  - thông báo "Bài ôn đã chuyển vào giỏ ôn, bạn có thể học tiếp", nút "Học tiếp";
  - gọi `resolvePendingReview(review.reviewId)` để mở khóa ở các trang khác.
- Bài ôn đang chặn hiện "Số bộ chưa đạt: 2/4". Bài ôn tùy chọn (`maxFailedSets = null`) hiện "Số bộ chưa đạt: 2".
- Sửa `attemptNumber`/`maxAttempts` trong adapter (671–680) theo ý nghĩa mới của `maxFailedSets` (số lần trượt trước khi
  thôi chặn, không phải số bộ tối đa). Sửa mock (`mockLessonHandlers.ts:152`) trả 4 cho bài ôn chặn.
- `SKIPPED`: "Đã bỏ qua bài ôn vì hết bộ câu hỏi", không nhắc số bộ.

## Hiển thị cho mục 7

`theoryScope = NO_LESSON` → thay khối lý thuyết bằng ghi chú "Kiến thức này chưa có bài học trong lộ trình của bạn, hãy làm
bộ câu hỏi và đọc lời giải."

## Hiển thị cho mục 8 và 9

- `ReviewBasketPage`, nhãn lý do:
  - `PRIORITY` → "Còn yếu";
  - `UNBLOCKED` → "Bài ôn chưa đạt";
  - `DECAYED` → "Lâu chưa ôn";
  - giữ nhãn cũ cho `CONSOLIDATE`, `DUE` và các trigger (`ASSESSMENT`, `DIAGNOSTIC`, `ERROR_PATTERN`…).
- Nút "Bắt đầu" gọi `POST /review-basket/{kp}/start` như các mục khác. `409 NO_UNREVEALED_SET` → báo "Đã hết bộ câu hỏi" và
  tải lại giỏ.

## File sửa

- `src/types/learningPath.ts`
- `src/features/learning-path/api/learningAdapters.ts`, `httpLearningApi.ts`, `learningApi.ts`, `api/mock/mockLessonHandlers.ts`
  và handler mock cho chẩn đoán
- `src/features/learning-path/pages/ReviewPage.tsx`, mới `DiagnosticProbePage.tsx`, `ReviewBasketPage.tsx`
- Router của learning-path (thêm route probe)
- Test Vitest cạnh các trang đã sửa (`ReviewPage.*.test.tsx`, `ReviewBasketPage`, adapter)

## Test

- `ReviewPage` với `state = DIAGNOSING`: chưa làm probe → nút bỏ qua tắt; làm 1 probe → bật; bấm → gọi skip → tải lại.
- `WAITING_ON_PREREQUISITES` → hiện link tới bài ôn cần làm trước.
- Nộp trả `blocking: false` → thông báo chuyển vào giỏ ôn và `resolvePendingReview` được gọi; `blocking: null` → không thông báo.
- `maxFailedSets = 4` → "2/4"; `null` → "2".
- Trang probe: nộp, hiện kết quả, `409` → tải lại.
- Giỏ ôn hiện nhãn `PRIORITY`, `UNBLOCKED`, `DECAYED`.
- `npm run lint`, `npm run typecheck`, `npm run build`, và các test Vitest đã có.

## Rủi ro

- Backend phase 2 và 3 phải deploy trước. Nếu FE gọi `skip` lên backend cũ sẽ nhận 404; khi đó ẩn nút.
- Chế độ mock (`VITE_USE_MOCK_LEARNING`) cần handler chẩn đoán, nếu không thì không vào được nhánh này.
