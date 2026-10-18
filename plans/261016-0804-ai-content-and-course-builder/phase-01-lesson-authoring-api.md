---
phase: 1
title: "API soạn lesson và mã tham chiếu câu hỏi"
status: pending
priority: P1
dependencies: []
---

# Phase 1: API soạn lesson và mã tham chiếu câu hỏi

## Overview

Content-service có bảng `lessons`, `lesson_blocks` (loại `TEXT`, `ASSET`, `VOCABULARY`, `EXERCISE`),
`lesson_block_questions`, `lesson_knowledge_points` nhưng **không có API nào ghi vào chúng**; lesson hiện chỉ có qua seed
SQL. Để duyệt bản nháp thành course thật và để import Excel, cần API soạn lesson. Câu hỏi cũng cần một mã tham chiếu để
Excel và bản nháp trỏ tới được.

## Requirements

- **Migration `V34__question_import_ref.sql`:**

  ```sql
  ALTER TABLE questions ADD COLUMN import_ref VARCHAR(100);
  CREATE UNIQUE INDEX uq_questions_import_ref ON questions (import_ref) WHERE import_ref IS NOT NULL;
  ```

  `import_ref` do R&D hoặc Excel đặt; câu cũ để null. Dùng làm khóa tham chiếu, không hiển thị cho học viên.
- **API lesson** (`ADMIN`, `CONTENT_AUTHOR`), đường dẫn dưới `/api/content/admin/`:
  - `POST /topics/{topicId}/lessons`: tạo lesson (`code`, `title`, `summary`, `sortOrder`); mặc định `DRAFT`.
  - `PUT /lessons/{id}`, `POST /lessons/{id}/publish`, `POST /lessons/{id}/archive`.
  - `PUT /lessons/{id}/blocks`: thay toàn bộ danh sách block theo thứ tự. Mỗi block có `type`; `TEXT` kèm nội dung,
    `ASSET` kèm `assetId`, `VOCABULARY` kèm danh sách `vocabularySenseId`, `EXERCISE` kèm danh sách `questionVersionId`.
    Mỗi block khai báo KP (`lesson_block_knowledge_points`).
  - `PUT /lessons/{id}/knowledge-points`: tập KP của lesson.
  - `GET /lessons/{id}`: bản đầy đủ để R&D sửa (khác bản cho học viên).
- **Luật** (aggregate `Lesson`):
  - `sortOrder` duy nhất trong topic; block `sortOrder` duy nhất trong lesson (đã có ràng buộc DB, domain kiểm trước để trả
    lỗi rõ);
  - lesson `PUBLISHED` không được thiếu KP và phải có ít nhất một block;
  - câu hỏi trong block `EXERCISE` phải `PUBLISHED`; KP của block phải `ACTIVE`;
  - `vocabularySenseId` kiểm tồn tại bằng client library hiện có (nếu đã có), không đọc database của library.
- **Câu hỏi:** `import_ref` nhận trong request tạo câu hỏi; `GET /api/content/admin/questions?importRef=` tìm theo mã.
- Không đổi API đọc nội bộ cho learning (`/internal/learning-content/lessons/{id}`).

## Architecture

```text
api/controller/LessonAdminController
application/usecase: CreateLessonUseCase, UpdateLessonUseCase, ReplaceLessonBlocksUseCase,
                     ReplaceLessonKnowledgePointsUseCase, PublishLessonUseCase, GetLessonForEditUseCase
domain/aggregate/Lesson (+ entity LessonBlock), domain/repository/LessonRepository
infrastructure/persistence: JPA hoặc JDBC theo convention content, mapper, adapter
```

## Related Code Files

(`C` = `services/content-service/src/main/java/com/ieltspath/content`)

- Tạo: các lớp ở mục Architecture, DTO request/response, `GlobalExceptionHandler` thêm mã lỗi
  (`LESSON_SORT_CONFLICT`, `LESSON_INVALID_BLOCK`, `LESSON_NOT_PUBLISHABLE`).
- Sửa: `C/domain/aggregate/Question.java` và request/response câu hỏi (thêm `importRef`), `JdbcLearningContentReader`
  (không đổi hành vi; chỉ kiểm không gãy).
- Test: `LessonTest` (luật domain), `LessonAdminControllerTest` (quyền theo role, CUSTOMER → 403),
  `LessonAuthoringIntegrationTest` (Testcontainers: tạo lesson, thay block, publish, đọc lại bằng API nội bộ của learning).

## Implementation Steps

1. **Đỏ trước:** `LessonTest` với các luật ở mục Requirements.
2. Migration V34, aggregate, repository, adapter.
3. Use case và controller; phân quyền; mã lỗi.
4. `import_ref` cho câu hỏi: domain, request, tìm theo mã.
5. Integration test trọn luồng: tạo lesson → block → publish → learning đọc được lesson qua API nội bộ.
6. `mvn -q -pl services/content-service -am test`.

## Success Criteria

- [ ] Tạo được lesson hoàn chỉnh chỉ bằng API; learning đọc và hiển thị được.
- [ ] Không publish được lesson thiếu KP hoặc chứa câu chưa `PUBLISHED`.
- [ ] Hai câu hỏi không dùng chung `import_ref`.

## Risk Assessment

- Thay toàn bộ block có thể làm mất liên kết với tiến độ học viên đang có (`lesson_progress` tham chiếu block). Chỉ cho
  thay block khi lesson ở `DRAFT`; lesson đã `PUBLISHED` muốn đổi phải tạo lesson mới hoặc lưu trữ (`ARCHIVED`).

## Bàn giao FE

Màn soạn lesson cho R&D: danh sách block kéo thả, chọn câu hỏi, chọn KP.
