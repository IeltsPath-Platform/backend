---
phase: 2
title: "Seed cây band 5.5 → 7 và difficulty"
status: pending
priority: P1
dependencies: [1]
---

# Phase 2: Seed cây band 5.5 → 7 và difficulty

## Overview

Mentor yêu cầu "dựng một cái cây từ band 5.5 cho đến band 7". Seed hiện có khoảng 20 KP, mã đặt không thống nhất
(`R65_PARAPHRASE`, `LS_NUM`, `DR_TOPIC_SENTENCE`, `DEMO_READING_W2_OPINION`…), chưa có cạnh, và chỉ đúng 1 câu có
`difficulty`. Phase này seed cây Reading, Listening, Writing qua 4 band, nối với KP cũ, và gắn difficulty cho mọi câu.

## Requirements

- Migration `V30__seed_knowledge_tree.sql`. Idempotent: dùng `ON CONFLICT DO NOTHING`, UUID cố định theo dải
  `29000000-...` để không đụng seed cũ.
- **Course:** thêm `IELTS_6_0` (6.0) và `IELTS_7_0` (7.0) với `status = 'INACTIVE'` (D13). Mỗi course có topic theo
  skill để chứa KP. Topic của course `INACTIVE` cũng để `INACTIVE`.
- **Tái dùng KP cũ**, không đổi mã. Map vào cây như sau:
  - 5.5: `TR_R_SCANNING`, `TR_R_WORD_IN_CONTEXT`, `DR_IDEA_OR_DETAIL`, `DR_TOPIC_SENTENCE`, `LS_NUM`, `LS_SPELL`,
    `TR_L_DETAIL`, `TR_W_OPINION`, `DEMO_READING_W1_CHART`.
  - 6.0: `DR_MATCHING_HEADINGS`, `TFNG_FALSE_VS_NOT_GIVEN`, `LS_PARA`, `LS_TRAP`, `PR_SENTENCE_COMPLETION`.
  - 6.5: `R65_EVIDENCE_INFERENCE`, `R65_PARAPHRASE`, `PR_MATCHING_INFORMATION`, `L65_SPEAKER_PURPOSE`,
    `L65_CORRECTION`, `W65_DISCUSS_BOTH_VIEWS`.

  KP cũ thuộc topic của course 5.5 hoặc 6.5. Band hiển thị lấy theo course nên không đổi `topic_id` của KP cũ. Mức band
  trong danh sách trên chỉ dùng để đặt cạnh.
- **KP mới (nháp, cần người hiểu IELTS duyệt trước khi merge)** theo quy ước mã của phase 1:

  | Skill | 6.0 | 7.0 |
  |---|---|---|
  | Reading | `R60_SKIM_STRUCTURE`, `R60_SUMMARY_COMPLETION` | `R70_WRITER_VIEW_YNNG`, `R70_HEDGING_SCOPE`, `R70_COMPLEX_PARAPHRASE` |
  | Listening | `L60_FORM_NOTE_COMPLETION`, `L60_SIGNPOSTING` | `L70_MULTI_SPEAKER_ATTITUDE`, `L70_MAP_DIAGRAM` |
  | Writing | `W60_PARAGRAPH_COHESION`, `W60_T1_KEY_FEATURES` | `W70_IDEA_DEVELOPMENT`, `W70_LEXICAL_PRECISION`, `W70_COMPLEX_STRUCTURES` |

- **Cạnh (nháp):**
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
- **Difficulty:**
  - Gán cho mọi `question_versions` chưa có giá trị, theo band của course chứa câu: 5.5 → `EASY`, 6.0–6.5 →
    `MEDIUM`, 7.0 → `HARD`. Câu placement → `MEDIUM`. R&D chỉnh lại sau.
  - Tạo version câu hỏi mới mà để `difficulty` null → `400 DIFFICULTY_REQUIRED`
    (`AddQuestionVersionRequest`, `QuestionVersion`).
- **Bộ câu cho bài ôn và thăm dò:** mỗi KP của course 5.5 và 6.5 cần ít nhất 3 gói `PRACTICE_SET` đã publish. Đây là
  việc soạn nội dung, viết vào một migration seed riêng sau V30 (đánh số theo thứ tự merge). Có thể giao nhóm nội dung
  theo [seed-template.md](./seed-template.md). Câu hỏi phải tự viết (D14).

## Related Code Files

- Migration: `services/content-service/src/main/resources/db/migration/V30__seed_knowledge_tree.sql`
- Sửa: `.../api/dto/request/AddQuestionVersionRequest.java`, `.../domain/entity/QuestionVersion.java`
  (`...` = content package)
- Kiểm tra learning: `RefreshLearningTopicsUseCase` và `JdbcLearningContentReader` (topic sequence) phải bỏ qua course
  `INACTIVE` và topic `INACTIVE`. Nếu chưa lọc, thêm điều kiện và test.
- Test: `KnowledgeTreeSeedTest` (Testcontainers: số node, số cạnh, không vòng, mọi mã mới đúng quy ước),
  `CourseSeedTest` (course 6.0, 7.0 là `INACTIVE` và không có trong danh sách course của học viên),
  `QuestionControllerTest` (thiếu difficulty → 400)

## Implementation Steps

1. Gửi bảng KP và cạnh ở trên cho người hiểu IELTS duyệt. Sửa theo góp ý rồi mới viết SQL.
2. **Đỏ trước:** `KnowledgeTreeSeedTest`, `CourseSeedTest`.
3. Viết V30: course, topic, KP mới, cạnh, cập nhật difficulty.
4. Chặn version câu hỏi mới thiếu difficulty, kèm test.
5. Kiểm tra learning bỏ qua course `INACTIVE` (test `CoursePathIntegrationTest` thêm kịch bản).
6. Soạn bộ câu bổ sung (có thể làm song song bởi nhóm nội dung).
7. `mvn -q -pl services/content-service,services/learning-service -am test`.

## Success Criteria

- [ ] `GET /api/content/knowledge-tree` trả đủ 4 band × 3 skill, không vòng.
- [ ] Học viên không thấy course 6.0, 7.0.
- [ ] 100% `question_versions` có difficulty.
- [ ] Mỗi KP của course 5.5, 6.5 có ít nhất 3 gói `PRACTICE_SET`.

## Risk Assessment

- Nội dung cây sai chuyên môn làm chẩn đoán sai hướng. Bắt buộc có người duyệt (bước 1).
- Seed lớn làm migration chậm. Chỉ insert, không có vòng lặp PL/pgSQL nặng.

## Bàn giao FE

Không có.
