---
phase: 1
title: "Content schema migration"
status: pending
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 1: Content schema migration

## Overview
Migration `V28__practice_test_library.sql` ở content-service: 1 bảng mới (`section_question_groups`), 4 cột mới ở bảng có sẵn (`source_code`, `part_no`, `cover_asset_id`, `group_id`, trong đó `group_id` là cột liên kết), 1 ràng buộc loại gói (S2-S7). Không seed dữ liệu. Chỉ schema; không đụng entity/code Java (plan thư viện đề làm).

## Requirements
- Functional: đúng DDL dưới đây (có thể chỉnh cú pháp, không đổi ý nghĩa, tên cột, tên ràng buộc, thứ tự cột).
- Non-functional: chạy trên DB có 56 gói hiện tại mà không `UPDATE`/`DELETE`; mọi cột mới nullable; thêm cột và index không khóa bảng lâu (bảng nhỏ); không dùng extension mới.

### Bảng mới

**`section_question_groups`** (nhóm câu hỏi trong section)

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `id` | UUID | PK, default `gen_random_uuid()` | |
| `section_id` | UUID | NOT NULL, FK → `content_sections(id)` ON DELETE CASCADE | Passage/section chứa nhóm. |
| `sort_order` | INT | NOT NULL | Thứ tự nhóm trong section; `UNIQUE (section_id, sort_order)`. |
| `format_code` | VARCHAR(40) | NOT NULL, CHECK `format_code ~ '^[A-Z][A-Z_]*$'` | Dạng câu (`GAP_FILLING`, `YES_NO_NOT_GIVEN`…). Giá trị hợp lệ do enum Java quyết định. |
| `instructions` | TEXT | nullable | Đề bài của nhóm ("Questions 1–5: Complete the notes…"). |
| | | `UNIQUE (id, section_id)` | Đích của khóa ngoại ghép từ `section_questions`. |

### Cột mới ở bảng có sẵn

| Bảng | Cột | Kiểu | Ràng buộc |
| --- | --- | --- | --- |
| `content_packages` | `source_code` | VARCHAR(40) | nullable, CHECK `source_code ~ '^[A-Z][A-Z0-9_]*$'`; CHECK `source_code IS NULL OR package_type = 'PRACTICE_TEST'`; index một phần `WHERE source_code IS NOT NULL`. Mã nguồn (`IELTS_SPACE_PRO`, `C10_C20`, `ACTUAL_TESTS`, `OTHER`) do enum Java `TestSource` kiểm; không có bảng nguồn. |
| `content_sections` | `part_no` | INT | nullable, CHECK `part_no > 0`; UNIQUE INDEX một phần `(package_version_id, part_no) WHERE part_no IS NOT NULL` |
| `content_sections` | `cover_asset_id` | UUID | nullable, FK → `content_assets(id)` |
| `section_questions` | `group_id` | UUID | nullable; FK ghép `(group_id, section_id)` → `section_question_groups(id, section_id)` (MATCH SIMPLE: rỗng thì không kiểm); index một phần `WHERE group_id IS NOT NULL` |

Và: drop `content_packages_package_type_check` rồi tạo lại thêm `'PRACTICE_TEST'` (cách V20 đã làm).

## Architecture
Quan hệ mới và ý nghĩa nằm ở `plan.md` (sơ đồ, bảng quan hệ). Điểm đáng chú ý: khóa ngoại ghép làm DB tự bảo đảm "câu hỏi thuộc nhóm của đúng section nó nằm";
`ON DELETE CASCADE` từ section xuống cả nhóm lẫn `section_questions` hoạt động vì PostgreSQL kiểm khóa NO ACTION ở cuối câu lệnh, sau khi cascade xong.
`source_code` là cột giá trị, không phải khóa ngoại: DB chỉ kiểm dạng chữ hoa và "chỉ cho `PRACTICE_TEST`"; mã hợp lệ do code quyết định (thêm nguồn = sửa enum).

## Related Code Files
- Create: `services/content-service/src/main/resources/db/migration/V28__practice_test_library.sql`,
  `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/PracticeTestMigrationTest.java` (Testcontainers, theo mẫu `CourseMigrationTest`)
- Modify: không có file Java; đã `grep` thấy không test content nào kiểm số bảng hay phiên bản Flyway, nhưng chạy toàn bộ test để chắc.
- Delete: không.

## DDL (bản nháp để implementer chỉnh cho đúng cú pháp)

