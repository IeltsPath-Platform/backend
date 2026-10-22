# Phase 09: Trang tổng quan quản trị (frontend)

Repo: `frontend`. Phụ thuộc phase 04 (API tổng quan) và phase 05 (shell).

## Mục tiêu

Trang chủ của khu quản trị cho R&D: số liệu nội dung và việc cần làm, mỗi thẻ dẫn tới mục menu tương ứng.

## Việc cần làm

1. Thẻ số liệu từ `GET /api/content/admin/overview` (số KP theo trạng thái/kỹ năng/CEFR, số lỗi sức khỏe đồ thị, KP active chưa có câu hỏi PUBLISHED, câu hỏi DRAFT chờ xuất bản, lesson theo trạng thái).
2. Danh sách hàng việc của R&D từ `GET /api/learning/admin/work-items` (queue `CONTENT_AUTHOR`: `CONTENT_GAP`, `QUESTION_TOO_EASY`, `QUESTION_TOO_HARD`, `EDGE_SUSPECT`) và thống kê câu hỏi từ `GET /api/learning/admin/question-stats?minAttempts=20`, theo `backend/docs/contracts/learning-staff-area-v1.md`. Hai nguồn gọi độc lập để một nguồn lỗi không làm hỏng cả trang.
3. Mỗi thẻ dẫn tới mục menu liên quan (Cây tri thức, Bài học, Ngân hàng câu hỏi).
4. Đủ trạng thái loading, lỗi từng khối, rỗng.

## Test

Mỗi khối độc lập khi nguồn kia lỗi; điều hướng từ thẻ; role `EXAMINER` chỉ thấy khối nó được phép đọc (mock `apiRequest`).

## Tiêu chí hoàn thành

Trang hiện số liệu thật từ backend khớp truy vấn DB; một nguồn lỗi không làm trắng trang. `typecheck`, `lint`, `test` xanh.
