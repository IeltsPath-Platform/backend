---
phase: 3
title: "Schema docs and verification"
status: pending
priority: P2
effort: "0.5d"
dependencies: [1, 2]
---

# Phase 3: Schema docs and verification

## Overview
Cập nhật tài liệu schema của dự án cho khớp với V28 và V8, rồi kiểm chứng trên DB thật rằng migration chỉ thêm, dữ liệu cũ không đổi và **code hiện tại vẫn chạy** trên schema mới (S1).

## Requirements
- Functional:
  - `.sdd/database/DATABASE_V5.md` (hiện V5.5, cập nhật 2026-10-03): thêm một dòng phiên bản `V5.6` vào bảng lịch sử, sửa `version`/`updated` ở frontmatter, thêm mô tả bảng mới `section_question_groups`
    và các cột mới ở các mục `content_packages` (§5.1), `content_sections` (§5.3), `section_questions`, `assessment_attempts`, `attempt_sections`; thêm quan hệ mới vào phần FK/sơ đồ nếu doc có.
    Tìm đúng mục bằng `grep -n '^## ' .sdd/database/DATABASE_V5.md`.
  - Doc đang **cũ**: không mô tả `courses` (V19) và Content V19–V27. Plan này **không** đối chiếu lại toàn bộ; chỉ thêm phần của mình và ghi một câu cảnh báo ở đầu doc nếu chưa có ("chưa phản ánh Content V19–V27").
  - `docs/system-architecture.md`: chỉ sửa nếu có chỗ liệt kê bảng/cột của content hoặc assessment mà giờ sai; nếu không, không đụng.
- Non-functional: mọi con số và tên trong doc lấy từ file migration thật, không viết từ trí nhớ.

## Related Code Files
- Modify: `.sdd/database/DATABASE_V5.md`; `docs/system-architecture.md` (chỉ khi cần)
- Create: không.
- Delete: không.

## Implementation Steps
1. Ghi **trước** khi áp dụng (trên DB compose hiện tại, qua `docker compose exec -T postgres psql -U postgres -d <db> -tAc`): số dòng `content_packages`, `content_sections`, `section_questions`, `section_question_groups` (chưa có), `assessment_attempts`, `attempt_sections`; phiên bản Flyway hiện tại (`select max(version::int) from flyway_schema_history`).
2. Áp dụng V28 và V8 bằng cách khởi động content-service và assessment-service (Flyway chạy khi khởi động), **vẫn bằng code hiện tại** (chưa có entity mới).
3. Ghi **sau**: cùng các số trên; phải bằng nhau. Chạy các truy vấn kiểm chứng (chỉ đọc):
   ```sql
   -- content_db
   select max(version::int) from flyway_schema_history;                         -- 28
   select count(*) from content_packages;                                       -- bằng số trước (56 lúc lập plan)
   select count(*) from content_packages where source_code is not null;         -- 0
   select count(*) from section_question_groups;                                -- 0 (bảng mới, chưa có dữ liệu)
   select conname from pg_constraint where conrelid = 'section_questions'::regclass and contype = 'f';  -- có fk_section_questions_group
   -- assessment_db
   select max(version::int) from flyway_schema_history;                         -- 8
   select scope, count(*) from assessment_attempts group by 1;                  -- toàn FULL
   select count(*) from assessment_attempts where package_id is not null or skill is not null;  -- 0
   ```
4. Xác nhận cả hai service khởi động sạch với `ddl-auto: validate` (không lỗi Hibernate) và một luồng cũ vẫn chạy (ví dụ tạo attempt placement hoặc đọc topic-sequence).
5. Sửa `.sdd/database/DATABASE_V5.md` theo yêu cầu trên, đối chiếu từng tên cột/ràng buộc với hai file migration.
6. Chạy kiểm chứng toàn bộ:
   - `mvn -q -pl services/content-service,services/assessment-service -am test`
   - `mvn -q compile -DskipTests` (cả reactor; không `clean` khi có service chạy từ IDE)
   - `docker compose config --quiet`
7. Điền mục Verification trong `plan.md`: lệnh đã chạy, bảng số dòng trước/sau, test bị skip vì thiếu Docker (nếu có), test cũ đã đổi kỳ vọng (nếu có).

## Success Criteria
- [ ] Số dòng mọi bảng cũ trước và sau bằng nhau; hai migration không `UPDATE`/`DELETE`.
- [ ] Content và assessment (code hiện tại) khởi động được trên schema mới và một luồng cũ vẫn chạy.
- [ ] `DATABASE_V5.md` khớp migration: mọi bảng/cột/ràng buộc mới đều có, không có tên nào sai.
- [ ] `git status` chỉ chứa file thuộc plan này; không có `.env`, secret hay `graphify-out/`.

## Risk Assessment
- Nếu Docker không chạy, test Testcontainers bị skip và không thể kiểm chứng trên DB thật: **không commit** phase có migration (xem `plan.md`), ghi `BLOCKED`.
- `DATABASE_V5.md` đang cũ; thêm phần mới vào doc cũ có thể khiến người đọc tưởng doc đã cập nhật đủ. Câu cảnh báo ở đầu doc giảm rủi ro này.
- Khởi động service để áp migration cũng chạy các seed/Flyway còn lại; dùng DB compose cục bộ, không chạy trên môi trường dùng chung.

## Câu hỏi chưa giải quyết
- O1-O3 trong `plan.md` (index đếm lượt làm, nhãn PRO của nguồn, giới hạn `part_no`) chưa được chủ dự án xác nhận; mặc định đã ghi sẵn.
