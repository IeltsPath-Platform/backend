---
phase: 6
title: "Import Excel theo template"
status: pending
priority: P2
dependencies: [1, 4]
---

# Phase 6: Import Excel theo template

## Overview

Mentor mô tả: import file Excel, có **template tải về ngay chỗ bấm Import**, hệ thống đọc file và tạo ra nội dung. Theo
quyết định D2, một file tạo được cả cấu trúc course lẫn câu hỏi. Vì file dễ nhập sai, import chạy hai bước: **kiểm tra
trước** (không ghi gì, trả báo cáo từng dòng) rồi **ghi** (tất cả hoặc không).

## Dependency

Apache POI (`poi-ooxml`) đã được duyệt (D12). Chọn phiên bản tương thích với BOM Spring Boot đang dùng, ghi vào
`pom.xml` của content-service; không thêm thư viện đọc file nào khác.

## Requirements

- **Template** (`GET /api/content/admin/course-import/template`, `ADMIN`, `CONTENT_AUTHOR`): trả file `.xlsx` gồm các sheet,
  dòng đầu là tiêu đề cột, dòng thứ hai là mô tả và ví dụ, có chú thích ô và danh sách giá trị cho cột có tập giá trị
  cố định:

  | Sheet | Cột chính |
  |---|---|
  | `Course` | code, name, bandLevel |
  | `Topics` | code, name, skill, sortOrder |
  | `Lessons` | code, topicCode, title, summary, sortOrder, knowledgePointCodes |
  | `Blocks` | lessonCode, sortOrder, type (`TEXT`, `EXERCISE`), text, questionRefs, knowledgePointCodes |
  | `Questions` | importRef, type, skill, stem, optionA…optionF, answer, explanation, difficulty, knowledgePointCodes, distractorErrorCodes |
  | `PracticeSets` | packageCode, title, lessonCode, knowledgePointCodes, questionRefs |
  | `Tests` | kind (`TOPIC_TEST`, `COURSE_TEST`, `PRACTICE_TEST_MILESTONE`), packageCode, topicCode, afterTopicSequence, questionRefs |

  Template nằm trong tài nguyên của service (một file mẫu), không sinh động; phiên bản template ghi trong sheet `Info`.
- **Kiểm tra trước** (`POST /api/content/admin/course-import/validate`, multipart): đọc file, **không ghi gì**, trả báo cáo:
  - `valid`: true khi không có lỗi;
  - `errors[]` và `warnings[]` mỗi mục có `sheet`, `row`, `column`, `code`, `message`;
  - thống kê: số topic, lesson, câu hỏi, gói sẽ tạo.
- **Luật kiểm tra:**
  - cấu trúc: sheet và cột đủ, đúng phiên bản template; mã duy nhất và không trùng cái đã có (`CODE_CONFLICT`);
  - tham chiếu: `topicCode`, `lessonCode`, `questionRefs` (trỏ `Questions.importRef` trong file **hoặc** câu đã có theo
    `import_ref`), `knowledgePointCodes` (phải là KP `ACTIVE`), `distractorErrorCodes` (phải thuộc danh mục `ACTIVE`);
  - câu hỏi: đáp án nằm trong phương án, kiểu câu khớp cột, `answer_spec` dựng ra phải qua bộ kiểm tra có sẵn của use case
    câu hỏi, có difficulty;
  - cây: chạy `CourseCoverageAnalyzer` (phase 4) lên cấu trúc dựng được; lỗi `PREREQUISITE_ORDER` chặn, `KP_NOT_COVERED` và
    `gaps` là cảnh báo.
- **Ghi** (`POST /api/content/admin/course-import`, multipart + `validationToken` hoặc tải lại file): kiểm tra lại, rồi
  trong **một transaction** tạo:
  - course `INACTIVE`, topic `INACTIVE`, lesson `DRAFT`, block, liên kết KP;
  - câu hỏi `DRAFT` kèm `import_ref`, KP, difficulty, `distractors`;
  - gói `PRACTICE_SET`, `TOPIC_TEST`, `COURSE_TEST` ở trạng thái `DRAFT`, mốc practice test;
  - có lỗi bất kỳ → `422` kèm báo cáo, **không tạo gì**.
  Dùng các use case của phase 1 và 4, không viết đường ghi riêng. Học viên không thấy gì cho tới khi R&D xuất bản và kích
  hoạt.
