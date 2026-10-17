---
phase: 2
title: "Seed cây band 5.5 → 7 và difficulty"
status: in_progress
priority: P1
dependencies: [1]
---

# Phase 2: Seed cây band 5.5 → 7 và difficulty

Phase này tách làm hai phần:
- **2a (kỹ thuật, giao Codex ngay):** course và topic 6.0/7.0 ở trạng thái `INACTIVE`, bắt buộc `difficulty`, lọc
  learning, test.
- **2b (nội dung, chờ duyệt):** KP mới, cạnh, difficulty cho câu cũ, bộ câu. Không bắt đầu khi chưa có người hiểu IELTS
  duyệt bảng ở bước 1 của 2b.

## Overview

Mentor yêu cầu "dựng một cái cây từ band 5.5 cho đến band 7". Seed hiện có khoảng 20 KP, mã đặt không thống nhất
(`R65_PARAPHRASE`, `LS_NUM`, `DR_TOPIC_SENTENCE`, `DEMO_READING_W2_OPINION`…), chưa có cạnh, và chỉ đúng 1 câu có
`difficulty`.

## Quyết định riêng của phase

- **P2-a. Không đổi `topic_id` của KP cũ** (theo P1-e và quyết định của plan). Đã xác minh: các KP cũ đều thuộc topic có
  `skill` nên V21 dòng 4 gán cho course 5.5, trừ các KP nằm trong topic của 6.5.
- **P2-b. Band hiển thị theo course đang chứa KP**, không theo mức IELTS trong bảng cạnh. Vì vậy:
  - KP cũ của course 5.5 hiện band 5.5, KP cũ của 6.5 hiện band 6.5, dù độ khó thực tế của chúng cao hơn.
  - Band 6.0 và 7.0 chỉ có KP mới (`R60_*`, `L60_*`, `W60_*`, `R70_*`, `L70_*`, `W70_*`). Band 6.0 vì vậy mỏng. Khi trình
    hội đồng, nói rõ đây là dữ liệu bắt đầu và nhóm nội dung sẽ bổ sung.
- **P2-c. Cạnh đặt theo độ khó thực tế, không theo band hiển thị.** Ví dụ `TFNG_FALSE_VS_NOT_GIVEN` (5.5) →
  `R65_EVIDENCE_INFERENCE` (6.5) là hợp lệ.
- **P2-d. ID mới không dùng dải `29000000-...`.** Dải này đã có 106 UUID trong migration cũ. Course 6.0/7.0 dùng
  `30000000-0000-4000-8000-000000000003` và `...000000000004` (hai dải này chưa dùng). Topic và KP mới dùng dải
  `31000000-0000-4000-8000-...`. Trước khi merge, `grep` để xác nhận không trùng.
- **P2-e. Số migration:** 2a dùng `V30`; 2b dùng số trống kế tiếp khi merge (plan.md đã ghi là đánh lại theo thứ tự
  merge).

## Phần 2a: kỹ thuật (giao Codex ngay)

### Requirements

- Migration `V30__courses_6_0_7_0_inactive.sql`, idempotent (`ON CONFLICT DO NOTHING`):
  - course `IELTS_6_0` (band 6.0) và `IELTS_7_0` (band 7.0), `status = 'INACTIVE'`;
  - 6 topic `INACTIVE`, mỗi course có 3 topic theo skill READING, LISTENING, WRITING, `course_id` trỏ đúng course.
  - Không thêm KP ở đây. KP nằm trong 2b.
- **Difficulty bắt buộc khi tạo version câu hỏi:**
  - `AddQuestionVersionRequest.difficulty` hiện không có ràng buộc. Trong `AddQuestionVersionUseCase` (hoặc ngay sau bean
    validation) kiểm `difficulty != null`, nếu không thì ném exception mới.
  - Lỗi trả `400` với `code = DIFFICULTY_REQUIRED`. Không dùng message mặc định của `MethodArgumentNotValidException`
    vì nó trả `Validation failed`, không có mã.
  - Trước khi sửa, `grep` mọi nơi gọi `AddQuestionVersion` và `QuestionVersion.create` để chắc không có đường nào tạo
    version mà không qua API (seed và test). Các test cũ tạo version không có difficulty phải được cập nhật.
- **Lọc `INACTIVE` ở learning:**
  - `RefreshLearningTopicsUseCase` và `JdbcLearningContentReader` (topic sequence) bỏ qua course `INACTIVE` và topic
    `INACTIVE`. Hiện chưa lọc (đã kiểm: `CoursePathIntegrationTest` chưa có kịch bản này).
  - Thêm test trong `CoursePathIntegrationTest`: course `INACTIVE` không xuất hiện trong danh sách của học viên.

### Related Code Files

