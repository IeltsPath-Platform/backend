---
phase: 1
title: "Content course model"
status: completed
priority: P1
dependencies: []
---

# Phase 1: Content course model

## Overview
Thêm aggregate `Course` (bậc band) ở content-service, API đọc/ghi cho author, và gán topic vào course (D1–D3).

## Requirements
- Functional:
  - `courses(id UUID PK, code VARCHAR(100) UNIQUE NOT NULL, name VARCHAR(255) NOT NULL, band_level NUMERIC(2,1) NOT NULL
    UNIQUE CHECK (band_level BETWEEN 0 AND 9 AND band_level*2 = trunc(band_level*2)), status VARCHAR(20) NOT NULL DEFAULT
    'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')), created_at, updated_at)`.
  - `topics.course_id UUID REFERENCES courses(id)` nullable + index.
  - `GET /api/content/courses` (ADMIN, CONTENT_AUTHOR, CUSTOMER, EXAMINER): danh sách course, sắp theo `band_level`;
    giới hạn tự nhiên vì số course nhỏ, nhưng vẫn query có `ORDER BY` ở DB, không `findAll()` rồi sort trong bộ nhớ.
  - `POST /api/content/admin/courses`, `PUT /api/content/admin/courses/{id}` (ADMIN, CONTENT_AUTHOR): tạo, sửa
    `name`, `bandLevel`, `status`. Trùng `code` hoặc `bandLevel` → 409.
  - `CreateTopicRequest`/`UpdateTopicRequest` thêm `courseId` (nullable; update null = giữ nguyên). Course không tồn
    tại → 404/400 theo `GlobalExceptionHandler` hiện có của content.
- Non-functional: theo layer content (aggregate tách JPA entity, MapStruct mapper, adapter); không đổi Gateway
  (route `/api/content/**` đã tới content).

## Architecture
```
api/controller/CourseController ── CreateCourseUseCase / UpdateCourseUseCase / ListCoursesUseCase
                                        │
                              domain/aggregate/Course ── domain/repository/CourseRepository
                                        │
            infrastructure/persistence: CourseJpaEntity, CourseJpaRepository, CoursePersistenceMapper, CourseRepositoryAdapter
Topic: thêm courseId vào domain/aggregate/Topic + TopicJpaEntity + mapper; Create/UpdateTopicUseCase kiểm course tồn tại.
```
`Course` tự bảo vệ invariant: band 0–9 bước 0.5, name không rỗng. Dùng lại cách validate band của `BandRange` nếu khớp
(không nhân bản regex hay hằng số).

## Related Code Files
- Create: `services/content-service/src/main/resources/db/migration/V19__courses.sql`
- Create: `domain/aggregate/Course.java`, `domain/repository/CourseRepository.java`,
  `application/usecase/{Create,Update,List}CourseUseCase.java`, `application/command/{Create,Update}CourseCommand.java`,
  `application/result/CourseResult.java`, `api/controller/CourseController.java`,
  `api/dto/request/{Create,Update}CourseRequest.java`, `api/dto/response/CourseResponse.java`,
  `infrastructure/persistence/{entity/CourseJpaEntity,repository/CourseJpaRepository,mapper/CoursePersistenceMapper,adapter/CourseRepositoryAdapter}.java`
  (gốc `services/content-service/src/main/java/com/group01/content/`)
- Modify: `domain/aggregate/Topic.java`, `infrastructure/persistence/entity/TopicJpaEntity.java`,
  `infrastructure/persistence/mapper/TopicPersistenceMapper.java`, `application/usecase/{Create,Update}TopicUseCase.java`,
  `api/dto/request/{Create,Update}TopicRequest.java`, topic response DTO (thêm `courseId`),
  `api/controller/TopicController.java`
- Tests: `domain/aggregate/CourseTest.java`, `api/controller/CourseControllerTest.java`,
  `api/controller/ContentAuthorizationWebMvcTest.java` (thêm case), `application/usecase/UpdateTopicUseCaseTest.java`,
  `infrastructure/persistence/CourseMigrationTest.java` (Testcontainers)

## Implementation Steps
1. **Test đỏ**: `CourseTest` (band 5.25 bị từ chối, 9.5 bị từ chối, name rỗng bị từ chối); `CourseControllerTest`
   (201 tạo, 409 trùng band, 400 band sai, list sắp theo band); phân quyền (CUSTOMER không POST được);
   `UpdateTopicUseCaseTest` (gán course, course không tồn tại → lỗi, null giữ nguyên); `CourseMigrationTest` (bảng, ràng
   buộc unique band, FK topic).
2. Viết `V19__courses.sql` (chưa seed, seed ở phase 3).
3. Domain + repository + adapter + mapper.
4. Use case + controller + DTO; map lỗi trùng sang 409 trong `GlobalExceptionHandler`.
5. Topic: thêm `courseId` xuyên domain → JPA → DTO.
6. `mvn -q -pl services/content-service -am test`.

## Success Criteria
- [x] Test mới đỏ trước khi code, xanh sau khi code; test content cũ vẫn xanh. PostgreSQL integration tests skipped vì Docker không khả dụng; xem Verification trong plan.md.
- [x] Không có import JPA/Spring trong `domain/aggregate/Course.java`.
- [x] Topic tạo không có course vẫn hợp lệ (nhưng sẽ không vào lộ trình, phase 2).

## Risk Assessment
- Unique `band_level` chặn hai course cùng band (ví dụ "6.5 Academic" và "6.5 General"): chấp nhận cho MVP, ghi vào
  câu hỏi mở.
- Thêm field vào topic response là additive; FE bỏ qua được.
