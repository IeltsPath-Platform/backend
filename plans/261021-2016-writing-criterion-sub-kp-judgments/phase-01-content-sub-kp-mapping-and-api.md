---
phase: 1
title: "Content: bảng KP con, API rubric/error type, hợp đồng"
status: complete
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 1: Content — bảng KP con, API rubric / error type, hợp đồng

Repo: `backend`. Module: `services/content-service`. Đọc [plan.md](./plan.md) trước (mục "Dữ kiện đã kiểm chứng", "Quyết định", "Hướng dẫn cho Codex").

## Overview
Content lưu nhiều KP con cho mỗi (task, tiêu chí); API rubric nội bộ trả chúng; API error type nội bộ trả thêm `knowledgePointId`; `LINKER_MISUSE` trỏ về KP từ nối đang dùng được.

## Requirements
- Bảng mới `writing_criterion_sub_knowledge_points`. Không sửa V69 / V70 / bất kỳ migration đã có.
- Chỉ seed 3 dòng (xem plan.md quyết định 3). Bảng và code phải tổng quát cho mọi tiêu chí, tối đa 4 KP con mỗi tiêu chí.
- Mỗi KP con phải: ACTIVE, skill `WRITING`, thuộc topic ACTIVE của course ACTIVE, khác KP tiêu chí của cùng (task, tiêu chí).
- `GET /internal/learning-content/rubrics/active` và `/{code}/{version}`: mỗi criterion có `subKnowledgePoints: [{id, code, name, description}]` theo `sort_order`; tiêu chí không có KP con → `[]`. Trường mới ở cuối, thêm-không-sửa.
- `GET /internal/learning-content/error-types`: thêm `knowledgePointId` (UUID hoặc null). Vẫn chỉ trả `ACTIVE` + `APPROVED`.
- API admin `/api/content/rubrics*` **không** đổi hình dạng (không lộ KP con).
- Không N+1: KP con của cả rubric lấy bằng 1 query.

## Architecture

### Migration `V71__writing_criterion_sub_knowledge_points.sql`
Tên file chính xác. Nội dung (viết đúng như sau, có thể thêm comment đầu file mô tả lý do):

```sql
CREATE TABLE writing_criterion_sub_knowledge_points (
    task VARCHAR(10) NOT NULL CHECK (task IN ('TASK_1', 'TASK_2')),
    criterion_code VARCHAR(8) NOT NULL,
    knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id),
    sort_order INTEGER NOT NULL CHECK (sort_order BETWEEN 1 AND 4),
    PRIMARY KEY (task, criterion_code, knowledge_point_id),
    CONSTRAINT uq_writing_sub_kp_task UNIQUE (task, knowledge_point_id),
    CONSTRAINT uq_writing_sub_kp_order UNIQUE (task, criterion_code, sort_order),
    CONSTRAINT fk_writing_sub_kp_criterion FOREIGN KEY (task, criterion_code)
        REFERENCES writing_criterion_knowledge_points (task, criterion_code),
    CONSTRAINT chk_writing_sub_kp_code CHECK (
        (task = 'TASK_1' AND criterion_code IN ('TA', 'CC', 'LR', 'GRA'))
        OR (task = 'TASK_2' AND criterion_code IN ('TR', 'CC', 'LR', 'GRA')))
);

INSERT INTO writing_criterion_sub_knowledge_points (task, criterion_code, knowledge_point_id, sort_order)
SELECT mapping.task, mapping.criterion_code, kp.id, mapping.sort_order
FROM (VALUES
    ('TASK_1', 'CC', 'DEMO_WRITING_LINKING', 1),
    ('TASK_2', 'CC', 'DEMO_WRITING_TOPIC_SENTENCE', 1),
    ('TASK_2', 'CC', 'DEMO_WRITING_LINKING', 2)
) AS mapping(task, criterion_code, kp_code, sort_order)
JOIN knowledge_points kp ON kp.code = mapping.kp_code;

UPDATE error_types
SET knowledge_point_id = (SELECT id FROM knowledge_points WHERE code = 'DEMO_WRITING_LINKING'),
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'LINKER_MISUSE';

DO $$
BEGIN
    IF (SELECT count(*) FROM writing_criterion_sub_knowledge_points) <> 3 THEN
        RAISE EXCEPTION 'writing_criterion_sub_knowledge_points must hold the 3 seeded mappings';
    END IF;
    IF EXISTS (
        SELECT 1 FROM writing_criterion_sub_knowledge_points s
        JOIN knowledge_points kp ON kp.id = s.knowledge_point_id
        JOIN topics t ON t.id = kp.topic_id
        JOIN courses c ON c.id = t.course_id
        WHERE kp.status <> 'ACTIVE' OR kp.skill <> 'WRITING' OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE') THEN
        RAISE EXCEPTION 'sub knowledge points must be ACTIVE WRITING points of an ACTIVE course';
    END IF;
    IF EXISTS (
        SELECT 1 FROM writing_criterion_sub_knowledge_points s
        JOIN writing_criterion_knowledge_points p ON p.task = s.task AND p.criterion_code = s.criterion_code
        WHERE p.knowledge_point_id = s.knowledge_point_id) THEN
        RAISE EXCEPTION 'a sub knowledge point must differ from its criterion knowledge point';
    END IF;
    IF (SELECT knowledge_point_id FROM error_types WHERE code = 'LINKER_MISUSE')
        IS DISTINCT FROM (SELECT id FROM knowledge_points WHERE code = 'DEMO_WRITING_LINKING') THEN
        RAISE EXCEPTION 'LINKER_MISUSE must point at DEMO_WRITING_LINKING';
    END IF;
END $$;
```