- Migration: `services/content-service/src/main/resources/db/migration/V30__courses_6_0_7_0_inactive.sql`
- Sửa:
  - `services/content-service/src/main/java/com/ieltspath/content/application/usecase/AddQuestionVersionUseCase.java`
  - `services/content-service/src/main/java/com/ieltspath/content/api/exception/GlobalExceptionHandler.java` (mã
    `DIFFICULTY_REQUIRED`)
  - `services/content-service/src/main/java/com/ieltspath/content/domain/exception/` (exception mới)
  - `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/RefreshLearningTopicsUseCase.java`
  - `services/learning-service/.../JdbcLearningContentReader` (topic sequence; tên đúng xem khi `grep`)
- Test:
  - `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/CourseSeedTest.java` (đã có,
    mở rộng: course 6.0, 7.0 là `INACTIVE`, topic 6 cái `INACTIVE`)
  - `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/KnowledgeTreeSeedTest.java`
    (mới, phần course/topic; phần KP và cạnh thêm ở 2b)
  - `services/content-service/src/test/java/com/ieltspath/content/api/controller/QuestionControllerTest.java` (thiếu
    difficulty → 400 `DIFFICULTY_REQUIRED`)
  - `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/persistence/CoursePathIntegrationTest.java`
    (thêm kịch bản course `INACTIVE`)

### Implementation Steps (2a)

1. **Đỏ trước:** `CourseSeedTest` (course 6.0/7.0 `INACTIVE`), `QuestionControllerTest` (thiếu difficulty), kịch bản
   `CoursePathIntegrationTest`.
2. Viết V30 (course, topic).
3. Thêm exception và kiểm difficulty; cập nhật test cũ dùng version không có difficulty.
4. Thêm lọc `INACTIVE` ở learning, kèm test.
5. `grep` xác nhận không trùng UUID mới trong `db/migration`.
6. `mvn -q -pl services/content-service,services/learning-service -am test`. Testcontainers bị skip thì không tính xanh.

### Success Criteria (2a)

- [x] Course 6.0, 7.0 và 6 topic tương ứng ở trạng thái `INACTIVE`. (test: KnowledgeTreeSeedTest.seedsOnlyTheInactiveCoursesAndSkillTopicsForBandsSixAndSeven)
- [ ] Học viên không thấy course 6.0, 7.0 trong danh sách course và topic sequence.
- [ ] Tạo version câu hỏi không có `difficulty` → `400 DIFFICULTY_REQUIRED`.

### Bàn giao FE (2a)

Không có.

## Phần 2b: nội dung (chờ duyệt, không giao Codex tự viết câu hỏi)

### Điều kiện bắt đầu

1. Người hiểu IELTS duyệt bảng KP và cạnh dưới đây. Sửa theo góp ý rồi mới viết SQL.
2. Phần 2a đã merge (course 6.0/7.0 và topic đã có).

### Requirements (2b)

- Migration `V3x__seed_knowledge_tree.sql` (số đánh khi merge). Idempotent, UUID theo dải `31000000-...` (P2-d).
- **KP cũ, tái dùng, không đổi mã và không đổi topic.** Band hiển thị theo course đang chứa (P2-b):

  | Course (band hiển thị) | KP cũ |
  |---|---|
  | 5.5 | `TR_R_SCANNING`, `TR_R_WORD_IN_CONTEXT`, `DR_IDEA_OR_DETAIL`, `DR_TOPIC_SENTENCE`, `DR_MATCHING_HEADINGS`, `TFNG_FALSE_VS_NOT_GIVEN`, `LS_NUM`, `LS_SPELL`, `LS_PARA`, `LS_TRAP`, `TR_L_DETAIL`, `TR_W_OPINION`, `PR_SENTENCE_COMPLETION`, `PR_MATCHING_INFORMATION`, `DEMO_READING_W1_CHART` |
  | 6.5 | `R65_EVIDENCE_INFERENCE`, `R65_PARAPHRASE`, `L65_SPEAKER_PURPOSE`, `L65_CORRECTION`, `W65_DISCUSS_BOTH_VIEWS` |

  Test ghi lại course thực tế của từng KP để xác nhận bảng này đúng với dữ liệu (không tin bảng mà không kiểm).

- **KP mới (nháp, cần duyệt).** Thuộc topic của course tương ứng do 2a tạo:

  | Skill | 6.0 | 7.0 |
  |---|---|---|
  | Reading | `R60_SKIM_STRUCTURE`, `R60_SUMMARY_COMPLETION` | `R70_WRITER_VIEW_YNNG`, `R70_HEDGING_SCOPE`, `R70_COMPLEX_PARAPHRASE` |
  | Listening | `L60_FORM_NOTE_COMPLETION`, `L60_SIGNPOSTING` | `L70_MULTI_SPEAKER_ATTITUDE`, `L70_MAP_DIAGRAM` |
  | Writing | `W60_PARAGRAPH_COHESION`, `W60_T1_KEY_FEATURES` | `W70_IDEA_DEVELOPMENT`, `W70_LEXICAL_PRECISION`, `W70_COMPLEX_STRUCTURES` |

  Mã tuân thủ `^[RLW](55|60|65|70)_[A-Z0-9_]{2,60}$` (phase 1).

