---
phase: 1
title: "Content: KP tiêu chí, bảng ánh xạ, rubric trả knowledgePointId"
status: completed
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 1: Content: KP tiêu chí, bảng ánh xạ, rubric trả `knowledgePointId`

## Mục tiêu

- Thêm 5 KP tiêu chí vào topic `DEMO_WRITING`.
- Thêm bảng ánh xạ `(task, criterion_code) -> KP`.
- `GET /internal/learning-content/rubrics/active` và `/rubrics/{code}/{version}` trả thêm `criteria[].knowledgePointId`
  (cộng thêm, nullable). API rubric admin/public (`RubricController`, `RubricResponse`) KHÔNG đổi.

## Files

| Hành động | File |
|---|---|
| Create | `services/content-service/src/main/resources/db/migration/V69__writing_criterion_knowledge_points.sql` |
| Modify | `services/content-service/src/main/java/com/ieltspath/content/domain/aggregate/Rubric.java` |
| Modify | `services/content-service/src/main/java/com/ieltspath/content/infrastructure/persistence/adapter/JdbcRubricRepository.java` |
| Modify | `services/content-service/src/main/java/com/ieltspath/content/api/controller/InternalRubricController.java` |
| Modify (test) | `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/RubricRepositoryIntegrationTest.java` |
| Modify (test) | `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/LessonPipelineSeedTest.java` (dòng 396-400) |
| Modify (doc) | `docs/contracts/learning-content-internal-v1.md` (mục "Essay rubrics", bảng internal ~dòng 376-377) |

## Bước 1: Migration V69

Kiểm tra lại `ls services/content-service/src/main/resources/db/migration` trước khi tạo. Nội dung:

```sql
-- Each IELTS Writing rubric criterion is evidence for its own knowledge point, so mastery can tell which criterion a
-- learner is weak in. The points live in the active DEMO_WRITING topic so they reach every learner's catalog. The
-- mapping is keyed by task and criterion code, not by rubric version: published rubrics are immutable, and a retired
-- version that an activated prompt still uses keeps its knowledge points.

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('3c000000-0000-4000-8000-020000000001', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_TA', 'Hoàn thành yêu cầu Task 1 (TA)', 'STRATEGY', 'PROCEDURE', 'WRITING',
     'Viết đoạn tổng quan, chọn và so sánh đúng các đặc điểm chính của biểu đồ, đủ số từ yêu cầu.', 'ACTIVE'),
    ('3c000000-0000-4000-8000-020000000002', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_TR', 'Trả lời đúng đề Task 2 (TR)', 'STRATEGY', 'PROCEDURE', 'WRITING',
     'Trả lời đủ mọi phần của đề, giữ quan điểm rõ và nhất quán, phát triển ý bằng lý do và ví dụ.', 'ACTIVE'),
    ('3c000000-0000-4000-8000-020000000003', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_CC', 'Mạch lạc và liên kết (CC)', 'STRATEGY', 'PROCEDURE', 'WRITING',
     'Chia đoạn hợp lý, sắp ý theo trình tự rõ ràng và nối câu bằng từ nối, đại từ tham chiếu đúng chức năng.', 'ACTIVE'),
    ('3c000000-0000-4000-8000-020000000004', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_LR', 'Vốn từ vựng (LR)', 'VOCABULARY', 'MEMORY', 'WRITING',
     'Dùng từ đa dạng, đúng nghĩa, đúng kết hợp từ và chính tả; diễn đạt lại thay vì lặp từ của đề.', 'ACTIVE'),
    ('3c000000-0000-4000-8000-020000000005', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_GRA', 'Ngữ pháp đa dạng và chính xác (GRA)', 'GRAMMAR', 'CONCEPT', 'WRITING',
     'Kết hợp câu đơn và câu phức, dùng đúng thì, hòa hợp chủ vị và dấu câu, ít lỗi làm sai nghĩa.', 'ACTIVE');

CREATE TABLE writing_criterion_knowledge_points (
    task VARCHAR(10) NOT NULL CHECK (task IN ('TASK_1', 'TASK_2')),
    criterion_code VARCHAR(8) NOT NULL,
    knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id),
    PRIMARY KEY (task, criterion_code),
    CONSTRAINT uq_writing_criterion_kp UNIQUE (task, knowledge_point_id),
    CONSTRAINT chk_writing_criterion_code CHECK (
        (task = 'TASK_1' AND criterion_code IN ('TA', 'CC', 'LR', 'GRA'))
        OR (task = 'TASK_2' AND criterion_code IN ('TR', 'CC', 'LR', 'GRA')))
);

INSERT INTO writing_criterion_knowledge_points (task, criterion_code, knowledge_point_id)
SELECT mapping.task, mapping.criterion_code, kp.id
FROM (VALUES
    ('TASK_1', 'TA', 'DEMO_WRITING_CRITERION_TA'),
    ('TASK_1', 'CC', 'DEMO_WRITING_CRITERION_CC'),
    ('TASK_1', 'LR', 'DEMO_WRITING_CRITERION_LR'),
    ('TASK_1', 'GRA', 'DEMO_WRITING_CRITERION_GRA'),
    ('TASK_2', 'TR', 'DEMO_WRITING_CRITERION_TR'),
    ('TASK_2', 'CC', 'DEMO_WRITING_CRITERION_CC'),
    ('TASK_2', 'LR', 'DEMO_WRITING_CRITERION_LR'),
    ('TASK_2', 'GRA', 'DEMO_WRITING_CRITERION_GRA')
) AS mapping(task, criterion_code, kp_code)
JOIN knowledge_points kp ON kp.code = mapping.kp_code;

DO $$
BEGIN
    IF (SELECT count(*) FROM writing_criterion_knowledge_points) <> 8 THEN
        RAISE EXCEPTION 'writing_criterion_knowledge_points must map all 8 task criteria';
    END IF;
END $$;
```