```sql
ALTER TABLE content_packages ADD COLUMN source_code VARCHAR(40)
    CONSTRAINT chk_content_packages_source_code_format CHECK (source_code ~ '^[A-Z][A-Z0-9_]*$');
ALTER TABLE content_packages ADD CONSTRAINT chk_content_packages_source_practice_test
    CHECK (source_code IS NULL OR package_type = 'PRACTICE_TEST');
CREATE INDEX idx_content_packages_source ON content_packages(source_code) WHERE source_code IS NOT NULL;

ALTER TABLE content_packages DROP CONSTRAINT content_packages_package_type_check;
ALTER TABLE content_packages ADD CONSTRAINT content_packages_package_type_check
    CHECK (package_type IN ('MOCK_TEST', 'PLACEMENT_TEST', 'PRACTICE_SET', 'QUIZ', 'LESSON', 'TOPIC_TEST',
                            'COURSE_TEST', 'PRACTICE_TEST'));

ALTER TABLE content_sections ADD COLUMN part_no INT CHECK (part_no > 0);
ALTER TABLE content_sections ADD COLUMN cover_asset_id UUID REFERENCES content_assets(id);
CREATE UNIQUE INDEX uq_content_sections_part_no
    ON content_sections(package_version_id, part_no) WHERE part_no IS NOT NULL;

CREATE TABLE section_question_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES content_sections(id) ON DELETE CASCADE,
    sort_order INT NOT NULL,
    format_code VARCHAR(40) NOT NULL CHECK (format_code ~ '^[A-Z][A-Z_]*$'),
    instructions TEXT,
    CONSTRAINT uq_section_question_groups_order UNIQUE (section_id, sort_order),
    CONSTRAINT uq_section_question_groups_id_section UNIQUE (id, section_id)
);

ALTER TABLE section_questions ADD COLUMN group_id UUID;
ALTER TABLE section_questions ADD CONSTRAINT fk_section_questions_group
    FOREIGN KEY (group_id, section_id) REFERENCES section_question_groups(id, section_id);
CREATE INDEX idx_section_questions_group ON section_questions(group_id) WHERE group_id IS NOT NULL;
```

## Implementation Steps
1. **Test đỏ** `PracticeTestMigrationTest` (tự `INSERT` fixture bằng `JdbcTemplate`):
   - `package_type = 'PRACTICE_TEST'` được chấp nhận, `'FOO'` bị từ chối;
   - `source_code` ở gói `TOPIC_TEST`/`PRACTICE_SET` bị từ chối, ở `PRACTICE_TEST` được; `'C10_C20'` (có chữ số) được; `'c10_c20'` (chữ thường) và `'10_C'` (bắt đầu bằng số) bị từ chối;
   - `part_no = 0` bị từ chối; hai section cùng `(package_version_id, part_no)` bị từ chối; cùng `part_no` ở hai version khác nhau được; nhiều section `part_no` rỗng cùng version được;
   - nhóm trùng `(section_id, sort_order)` bị từ chối; `format_code = 'gap'` (chữ thường) bị từ chối;
   - câu hỏi gắn `group_id` của nhóm **section khác** bị từ chối; gắn nhóm đúng section được; `group_id` rỗng được;
   - xóa section kéo theo nhóm và `section_questions` của nó, không lỗi; xóa nhóm còn câu hỏi bị chặn;
   - `cover_asset_id` trỏ asset không tồn tại bị từ chối; xóa asset đang làm bìa bị chặn;
   - dữ liệu cũ: không gói nào có `source_code`; không section nào có `part_no`/`cover_asset_id`; không câu nào có `group_id`; `section_question_groups` rỗng.
2. Viết `V28__practice_test_library.sql` từ bản nháp.
3. `mvn -q -pl services/content-service -am test`. Ghi số dòng `content_packages`, `content_sections`, `section_questions` trước và sau vào Verification (phải bằng nhau).
4. Khởi động content-service (code cũ) trên DB đã migrate để xác nhận `ddl-auto: validate` không lỗi (xem phase 3).

## Success Criteria
- [ ] Test migration đỏ trước, xanh sau; mọi test content cũ xanh không sửa kỳ vọng nào.
- [ ] 56 gói cũ không đổi; mọi cột mới rỗng với dữ liệu cũ.
- [ ] Mọi ràng buộc trong bảng trên có test chặn đúng.
- [ ] Không có `UPDATE`/`DELETE`/`INSERT` nào trong migration.

## Risk Assessment
- Drop rồi tạo lại `content_packages_package_type_check` sẽ quét lại 56 dòng; nhanh, nhưng nếu một môi trường khác có loại gói ngoài danh sách thì migration lỗi. Kiểm bằng `SELECT DISTINCT package_type` trước khi áp dụng ở môi trường chung.
- Khóa ngoại ghép cần `UNIQUE (id, section_id)` ở bảng nhóm; quên thì tạo FK lỗi.
- `source_code` không có khóa ngoại nên DB không chặn mã nguồn sai chính tả (chỉ kiểm dạng chữ hoa); `TestSource` ở code và luật publish là chỗ chặn. Mã nguồn đã ghi vào dữ liệu thì đổi tên enum phải kèm migration cập nhật dữ liệu.
- `part_no` ổn định giữa các version là quy ước soạn đề, không phải ràng buộc DB; đổi `part_no` giữa version làm tiến độ "đã làm" lệch. Ghi vào tài liệu soạn đề.
- Rollback: không sửa V28 đã áp dụng; đảo ngược bằng migration mới (cần duyệt vì xóa bảng/cột). Vì chỉ thêm, rủi ro khi giữ lại thấp.
