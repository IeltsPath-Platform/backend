---
phase: 6
title: "Seed verification, docs và e2e"
status: pending
priority: P2
effort: "1d"
dependencies: [1, 2, 3, 4, 5]
---

# Phase 6: Seed verification, docs và e2e

## Overview
Seed demo **đã được viết** (`V30__seed_reading_practice_demo.sql`) và đã chèn vào `content_db` local. Phase này kiểm seed bằng test, cập nhật tài liệu, chạy toàn bộ kiểm chứng và luồng chạy thật.

**Thiết kế gốc: [`../261014-1002-practice-test-library/phase-08-seed-docs-and-verification.md`](../261014-1002-practice-test-library/phase-08-seed-docs-and-verification.md)**
(bỏ phần "viết seed", "Listening demo", "stats", "băng quy đổi", "Q1/Q2").

## Seed: những gì đã có (không viết lại)
- File: `services/content-service/src/main/resources/db/migration/V30__seed_reading_practice_demo.sql`. Idempotent (`ON CONFLICT DO NOTHING`), UUID cố định tiền tố `2d000000-0000-4000-8000-`.
  Bảng chạm tới: `content_assets`, `questions`, `question_versions`, `content_packages`, `content_package_versions`, `content_sections`, `section_question_groups`, `section_questions`, `content_asset_links`.
- Nội dung và số liệu: bảng "Dữ liệu demo" trong `plan.md`. File sinh ra từ script một lần; **không sửa tay `V30`** (đã áp dụng ở DB local; sửa nội dung = migration `V31` mới, cần hỏi chủ dự án).
- DB local đã có dữ liệu này nhưng `flyway_schema_history` của content **chưa có dòng `30`**. Lần đầu content-service khởi động Flyway sẽ chạy `V30` (idempotent, không lỗi, không nhân đôi).

## Requirements
- `PracticeTestSeedTest` (Testcontainers, Flyway tới `V30`): 3 gói đúng `source_code`/`required_feature_key`; `part_no` duy nhất trong version; mọi section có nhóm và mọi câu thuộc nhóm của đúng section;
  mọi câu `purpose = EXAM`, tự chấm được (`CHOICE`/`FILL`); gói `PUBLISHED` có `current_published_version_id`; `questionsUsedElsewhere` không báo trùng; catalog trả đúng số thẻ `FULL`/`PART`.
- Test cũ đếm câu hỏi/gói bị đổi số bởi `V30` (ví dụ trong `LessonPipelineSeedTest`, `MultiSkillSeedTest`, `CourseSeedTest`, `LearningContentUseCasesTest`): sửa **chỉ con số/danh sách**,
  theo luật số 7 của `plan.md`, ghi từng test (cũ → mới, lý do) vào Verification.
- Tài liệu (chỉ sửa chỗ khớp thay đổi, đọc file trước khi sửa):
  - `docs/system-architecture.md`: content có thư viện đề (catalog, nguồn, nhóm câu hỏi), assessment có phạm vi attempt, tiến độ, lịch sử; bảng route mới; khóa PRO còn là nhãn.
  - `docs/fe-main-flow-guide.md`: mục luồng thư viện đề Reading: `POST /api/content/practice-tests/search` (+ `/sources`) → `GET /api/assessments/practice/progress` ghép bằng `only`/`exclude`
    → `POST /api/assessments/attempts` với `sectionIds` → lưu đáp án → **hết giờ gọi `submit`, không gọi `expire`** → kết quả (lời giải có ngay) → `history`.
  - `services/content-service/README.md`, `services/assessment-service/README.md`: endpoint mới, bảng/cột mới, `CONTENT_MEDIA_BASE_URL` chỉ cần cho ảnh object key (ảnh demo là data URI).
  - `docs/contracts/learning-content-internal-v1.md`: payload `package-versions/{id}` thêm `partNo`, `timeLimitSeconds`, `groups`, `groupId`.
  - `.sdd/database/DATABASE_V5.md`: các bảng/cột `V28` và `V8` (phase 3 của plan data-model cũ, chỉ phần liên quan).
  - `AGENTS.md`/`CLAUDE.md`: chỉ khi một dòng dữ kiện trở nên sai (ví dụ danh sách loại gói, số migration). Không thêm quy tắc mới.

## Implementation Steps
1. Test đỏ `PracticeTestSeedTest`; chạy toàn bộ test content; sửa kỳ vọng số đếm theo luật.
2. Cập nhật tài liệu.
3. Kiểm chứng:
   - `docker compose config --quiet`
   - `mvn -q -pl services/content-service,services/assessment-service,services/learning-service -am test`
   - `mvn -q compile -DskipTests` (cả reactor, không `clean` khi service đang chạy từ IDE)
   - **Chạy thật** (Docker đang chạy sẵn `postgres` + `rabbitmq`): khởi động config-server → eureka → gateway → user → content → assessment → learning (+ access nếu cần), đăng nhập tài khoản demo `CUSTOMER`
     (xem `README.md` §6, `DEMO_DATA_ENABLED=true`), rồi qua Gateway: `search` `PART` và `FULL`, bắt đầu attempt Passage 2 với `sectionIds`, `PUT` đáp án, `submit`, đọc kết quả,
     `progress`, `history`. Chép lệnh và kết quả (không chép token/secret) vào Verification.
4. Điền Verification trong `plan.md`.

## Success Criteria
- [ ] Mọi acceptance criteria trong `plan.md` đạt, có bằng chứng lệnh.
- [ ] `GET /api/content/packages` (admin) không còn lỗi (cảnh báo tạm thời trong `plan.md` hết hiệu lực).
- [ ] Thiếu Docker thì ghi rõ test Testcontainers nào bị skip; không tuyên bố đã chạy.
- [ ] Tài liệu khớp hành vi thật; không còn mô tả "chưa có thư viện đề".
- [ ] `git status` chỉ chứa file thuộc plan này; không `.env`, secret hay `graphify-out/`.

## Rollback dữ liệu demo (đã kiểm trong transaction rollback ngày 2026-10-15)
Chỉ chạy khi chủ dự án yêu cầu gỡ; là xóa dữ liệu nên cần duyệt. Chạy trên `content_db`, theo đúng thứ tự (gói trước, ảnh sau vì `cover_asset_id` không cascade):
```sql
DELETE FROM content_packages WHERE id::text LIKE '2d000000-%';   -- cascade: version, section, nhóm, section_questions, asset links
DELETE FROM questions        WHERE id::text LIKE '2d000000-%';   -- cascade: question_versions
DELETE FROM content_assets   WHERE id::text LIKE '2d000000-%';
```
Muốn gỡ vĩnh viễn khỏi repo (không chỉ DB local) phải thêm migration xóa mới; không sửa `V30` đã áp dụng.

## Risk Assessment
- Seed làm đổi số đếm ở vài test seed cũ; phạm vi sửa kỳ vọng bị giới hạn bởi luật số 7, vượt thì `BLOCKED`.
- Câu hỏi `EXAM` đầu tiên trong DB: kiểm các truy vấn Learning/Game chọn câu theo skill không rò câu `EXAM` vào bài ôn (phase 1 đã có test `questionsUsedElsewhere`; thêm một test hồi quy ở Learning nếu truy vấn không lọc `purpose`).
- Chạy thật cần nhiều service khởi động đúng thứ tự; nếu không chạy được, ghi rõ là chưa e2e, không tuyên bố đạt.

## Câu hỏi chưa giải quyết
- Q4 (band) và Q2 (chặn PRO) vẫn mở; xem `plan.md`.