Ghi chú:
- Nếu topic `DEMO_WRITING` không tồn tại, `(SELECT id ...)` ra NULL và `topic_id NOT NULL` làm migration fail. Đây là
  hành vi mong muốn, không thay bằng `INSERT ... SELECT FROM topics` (vì cách đó chèn 0 dòng mà không báo lỗi).
- Tiền tố id `3c000000-` chưa được dùng ở content-service (đã grep). Grep lại trước khi chạy.
- KP mới không cần `lesson_knowledge_points`, cạnh tiên quyết hay `cefr_level`.

## Bước 2: `Rubric.Criterion` thêm `knowledgePointId`

Trong `Rubric.java`, thêm component cuối và giữ constructor cũ (5 tham số) để `RubricController.java:76` và các test
hiện có không phải sửa:

```java
public record Criterion(String code, String name, String description, int sortOrder,
                        List<BandDescriptor> bandDescriptors, UUID knowledgePointId) {
    public Criterion {
        // giữ nguyên validation hiện có
    }

    /** A criterion as authored; its knowledge point comes from the task mapping when the rubric is read. */
    public Criterion(String code, String name, String description, int sortOrder, List<BandDescriptor> bandDescriptors) {
        this(code, name, description, sortOrder, bandDescriptors, null);
    }
}
```

Thêm `import java.util.UUID;`. `knowledgePointId` được phép `null`.

## Bước 3: `JdbcRubricRepository.load` đọc ánh xạ

Thay câu truy vấn tiêu chí ở `load(UUID id)` (dòng 96-99):

```java
List<CriterionRow> criterionRows = jdbc.query("""
        SELECT c.id, c.code, c.name, c.description, c.sort_order, m.knowledge_point_id
        FROM rubric_criteria c
        JOIN rubrics r ON r.id = c.rubric_id
        LEFT JOIN writing_criterion_knowledge_points m ON m.task = r.task AND m.criterion_code = c.code
        WHERE c.rubric_id = :id ORDER BY c.sort_order
        """, p,
        (rs, row) -> new CriterionRow(rs.getObject("id", UUID.class), rs.getString("code"),
                rs.getString("name"), rs.getString("description"), rs.getInt("sort_order"),
                rs.getObject("knowledge_point_id", UUID.class)));
```

- `CriterionRow` thêm `UUID knowledgePointId`.
- Dòng 105-106 dựng `new Rubric.Criterion(..., descriptors, criterion.knowledgePointId())`.
- `saveChildren`, `updateDraft`, `activate`, `validateForActivation` không đổi: ánh xạ không lưu theo tiêu chí.
- Hệ quả: draft mới tạo cũng đọc ra KP theo task. Đúng như mong muốn, không cần xử lý riêng.

