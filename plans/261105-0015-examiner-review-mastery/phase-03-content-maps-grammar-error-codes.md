---
phase: 3
title: "Content maps grammar error codes"
status: pending
priority: P2
dependencies: []
---

# Phase 3: Content maps grammar error codes

## Overview
Gắn các mã lỗi Writing chưa có KP vào KP ngữ pháp ACTIVE (V84) và tạo KP con bẫy `TRAP_<mã>` như V79, để lỗi examiner
đánh dấu (và correction của LLM) tác động mastery. **Gate: mapping phải được chuyên gia IELTS duyệt trước khi merge.**

## Requirements
- Mapping đề xuất (chờ duyệt; người làm **dừng và hỏi** nếu chưa có xác nhận):

  | Mã | KP | Độ khớp |
  |---|---|---|
  | `ARTICLE` | `G55_ARTICLES` | Tốt |
  | `SUBJECT_VERB_AGREEMENT` | `G55_SUBJECT_VERB_AGREEMENT` | Tốt |
  | `PLURAL_FORM` | `G55_ARTICLES` ("Articles and countability") | Trung bình |
  | `TENSE` | `G60_PERFECT_TENSES` | Yếu |
  | `SENTENCE_FRAGMENT` | `G55_COMPLEX_SENTENCES` | Yếu |
  | `NO_CLEAR_POSITION`, `OFF_TOPIC` | — | Giữ NULL |

  Chỉ đưa vào migration những dòng đã được duyệt. Người dùng chọn giữ **cả 5 dòng** để chuyên gia duyệt từng dòng (không cắt trước).
  <!-- Updated: Validation Session 1 - keep all 5 rows pending expert review -->
- Chỉ gắn khi KP đích `ACTIVE`, skill `WRITING`, topic + course `ACTIVE` (kiểm trong migration, `RAISE EXCEPTION` nếu không).
- Không sửa migration cũ. Không đổi `review_status` (V82 đã duyệt).
- Không tạo bộ luyện bẫy cho các mã này (ngoài phạm vi): review `ERROR_PATTERN` sẽ tự bỏ qua vì không có bộ ≥2 câu bẫy.

## Architecture
Migration `V115__map_grammar_error_codes_to_knowledge_points.sql` (V113/V114 đã được dùng trên main):
1. `UPDATE error_types SET knowledge_point_id = …` cho các mã đã duyệt.
2. Tạo KP con bẫy cho đúng các mã đó (copy logic V79: `TRAP_<code>`, topic của KP cha, `status = 'ACTIVE'`), set `trap_knowledge_point_id`.
3. Khối `DO $$` kiểm điều kiện ở Requirements.

Learning nhận KP con mới qua đồng bộ catalog (`knowledge_point_catalog.parent_kp_id`, `error_code`, V36). Kiểm lại
`RefreshLearningTopicsUseCase` / `JdbcLearningContentReader.topicSequence` có trả KP con bẫy của topic không (V79 đã chạy qua đường này).

## Related Code Files
- Create: `services/content-service/src/main/resources/db/migration/V115__map_grammar_error_codes_to_knowledge_points.sql`
- Create: `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/GrammarErrorCodeMappingMigrationTest.java` (theo mẫu `CourseLadderSeedTest`)

## Implementation Steps
1. Xác nhận mapping đã duyệt (ghi người duyệt + ngày vào đầu migration như comment V84). Chưa có → dừng.
2. Baseline: `mvn -q -pl services/content-service -am test`.
3. Test đỏ (Testcontainers): sau migrate, mỗi mã đã duyệt có `knowledge_point_id` = KP đích và `trap_knowledge_point_id` trỏ KP `TRAP_<mã>` ACTIVE cùng topic; mã giữ NULL không đổi; mã cũ (`LINKER_MISUSE`…) không đổi.
4. Viết migration.
5. Chạy lại module; chạy thêm `mvn -q -pl services/learning-service -am test` nếu có test đồng bộ catalog.

## Success Criteria
- [ ] Migration chỉ chứa mapping đã duyệt; test xanh.
- [ ] Learning catalog có KP con bẫy mới sau khi refresh topic (kiểm tay hoặc test integration hiện có).

## Risk Assessment
- Mapping sai → mastery KP ngữ pháp bị kéo sai. Giảm rủi ro: chỉ dòng "Tốt" nếu chuyên gia do dự.
- KP ngữ pháp chưa được bài học nào dạy → `CompletedLessonLocator.teaching` rỗng → không có review cho learner chưa học; mastery vẫn đổi. Đúng mong đợi.
