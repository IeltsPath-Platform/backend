---
phase: 1
title: "Cây tri thức: cạnh tiên quyết và API"
status: completed
priority: P1
dependencies: []
---

# Phase 1: Cây tri thức – cạnh tiên quyết và API

## Overview

KP hiện là danh sách phẳng thuộc topic. Phase này thêm cạnh "KP A là tiên quyết của KP B", API để R&D quản lý cây, và
API nội bộ để learning lấy tiên quyết khi chẩn đoán. Level / Lesson / Skill đã suy ra được:
- Level: KP → `topics.course_id` → `courses.band_level`. Topic chưa gắn course thì `band` và `courseId` là `null`.
- Lesson: `lesson_knowledge_points`.
- Skill: `knowledge_points.skill`.

## Quyết định riêng của phase

- **P1-a. KP `INACTIVE` mặc định bị ẩn.** `GET /knowledge-tree` chỉ trả node `ACTIVE`. Có `?includeInactive=true`
  (chỉ `ADMIN`, `CONTENT_AUTHOR`) để R&D xem cả KP đã ngưng. Cạnh chỉ trả khi cả hai đầu đều có trong `nodes[]`.
- **P1-b. Ngưng KP không xóa cạnh.** Cạnh vẫn nằm trong bảng, chỉ bị lọc khi đọc. Bật lại KP thì cạnh hiện lại.
- **P1-c. Bật lại KP** bằng `POST /api/content/knowledge-points/{id}/activate`. Invariant `learningType` vẫn áp.
- **P1-d. Quy ước mã chỉ kiểm định dạng**, không bắt tiền tố khớp skill hay band của course.
- **P1-e. Đổi `topicId` bị chặn khi KP đang được dùng**, tức là thỏa một trong hai điều kiện:
  - có `question_versions.status = 'PUBLISHED'` nối qua `question_knowledge_points`;
  - có dòng trong `lesson_knowledge_points`.
  Khi đó trả `409 KNOWLEDGE_POINT_IN_USE`. Đổi các trường khác (kể cả `code`) vẫn cho phép.

## Requirements

- Bước 0: khôi phục `docs/contracts/` từ commit cha của `fee364e` (`git checkout fee364e^ -- docs/contracts`). AGENTS.md
  vẫn trỏ tới thư mục này và nhiều phase sẽ sửa contract. Thêm file mới `docs/contracts/knowledge-tree-v1.md`.
- Migration `V29__knowledge_point_prerequisites.sql`:

  ```sql
  CREATE TABLE knowledge_point_prerequisites (
      knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id) ON DELETE CASCADE,
      prerequisite_id    UUID NOT NULL REFERENCES knowledge_points(id) ON DELETE CASCADE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (knowledge_point_id, prerequisite_id),
      CONSTRAINT chk_kp_prerequisite_not_self CHECK (knowledge_point_id <> prerequisite_id)
  );
  CREATE INDEX idx_kp_prerequisites_prerequisite ON knowledge_point_prerequisites (prerequisite_id);
  ```

- **Domain:**
  - `KnowledgeGraph` (package mới `domain/service`): domain service thuần, không phụ thuộc Spring. Nhận danh sách cạnh
    và kiểm tra không vòng bằng DFS. Ném `PrerequisiteCycleException` kèm đường vòng (danh sách KP id theo thứ tự).
  - `KnowledgePoint` **sửa hàm `update(...)` đã có**, không thêm hàm mới:
    - chữ ký mới: `update(UUID topicId, String code, String name, KnowledgePointKind kind, LearningType learningType,
      Skill skill, String description)`;
    - bỏ tham số `status` khỏi `update`; trạng thái chỉ đổi qua `deactivate()` và `activate()` (thêm mới);
    - `topicId` và `code` bỏ `final`;
    - giữ invariant: KP `ACTIVE` phải có `learningType`. `activate()` ném lỗi nếu `learningType` null.
  - Trong code main chưa có chỗ nào gọi `KnowledgePoint.update(...)`; chỉ `KnowledgePointTest` gọi chữ ký cũ, sửa test
    theo chữ ký mới.
