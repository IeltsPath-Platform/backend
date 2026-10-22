# Phase 06: Đồ thị cây tri thức (frontend)

Repo: `frontend`. Phụ thuộc phase 05 (shell). Dùng API sẵn có, không chờ backend mới.

## Mục tiêu

Màn "Cây tri thức" trong khu quản trị: vẽ đồ thị KP và cạnh tiên quyết, thấy ngay KP lỗi (cô lập, thiếu metadata, cạnh ngược cấp).

## Việc cần làm

1. Thêm `@xyflow/react` (đã được người dùng duyệt thêm dependency; chọn bản tương thích React 19 và ghim phiên bản).
2. Bố cục tự tính, không thêm thư viện layout: hàm thuần `features/admin/knowledge-graph/lib/layout.ts` có unit test. Cột là CEFR (A1 đến C2); hàng là kỹ năng (Listening, Reading, Writing, Speaking, Grammar/Vocabulary). KP `skill = ALL` (Grammar/Vocabulary) xếp hàng riêng.
3. API (`features/admin/knowledge-graph/api/`, dùng `apiRequest` với zod như `features/learning-path/api/lessonVocabularyApi.ts`): `GET /api/content/knowledge-tree` (`courseId`, `skill`, `includeInactive`) và `GET /api/content/knowledge-tree/health`, theo `docs/contracts/knowledge-tree-v1.md`. Node gồm `id, code, name, skill, kind, learningType, band, courseId, topicId, lessonIds, status, cefrLevel, canDo, scopeIn, scopeOut`; edge `{from, to}` với `from` là tiên quyết. Health: `isolatedIds`, `missingMetadataIds`, `levelViolations[{from,to}]`.
4. Hiển thị: nút tô màu theo kỹ năng, độ đậm theo CEFR (token CSS mới trong `admin.css`); KP inactive mờ; KP cô lập hoặc thiếu metadata có dấu cảnh báo; cạnh ngược cấp tô đỏ.
5. Panel chi tiết khi bấm nút: mã, tên, kind, CEFR, can-do, scope, tiên quyết, bài học liên kết.
6. Bộ lọc: kỹ năng, course, hiện KP inactive (chỉ `ADMIN` và `CONTENT_AUTHOR` nhận được KP inactive; EXAMINER bị bỏ qua tham số này).
7. Đủ trạng thái loading, lỗi, rỗng; bọc `ErrorBoundary`.

## Ngoài phạm vi lượt này

Sửa KP và tiên quyết ngay trên đồ thị (`PUT /api/content/knowledge-points/{id}` và `.../prerequisites`, xử lý `PREREQUISITE_CYCLE`, `PREREQUISITE_INVALID`, `PREREQUISITE_LEVEL_INVALID`). Làm ở lượt sau.

## Test

Layout (hàm thuần), trang với `apiRequest` được mock, stub `ResizeObserver` cho xyflow trong jsdom (`vi.stubGlobal`), cảnh báo health hiển thị đúng.

## Tiêu chí hoàn thành

Với backend thật đã áp V73: đồ thị hiện đủ KP active, KP nháp Speaking và nền hiện khi bật "KP inactive", không KP active nào bị đánh dấu cô lập, không cạnh đỏ. `typecheck`, `lint`, `test` xanh.
