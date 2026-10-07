---
phase: 7
title: "Docs and verification"
status: pending
priority: P2
dependencies: [1, 2, 3, 4, 5, 6]
---

<!-- Updated: Validation Session 2 - tài liệu theo mô hình chuỗi theo course, recommended, không OPTIONAL/level -->

# Phase 7: Docs and verification

## Overview
Cập nhật tài liệu dự án theo hành vi mới và chạy kiểm chứng toàn bộ các module bị đụng.

## Requirements
- Docs:
  - `docs/contracts/lesson-learning-v1.md`:
    - `GET /topics` có `course`; trạng thái theo chuỗi theo course;
    - endpoint `GET /courses` (`recommended`, `testStatus`) và `POST /courses/{id}/test-assignments` với mã lỗi
      (`COURSE_TEST_LOCKED`, `NO_COURSE_TEST`, `COURSE_ALREADY_PASSED`, `TEST_UNAVAILABLE`);
    - ghi rõ hai API này chỉ cho role CUSTOMER.
  - `docs/contracts/learning-content-internal-v1.md` (phase 2–3) và `docs/contracts/assessment-completed-v2.md` (phase 4,
    thêm `COURSE_GATE`; learning dùng `overall_band` của PLACEMENT để gợi ý course): kiểm lại ví dụ JSON khớp code.
  - `docs/system-architecture.md`:
    - §6 "Học topic và bài": course, chuỗi theo course, gợi ý từ placement, thi cuối course;
    - §2 data ownership: `courses` ở content; `learner_placements`, `course_progress`, `course_test_assignments` ở learning.
  - `AGENTS.md` §3.8:
    - thay câu "mỗi skill là một chuỗi riêng (mỗi skill một topic `IN_PROGRESS`)" bằng "mỗi course là một chuỗi riêng
      (không tách skill); mọi course đều mở; band placement chỉ gợi ý course";
    - thêm "Thi cuối course không chặn tiến độ";
    - cập nhật dòng "Cập nhật lần cuối".
  - `CLAUDE.md` §5: thêm cạm bẫy "assessment phát `COURSE_GATE` khi learning còn bản cũ → DLQ; restart learning rồi
    replay".
  - `docs/fe-main-flow-guide.md`: nhóm topic theo course, màn danh sách course (gợi ý, thi cuối course).
- Verification:
  - `mvn -q -pl services/content-service,services/assessment-service,services/learning-service -am test`
  - `mvn -q compile -DskipTests` cả reactor
  - Smoke thủ công (nếu Docker có): chạy stack, `GET /api/learning/topics` thấy topic đầu của mỗi course cùng `IN_PROGRESS`; xong topic
    6.5 → `POST /courses/{id}/test-assignments` → làm bài qua assessment → `GET /courses` thấy `PASSED`.
  - `graphify update .`

## Related Code Files
- Modify: `docs/contracts/lesson-learning-v1.md`, `docs/system-architecture.md`, `AGENTS.md`, `CLAUDE.md`,
  `docs/fe-main-flow-guide.md`

## Implementation Steps
1. Sửa tài liệu theo code thật (đọc lại controller/DTO cuối cùng, không viết theo plan).
2. Chạy lệnh kiểm chứng; nếu Docker không có, ghi rõ test Testcontainers bị skip.
3. Sweep: grep `TopicStatus`, `package_type IN`, `"TOPIC_GATE"` để chắc không bỏ sót chỗ cần thêm course.

## Success Criteria
- [ ] Ba module test xanh; reactor compile xanh.
- [ ] Contract và architecture doc khớp code (field, mã lỗi, ví dụ).
- [ ] Không có mã plan/phase trong code, test, migration, commit.

## Risk Assessment
- Tài liệu lệch code là lỗi hay gặp nhất ở repo này: viết doc sau cùng, đối chiếu từng field.
