# Writing progress (1557, phase 1–2): báo cáo

- Ngày: 2026-11-11
- Backend nhánh `feat/writing-progress`, commit `e9041a53`
- Frontend nhánh `feat/writing-progress-view`, commit `0a4a249`
- Trạng thái: DONE_WITH_CONCERNS

## Phase 1 (backend)
- `GET /api/learning/writing/progress?task=&limit=`: điểm từng bài, lỗi hay gặp (đếm theo bài, tối đa 10), lỗi đã khắc phục (N bài sạch sau lần xuất hiện cuối, mặc định 2), `insufficient` dưới 3 bài.
- Chỉ đọc; không gọi LLM; tối đa một truy vấn mỗi task và một lần gọi Content (tra tên lỗi khi bài cũ thiếu).
- Migration `V52` thêm index. Contract `docs/contracts/writing-progress-v1.md`.
- Test: calculator (10), truy vấn Testcontainers Postgres thật (2), WebMvc và nhóm Writing cũ — tất cả pass.
- Cấu hình: `learning.writing.progress-fixed-after` (plan ghi `learning.writing.progress.fixed-after`; đã đặt tên phẳng theo `WritingSettings`).

## Phase 2 (frontend)
- Panel "Tiến bộ Writing" trong tab thống kê chi tiết, bọc `ErrorBoundary`. Biểu đồ SVG thuần (không thêm dependency), kèm danh sách điểm để đọc bằng trình đọc màn hình.
- Nút "Luyện lỗi này" dùng `learningApi.startFreePractice`, chỉ hiện khi có `practiceKnowledgePointId`.

## Kiểm chứng
- `npm run typecheck`: pass. `npm run build`: pass.
- `npm run lint`: 2 lỗi, đều ở `src/features/practice/listening/ListeningScript.tsx` (có sẵn, không thuộc thay đổi này).
- `npm test` (vitest): mọi file test trong repo fail ở `src/test/setup.ts` ("Vitest failed to find the current suite"), kể cả test cũ. Test mới (`WritingProgressPanel.test.tsx`, `writingProgressModel.test.ts`) chưa chạy được vì lỗi này.

## Concerns
- Nút "Luyện lỗi này" truyền mã lỗi làm `pointCode`, vì response chưa có mã knowledge point. Cần thêm `practiceKnowledgePointCode` vào API.
- Phase 2 yêu cầu dùng skill `dataviz` trước khi vẽ biểu đồ; chưa làm.
- Chưa có `certainty` (plan 1102 phase 4): mọi mã được coi là `ERROR`.
- Chưa có test WebMvc cho endpoint progress (auth, tham số sai trả 400).
