---
phase: 8
title: "Practice test theo mốc course"
status: completed
priority: P2
dependencies: []
---

# Phase 8: Practice test theo mốc course

## Overview

Board: "Đề luyện bắt buộc theo mốc Lesson Plan", chiếm 25% trong Process Score. PR #70 đã thêm package
`PRACTICE_TEST` (thư viện đề luyện), nhưng `AttemptType.forContentPackageType` chưa nhận loại này nên chưa tạo được
attempt. Phase này:
- cho phép làm practice test;
- gắn practice test vào mốc trong course;
- learning giao đề theo mốc và lưu điểm lần thi đầu.

Practice test làm tự do vẫn được làm nhưng không tính điểm (D5).

## Requirements

- **Assessment:** `AttemptType.forContentPackageType("PRACTICE_TEST")` → `OFFICIAL_PRACTICE`. Không cần migration vì
  CHECK đã có `OFFICIAL_PRACTICE`. Parser của learning đã nhận `OFFICIAL_PRACTICE`.
- **Content, migration `V38__practice_test_milestones.sql`:**

  ```sql
  CREATE TABLE course_practice_test_milestones (
      course_id UUID NOT NULL REFERENCES courses(id),
      package_id UUID NOT NULL UNIQUE REFERENCES content_packages(id),
      after_topic_sequence INTEGER NOT NULL CHECK (after_topic_sequence > 0),
      PRIMARY KEY (course_id, package_id)
  );
  ```

  - Package phải là `PRACTICE_TEST` và đã `PUBLISHED` (kiểm ở use case).
  - API `ADMIN` / `CONTENT_AUTHOR`: `GET` / `PUT /api/content/admin/courses/{id}/practice-test-milestones`.
  - API nội bộ: `GET /internal/learning-content/courses/{id}/practice-test-milestones`, trả package, version đang
    publish, mốc.
  - Seed: mỗi course 5.5 và 6.5 có ít nhất 1 mốc, sau topic giữa lộ trình.
- **Learning, migration `V12__practice_test_assignments.sql`:**

  ```sql
  CREATE TABLE practice_test_assignments (
      id UUID PRIMARY KEY,
      user_id UUID NOT NULL,
      course_id UUID NOT NULL,
      package_id UUID NOT NULL,
      package_version_id UUID NOT NULL,
      assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      consumed_attempt_id UUID,
      consumed_at TIMESTAMPTZ,
      percent DOUBLE PRECISION,
      UNIQUE (user_id, package_id)
  );
  ```

  - `GET /api/learning/courses/{id}/practice-tests`: danh sách mốc, trạng thái `LOCKED` / `AVAILABLE` / `DONE`, % lần
    đầu. Mốc mở khi topic ở vị trí `after_topic_sequence` đã thi topic test (theo luật phase 9; trước phase 9 thì là đã
    `PASSED`).
  - `POST /api/learning/courses/{id}/practice-tests/{packageId}/assignments`: giao đề, trả `packageVersionId` để FE tạo
    attempt bên assessment như topic test.
  - `ApplyAssessmentResultUseCase` với `OFFICIAL_PRACTICE`:
    - attempt khớp assignment đang mở → ghi `percent` của lần đầu (giống `TopicTestAssignment.consume`);
    - không khớp → practice test tự do, chỉ ghi evidence/event, bài ôn sinh ra là bài ôn không chặn (phase 6).
- Lần thi lại cùng mốc vẫn làm được nhưng không đổi `percent` (chỉ tính lần đầu, đúng tinh thần đề bắt buộc).

## Related Code Files

- assessment: `domain/vo/AttemptType.java`, `AttemptTypeTest`
- content: migration V38, use case, controller admin, `InternalPracticeTestMilestoneController`, `JdbcLearningContentReader`
- learning: migration V12, `domain/aggregate/PracticeTestAssignment.java`, use case giao đề và liệt kê,
  `ApplyAssessmentResultUseCase`, `LearningContentClient` thêm hàm đọc mốc
- Test:
  - `AttemptTypeTest` (`PRACTICE_TEST` → `OFFICIAL_PRACTICE`);
  - `PracticeTestMilestoneIntegrationTest` (content);
  - `PracticeTestAssignmentIntegrationTest` (learning): mốc khóa / mở, lần đầu được ghi, lần sau không đổi, làm tự
    do không ghi.

## Implementation Steps

1. **Đỏ trước:** `AttemptTypeTest`, test giao đề và ghi điểm.
2. Assessment map loại attempt.
3. Content: bảng mốc, API, seed.
4. Learning: bảng giao đề, API, xử lý kết quả.
5. Contract: `learning-content-internal-v1.md`, `lesson-learning-v1.md`.
6. `mvn -q -pl services/assessment-service,services/content-service,services/learning-service -am test`.

## Success Criteria

- [x] Học viên làm được practice test thuộc mốc. Điểm lần đầu được lưu cho phase 9.
- [x] Practice test tự do không ảnh hưởng điểm và không khóa lesson.

Kiểm chứng 2026-10-16: `mvn -q -B -ntp -pl services/assessment-service,services/content-service,services/learning-service -am test`
thoát 0. Content 377 test; learning 305 test; assessment 251 test; không failure/error/skip. `git diff --check` qua.
Chi tiết: [báo cáo triển khai](../reports/pm-261016-2250-practice-test-milestones.md).

## Risk Assessment

- Mốc dùng vị trí topic **trong course**, không dùng trực tiếp `sequence_order` toàn curriculum.
- Một package chỉ thuộc mốc của một course vì assignment có khóa duy nhất `(user_id, package_id)`.
- Điểm đóng băng theo kết quả hoàn thành khớp đầu tiên được consumer áp dụng, giống topic assignment;
  replay/regrade/retake không đổi điểm đó. Regrade của lần làm tự do đã áp dụng không được ăn assignment tạo sau.
- Chưa có review tiers: `OFFICIAL_PRACTICE` giữ evidence/event nhưng không gọi bộ sinh review chặn.
  Sinh review không chặn sẽ nối ở phase 6. Không đổi review hoặc hành vi topic test hiện có.
- Seed V38 là hai checkpoint Reading/Writing ngắn, nội dung tự viết, không phải đề IELTS đầy đủ.
- Người dùng cho phép bổ sung `OFFICIAL_PRACTICE` vào `EnqueueGateEssayGradingService` và test tương ứng.
- Người dùng cho phép sửa thêm `PackageType`, `JdbcGateEssayJobStore`, `PublishContentPackageUseCase` và test:
  đọc được đề practice, worker lấy job `OFFICIAL_PRACTICE`, publisher kiểm ownership/purpose/essay.

- Practice test là đề dài đủ 4 phần, essay chấm LLM qua luồng gate essay hiện có. Kiểm tra luồng chấm essay có nhận
  attempt `OFFICIAL_PRACTICE` không; nếu chưa thì mở thêm, và LLM vẫn là bên chấm vì đây không phải quyết định đạt/trượt.

## Bàn giao FE

- Danh sách đề luyện theo mốc trong trang course.
- Thư viện practice test tự do ghi rõ "không tính điểm".