- **An toàn khi đọc file:**
  - chỉ nhận `.xlsx` (kiểm cả phần mở rộng lẫn chữ ký tệp), tối đa 5 MB, tối đa 2000 dòng tổng;
  - không thực thi công thức hay macro: ô công thức coi là lỗi `FORMULA_NOT_ALLOWED`; tắt giải mã thực thể ngoài (XXE) và
    chặn tệp nén phình (giới hạn tỉ lệ giải nén và dung lượng sau giải nén);
  - mọi chuỗi cắt gọn và chuẩn hóa; không log nội dung ô;
  - giới hạn tần suất theo người dùng (dùng cùng hạn mức đếm theo ngày kiểu phase 2, nhưng đếm riêng loại `IMPORT`).
- Kiểm tra giới hạn kích thước multipart ở Spring và Gateway; chỉ sửa cấu hình Gateway nếu thật sự chặn (AGENTS.md §5, báo
  người dùng trước khi sửa).

## Architecture

```text
CourseImportController ── template / validate / import
application: DownloadImportTemplateUseCase, ValidateCourseImportUseCase, ExecuteCourseImportUseCase
port: CourseWorkbookReader (đọc .xlsx → CourseImportModel, thuần dữ liệu)
infrastructure: PoiCourseWorkbookReader (hoặc thư viện đã duyệt), tài nguyên template
domain/service: CourseImportValidator (luật, không phụ thuộc thư viện đọc file)
```

Luật kiểm tra nằm ở domain, thư viện đọc file chỉ ở infrastructure sau port; đổi thư viện sau này không đụng luật.

## Related Code Files

(`C` = content package)

- Tạo: các lớp ở mục Architecture, `C/domain/vo/CourseImportModel.java`, `C/api/controller/CourseImportController.java`,
  `src/main/resources/import/course-import-template-v1.xlsx`
- Sửa: `pom.xml` của content-service (thêm dependency **sau khi được duyệt**), cấu hình multipart nếu cần
- Test: `CourseImportValidatorTest` (bảng lỗi: mã trùng, tham chiếu lạ, đáp án ngoài phương án, tiên quyết sai thứ tự),
  `PoiCourseWorkbookReaderTest` (file mẫu đúng; thiếu sheet; công thức; file quá lớn; zip bomb nhỏ giả lập),
  `CourseImportIntegrationTest` (Testcontainers: validate rồi import file mẫu tạo đủ course, topic, lesson, câu hỏi, gói;
  file lỗi một dòng thì không tạo gì)

## Implementation Steps

1. Thêm `poi-ooxml` vào pom của content-service, kiểm `mvn dependency:tree` không kéo xung đột.
2. **Đỏ trước:** `CourseImportValidatorTest` (chưa cần đọc file, dựng `CourseImportModel` bằng tay).
3. Validator và model; use case validate và import dùng model.
4. Reader và template; test an toàn khi đọc file.
5. Controller, kích thước multipart, tải template.
6. Integration test với file mẫu và file lỗi.
7. `mvn -q -pl services/content-service -am test`.

## Success Criteria

- [ ] Tải template, điền theo mẫu, import thành công tạo đúng cấu trúc và câu hỏi.
- [ ] File lỗi một dòng → báo cáo chỉ đúng sheet, dòng, cột; không tạo bản ghi nào.
- [ ] Công thức, macro, tệp quá lớn bị từ chối.
- [ ] Course tạo ra là `INACTIVE`, học viên không thấy.

## Risk Assessment

- File Excel là đầu vào không tin cậy: các giới hạn và tắt XXE ở trên là bắt buộc, có test riêng.
- Template đổi phiên bản làm file cũ hỏng: `Info` ghi phiên bản và báo `TEMPLATE_VERSION_MISMATCH`.
- Nội dung câu hỏi trong Excel do con người nhập, không qua AI nhưng vẫn là `DRAFT` và cần duyệt trước khi xuất bản.

## Bàn giao FE

Màn Import: nút "Tải template" ngay cạnh nút chọn file, bước kiểm tra hiển thị bảng lỗi theo sheet và dòng, nút "Ghi" chỉ
bật khi `valid = true`.