## Bước 4: `InternalRubricController.CriterionResponse`

```java
public record CriterionResponse(String code, String name, String description, int sortOrder,
                                List<BandDescriptorResponse> bandDescriptors, UUID knowledgePointId) {
    static CriterionResponse from(Rubric.Criterion criterion) {
        return new CriterionResponse(criterion.code(), criterion.name(), criterion.description(), criterion.sortOrder(),
                criterion.bandDescriptors().stream().map(BandDescriptorResponse::from).toList(),
                criterion.knowledgePointId());
    }
}
```

Assessment-service cũng đọc endpoint này (`ActiveRubricProvider`). Trường lạ được bỏ qua vì ObjectMapper mặc định của
Spring Boot tắt `FAIL_ON_UNKNOWN_PROPERTIES`, và repo không bật lại (đã grep). Chỉ cần chạy lại test assessment ở phase 4.

## Bước 5: Test

1. `RubricRepositoryIntegrationTest`: thêm test
   `seededRubricsGiveEveryCriterionItsOwnKnowledgePointPerTask`:
   - `rubrics.findActive("TASK_1").getFirst().criteria()`: mọi `knowledgePointId` khác null, đôi một khác nhau.
     Tra `code` của KP bằng SQL `SELECT code FROM knowledge_points WHERE id = ?` và so với
     `TA→DEMO_WRITING_CRITERION_TA, CC→..._CC, LR→..._LR, GRA→..._GRA`.
   - Tương tự `TASK_2` với `TR→DEMO_WRITING_CRITERION_TR`.
   - `TASK_1.CC` và `TASK_2.CC` cùng một KP.
   Trong test có sẵn `draftsCanBeEditedOnceAndActivationRetiresTheOldVersionButKeepsItReadable`, thêm assert
   `previous.criteria()` (bản `RETIRED`) đều có `knowledgePointId` khác null.
2. `LessonPipelineSeedTest` dòng 396-400: KP của topic sắp theo `created_at, id`
   (`JdbcLearningContentReader.java:182`), nên 5 KP mới đứng cuối, theo thứ tự id. Sửa thành:
   ```java
   .containsExactly(tuple("DEMO_READING_W2_OPINION", true), tuple("DEMO_READING_W1_CHART", true),
           tuple("DEMO_WRITING_TOPIC_SENTENCE", true), tuple("DEMO_WRITING_LINKING", true),
           tuple("DEMO_WRITING_TREND_LANGUAGE", true), tuple("DEMO_WRITING_CRITERION_TA", false),
           tuple("DEMO_WRITING_CRITERION_TR", false), tuple("DEMO_WRITING_CRITERION_CC", false),
           tuple("DEMO_WRITING_CRITERION_LR", false), tuple("DEMO_WRITING_CRITERION_GRA", false));
   ```
   Sửa comment dòng 395 cho khớp, ví dụ: "...the three added later have practice sets; the five rubric-criterion points
   have none."
3. Nếu có test WebMvc/serialization cho `InternalRubricController`, thêm assert `criteria[0].knowledgePointId`. Nếu
   không có thì không tạo mới; test repository ở trên là đủ.

Chạy: `mvn -q -pl services/content-service -am package`.

## Bước 6: Contract doc

Trong `docs/contracts/learning-content-internal-v1.md`, mục "Essay rubrics", thêm sau bảng endpoint nội bộ:

> Each internal criterion also carries `knowledgePointId` (UUID, nullable, added 2026-10-21): the knowledge point that
> the criterion is evidence for, from `writing_criterion_knowledge_points` keyed by task and criterion code
> (`V69__writing_criterion_knowledge_points.sql`). It does not depend on the rubric version. Admin and public rubric
> responses do not include it.

## Tiêu chí hoàn thành

- [ ] V69 chạy được trên DB đã có V68; có 5 KP mới (ACTIVE, topic `DEMO_WRITING`) và 8 dòng ánh xạ.
- [ ] Internal rubric active và theo phiên bản (kể cả `RETIRED`) trả `knowledgePointId` cho mọi tiêu chí.
- [ ] `RubricResponse` admin/public không đổi.
- [ ] content-service `package` xanh (ghi rõ test Testcontainers nào bị bỏ qua nếu không có Docker).
