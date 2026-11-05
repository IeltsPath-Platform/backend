# Phase 1 - Migration và content-service

## Context

- `courses` tạo ở `V19__courses.sql`; `topics` ở `V1`. Migration mới nhất là V85.
- Path gốc: `backend/services/content-service/src/main/`

## Files

Tạo:
- `resources/db/migration/V86__course_cover_and_descriptions.sql`

Sửa (course):
- `domain/aggregate/Course.java`
- `infrastructure/persistence/entity/CourseJpaEntity.java`
- `infrastructure/persistence/adapter/CourseRepositoryAdapter.java`
- `application/command/CreateCourseCommand.java`, `UpdateCourseCommand.java`
- `application/usecase/CreateCourseUseCase.java`, `UpdateCourseUseCase.java`
- `application/result/CourseResult.java`
- `api/dto/request/CreateCourseRequest.java`, `UpdateCourseRequest.java`
- `api/dto/response/CourseResponse.java`
- `api/controller/CourseController.java` (map request → command)

Sửa (topic):
- `domain/aggregate/Topic.java`
- `infrastructure/persistence/entity/TopicJpaEntity.java` (+ adapter nếu có mapping tay)
- `application/command/CreateTopicCommand.java`, `UpdateTopicCommand.java`
- `application/result/TopicResult.java`
- `api/dto/request/CreateTopicRequest.java`, `UpdateTopicRequest.java`
- `api/dto/response/TopicResponse.java`
- `api/controller/TopicController.java`

## Steps

1. Migration:
   ```sql
   ALTER TABLE courses ADD COLUMN cover_image_url VARCHAR(500);
   ALTER TABLE courses ADD COLUMN description TEXT;
   ALTER TABLE topics ADD COLUMN description TEXT;
   ```
2. Thêm field vào aggregate/entity, giữ constructor/factory cũ gọi được (truyền null) để không vỡ code seed, import, course-draft.
3. Request: `coverImageUrl` `@Size(max = 500)`, `description` `@Size(max = 2000)`; cả hai optional. Chuẩn hóa chuỗi rỗng/blank thành `null`.
4. Validate `coverImageUrl`: chỉ nhận `https://` (hoặc `http://`), reject scheme khác (`javascript:`, `data:`). URL này render vào `<img src>` ở learner nên không được để lọt scheme lạ.
5. Update course: không đổi `code`/`band` như cũ; ghi đè `coverImageUrl` + `description` theo request (null = xóa).
6. Kiểm tra `CourseMaterializer` và `CourseImportUseCases` (tạo course/topic từ draft/import) vẫn compile và để 2 cột là null.

## Tests

- Unit/controller test của course: tạo + sửa + đọc có và không có 2 trường; URL scheme không hợp lệ bị 400.
- Test tương tự cho topic `description`.
- Chạy `CourseDraftController*`, `ApproveCourseDraftIntegrationTest` để chắc không hồi quy.

## Risks / rollback

- Cột nullable nên migration an toàn; rollback = `DROP COLUMN` 3 cột.
- Constructor aggregate bị gọi ở nhiều nơi (draft, import, test): dùng overload giữ chữ ký cũ để khỏi sửa hàng loạt.