- **Quy ước mã cho KP mới:** `^[RLW](55|60|65|70)_[A-Z0-9_]{2,60}$`, ví dụ `R60_TFNG_F_VS_NG`.
  - Áp khi tạo mới (`CreateKnowledgePointUseCase`) và khi PUT đổi sang mã khác. PUT giữ nguyên mã cũ thì không kiểm.
  - Mã cũ trong seed (thêm bằng SQL) giữ nguyên.
  - Sai định dạng → `400` qua `MethodArgumentNotValidException` (dùng `@Pattern` trên request DTO), `details` chỉ ra trường
    `code`. Trùng mã → `DuplicateCodeException` như hiện tại.
  - Đợt này không có KP Speaking/`ALL` mới, nên regex không có tiền tố cho hai skill này.
  - Lưu ý: `KnowledgePointControllerTest` và `ContentAuthorizationWebMvcTest` đang tạo KP với mã `KP-1`. Đổi fixture sang
    mã hợp lệ (ví dụ `R55_EXAMPLE`), không nới regex.
- **API cho `ADMIN` và `CONTENT_AUTHOR`:**
  - `PUT /api/content/knowledge-points/{id}`: sửa code, name, kind, learningType, skill, description, topicId. Theo
    P1-e, đổi `topicId` khi KP đang dùng → `409 KNOWLEDGE_POINT_IN_USE`.
  - `DELETE /api/content/knowledge-points/{id}`: chuyển sang `INACTIVE` (xóa mềm), trả `204`. Câu hỏi và lesson đang gắn
    vẫn giữ. Gọi trên KP đã `INACTIVE` → `204` (idempotent).
  - `POST /api/content/knowledge-points/{id}/activate`: chuyển về `ACTIVE`, trả KP. Thiếu `learningType` → `400`.
  - `GET /api/content/knowledge-points/{id}/prerequisites`: trả danh sách KP tiên quyết trực tiếp (kể cả `INACTIVE`,
    có trường `status`, để R&D thấy cạnh đang bị ẩn).
  - `PUT /api/content/knowledge-points/{id}/prerequisites`, body `{ "prerequisiteIds": [...] }`: thay toàn bộ tập
    tiên quyết, trả danh sách mới. Mảng rỗng xóa hết. ID trùng trong body được gộp.
    - Tạo vòng → `422 PREREQUISITE_CYCLE`, `details` có `{ "code": "PREREQUISITE_CYCLE", "cycle": [id...] }`.
    - KP tiên quyết không tồn tại, `INACTIVE`, hoặc trỏ vào chính nó → `422 PREREQUISITE_INVALID`, `details` có
      `invalidIds`.
    - KP đích (`{id}`) không tồn tại → `404`.
    - **Chống race:** use case chạy trong một transaction và lấy `pg_advisory_xact_lock` cố định cho cây trước khi đọc
      cạnh và kiểm vòng, để hai PUT đồng thời (A→B và B→A) không cùng lọt qua.
- **`GET /api/content/knowledge-tree?courseId=&skill=&includeInactive=`** (`ADMIN`, `CONTENT_AUTHOR`, `EXAMINER`):
  - `nodes[]`: id, code, name, skill, kind, learningType, band, courseId, topicId, lessonIds, status.
  - `edges[]`: `{ from, to }` với from = prerequisite, to = kp. Chỉ gồm cạnh mà cả hai đầu nằm trong `nodes[]`.
  - Không truyền `courseId` thì trả cả cây. Lọc theo `courseId` có thể làm mất cạnh nối sang course khác; contract ghi rõ
    điều này (FE gọi không lọc nếu cần xem cạnh liên band).
  - `includeInactive=true` do `EXAMINER` gọi → bỏ qua tham số (vẫn chỉ trả `ACTIVE`).