Không đổi `error_types.review_status` (vẫn DRAFT) và không đụng error type khác.

### Domain `Rubric.java` (`domain/aggregate/Rubric.java`)
- Thêm `public record SubKnowledgePoint(UUID id, String code, String name, String description) {}`.
- `Criterion` thêm thành phần cuối `List<SubKnowledgePoint> subKnowledgePoints`; compact constructor chuẩn hóa `null → List.of()` (cạnh dòng chuẩn hóa `bandDescriptors`).
- Giữ constructor 6 tham số `(code, name, description, sortOrder, bandDescriptors, knowledgePointId)` và 5 tham số, cả hai gọi canonical với `List.of()`; `RubricController.CriterionRequest.toDomain` dùng 5-tham-số nên không đổi.

### `JdbcRubricRepository.load(UUID id)`
Sau khi có `criterionRows` và trước khi dựng `criteria`, thêm một query theo task của rubric:

```java
record SubRow(String criterionCode, Rubric.SubKnowledgePoint point) {}
String task = (String) parent.get("task");
Map<String, List<Rubric.SubKnowledgePoint>> subPoints = jdbc.query("""
        SELECT s.criterion_code, kp.id, kp.code, kp.name, kp.description
        FROM writing_criterion_sub_knowledge_points s
        JOIN knowledge_points kp ON kp.id = s.knowledge_point_id
        WHERE s.task = :task AND kp.status = 'ACTIVE'
        ORDER BY s.criterion_code, s.sort_order
        """, new MapSqlParameterSource("task", task),
        (rs, i) -> new SubRow(rs.getString("criterion_code"), new Rubric.SubKnowledgePoint(
                rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getString("description"))))
        .stream().collect(java.util.stream.Collectors.groupingBy(SubRow::criterionCode, java.util.LinkedHashMap::new,
                java.util.stream.Collectors.mapping(SubRow::point, java.util.stream.Collectors.toList())));
```

Dùng `subPoints.getOrDefault(criterion.code(), List.of())` làm thành phần mới khi dựng `Rubric.Criterion`. Giữ nguyên các query hiện có.

### `InternalRubricController`
`CriterionResponse` thêm thành phần cuối `List<SubKnowledgePointResponse> subKnowledgePoints`; thêm `public record SubKnowledgePointResponse(UUID id, String code, String name, String description)` với `from(Rubric.SubKnowledgePoint)`; `CriterionResponse.from` map danh sách. Cập nhật Javadoc "shared with Learning and Assessment clients" nói thêm trường là additive.

### `InternalLearningContentController.ActiveErrorTypeResponse`
Thêm thành phần cuối `UUID knowledgePointId`, lấy từ `errorType.getKnowledgePointId()` (kiểm tên getter trong `domain/aggregate/ErrorType.java`; field đã có là `knowledgePointId`).

## Related Code Files
- Create: `services/content-service/src/main/resources/db/migration/V71__writing_criterion_sub_knowledge_points.sql`
- Modify: `services/content-service/src/main/java/com/ieltspath/content/domain/aggregate/Rubric.java`
- Modify: `services/content-service/src/main/java/com/ieltspath/content/infrastructure/persistence/adapter/JdbcRubricRepository.java`
- Modify: `services/content-service/src/main/java/com/ieltspath/content/api/controller/InternalRubricController.java`
- Modify: `services/content-service/src/main/java/com/ieltspath/content/api/controller/InternalLearningContentController.java`
- Modify tests: `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/RubricRepositoryIntegrationTest.java`, `.../api/controller/InternalLearningContentControllerTest.java`, `.../infrastructure/persistence/ErrorTypeSeedMigrationTest.java` (nếu cần)
- Modify docs: `backend/docs/contracts/learning-content-internal-v1.md`
- Không sửa: V69, V70, `RubricController`, `ManageRubricsUseCase` (chỉ sửa nếu compile yêu cầu).

