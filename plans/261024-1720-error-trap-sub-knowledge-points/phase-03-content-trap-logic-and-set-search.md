---
phase: 3
title: Content trap logic and set search
status: completed
priority: P1
effort: 1.5d
dependencies:
  - 2
---

# Phase 3: Content trap logic and set search

## Overview
Giữ KP con đồng bộ với mã lỗi và KP cha; tự điền bẫy T/F/NG khi lưu câu; chặn gắn câu/tiên quyết vào KP con; tìm bộ xếp
theo câu bẫy; `topic-sequence` trả KP con cho learning khi được xin.

## Requirements
1. **Đồng bộ KP con** (application service mới `TrapKnowledgePointSync`, gọi trong cùng transaction):
   - Sau `CreateErrorTypeUseCase`/`UpdateErrorTypeUseCase`: mã ACTIVE + có KP cha ACTIVE → có KP con (tạo nếu thiếu; cập
     nhật `name` = tên lỗi, `description` = `fix_hint`, `topic_id`/`kind`/`learning_type`/`skill` = của KP cha, ACTIVE).
     Ngược lại (mã INACTIVE, bỏ KP cha, KP cha INACTIVE) → KP con INACTIVE nếu đã có. Không bao giờ xóa KP con.
   - Sau khi đổi trạng thái KP (use case cập nhật KP; tìm bằng `grep -rn "ContentStatus.INACTIVE\|setStatus" application/usecase`
     trong các use case KP): đồng bộ lại mọi mã có `knowledge_point_id` = KP đó. <!-- red-team PH6 -->
   - Tạo KP qua aggregate/repository có sẵn (`KnowledgePoint` + `KnowledgePointRepository`); thiếu factory phù hợp thì thêm
     factory vào aggregate, không ghi SQL thẳng từ use case.
2. **Tự điền T/F/NG** trong `AddQuestionVersionUseCase.execute`, trước `validateDistractors` (dòng 45): câu loại
   `TRUE_FALSE_NOT_GIVEN`, `answerSpec` không có `distractors`, và `errorTypeRepository.findActiveCodes(Set.of("TFNG_NG_AS_FALSE","TFNG_FALSE_AS_NG"))`
   trả đủ hai mã → gộp `distractors` từ `TrueFalseNotGivenTraps` vào `answerSpec`. Thiếu mã → giữ nguyên (red-team PH4).
3. **Chặn KP con:** `AddQuestionVersionUseCase` từ chối `KnowledgePointInput` là KP con; use case quan hệ tiên quyết từ chối
   KP con ở cả hai đầu. Lỗi 400 qua `GlobalExceptionHandler`, mã `TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE`. Danh sách KP cho
   picker của author loại KP con (tìm query của `KnowledgePointPicker` phía content: endpoint list KP của author).
4. **Tìm bộ theo bẫy:** `searchPracticeSets` nhận thêm `trapErrorCode` (nullable). Lọc giữ nguyên. Kết quả có thêm
   `trapQuestionCount`. Thứ tự: số câu bẫy (chỉ tính khi ≥ 2) → tỷ lệ câu bẫy (chỉ khi ≥ 2) → thứ tự hiện tại. `trapErrorCode`
   null → `trapQuestionCount = 0`, thứ tự cũ y nguyên.
5. **`topic-sequence`** nhận `includeErrorTraps` (mặc định false). False → không trả KP con (**bộ lọc này đã có từ phase 2**,
   không thêm lại). True → bỏ bộ lọc và trả KP con với `parentKnowledgePointId`, `errorCode`, `hasPracticeSet = false` (KP
   con không bao giờ được ôn trực tiếp). <!-- red-team PH1 -->

## Architecture

**Luật T/F/NG** — `content/domain/service/TrueFalseNotGivenTraps.java` (thuần, không Spring):

```java
public static Map<String, String> distractorsFor(String correct, Set<String> optionKeys)
// "NOT_GIVEN": optionKeys có "FALSE" → {"FALSE": "TFNG_NG_AS_FALSE"}; có "NO" → {"NO": "TFNG_NG_AS_FALSE"}
// "FALSE" hoặc "NO": optionKeys có "NOT_GIVEN" → {"NOT_GIVEN": "TFNG_FALSE_AS_NG"}
// còn lại (TRUE, YES, null, thiếu khóa) → Map.of()
```

**SQL `searchPracticeSets`** (`JdbcLearningContentReader.java:640`). Postgres không cho dùng alias trong biểu thức `ORDER BY`,
nên bọc subquery:

```sql
SELECT * FROM (
    SELECT p.id, p.current_published_version_id, p.code, p.required_feature_key, p.lesson_id,
           COALESCE(c.band_level, t.band_max, t.band_min) AS band,
           (... question_count như cũ ...) AS question_count,
           (... matched_question_count như cũ ...) AS matched_question_count,
           %TRAP% AS trap_question_count
    FROM content_packages p
    LEFT JOIN lessons package_lesson ON package_lesson.id = p.lesson_id
    LEFT JOIN topics t ON t.id = COALESCE(p.topic_id, package_lesson.topic_id)
    LEFT JOIN courses c ON c.id = COALESCE(p.course_id, t.course_id)
    WHERE <eligiblePracticeSet như cũ> <exclusion như cũ>
) ranked
ORDER BY CASE WHEN trap_question_count >= 2 THEN trap_question_count ELSE 0 END DESC,
         CASE WHEN trap_question_count >= 2 THEN trap_question_count::numeric / NULLIF(question_count, 0) ELSE 0 END DESC,
         <preferred như cũ, đổi p.lesson_id → ranked.lesson_id> matched_question_count DESC, id
LIMIT :limit
```

