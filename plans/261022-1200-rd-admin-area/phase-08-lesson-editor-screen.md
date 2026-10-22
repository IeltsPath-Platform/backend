# Phase 08: Soạn bài học (frontend)

Repo: `frontend`. Phụ thuộc phase 02, 03 và 05. Đọc `backend/docs/contracts/lesson-authoring-v1.md` (viết ở phase 03) trước khi code.

## Mục tiêu

R&D soạn lesson trong khu quản trị mà không cần SQL.

## Việc cần làm

1. Luồng chọn: course, topic, danh sách lesson với badge DRAFT/PUBLISHED/ARCHIVED, sắp xếp lesson bằng nút lên/xuống (không thêm thư viện kéo thả).
2. Màn soạn lesson: metadata (tiêu đề, tóm tắt, markdown, số phút), chip KP (KP phải ACTIVE), danh sách block có nút lên/xuống.
3. Trình soạn theo loại block:
   - TEXT: markdown, kèm KP của block.
   - ASSET: chọn asset đã có.
   - VOCABULARY: chọn sense qua `GET /api/content/vocabulary/search` (library-service).
   - EXERCISE: chọn câu hỏi từ ngân hàng, chỉ câu PUBLISHED; hiển thị loại block essay khi câu hỏi là ESSAY.
4. Nút xuất bản và lưu trữ. Hiển thị lỗi kiểm tra xuất bản theo `details` của `LESSON_NOT_PUBLISHABLE` (liệt kê từng vấn đề).
5. Khi lesson đã xuất bản: khóa chỉnh sửa bộ câu hỏi của block EXERCISE (theo `LESSON_EXERCISE_LOCKED`) và nói rõ lý do, vì học viên đã có tiến độ theo block. Block TEXT/ASSET/VOCABULARY vẫn sửa được; `blockId` không đổi.
6. Đủ trạng thái loading, lỗi, rỗng; cảnh báo khi rời trang có thay đổi chưa lưu.

## Test

Danh sách và sắp xếp, thêm/sửa/xóa block, lỗi xuất bản hiển thị đúng, khóa EXERCISE khi lesson đã xuất bản (mock `apiRequest`).

## Tiêu chí hoàn thành

Với backend thật: tạo lesson DRAFT, thêm block, xuất bản bị từ chối khi thiếu KP hoặc có câu DRAFT; xuất bản câu hỏi rồi xuất bản lesson; sửa block TEXT của lesson đã xuất bản và xác nhận `blockId` không đổi. `typecheck`, `lint`, `test` xanh.
