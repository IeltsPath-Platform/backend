# Báo cáo nghiệm thu — Band-skill paywall

Ngày cập nhật: 2026-10-30
Nhánh: `feat/band-skill-paywall` ở backend và frontend.

## Tiến độ

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Graphify backend/frontend | DONE | AST cục bộ; backend sau phase C 22,750 nút/91,898 cạnh, frontend sau A.3 8,267 nút/20,766 cạnh. Graph query xác nhận các node course, `course_skill_prices`, reader, `LearningContentClient` và lesson. Output nằm trong `graphify-out/` và không được đưa vào commit. |
| A.1 Cover content-service | DONE | V86 và API/content mapping đã commit; test controller và draft controller chạy xanh. TDD red-first bị lệch: dependency download bị chặn nên test chỉ chạy sau khi triển khai. |
| A.2 Cover learning-service passthrough | DONE | Commit `7ce7556b`; DTO/client/content response mapping và V86 reader integration xanh. |
| A.3 Cover frontend | DONE | Admin forms, learner course/topic adapters, banner/cards and placement summary updated; focused tests, typecheck and lint xanh. Commit `cdcad2e`. |
| B. Access unlock debit | DONE | V5 ledger check plus `COURSE_UNLOCK_DEBIT` mapping; focused tests + Testcontainers integration xanh. |
| C. Content unlock prices/facts | DONE | V87 prices, V88 clear topic gates, one-skill authoring checks, learning payload fields and SQL ordering facts. Data audit identifies the existing `TREES_MULTI_SKILL` violation; content owner fixes remain a ship prerequisite. |
| D. Per-skill progression | DONE | `TopicStatusDeriver` now opens one topic per course skill. Focused tests 33/33; full learning-service reactor 1,002/1,002. Commit `b302c4f0`. |
| E. Learning purchase | DONE | Durable V38 unlock purchase, replay/reconciliation and `/courses` ownership/pricing fields. Focused unit/API + 3 PostgreSQL integration tests passed; full learning-service reactor: 1,019 tests passed. Commit `6ec31d7a`. |
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
- Backend: phase C commit pending.

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

## C — Content prices and paywall facts

- Added `course_skill_prices` and `courses.combo_unlock_points` in V87, plus V88 clearing `required_feature_key` from every topic. Prices are not seeded; the course create/update API replaces the skill-price map, preserves omitted legacy fields, and validates positive prices and combo boundaries.
- Course topics now require one non-`ALL` skill; course topic knowledge points must use that skill or `ALL`, and lesson knowledge point assignments reject mismatches. The audit is diagnostic only and does not mutate seed content.
- Content reader returns course prices, available skills, topic skill, 1-based skill position and earliest lesson position. Position ranking uses active course topics and published lessons ordered by topic order/id then lesson order/id. Legacy schema targets keep passing through nullable projection fallbacks.
- Learning-content client, learner topic result and lesson response now carry course prices and lesson course/skill/position facts.
- Added audit SQL and report. Latest test database reports one unskilled course topic (`TREES_MULTI_SKILL`), five mismatched knowledge points (`TRAP_KEYWORD_MATCH_ONLY`, `TR_L_DETAIL`, `TR_R_SCANNING`, `TR_R_WORD_IN_CONTEXT`, `TR_W_OPINION`), and two multi-skill lessons (`T1`, `T2`). These seed violations remain a content-owner prerequisite before enabling the paywall.
- Focused clean Maven run: 91 tests, 0 failures/errors, including course pricing/controller/persistence, topic/KP/lesson validation, course sequence on the old v52 schema, lesson pipeline on v79, and V87/V88 Testcontainers migration/reader/audit checks.
- Follow-up `TopicSequenceCourseMetadataIntegrationTest`: 4/4 passed after adding cross-topic skill ranking edge cases (inactive topics and draft lessons are excluded; knowledge-point earliest position is skill-local).
- Graphify refreshed after phase C: backend graph reports 22,750 nodes and 91,898 edges.

## D — Per-skill topic progression

- Topic ordering now groups by `(courseId, skill)`, so a learner can have one `IN_PROGRESS` topic in each course skill. Topics without a course retain a shared sequence; completed and passed topics keep their prior status meaning. No schema migration is needed, and current progress remains derived from stored `passedAt` / `testedAt` values.
- Updated the `LearnerCurriculum` rule comment and learning-service README. Lesson access, course aggregation and suggestions already consume derived statuses and aggregate by course, so they needed no behavior changes.
- Focused regression tests for progression and lesson gates: 33 passed, 0 failures/errors.
- Full learning-service reactor: 1,002 tests passed, 0 failures/errors/skips, including Testcontainers integrations. Surefire waited 30 seconds for broker-listener shutdown after the suite, then reported BUILD SUCCESS.
- Commit: `b302c4f0`.

## E — Learning unlock purchase

- Added V38 learner unlock persistence with one non-cancelled row per `(user, course, skill-or-combo)` and unique request ids. Pending intents and insufficient-balance cancellations commit in `REQUIRES_NEW` transactions.
- `POST /courses/{id}/unlocks` validates target/availability/ownership, prices skill purchases from Content, calculates combo price using actual paid ACTIVE skill costs, records a pending intent before the Access debit, and replays with the stored request id and amount.
- `/courses` now reconciles pending purchases first and returns prices, available/unlocked skills, combo payable points and `premium`. Access outage leaves pending rows for retry and degrades the display-only premium field to false.
- Focused unit/API tests: 16 passed. V38/PostgreSQL persistence integration tests: 3 passed. Combined focused coverage: 21 passed, 0 failures/errors/skips.
- Full learning-service reactor: 1,019 tests passed, 0 failures/errors/skips. Surefire waited 30 seconds for broker-listener shutdown, then reported BUILD SUCCESS.
- Implementation commit: `6ec31d7a`.