`%TRAP%` khi có `trapErrorCode`:

```sql
(SELECT count(DISTINCT sq.question_version_id) FROM content_sections s
   JOIN section_questions sq ON sq.section_id = s.id
   JOIN question_versions qv ON qv.id = sq.question_version_id
 WHERE s.package_version_id = p.current_published_version_id
   AND jsonb_typeof(qv.answer_spec->'distractors') = 'object'
   AND EXISTS (SELECT 1 FROM jsonb_each_text(qv.answer_spec->'distractors') d WHERE d.value = :trapErrorCode))
```

khi không có: `0`.

**SQL `topic-sequence`** (`JdbcLearningContentReader.java:176`; phase 2 đã thêm
`AND NOT EXISTS (... et.trap_knowledge_point_id = kp.id)`): `LEFT JOIN error_types trap ON trap.trap_knowledge_point_id = kp.id`;
`includeErrorTraps = false` → giữ `AND trap.id IS NULL` (thay cho `NOT EXISTS` của phase 2); chọn thêm `trap.knowledge_point_id AS parent_kp_id`, `trap.code AS error_code`;
`has_practice_set` = `CASE WHEN trap.id IS NOT NULL THEN FALSE ELSE EXISTS (...) END`.

## Related Code Files
- Create: `content/domain/service/TrueFalseNotGivenTraps.java`, `content/application/service/TrapKnowledgePointSync.java` (+ test)
- Modify: `content/application/usecase/CreateErrorTypeUseCase.java`, `UpdateErrorTypeUseCase.java`, use case cập nhật KP,
  `AddQuestionVersionUseCase.java`, use case quan hệ tiên quyết KP
- Modify: `content/domain/aggregate/ErrorType.java` (+ `trapKnowledgePointId`), `JdbcErrorTypeRepository.java`,
  `ErrorTypeRepository.java` (+ `isTrapKnowledgePoint(Collection<UUID>)` hoặc tương đương)
- Modify: `content/infrastructure/persistence/adapter/JdbcLearningContentReader.java`, `application/port/LearningContentReader.java`,
  `SearchPracticeSetsUseCase`/`SearchPracticeSetsCommand`, `PracticeSetResult`, `TopicSequenceResult`
- Modify: `content/api/controller/InternalLearningContentController.java`, `api/dto/internal/PracticeSetSearchRequest.java`,
  `PracticeSetResponse.java`, `TopicSequenceResponse.java`; endpoint/query danh sách KP cho author
- Modify: `docs/contracts/learning-content-internal-v1.md`, `docs/contracts/answer-spec-v1.md`

## Implementation Steps
1. Baseline test content.
2. **Đỏ trước:**
   - `TrueFalseNotGivenTrapsTest`: NOT_GIVEN+FALSE, NOT_GIVEN+NO, FALSE, NO, TRUE, YES, thiếu khóa.
   - `TrapKnowledgePointSyncTest` (Mockito): tạo mã ACTIVE có KP cha → tạo KP con; đổi tên → KP con đổi tên; INACTIVE →
     KP con INACTIVE; bỏ KP cha → KP con INACTIVE; KP cha INACTIVE → KP con INACTIVE; KP cha ACTIVE lại → KP con ACTIVE.
   - `AddQuestionVersionUseCaseTest`: T/F/NG không `distractors` → được điền; đã có → giữ; một mã T/F/NG INACTIVE → không
     điền và lưu thành công; gắn KP con → 400 `TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE`.
   - Testcontainers `searchPracticeSets` (tạo 3 bộ PS-A 4 NG/1 F, PS-B 1 NG/4 F, PS-C thuộc lesson 2 NG/3 F, cùng KP):
     không mã → PS-C, …(thứ tự cũ); `TFNG_NG_AS_FALSE` → PS-A, PS-C, PS-B; `TFNG_FALSE_AS_NG` → PS-B, PS-C, PS-A; bộ chỉ
     1 câu bẫy không vượt bộ thuộc lesson; `trapQuestionCount` đúng.
   - Testcontainers `topicSequence`: không cờ → không KP con; có cờ → KP con có `parentKnowledgePointId`, `errorCode`,
     `hasPracticeSet = false`.
   - Test controller: trường mới tùy chọn (request cũ không có `trapErrorCode` vẫn chạy).
3. Cài đặt theo thứ tự domain → service đồng bộ → use case → SQL → controller/DTO → contract.
4. `mvn -q -pl services/content-service -am test`.

## Success Criteria
- [ ] Mọi test ở bước 2 xanh; test đặc tả thứ tự của phase 1 xanh.
- [ ] Không đường nào gắn câu hay tiên quyết vào KP con.
- [ ] Contract ghi `trapErrorCode`, `trapQuestionCount`, `includeErrorTraps`, trường KP con, `distractors` tự điền cho T/F/NG.

## Risk Assessment
- `jsonb_each_text` mỗi câu trong bộ đã lọc theo KP: nhỏ; đo `EXPLAIN` nếu chậm trước khi tính index.
- Màn mã lỗi FE (plan 261023-0050) không đổi; KP con tự đồng bộ ở backend.