## Implementation Steps (TDD)
1. Baseline: `mvn -q -pl services/content-service -am test`; ghi test đỏ có sẵn. Xác nhận Docker (`docker ps`) vì test persistence dùng Testcontainers.
2. Viết test (đỏ):
   - `RubricRepositoryIntegrationTest.rubricCriteriaListTheirSubKnowledgePointsInOrder`: TASK_2 active → CC có `[DEMO_WRITING_TOPIC_SENTENCE, DEMO_WRITING_LINKING]` theo thứ tự, mỗi phần tử có id/code/name/description khác null; TASK_1 active → CC có `[DEMO_WRITING_LINKING]`; TA, TR, LR, GRA có `[]`; `knowledgePointId` của mọi tiêu chí vẫn bằng bảng V69 (test `seededRubricsGiveEveryCriterionItsOwnKnowledgePointPerTask` hiện có phải vẫn xanh).
   - `...subKnowledgePointsAreActiveWritingPointsOfActiveCourses`: truy vấn bảng join `knowledge_points/topics/courses`, 3 dòng đều ACTIVE/WRITING.
   - `...theTableRejectsDuplicatesAndOrphans`: chèn trùng `(task, knowledge_point_id)` → `DuplicateKeyException`; chèn `sort_order = 5` → lỗi CHECK; chèn tiêu chí không có KP cha (ví dụ `('TASK_1','TR',...)`) → lỗi ràng buộc (dùng `DataIntegrityViolationException` hoặc `assertThatThrownBy` chung, theo phong cách file).
   - `...linkerMisuseIsLinkedToTheLinkingKnowledgePoint`: `error_types.knowledge_point_id` của `LINKER_MISUSE` = id KP `DEMO_WRITING_LINKING`.
   - `InternalLearningContentControllerTest`: error type có `knowledgePointId` được trả (UUID) và null được bỏ qua/serialize thành null theo cấu hình hiện có của test; rubric response có `subKnowledgePoints`. Làm theo cách file đó đang test (mock use case).
3. Viết V71, domain, repository, controller. Chạy lại: test mới xanh, toàn module xanh.
4. Cập nhật `learning-content-internal-v1.md`:
   - Mục `## Error types (phase 3a)` (~dòng 343): đổi mảng thành `{"code","name","category","severity","fixHint","knowledgePointId"}`, nói `knowledgePointId` nullable và là KP gắn với mã lỗi.
   - Mục `## Essay rubrics (phase 11)` (~dòng 347): thêm mô tả `criteria[].subKnowledgePoints` (`{id, code, name, description}`, thứ tự `sort_order`, `[]` khi không có, nguồn bảng `writing_criterion_sub_knowledge_points`, chỉ KP ACTIVE) và ví dụ ngắn.
5. Chạy `mvn -q -pl services/assessment-service -am test` để chắc client Assessment đọc rubric nội bộ không vỡ vì trường mới.

## Success Criteria
- [ ] Mọi test mới ở bước 2 xanh; test cũ của module xanh (trừ lỗi nền đã ghi).
- [ ] `GET /internal/learning-content/rubrics/active?task=TASK_2` trả CC với 2 KP con, các tiêu chí khác `[]`.
- [ ] Không file migration cũ bị sửa (`git diff --stat` chỉ có file mới cho `db/migration`).
- [ ] Assessment test xanh.
- [ ] Tài liệu contract khớp response thực tế.

## Risk Assessment
- Số migration: nếu `V71` đã bị chiếm, dùng số kế tiếp và cập nhật plan.
- `DO $$` kiểm tra join `topics` → `courses`: KP không có `topic_id` hợp lệ làm join rỗng, kiểm tra không bắt được. Đã đủ vì dữ liệu seed cố định và test integration kiểm lại 3 dòng.
- Dữ liệu `error_types` cũ trỏ KP INACTIVE ở các mã khác (ví dụ `W70_LEXICAL_PRECISION`): ngoài phạm vi, không sửa.
- Rollback: migration chỉ thêm bảng và đổi 1 dòng dữ liệu; hoàn tác bằng migration mới `DROP TABLE` + đặt lại `knowledge_point_id`.
