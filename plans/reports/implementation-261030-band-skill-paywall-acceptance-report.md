# Báo cáo nghiệm thu — Band-skill paywall

Ngày cập nhật: 2026-10-30
Nhánh: `feat/band-skill-paywall` ở backend và frontend.

## Tiến độ

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Graphify backend/frontend | DONE | AST cục bộ; backend 22,298 nút/90,468 cạnh, frontend sau A.3 8,267 nút/20,766 cạnh. Bản chạy semantic cần API key nên chuyển `--code-only`. Output nằm trong `graphify-out/` và không được đưa vào commit. |
| A.1 Cover content-service | DONE | V86 và API/content mapping đã commit; test controller và draft controller chạy xanh. TDD red-first bị lệch: dependency download bị chặn nên test chỉ chạy sau khi triển khai. |
| A.2 Cover learning-service passthrough | DONE | Commit `7ce7556b`; DTO/client/content response mapping và V86 reader integration xanh. |
| A.3 Cover frontend | DONE | Admin forms, learner course/topic adapters, banner/cards and placement summary updated; focused tests, typecheck and lint xanh. Commit `cdcad2e`. |
| B. Access unlock debit | DONE | V5 ledger check plus `COURSE_UNLOCK_DEBIT` mapping; focused tests + Testcontainers integration xanh. |
| C. Content unlock prices/facts | PENDING |  |
| D. Per-skill progression | PENDING |  |
| E. Learning purchase | PENDING |  |
| F. Paywall gate | PENDING |  |
| G. Frontend paywall/admin prices | PENDING |  |
| H. Audit/contracts/scenarios | PENDING |  |

## Môi trường

- Java: 21.0.6.
- Node.js: 24.21.0.
- Docker Desktop/daemon: chạy được trong test nâng quyền; A.2 reader integration với Testcontainers đã xanh. Cần xác nhận lại trạng thái khi chạy các integration suite cuối và audit DB.
- `npm --version` hiện lỗi EPERM khi Node cố truy cập `C:\Users\pduy8`; sẽ thử lệnh trực tiếp trong workspace và ghi rõ kết quả.
- Chưa đọc hoặc sửa tệp `.env`/secret.

## Commits

- Backend: `db9439df docs(plans): finalize course cover and skill paywall plans`.
- Backend: `820e15dc feat(content): add course covers and descriptions`.
- Backend: `3534de4f test(content): assert topic descriptions pass through requests`.
- Backend: `7ce7556b feat(learning): pass course cover and descriptions`.
- Frontend: `cdcad2e feat(frontend): show course covers and descriptions`.

## A.1 — Cover content-service

- Migration: `V86__course_cover_and_descriptions.sql`.
- Thay đổi: course `coverImageUrl`/`description`, topic `description`; request limits, URL scheme validation, blank-to-null normalization; giữ constructor cũ và mapping persistence/API.
- Test viết trước implementation: `CourseControllerTest`, `TopicControllerTest`, `CoursePersistenceTest` được thêm/cập nhật. Do Maven Central chưa tải dependency được trong sandbox, red run không thể thực hiện trước implementation.
- Lệnh xanh: Maven `-pl services/content-service -am -Dtest=CourseControllerTest,TopicControllerTest,CourseDraftControllerTest,CoursePersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test` — 33 test, 0 failure/error. Build biên dịch 553 source và 178 test source.
- TDD deviation: lần chạy đầu sau commit cho thấy test `TopicControllerTest` khẳng định sai rằng controller trim command; sửa test để kiểm tra request được chuyển nguyên vẹn, còn aggregate chuẩn hóa nội dung. Lần chạy clean từ đúng working directory xanh.
- `CourseMaterializer`/import/draft vẫn dùng được nhờ constructor tương thích; `CourseDraftControllerTest` 11/11 và `CoursePersistenceTest` 5/5 xanh. Chưa chạy `ApproveCourseDraftIntegrationTest` (Docker/Testcontainers không khả dụng).
- Kiểm tra `git diff --check`: xanh.

## A.2 — Cover learning-service passthrough

- Learning content client và response mapping chuyển `coverImageUrl`, course description và topic description; tương thích payload cũ thiếu field; API contract được cập nhật.
- Test xanh: learning client 33/33, learning response mapping 3/3, content response mapping 2/2, Testcontainers reader integration 1/1; cả hai service qua `test-compile`.
- Commit: `7ce7556b1ea3178f632ea3628f30f56b7266361e`.

## A.3 — Cover frontend

- Admin course drawer gửi cover URL/mô tả dạng nullable và có preview; topic drawer gửi mô tả. Learner adapter truyền fields tới course hero, spotlight, tiles, topic hero và placement cards; ảnh lỗi/quên URL dùng band art hiện có.
- Focused Vitest: 45 test, 0 failure/error. `tsc -b --pretty false` và ESLint cho file đã sửa xanh.
- Graphify frontend được cập nhật bằng AST cục bộ sau thay đổi.

## B — Access unlock debit

- Thêm `COURSE_UNLOCK_DEBIT` và ánh xạ reference `COURSE_UNLOCK` trong writer và idempotent replay; V5 mở rộng transaction-type check mà giữ nguyên các giá trị cũ.
- Tests trước implementation đỏ do enum chưa có giá trị mới (test compile); sau implementation, focused Access tests xanh: DebitPointsUseCase, PointDebitWriter và `PointCreditIntegrationTest`, tổng 16 test, 0 lỗi. Testcontainers xác nhận V5 chạy và debit trả đúng ledger type.
