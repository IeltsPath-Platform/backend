---
phase: 2
title: Content schema and trap knowledge points
status: completed
priority: P1
effort: 0.5d
dependencies:
  - 1
---

# Phase 2: Content schema and trap knowledge points

## Overview
Một migration content: cột liên kết mã lỗi ↔ KP con, tạo KP con cho mã hiện có, ghi bẫy T/F/NG vào `distractors` của câu cũ.

## Requirements
- Mỗi mã ACTIVE có `knowledge_point_id` trỏ KP cha ACTIVE có đúng một KP con.
- Câu `TRUE_FALSE_NOT_GIVEN` chưa có `distractors` được thêm theo bảng dưới, chỉ khi cả hai mã T/F/NG tồn tại và ACTIVE.
- Không sửa migration cũ; không đổi `error_types.knowledge_point_id`; chỉ thêm khóa `distractors` vào `answer_spec` (không
  đổi `correct`; phiên bản đã xuất bản không chấm lại, như V46).
- `PracticeSetCoverageMigrationTest` loại KP con (red-team PH2).
- **KP con ẩn mặc định khỏi `topic-sequence` ngay từ phase này** (red-team PH1): V79 tạo KP con trong topic của KP cha nên
  nếu không ẩn, mọi người đọc `topic-sequence` (learning, `LessonPipelineSeedTest`) thấy KP lạ ngay. Phase 3 chỉ thêm đường
  mở (`includeErrorTraps=true`) chứ không thêm bộ lọc.
- Test cũ ghim số liệu của seed khác (ví dụ `DistractorSeedMigrationTest`) bị V79 làm đổi số đếm hợp lệ: được phép ghim lại
  test vào đúng phiên bản nó viết cho (`Flyway.target`) hoặc giới hạn phạm vi đếm; **giữ nguyên ý định và mọi assert**, ghi
  từng test đã đổi và lý do vào báo cáo. Không nới lỏng assert, không xóa test.

## Architecture

Kiểm số migration ngay trước khi tạo (`ls services/content-service/src/main/resources/db/migration | sort -V | tail -1`;
hiện tới V78). File `V79__error_trap_knowledge_points.sql`:

```sql
-- KP con: kỹ năng tránh một bẫy. error_types.knowledge_point_id vẫn là KP cha (Writing và màn mã lỗi đang dùng).
ALTER TABLE error_types ADD COLUMN trap_knowledge_point_id UUID UNIQUE REFERENCES knowledge_points(id);

INSERT INTO knowledge_points (topic_id, code, name, kind, learning_type, skill, description, status)
SELECT parent.topic_id, 'TRAP_' || et.code, et.name, parent.kind, parent.learning_type, parent.skill, et.fix_hint, 'ACTIVE'
FROM error_types et
JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
WHERE et.status = 'ACTIVE' AND parent.status = 'ACTIVE';

UPDATE error_types et SET trap_knowledge_point_id = kp.id
FROM knowledge_points kp
WHERE kp.code = 'TRAP_' || et.code;

-- Bẫy T/F/NG (và Y/N/NG cùng loại câu): đúng NOT_GIVEN → chọn FALSE/NO là TFNG_NG_AS_FALSE;
-- đúng FALSE/NO → chọn NOT_GIVEN là TFNG_FALSE_AS_NG. Đúng TRUE/YES: không thêm.
UPDATE question_versions qv
SET answer_spec = qv.answer_spec || jsonb_build_object('distractors',
        CASE qv.answer_spec->>'correct'
            WHEN 'NOT_GIVEN' THEN jsonb_build_object(
                CASE WHEN qv.options @> '[{"optionKey":"NO"}]' THEN 'NO' ELSE 'FALSE' END, 'TFNG_NG_AS_FALSE')
            ELSE jsonb_build_object('NOT_GIVEN', 'TFNG_FALSE_AS_NG')
        END)
FROM questions q
WHERE q.id = qv.question_id
  AND q.question_type = 'TRUE_FALSE_NOT_GIVEN'
  AND NOT (qv.answer_spec ? 'distractors')
  AND qv.answer_spec->>'correct' IN ('NOT_GIVEN', 'FALSE', 'NO')
  AND (qv.answer_spec->>'correct' <> 'NOT_GIVEN'
       OR qv.options @> '[{"optionKey":"FALSE"}]' OR qv.options @> '[{"optionKey":"NO"}]')
  AND (qv.answer_spec->>'correct' = 'NOT_GIVEN' OR qv.options @> '[{"optionKey":"NOT_GIVEN"}]')
  AND (SELECT count(*) FROM error_types e
       WHERE e.code IN ('TFNG_NG_AS_FALSE', 'TFNG_FALSE_AS_NG') AND e.status = 'ACTIVE') = 2;
```

