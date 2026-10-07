---
phase: 7
title: "Docs and verification"
status: pending
priority: P2
dependencies: [1, 2, 3, 4, 5, 6]
---

# Phase 7: Docs and verification

## Overview
Cập nhật tài liệu theo hành vi lesson nhiều skill và chạy kiểm chứng ba service.

## Requirements
- Docs:
  - `docs/contracts/learning-content-internal-v1.md`: `skills` ở topic, lesson, Practice set; `?skill=`; luật publish
    mới.
  - `docs/contracts/lesson-learning-v1.md`:
    - `skills`, `?skill=`, `skillScores`;
    - endpoint essay Practice và mã lỗi (`ESSAY_NOT_GRADED`, `NOT_ESSAY_ITEM`, 402/429/503);
    - luật khóa theo skill của lesson.
  - `docs/contracts/assessment-completed-v2.md`: gate có essay được LLM chấm; event vẫn cùng shape.
  - `docs/system-architecture.md`: §6 luồng lesson nhiều skill, Practice theo skill, chấm gate bằng LLM + fallback
    EXAMINER; §3 assessment gọi LLM (ra ngoài).
  - `AGENTS.md`:
    - §3.8 thay "Lesson thuộc một skill" bằng luật skill suy từ câu hỏi, khóa theo skill của lesson, Practice đạt theo
      từng skill;
    - §2 ghi assessment có client LLM;
    - §3.6 thêm biến `ASSESSMENT_LLM_*`;
    - cập nhật ngày.
  - `CLAUDE.md` §3 biến môi trường assessment LLM; §5 cạm bẫy "thiếu `ASSESSMENT_LLM_*` thì mọi thi topic có essay
    chuyển EXAMINER".
  - `docs/fe-main-flow-guide.md`: lesson nhiều skill, chọn bộ Practice theo skill, nộp essay trong Practice.
- Verification:
  - `mvn -q -pl services/content-service,services/learning-service,services/assessment-service -am test`
  - `mvn -q compile -DskipTests`
  - smoke (nếu Docker có): học T1 → Practice R+L + W → thi topic trộn → event → topic PASS.
  - `graphify update .`

## Related Code Files
- Modify: các file docs ở trên, `AGENTS.md`, `CLAUDE.md`.

## Implementation Steps
1. Viết tài liệu theo code cuối (đọc lại controller, DTO).
2. Chạy kiểm chứng; Docker không có thì ghi rõ test bị skip.
3. Grep `\.skill()` trong `application`/`domain` của learning để chắc không còn quyết định dựa trên một skill.

## Success Criteria
- [ ] Ba module xanh; reactor compile xanh.
- [ ] Tài liệu khớp code; không có mã plan/phase trong code, test, migration, commit.

## Risk Assessment
- AGENTS §3.8 là quy tắc chính cho agent: viết sai thì agent sau làm sai. Đối chiếu từng câu với code.