- **API nội bộ cho learning:** `POST /internal/learning-content/knowledge-points/prerequisites` nhận `{ "ids": [...] }`,
  trả `{ kpId: [prerequisiteId...] }`.
  - Chỉ trả key cho KP `ACTIVE` trong `ids`; chỉ trả prerequisite `ACTIVE`.
  - KP `ACTIVE` không có tiên quyết → key có mảng rỗng. ID không tồn tại hoặc `INACTIVE` → không có key.
  - Thêm vào `InternalLearningContentController`, đọc qua `JdbcLearningContentReader`.
- **Mã lỗi mới trong `GlobalExceptionHandler`** (theo mẫu `Map.of("code", ...)` đang có):
  - `PrerequisiteCycleException` → `422 PREREQUISITE_CYCLE`;
  - `InvalidPrerequisiteException` → `422 PREREQUISITE_INVALID`;
  - `KnowledgePointInUseException` → `409 KNOWLEDGE_POINT_IN_USE`.
- Gateway: `/api/content/**` đã chuyển tới content; `/internal/**` đã bị chặn (`infra/api-gateway`,
  `InternalPathSecurityTest`). Chỉ cần kiểm lại, không sửa.

## Architecture

```text
api/controller/KnowledgePointController ── PUT, DELETE, POST activate, GET/PUT prerequisites
api/controller/KnowledgeTreeController  ── GET /api/content/knowledge-tree (mới)
application/usecase: UpdateKnowledgePointUseCase, DeactivateKnowledgePointUseCase, ActivateKnowledgePointUseCase,
                     ReplacePrerequisitesUseCase, GetKnowledgeTreeUseCase, GetPrerequisitesUseCase
domain/service/KnowledgeGraph (DFS, không vòng)
domain/repository/KnowledgePrerequisiteRepository
infrastructure/persistence/adapter/JdbcKnowledgePrerequisiteRepository (NamedParameterJdbcTemplate,
                     giống JdbcLearningContentReader; không tạo JPA entity cho bảng nối)
```

## Related Code Files

(`...` = `services/content-service/src/main/java/com/ieltspath/content`)

- Sửa:
  - `.../domain/aggregate/KnowledgePoint.java`
  - `.../api/controller/KnowledgePointController.java`
  - `.../api/controller/InternalLearningContentController.java`
  - `.../api/dto/request/CreateKnowledgePointRequest.java` (`@Pattern` cho `code`)
  - `.../application/usecase/CreateKnowledgePointUseCase.java`
  - `.../infrastructure/persistence/adapter/KnowledgePointRepositoryAdapter.java` (và mapper nếu cần cho `topicId`/`code`)
  - `.../infrastructure/persistence/adapter/JdbcLearningContentReader.java`
  - `.../api/exception/GlobalExceptionHandler.java`
- Tạo:
  - `.../domain/service/KnowledgeGraph.java`
  - `.../domain/exception/PrerequisiteCycleException.java`, `InvalidPrerequisiteException.java`,
    `KnowledgePointInUseException.java`
  - `.../domain/repository/KnowledgePrerequisiteRepository.java` và adapter JDBC
  - các use case và DTO ở trên, `.../api/controller/KnowledgeTreeController.java`
- Migration: `src/main/resources/db/migration/V29__knowledge_point_prerequisites.sql`
- Test:
  - `KnowledgeGraphTest`, `KnowledgePointTest` (update/deactivate/activate)
  - `KnowledgePointControllerTest`, `KnowledgeTreeControllerTest` (`@WebMvcTest`, quyền theo role)
  - `InternalLearningContentControllerTest` (endpoint mới)
  - `KnowledgeTreeIntegrationTest` (Testcontainers, theo kiểu `CourseSequenceIntegrationTest`: Flyway + JDBC, test ở tầng
    adapter/reader, không dựng cả ứng dụng Spring)

