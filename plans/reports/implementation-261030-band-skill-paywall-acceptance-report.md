# Báo cáo nghiệm thu — Band-skill paywall

Ngày cập nhật: 2026-10-30
Nhánh: `feat/band-skill-paywall` ở backend và frontend.

## Tiến độ

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Graphify backend/frontend | DONE | AST cục bộ; backend 22,298 nút/90,468 cạnh, frontend 8,227 nút/20,704 cạnh. Bản chạy semantic cần API key nên chuyển `--code-only`. Output nằm trong `graphify-out/` và không được đưa vào commit. |
| A.1 Cover content-service | DONE | V86 và API/content mapping đã commit; test controller và draft controller chạy xanh. TDD red-first bị lệch: dependency download bị chặn nên test chỉ chạy sau khi triển khai. |
| A.2 Cover learning-service passthrough | PENDING | Chờ A.1. |
| A.3 Cover frontend | PENDING | Chờ A.1–A.2. |
| B. Access unlock debit | PENDING |  |
| C. Content unlock prices/facts | PENDING |  |
| D. Per-skill progression | PENDING |  |
| E. Learning purchase | PENDING |  |
| F. Paywall gate | PENDING |  |
| G. Frontend paywall/admin prices | PENDING |  |
| H. Audit/contracts/scenarios | PENDING |  |

## Môi trường

- Java: 21.0.6.
- Node.js: 24.21.0.
- Docker daemon: không chạy/không truy cập được; Testcontainers và audit DB chưa thể xác nhận.
- `npm --version` hiện lỗi EPERM khi Node cố truy cập `C:\Users\pduy8`; sẽ thử lệnh trực tiếp trong workspace và ghi rõ kết quả.
- Chưa đọc hoặc sửa tệp `.env`/secret.

## Commits

- Backend: `db9439df docs(plans): finalize course cover and skill paywall plans`.
- Backend: `820e15dc feat(content): add course covers and descriptions`.
- Backend: `3534de4f test(content): assert topic descriptions pass through requests`.

## A.1 — Cover content-service

- Migration: `V86__course_cover_and_descriptions.sql`.
- Thay đổi: course `coverImageUrl`/`description`, topic `description`; request limits, URL scheme validation, blank-to-null normalization; giữ constructor cũ và mapping persistence/API.
- Test viết trước implementation: `CourseControllerTest`, `TopicControllerTest`, `CoursePersistenceTest` được thêm/cập nhật. Do Maven Central chưa tải dependency được trong sandbox, red run không thể thực hiện trước implementation.
- Lệnh xanh: Maven `-pl services/content-service -am -Dtest=CourseControllerTest,TopicControllerTest,CourseDraftControllerTest,CoursePersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test` — 33 test, 0 failure/error. Build biên dịch 553 source và 178 test source.
- TDD deviation: lần chạy đầu sau commit cho thấy test `TopicControllerTest` khẳng định sai rằng controller trim command; sửa test để kiểm tra request được chuyển nguyên vẹn, còn aggregate chuẩn hóa nội dung. Lần chạy clean từ đúng working directory xanh.
- `CourseMaterializer`/import/draft vẫn dùng được nhờ constructor tương thích; `CourseDraftControllerTest` 11/11 và `CoursePersistenceTest` 5/5 xanh. Chưa chạy `ApproveCourseDraftIntegrationTest` (Docker/Testcontainers không khả dụng).
- Kiểm tra `git diff --check`: xanh.
