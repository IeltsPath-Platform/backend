# Phase 07: Mã tham chiếu câu hỏi `import_ref`

Người làm: **Claude Code**. Nhánh: `feat/content-question-import-ref` (backend), tạo từ `origin/main` sau khi phase 14 đã merge.
Trạng thái: Pending

Repo: `backend`, service `content-service`. Phụ thuộc: phase 01 đã merge (để lấy đúng số migration).

## Mục tiêu

Bảng `questions` không có cột mã, nên Excel (phase 12), bản nháp course (phase 10–11) và câu AI đã duyệt (phase 09) không trỏ được tới một câu hỏi bằng mã ổn định. Thêm `import_ref`.

API soạn lesson mà plan 0804 từng định làm ở đây **đã có** từ plan `261022-1200-rd-admin-area` (`docs/contracts/lesson-authoring-v1.md`); phase này không làm lại.

## Đọc trước

- `docs/contracts/question-publication-v1.md` và controller/use case tạo, liệt kê câu hỏi admin (`/api/content/admin/questions`).
- `docs/contracts/lesson-authoring-v1.md` (block `EXERCISE` trỏ câu bằng `questionVersionId`; không đổi).

## Yêu cầu

- **Migration** (số kế tiếp trên `origin/main`, kiểm ngay trước khi tạo):

  ```sql
  ALTER TABLE questions ADD COLUMN import_ref VARCHAR(100);
  CREATE UNIQUE INDEX uq_questions_import_ref ON questions (import_ref) WHERE import_ref IS NOT NULL;
  ```

  Câu cũ để null. Không hiển thị cho học viên; route internal của learning không đổi.
- Request tạo câu hỏi admin nhận `importRef` tùy chọn: 1–100 ký tự `[A-Za-z0-9._-]`. Trùng → `409` `details.code = "IMPORT_REF_CONFLICT"`.
- Response câu hỏi admin (danh sách, chi tiết) trả `importRef`.
- `GET /api/content/admin/questions?importRef=` tìm đúng một mã (khớp chính xác).
- Tiền tố `ai-` dùng cho câu do phase 09 tạo (`ai-<draftId>-<index>`); không cần chặn client dùng, unique index đủ tránh trùng.
- Cập nhật contract `question-publication-v1.md` (hoặc contract đang mô tả route câu hỏi admin) trong cùng commit.

## Test

- Tạo câu có `importRef` → trả lại trong chi tiết; tạo câu thứ hai cùng mã → `409 IMPORT_REF_CONFLICT`; mã có ký tự lạ → `400`.
- Lọc `?importRef=` trả đúng một câu; mã không có → danh sách rỗng.
- Route internal learning đọc câu không có trường mới (test hiện có vẫn xanh).

## Hoàn thành khi

`mvn -q -pl services/content-service -am test` xanh; contract cập nhật; `services/content-service/README.md` nhắc cột mới nếu README liệt kê schema.

## Kết quả

(Người làm điền.)
