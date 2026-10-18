---
phase: 2
title: "Assessment schema migration"
status: pending
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 2: Assessment schema migration

## Overview
Migration `V8__practice_attempt_scope.sql` ở assessment-service: 4 cột mới, vài ràng buộc và hai index một phần, để attempt biết nó thuộc **gói nào** (không phụ thuộc phiên bản), làm **cả đề hay một part**, và **skill** nào (S8, S9). Không bảng mới, không FK sang content (hai DB riêng).

## Requirements
- Functional: đúng DDL dưới đây (có thể chỉnh cú pháp, không đổi ý nghĩa).
- Non-functional: dòng attempt cũ không đổi (trừ `scope` nhận default `FULL`); `ADD COLUMN … NOT NULL DEFAULT` hằng số không viết lại bảng với PostgreSQL 15; không `UPDATE`/`DELETE`.

### Cột mới

| Bảng | Cột | Kiểu | Ràng buộc | Ý nghĩa |
| --- | --- | --- | --- | --- |
| `assessment_attempts` | `package_id` | UUID | nullable, **không FK** | Tham chiếu logic tới `content_packages.id`. Khác `package_version_id`: không đổi khi đề ra version mới. |
| `assessment_attempts` | `scope` | VARCHAR(10) | NOT NULL, default `FULL`, CHECK `FULL`/`PART` | Làm cả đề hay một phần các section. |
| `assessment_attempts` | `skill` | VARCHAR(20) | nullable, CHECK 4 skill | Skill của đề luyện (một đề một skill), để lọc theo skill ở DB. |
| `attempt_sections` | `part_no` | INT | nullable, CHECK `part_no > 0` | Bản chụp số part của section lúc bắt đầu, ổn định qua các phiên bản. |

### Ràng buộc nghiệp vụ trên `assessment_attempts`

| Tên | Điều kiện | Ghi chú |
| --- | --- | --- |
| `chk_assessment_attempts_part_practice` | `scope = 'FULL' OR attempt_type = 'OFFICIAL_PRACTICE'` | Chỉ đề luyện được làm một phần. |
| `chk_assessment_attempts_skill_practice` | `skill IS NULL OR attempt_type = 'OFFICIAL_PRACTICE'` | `skill` chỉ có ý nghĩa với đề luyện. |
| `chk_assessment_attempts_practice_identity` | `attempt_type <> 'OFFICIAL_PRACTICE' OR (package_id IS NOT NULL AND skill IS NOT NULL)` | **`NOT VALID`**: chỉ kiểm dòng mới, không quét dòng cũ (an toàn nếu môi trường nào có dòng `OFFICIAL_PRACTICE` cũ). |

### Index

| Tên | Cột | Điều kiện | Phục vụ |
| --- | --- | --- | --- |
| `idx_assessment_attempts_user_package` | `(user_id, package_id)` | `package_id IS NOT NULL` | Tiến độ "đã làm/chưa làm" của một học viên. |
| `idx_assessment_attempts_package_status` | `(package_id, status)` | `package_id IS NOT NULL` | Đếm lượt làm bài (O1; bỏ nếu chủ dự án xác nhận bỏ đếm). |

## Architecture
```
assessment_attempts ──< attempt_sections ──< attempt_items          (quan hệ cũ, không đổi)
        ┆ package_id (logic)         ┆ part_no (logic)
        ▼                            ▼
content_packages (content_db)   content_sections (content_db)       (không FK xuyên DB, AGENTS §3.5)
```
Định danh part ổn định = `(package_id, part_no)`; attempt cũ để `package_id` rỗng (assessment không tự suy ra được từ DB của mình), nhưng chúng là placement/gate/mock chứ không phải đề luyện nên không nằm trong thư viện.

## Related Code Files
- Create: `services/assessment-service/src/main/resources/db/migration/V8__practice_attempt_scope.sql`,
  `services/assessment-service/src/test/java/com/ieltspath/assessment/infrastructure/persistence/PracticeAttemptScopeMigrationTest.java` (Testcontainers)
- Modify: không có file Java. Kiểm `LearningServiceApplicationTests`-kiểu test ở assessment (nếu có test đếm phiên bản Flyway/số bảng) và sửa theo luật số 6 của `plan.md`.
- Delete: không.

## DDL (bản nháp để implementer chỉnh cho đúng cú pháp)

