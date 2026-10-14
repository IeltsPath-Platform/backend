---
title: Replace block-based lesson with Figma lesson (slides, summary, documents, embedded Practice)
status: implemented, not committed (backend and frontend verified by tests; not yet run against a live stack)
branch: feat/lesson-resources (backend and frontend)
---

## Decisions (user, 2026-10-14)
- Lesson = title + Slide + Lesson Summary (Markdown) + Documents, in backend and frontend. Old block lesson is removed.
- Reviews take theory from `summary_markdown`; quick check and `WRONG_IN_LESSON` go away.
- Practice (`/learn/lessons/:id/practice`) is embedded in the lesson page. Completion unchanged: button, then Practice opens.
- Topic, courses, placement, Practice, topic tests unchanged. No Homework Hub, mentor, timetable.
- Lives under `/learn/lessons/:lessonId`.
- Later change (2026-10-14, plan 261014-2229-lesson-theory-only-practice-gated-unlock): the Practice tab was removed from the lesson page; Practice has its own page and a step on the trail, and a passed Practice completes the lesson.

## Design notes
- Lesson skills come from `lesson_knowledge_points` -> `knowledge_points.skill` (was: block questions and TEXT block KPs). V30 first copies those block KPs into `lesson_knowledge_points`.
- Learner history tables (`lesson_exercise_submissions`, `lesson_writing_submissions`, `lesson_progress.passed_block_ids`) are not dropped; the code stops using them. Dropping them is a separate decision.
- Old TEXT blocks become `summary_markdown` for lessons that have none; V29 demo summaries (L1, T1) stay.

## Phases
1. content-service: V30 (copy KPs, TEXT -> summary, drop block tables); remove block code, lesson question ownership, block seeds in tests; lesson content carries `summaryMarkdown`.
2. learning-service: lesson API without blocks; delete exercise/essay lesson use cases; progress without block ids; review theory from summary; tests.
3. frontend: new lesson page in `/learn` (tabs Slide, Summary, Tài liệu, Luyện tập); drop block components and the mock lesson blocks; review theory renders Markdown.
4. READMEs; full test run both services; typecheck, lint, build.

## Acceptance
- No `lesson_blocks` table or block code in either service.
- Both services' tests pass (except the known `AnswerSpecGraderTest` missing shared vectors file).
- Frontend typecheck, lint and build pass; `/learn/lessons/:id` shows the four tabs under the "Khóa học" navbar.
