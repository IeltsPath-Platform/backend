---
phase: 3
title: "Content seed courses"
status: pending
priority: P1
dependencies: [2]
---

# Phase 3: Content seed courses

## Overview
Backfill seed hiện có vào course 5.5, thêm course 6.5 với một topic Reading demo, và mỗi course một `COURSE_TEST`, để
demo được "hai học viên có band khác nhau học khác nhau".

## Requirements
- Functional (migration `V21__seed_courses.sql`, UUID cố định theo quy ước seed hiện có):
  - Course `IELTS_5_5` ("IELTS 5.5", band 5.5) và `IELTS_6_5` ("IELTS 6.5", band 6.5).
  - Mọi topic seed hiện có trong lộ trình (`DEMO_READING`, `TFNG_SKILLS`, premium Reading, Listening, `DEMO_WRITING`…)
    gán `IELTS_5_5`. Dùng `UPDATE topics SET course_id = … WHERE skill IS NOT NULL` để không bỏ sót topic nào.
  - Topic mới `READING_6_5_INFERENCE` (skill READING, course 6.5, `sort_order` sau các topic Reading cũ): 2 KP, 1 lesson
    PUBLISHED (TEXT lý thuyết có map `lesson_block_knowledge_points` + 1 EXERCISE), 1 PRACTICE_SET gắn lesson cho mỗi KP
    (≥ 3 câu, `hasPracticeSet=true`), 1 `TOPIC_TEST` PUBLISHED. Passage và câu hỏi khó hơn bậc 5.5 (suy luận, paraphrase).
  - `COURSE_TEST` cho mỗi course: mỗi package một section gồm 6–10 câu **Reading** tự chấm (không cần media), câu
    **mới**, purpose `LEARNING`, không trùng câu của package khác.
- Non-functional: theo đúng pattern của V9/V18 (version PUBLISHED, `current_published_version_id`, `answer_spec` đúng
  `answer-spec-v1`).
<!-- Updated: Validation Session 1 - COURSE_TEST chỉ Reading; content không có validator answer-spec nên dùng test cấu trúc -->

> **Lưu ý:** dù course 6.5 chỉ có 1 topic Reading, course đó vẫn mở ngay cho mọi học viên (D4). Chuỗi Reading 6.5 độc
> lập với Reading 5.5.

## Related Code Files
- Create: `services/content-service/src/main/resources/db/migration/V21__seed_courses.sql`
- Tests: `services/content-service/src/test/java/com/group01/content/infrastructure/persistence/CourseSeedTest.java`
  (Testcontainers)
- Docs: `services/content-service/README.md` (bảng seed), `docs/contracts/learning-content-internal-v1.md` (ví dụ JSON
  có `course`)

## Implementation Steps
1. **Test đỏ** `CourseSeedTest`:
   - mọi topic có skill đều có course;
   - `topicSequence` trả topic Reading 6.5 sau mọi topic Reading 5.5;
   - mỗi KP của topic 6.5 có `hasPracticeSet=true`;
   - mỗi course có `hasCourseTest=true`;
   - câu trong `COURSE_TEST` đều là skill READING và không có `answer_spec.type = "ESSAY"`;
   - mọi câu mới có `answer_spec` với `type` khớp loại câu và đủ khóa bắt buộc theo `docs/contracts/answer-spec-v1.md`
     (kiểm cấu trúc bằng SQL/JSON; content không có validator). Phần chấm thật được kiểm ở smoke test phase 7.
2. Viết V21: course → backfill → topic 6.5 (KP, lesson, block, practice set, topic test) → 2 course test.
3. Chạy test seed + toàn bộ test content (seed test cũ `LessonPipelineSeedTest`, `DemoReadingPassageSeedTest` phải xanh).

## Success Criteria
- [ ] `CourseSeedTest` xanh; test seed cũ xanh.
- [ ] Không câu nào xuất hiện ở hai package (rule `questionsUsedElsewhere` không vi phạm).

## Risk Assessment
- Viết nội dung là việc tốn nhất của plan: giới hạn ở 1 topic 6.5, 2 KP, 1 lesson. Nhóm BA/author có thể thay nội
  dung thật sau bằng migration mới.
- DB local đã chạy V1–V18: V21 chỉ thêm và UPDATE `course_id`, không sửa migration cũ.