```sql
ALTER TABLE assessment_attempts ADD COLUMN package_id UUID;
ALTER TABLE assessment_attempts ADD COLUMN scope VARCHAR(10) NOT NULL DEFAULT 'FULL';
ALTER TABLE assessment_attempts ADD COLUMN skill VARCHAR(20);

ALTER TABLE assessment_attempts ADD CONSTRAINT chk_assessment_attempts_scope
    CHECK (scope IN ('FULL', 'PART'));
ALTER TABLE assessment_attempts ADD CONSTRAINT chk_assessment_attempts_skill
    CHECK (skill IS NULL OR skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING'));
ALTER TABLE assessment_attempts ADD CONSTRAINT chk_assessment_attempts_part_practice
    CHECK (scope = 'FULL' OR attempt_type = 'OFFICIAL_PRACTICE');
ALTER TABLE assessment_attempts ADD CONSTRAINT chk_assessment_attempts_skill_practice
    CHECK (skill IS NULL OR attempt_type = 'OFFICIAL_PRACTICE');
ALTER TABLE assessment_attempts ADD CONSTRAINT chk_assessment_attempts_practice_identity
    CHECK (attempt_type <> 'OFFICIAL_PRACTICE' OR (package_id IS NOT NULL AND skill IS NOT NULL)) NOT VALID;

ALTER TABLE attempt_sections ADD COLUMN part_no INT CHECK (part_no > 0);

CREATE INDEX idx_assessment_attempts_user_package
    ON assessment_attempts(user_id, package_id) WHERE package_id IS NOT NULL;
CREATE INDEX idx_assessment_attempts_package_status
    ON assessment_attempts(package_id, status) WHERE package_id IS NOT NULL;
```

## Implementation Steps
1. **Test đỏ** `PracticeAttemptScopeMigrationTest`:
   - attempt cũ (chèn trước khi migrate bằng cách chạy Flyway tới V7, hoặc kiểm sau migrate): `scope = 'FULL'`, `package_id` và `skill` rỗng;
   - `scope = 'BOTH'` bị từ chối; `skill = 'DANCE'` bị từ chối;
   - `scope = 'PART'` với `attempt_type` khác `OFFICIAL_PRACTICE` bị từ chối; với `OFFICIAL_PRACTICE` được;
   - `skill` đặt cho `TOPIC_GATE` bị từ chối;
   - attempt `OFFICIAL_PRACTICE` thiếu `package_id` hoặc thiếu `skill` bị từ chối khi chèn mới; attempt `PLACEMENT`/`TOPIC_GATE` không có hai cột này vẫn chèn được;
   - `part_no = 0` ở `attempt_sections` bị từ chối;
   - hai index tồn tại và là index một phần (`pg_indexes.indexdef` có `WHERE package_id IS NOT NULL`).
2. Viết `V8__practice_attempt_scope.sql` từ bản nháp.
3. `mvn -q -pl services/assessment-service -am test`; ghi số dòng `assessment_attempts` và `attempt_sections` trước/sau vào Verification (phải bằng nhau).
4. Khởi động assessment-service (code cũ) trên DB đã migrate để xác nhận `validate` không lỗi và tạo attempt cũ vẫn chạy (xem phase 3).

## Success Criteria
- [ ] Test migration đỏ trước, xanh sau; mọi test assessment cũ xanh không sửa kỳ vọng nào (hoặc chỉ sửa theo luật số 6, có ghi).
- [ ] Code hiện tại tạo được attempt (không biết các cột mới) trên schema mới: `scope` nhận `FULL`, hai cột còn lại rỗng, không vi phạm ràng buộc nào.
- [ ] `NOT VALID` có hiệu lực với dòng mới nhưng không quét dòng cũ.

## Risk Assessment
- Ràng buộc `chk_assessment_attempts_practice_identity` sẽ **chặn code hiện tại** nếu nó tạo attempt `OFFICIAL_PRACTICE` mà không ghi hai cột. Hiện không có đường nào tạo loại này (`AttemptType.forContentPackageType` không ánh xạ ra nó), nên an toàn; plan thư viện đề phase 3 bắt đầu tạo chúng và đồng thời ghi `package_id`/`skill`.
- Chưa `VALIDATE CONSTRAINT` cho ràng buộc `NOT VALID`; làm sau khi chắc môi trường không có dòng cũ vi phạm (ngoài phạm vi).
- Attempt cũ không có `package_id` nên không xuất hiện trong "đã làm" của thư viện đề: chấp nhận (chúng không phải đề luyện).
- Rollback: không sửa V8 đã áp dụng; đảo bằng migration mới.
