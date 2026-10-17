---
phase: 1
title: "Cây tri thức: cạnh tiên quyết và API"
status: pending
priority: P1
dependencies: []
---

# Phase 1: Cây tri thức – cạnh tiên quyết và API

## Overview

KP hiện là danh sách phẳng thuộc topic. Phase này thêm cạnh "KP A là tiên quyết của KP B", API để R&D quản lý cây, và
API nội bộ để learning lấy tiên quyết khi chẩn đoán. Level / Lesson / Skill đã suy ra được:
- Level: KP → topic → course `band_level`.
- Lesson: `lesson_knowledge_points`.
- Skill: `knowledge_points.skill`.

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
  - `KnowledgeGraph`: domain service thuần, không phụ thuộc Spring. Nhận danh sách cạnh và kiểm tra không vòng bằng DFS.
    Ném `PrerequisiteCycleException` kèm đường vòng.
  - `KnowledgePoint` thêm `update(...)` và `deactivate()`. Giữ invariant hiện có: KP `ACTIVE` phải có `learningType`.
- **Quy ước mã cho KP mới:** `^[RLW](55|60|65|70)_[A-Z0-9_]{2,60}$`, ví dụ `R60_TFNG_F_VS_NG`. Mã cũ trong seed giữ
  nguyên. Chỉ áp quy ước khi tạo mới hoặc khi đổi mã.
- **API cho `ADMIN` và `CONTENT_AUTHOR`:**
  - `PUT /api/content/knowledge-points/{id}`: sửa code, name, kind, learningType, skill, description, topicId.
  - `DELETE /api/content/knowledge-points/{id}`: chuyển sang `INACTIVE` (xóa mềm). Câu hỏi và lesson đang gắn vẫn giữ.
  - `GET /api/content/knowledge-points/{id}/prerequisites`.
  - `PUT /api/content/knowledge-points/{id}/prerequisites`, body `{ "prerequisiteIds": [...] }`: thay toàn bộ tập
    tiên quyết. Tạo vòng → `422 PREREQUISITE_CYCLE`; KP không tồn tại hoặc `INACTIVE` → `422 PREREQUISITE_INVALID`.
- **`GET /api/content/knowledge-tree?courseId=&skill=`** (`ADMIN`, `CONTENT_AUTHOR`, `EXAMINER`):
  - `nodes[]`: id, code, name, skill, kind, band, courseId, topicId, lessonIds, status.
  - `edges[]`: from = prerequisite, to = kp.
  - Không truyền `courseId` thì trả cả cây.
- **API nội bộ cho learning:** `POST /internal/learning-content/knowledge-points/prerequisites` nhận `{ "ids": [...] }`,
  trả `{ kpId: [prerequisiteId...] }`, chỉ gồm KP `ACTIVE`. Thêm vào `InternalLearningContentController`.
- Kiểm tra Gateway: `/api/content/**` đã chuyển tới content; `/internal/**` không mở ra ngoài.

## Architecture

```text
api/controller/KnowledgePointController ── PUT, DELETE, GET/PUT prerequisites
api/controller/KnowledgeTreeController  ── GET /api/content/knowledge-tree (mới)
application/usecase: UpdateKnowledgePointUseCase, DeactivateKnowledgePointUseCase,
                     ReplacePrerequisitesUseCase, GetKnowledgeTreeUseCase, GetPrerequisitesUseCase
domain/service/KnowledgeGraph (DFS, không vòng)
domain/repository/KnowledgePrerequisiteRepository ── infrastructure/persistence (JDBC hoặc JPA, theo convention content)
```

## Related Code Files

(`...` = `services/content-service/src/main/java/com/ieltspath/content`)

- Sửa: `.../domain/aggregate/KnowledgePoint.java`, `.../api/controller/KnowledgePointController.java`,
  `.../api/controller/InternalLearningContentController.java`,
  `.../infrastructure/persistence/adapter/KnowledgePointRepositoryAdapter.java`,
  `.../infrastructure/persistence/adapter/JdbcLearningContentReader.java`
- Tạo: `.../domain/service/KnowledgeGraph.java`, `.../domain/exception/PrerequisiteCycleException.java`,
  `.../domain/repository/KnowledgePrerequisiteRepository.java`, adapter tương ứng, các use case và DTO ở trên,
  `.../api/controller/KnowledgeTreeController.java`, `GlobalExceptionHandler` thêm mã lỗi mới
- Migration: `src/main/resources/db/migration/V29__knowledge_point_prerequisites.sql`
- Test: `KnowledgeGraphTest`, `KnowledgePointTest` (update/deactivate), `KnowledgePointControllerTest`
  (`@WebMvcTest`, quyền theo role), `KnowledgeTreeIntegrationTest` (Testcontainers)

## Implementation Steps

1. Khôi phục `docs/contracts/`, viết nháp `knowledge-tree-v1.md` (schema request/response, mã lỗi).
2. **Đỏ trước:** `KnowledgeGraphTest`:
   - A→B, B→C, thêm C→A → vòng;
   - tự trỏ → lỗi;
   - đồ thị rỗng → hợp lệ;
   - 200 node → dưới 50 ms.
3. Viết `KnowledgeGraph`, migration V29, repository và adapter.
4. Viết use case và controller. Phân quyền đúng như mục Requirements; `CUSTOMER` gọi → 403.
5. Viết API nội bộ, test `InternalLearningContentControllerTest`.
6. Test integration với Testcontainers:
   - tạo 3 KP, đặt cạnh, đọc cây;
   - thử tạo vòng → 422;
   - ngưng một KP → không còn trong cây và trong API nội bộ.
7. `mvn -q -pl services/content-service -am test`.

## Success Criteria

- [ ] Không thể lưu vòng, kể cả vòng dài (A→B→C→A).
- [ ] Ngưng KP không làm gãy câu hỏi hoặc lesson đang gắn (`question_knowledge_points` giữ nguyên).
- [ ] Contract `knowledge-tree-v1.md` khớp controller.

## Risk Assessment

- Đổi `topicId` của KP làm đổi band. Chỉ cho đổi khi KP chưa gắn câu hỏi nào đã `PUBLISHED`; nếu đã gắn → `409 KNOWLEDGE_POINT_IN_USE`.
- Cây lớn: `GET /knowledge-tree` không phân trang. Chấp nhận được vì cây dự kiến dưới 300 node.

## Bàn giao FE

Màn quản lý cây:
- danh sách KP lọc theo course và skill;
- form sửa KP;
- chọn tiên quyết;
- xem đồ thị (node/edge từ `/knowledge-tree`);
- báo lỗi `PREREQUISITE_CYCLE`.