Trước khi chạy, xác nhận định dạng `question_versions.options` là mảng `[{"optionKey": ..., "content": ..., "sortOrder": ...}]`
(xem seed V30). Nếu khác → dừng và báo.

## Related Code Files
- Create: `services/content-service/src/main/resources/db/migration/V79__error_trap_knowledge_points.sql`
- Create: `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/ErrorTrapKnowledgePointMigrationTest.java`
  (Testcontainers, cùng khuôn `PracticeSetCoverageMigrationTest`: chạy toàn bộ Flyway rồi truy vấn)
- Modify: `services/content-service/src/main/java/com/ieltspath/content/infrastructure/persistence/adapter/JdbcLearningContentReader.java`
  (chỉ câu SQL KP của `topicSequence`, khoảng dòng 176-182: thêm
  `AND NOT EXISTS (SELECT 1 FROM error_types et WHERE et.trap_knowledge_point_id = kp.id)`; không đổi gì khác)
- Modify: `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/DistractorSeedMigrationTest.java`:
  ghim cả lớp vào `Flyway.target("78")` (seed Reading/Listening V46/V47 phải đúng 20 câu và 31 đáp án sai như khi viết test) và
  thêm comment: dữ liệu bẫy về sau do test `ErrorTrap*` kiểm. Giữ nguyên 6 test và mọi assert.
- Modify: `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/PracticeSetCoverageMigrationTest.java`:
  trong truy vấn đếm bộ theo KP, thêm điều kiện
  `kp.id NOT IN (SELECT trap_knowledge_point_id FROM error_types WHERE trap_knowledge_point_id IS NOT NULL)`

## Implementation Steps
1. Baseline: `mvn -q -pl services/content-service -am test`; ghi test đỏ có sẵn.
2. **Đỏ trước** — `ErrorTrapKnowledgePointMigrationTest`:
   - `everyActiveErrorTypeWithActiveParentHasOneTrapPoint`: với mỗi mã ACTIVE có KP cha ACTIVE, `trap_knowledge_point_id`
     không null, KP con có `code = 'TRAP_' || mã`, cùng `topic_id`, `kind`, `learning_type`, `skill` với KP cha.
   - `parentKnowledgePointIsUnchanged`: `LINKER_MISUSE.knowledge_point_id` vẫn là id của `DEMO_WRITING_LINKING`.
   - `notGivenQuestionsGetTheFalseTrap`: mọi câu T/F/NG đúng `NOT_GIVEN` có lựa chọn `FALSE` → `distractors.FALSE = TFNG_NG_AS_FALSE`.
   - `falseQuestionsGetTheNotGivenTrap`: đúng `FALSE` → `distractors.NOT_GIVEN = TFNG_FALSE_AS_NG`.
   - `trueQuestionsGetNoTrap`: đúng `TRUE` → không có khóa `distractors`.
   - `correctAnswersAreUnchanged`: số câu theo từng giá trị `correct` trước/sau V79 bằng nhau (so với truy vấn trên V78:
     dùng `Flyway.target("78")` ở một container riêng, hoặc so với số đếm cố định ghi ở phase 1).
   - `topicSequenceDoesNotListTrapPoints` (cùng khuôn `PracticeSetCoverageMigrationTest`: `new JdbcLearningContentReader(jdbc)`
     rồi `topicSequence(3)`): không KP nào có code `TRAP_*`; `LessonPipelineSeedTest` (đang đỏ 2 ca vì KP con) phải xanh
     mà không sửa file đó.
3. Viết migration; thêm bộ lọc `topicSequence`; chỉnh `PracticeSetCoverageMigrationTest` và `DistractorSeedMigrationTest`;
   chạy lại toàn module. Toàn module phải xanh ở cuối phase này (bốn test cũ đã đỏ ở lần chạy trước).

## Success Criteria
- [ ] Test migration mới xanh; `PracticeSetCoverageMigrationTest` xanh; test cũ xanh (trừ đỏ có sẵn).
- [ ] `trap_knowledge_point_id` unique; null với mã không có KP cha hoặc KP cha INACTIVE.

## Risk Assessment
- KP con hiện ra ở danh sách KP của author và có thể bị gắn câu → chặn ở phase 3.
- Câu khác T/F/NG chưa có `distractors` → KP con của chúng chưa có bằng chứng; dữ liệu mẫu bổ sung ở phase 9.
- Rollback: KP con chưa có bằng chứng; gỡ bằng migration mới (không sửa V79).