- **Cạnh (nháp, theo độ khó, P2-c):**
  - Reading: `TR_R_SCANNING → PR_SENTENCE_COMPLETION → R60_SUMMARY_COMPLETION`;
    `DR_TOPIC_SENTENCE → DR_MATCHING_HEADINGS → R60_SKIM_STRUCTURE`;
    `TR_R_WORD_IN_CONTEXT → R65_PARAPHRASE → R70_COMPLEX_PARAPHRASE`;
    `TFNG_FALSE_VS_NOT_GIVEN → R65_EVIDENCE_INFERENCE → R70_WRITER_VIEW_YNNG`;
    `R65_EVIDENCE_INFERENCE → R70_HEDGING_SCOPE`.
  - Listening: `LS_NUM, LS_SPELL → L60_FORM_NOTE_COMPLETION → L65_CORRECTION`;
    `LS_PARA → LS_TRAP → L65_CORRECTION`; `TR_L_DETAIL → L60_SIGNPOSTING → L65_SPEAKER_PURPOSE →
    L70_MULTI_SPEAKER_ATTITUDE`; `L60_SIGNPOSTING → L70_MAP_DIAGRAM`.
  - Writing: `TR_W_OPINION → W60_PARAGRAPH_COHESION → W65_DISCUSS_BOTH_VIEWS → W70_IDEA_DEVELOPMENT`;
    `DEMO_READING_W1_CHART → W60_T1_KEY_FEATURES`; `W60_PARAGRAPH_COHESION → W70_COMPLEX_STRUCTURES`;
    `TR_R_WORD_IN_CONTEXT → W70_LEXICAL_PRECISION`.
  - Mọi cạnh phải thỏa DAG (kiểm bằng `KnowledgeGraph` của phase 1 trong test seed).

- **Difficulty:**
  - Gán cho mọi `question_versions` đang null, theo course chứa câu: 5.5 → `EASY`, 6.5 → `MEDIUM`, câu placement →
    `MEDIUM`. R&D chỉnh lại sau.
  - Câu không xác định được course hay placement phải được liệt kê trong báo cáo migration, không gán đại.
  - Tiêu chí: sau migration không còn `question_versions.difficulty IS NULL`.
- **Bộ câu cho bài ôn và thăm dò:** mỗi KP của course 5.5 và 6.5 cần ít nhất 3 gói `PRACTICE_SET` đã publish. Đây là
  việc soạn nội dung, viết vào migration seed riêng sau V3x. Dùng [seed-template.md](./seed-template.md). Câu hỏi phải tự
  viết (D14), mỗi file kèm tên người duyệt chuyên môn.

### Related Code Files (2b)

- Migration: `services/content-service/src/main/resources/db/migration/V3x__seed_knowledge_tree.sql`, và migration
  seed bộ câu riêng.
- Test: `KnowledgeTreeSeedTest` (phần KP và cạnh: số node, không vòng, mọi mã đúng quy ước, mọi KP cũ giữ nguyên topic)

### Implementation Steps (2b)

1. Gửi bảng KP và cạnh cho người duyệt. Sửa theo góp ý.
2. **Đỏ trước:** test seed (số node, cạnh không vòng, KP cũ giữ topic).
3. Viết migration seed KP, cạnh, backfill difficulty.
4. Nhóm nội dung soạn bộ câu theo template. Người duyệt ký.
5. Viết migration bộ câu sau khi duyệt.
6. Test đọc cây qua `GET /api/content/knowledge-tree`.
7. `mvn -q -pl services/content-service,services/learning-service -am test`.

### Success Criteria (2b)

- [x] `GET /api/content/knowledge-tree` có node ở đủ 4 band × 3 skill, không vòng. (test: KnowledgeTreeDraftSeedMigrationTest.treeCoversFourBandsAndThreeSkills, treeIsAcyclic)
- [ ] 100% `question_versions` có difficulty.
- [ ] Mỗi KP của course 5.5, 6.5 có ít nhất 3 gói `PRACTICE_SET` đã publish.
- [x] Mọi KP cũ giữ nguyên `topic_id` và `code`. (test: KnowledgeTreeDraftSeedMigrationTest.keepsEveryOlderKnowledgePointUnchanged)

### Risk Assessment

- Nội dung cây sai chuyên môn làm chẩn đoán sai hướng. Bắt buộc có người duyệt.
- Band 6.0 và 7.0 mỏng ở đợt này (chỉ KP mới). Đã chấp nhận theo P2-b; phải nói rõ khi trình hội đồng.
- Seed lớn làm migration chậm. Chỉ insert và update một lượt, không có vòng lặp PL/pgSQL nặng.

### Bàn giao FE

Không có.

## Unresolved questions

- Người duyệt chuyên môn IELTS là ai, và thời hạn duyệt bảng KP là khi nào.
- Số câu cần soạn cho bộ `PRACTICE_SET` (ước tính 180–300 câu) do nhóm nội dung nào đảm nhận.