## Implementation Steps

1. Khôi phục `docs/contracts/`, viết nháp `knowledge-tree-v1.md` (schema request/response, mã lỗi, quy tắc lọc cạnh).
2. **Đỏ trước:** `KnowledgeGraphTest`:
   - A→B, B→C, thêm C→A → vòng, đường vòng trả đúng thứ tự;
   - tự trỏ → lỗi;
   - đồ thị rỗng → hợp lệ;
   - kim cương (A→B, A→C, B→D, C→D) → hợp lệ, không báo vòng nhầm;
   - 300 node dạng chuỗi → không tràn stack (DFS lặp hoặc đệ quy đủ sâu). Không đặt ngưỡng thời gian.
3. Sửa `KnowledgePoint` (update/deactivate/activate) và `KnowledgePointTest`.
4. Viết `KnowledgeGraph`, migration V29, repository và adapter JDBC.
5. Viết use case và controller. Phân quyền đúng như mục Requirements; `CUSTOMER` gọi → 403. Đổi fixture `KP-1` trong
   test hiện có sang mã hợp lệ.
6. Viết API nội bộ, test `InternalLearningContentControllerTest`.
7. Test integration với Testcontainers:
   - tạo 3 KP, đặt cạnh, đọc cây;
   - thử tạo vòng → bị chặn;
   - ngưng một KP → không còn trong cây mặc định và trong API nội bộ; `includeInactive=true` vẫn thấy; bật lại → cạnh
     hiện lại;
   - KP gắn câu hỏi `PUBLISHED` hoặc lesson → đổi topic bị chặn.
8. `mvn -q -pl services/content-service -am test`. Máy chạy phải có Docker; test Testcontainers đang dùng
   `disabledWithoutDocker = true`, bị skip thì không tính là xanh.

## Success Criteria

- [x] Không thể lưu vòng, kể cả vòng dài (A→B→C→A) và hai PUT đồng thời. (test: KnowledgeTreeIntegrationTest.readsPrerequisiteEdgesAndRejectsAnIntroducedCycle, concurrentOpposingPrerequisiteUpdatesAllowOnlyOneEdge)
- [x] Ngưng KP không làm gãy câu hỏi hoặc lesson đang gắn (`question_knowledge_points`, `lesson_knowledge_points` giữ
  nguyên); bật lại khôi phục cạnh. (test: KnowledgeTreeIntegrationTest.inactiveKnowledgePointIsHiddenFromDefaultTreeAndInternalPrerequisitesUntilReactivated)
- [x] Đổi `topicId` của KP đang dùng → `409 KNOWLEDGE_POINT_IN_USE`. (test: KnowledgeTreeIntegrationTest.changingTopicIsRejectedWhenKnowledgePointHasPublishedQuestionOrLessonUsage)
- [ ] Contract `knowledge-tree-v1.md` khớp controller.

## Risk Assessment

- Đổi `topicId` của KP làm đổi band. Đã chặn theo P1-e.
- Đổi `code` có thể làm lệch dữ liệu nơi khác nếu có chỗ lưu theo mã. Learning lưu theo KP id; kiểm `grep` trước khi
  merge rằng không service nào join theo `code`.
- Cây lớn: `GET /knowledge-tree` không phân trang. Chấp nhận được vì cây dự kiến dưới 300 node.

## Bàn giao FE

Màn quản lý cây:
- danh sách KP lọc theo course và skill, bật/tắt hiện KP đã ngưng;
- form sửa KP (báo `KNOWLEDGE_POINT_IN_USE` khi đổi topic);
- nút ngưng / bật lại KP;
- chọn tiên quyết;
- xem đồ thị (node/edge từ `/knowledge-tree`);
- báo lỗi `PREREQUISITE_CYCLE` (tô đường vòng từ `details.cycle`) và `PREREQUISITE_INVALID`.
