---
phase: 2
title: "Content seed multi-skill topic"
status: completed
priority: P2
dependencies: [1]
---

# Phase 2: Content seed multi-skill topic

## Overview
Seed một topic nhiều skill để demo và làm dữ liệu cho test integration của learning/assessment.

## Requirements
- Migration `V23__seed_multi_skill_topic.sql`, UUID cố định, theo pattern V9/V12/V18/V21:
  - Topic `TREES_MULTI_SKILL` ("Cây cối"), course `IELTS_5_5`, `skill` NULL, `sort_order` sau các topic 5.5 hiện có.
  - KP: 2 Reading, 1 Listening, 1 Writing.
  - Lesson T1, T2 PUBLISHED, mỗi lesson:
    - TEXT lý thuyết theo từng skill (map `lesson_block_knowledge_points` đúng KP);
    - ASSET passage + EXERCISE Reading;
    - ASSET audio (media key, cần `CONTENT_MEDIA_BASE_URL`) + EXERCISE Listening;
    - EXERCISE essay Writing (`answer_spec.type=ESSAY`, `passBand`).
  - Practice cho T1:
    - bộ R (≥ 3 câu), bộ L (≥ 3 câu), bộ W (1 essay có `passBand`), bộ R+L (≥ 3 câu mỗi skill);
    - mọi câu mới, không dùng lại câu của package khác.
  - `TOPIC_TEST` 1 package: section R, section L, section W (1 essay có `passBand`).
- Audio: dùng lại một file mp3 đã có trong bảng media của content README nếu hợp nghĩa, để khỏi upload file mới; nếu
  không hợp thì ghi key mới vào bảng README.

## Related Code Files
- Create: `services/content-service/src/main/resources/db/migration/V23__seed_multi_skill_topic.sql`,
  `services/content-service/src/test/java/com/group01/content/infrastructure/persistence/MultiSkillSeedTest.java`
- Docs: `services/content-service/README.md` (bảng seed + media)

## Implementation Steps
1. **Test đỏ** `MultiSkillSeedTest`:
   - sequence có `TREES_MULTI_SKILL` với `skills=[LISTENING, READING, WRITING]`;
   - T1 `skills` đủ 3 skill; mỗi TEXT block có KP mapping;
   - `?skill=WRITING` trả đúng bộ W;
   - mọi essay có `passBand`;
   - không câu nào nằm ở hai package.
2. Viết V23.
3. Chạy test content toàn bộ.

## Success Criteria
- [x] `MultiSkillSeedTest` và test seed cũ xanh.

## Risk Assessment
- Soạn nội dung là phần tốn công nhất; giữ ở mức tối thiểu đủ cho acceptance criteria của plan.
