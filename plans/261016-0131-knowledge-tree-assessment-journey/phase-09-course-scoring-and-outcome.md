---
phase: 9
title: "Điểm có trọng số và kết quả khóa"
status: pending
priority: P1
dependencies: [4, 8]
---

# Phase 9: Điểm có trọng số và kết quả khóa

## Overview

Board: Process 20% + Giữa khóa 30% + Cuối khóa 50%; giữa khóa dưới 60% thì bổ trợ nhưng không khóa thi cuối khóa;
cuối khóa từ 70% là đạt đầu ra, chưa đạt thì bổ trợ theo nhóm lỗi, kiểm tra lại rồi xét lại. Mentor nhấn mạnh trọng
số là "Config Admin". Hiện tại có hai vấn đề:
- topic test đạt 70% mới mở topic sau (`TopicStatusDeriver`);
- course test đòi mọi topic `PASSED` (`AssignCourseTestUseCase.java:51`).

Nghĩa là điểm giữa khóa đang khóa thi cuối khóa, ngược với board. Phase này áp D2, D3, D4.

## Requirements

- **Migration `V16__course_scoring.sql`:**

  ```sql
  ALTER TABLE topic_progress ADD COLUMN tested_at TIMESTAMPTZ;
  ALTER TABLE topic_progress ADD COLUMN first_test_percent DOUBLE PRECISION;

  CREATE TABLE course_score_configs (
      id UUID PRIMARY KEY,
      course_id UUID UNIQUE,                       -- NULL = cấu hình mặc định
      process_weight NUMERIC(4,3) NOT NULL, mid_weight NUMERIC(4,3) NOT NULL, final_weight NUMERIC(4,3) NOT NULL,
      practice_share NUMERIC(4,3) NOT NULL, practice_test_share NUMERIC(4,3) NOT NULL,
      pass_threshold NUMERIC(4,3) NOT NULL, mid_remediation_below NUMERIC(4,3) NOT NULL,
      updated_by UUID, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      CHECK (process_weight + mid_weight + final_weight = 1),
      CHECK (practice_share + practice_test_share = 1)
  );
  -- seed mặc định: 0.2 / 0.3 / 0.5, practice 0.64 / practice test 0.36 (giữ tỷ lệ 45:25 của board khi bỏ BTVN),
  --                 pass 0.70, bổ trợ giữa khóa dưới 0.60

  CREATE TABLE remediation_flags (
      id UUID PRIMARY KEY, user_id UUID NOT NULL, course_id UUID NOT NULL, topic_id UUID,
      kind VARCHAR(12) NOT NULL CHECK (kind IN ('MID_COURSE','POST_COURSE')),
      source_attempt_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      resolved_at TIMESTAMPTZ, UNIQUE (source_attempt_id, kind)
  );

  CREATE TABLE course_outcomes (
      user_id UUID NOT NULL, course_id UUID NOT NULL,
      status VARCHAR(14) NOT NULL CHECK (status IN ('PASSED','REMEDIATION','RETEST_READY')),
      total NUMERIC(5,4) NOT NULL, components JSONB NOT NULL, final_attempt_id UUID NOT NULL,
      top_error_codes VARCHAR(60)[] NOT NULL DEFAULT '{}', decided_at TIMESTAMPTZ NOT NULL,
      PRIMARY KEY (user_id, course_id)
  );
  ```

- **Topic (D2):**
  - `applyTopicGate` ghi `tested_at` và `first_test_percent` ở lần thi đầu, bất kể điểm. Đạt 70% vẫn ghi `passed_at`
    như cũ để hiển thị.
  - `TopicStatusDeriver`: topic có `tested_at` thì coi là xong và mở topic tiếp theo. Thêm trạng thái `COMPLETED` (đã
    thi, chưa đạt 70%) bên cạnh `PASSED`.
  - Lần thi đầu dưới `mid_remediation_below` → tạo `remediation_flags` loại `MID_COURSE`. Cờ tự `resolved` khi các bài
    ôn sinh từ attempt đó không còn `PENDING`.
- **Course test (D3):** `AssignCourseTestUseCase` đổi điều kiện `COURSE_TEST_LOCKED` thành "mọi topic của course đã có
  `tested_at`". Bỏ chặn `COURSE_ALREADY_PASSED` khi trạng thái là `RETEST_READY`.
- **Điểm (D4)**, domain service `CourseScoreCalculator`, thuần:
  - `practice` = trung bình % lần nộp đầu của các Practice attempt thuộc lesson trong course (`counted_as_evidence`);
  - `practiceTest` = trung bình % lần đầu của các mốc practice test (phase 8);
  - `process = practice_share × practice + practice_test_share × practiceTest`;
  - `mid` = trung bình `first_test_percent` của các topic có topic test;
  - `final` = % của lần course test mới nhất;
  - học viên chưa làm thành phần nào → thành phần đó = 0;
  - course không có loại bài nào (ví dụ không có mốc practice test) → bỏ thành phần đó và chia lại trọng số cho các
    phần còn lại, ở cả tầng con lẫn tầng tổng;
  - `total = Σ weight × component`.
- **Kết quả khóa** (trong `applyCourseGate`, thay luật "course test ≥ 70%"):
  - `total ≥ pass_threshold` → `course_outcomes.status = PASSED`, `CourseProgress.pass(...)`;
  - ngược lại → `REMEDIATION`:
    - tạo `remediation_flags` loại `POST_COURSE`;
    - `top_error_codes` lấy từ event của attempt cuối khóa, gom theo mã lỗi ("bổ trợ theo nhóm lỗi");
    - bài ôn chặn của các KP sai đã tự sinh như hiện nay. Thứ tự bài ôn ưu tiên theo nhóm lỗi nhiều nhất.
  - Bài ôn từ attempt đó hết `PENDING` → `RETEST_READY`. `AssignCourseTestUseCase` cho thi lại, LRU chọn mã đề khác.
    Thi lại → tính lại `total`.
- **API:**
  - `GET /api/learning/courses/{id}/score` (`CUSTOMER`): các thành phần, trọng số, tổng, ngưỡng, trạng thái kết quả,
    nhóm lỗi.
  - `GET` / `PUT /api/learning/admin/score-config?courseId=` (`ADMIN`): sửa cấu hình. Validate tổng bằng 1, ngưỡng trong
    (0, 1]. Ghi `updated_by`.
  - `GET /api/learning/admin/learners/{userId}/courses/{courseId}/score` (`ADMIN`, `EXAMINER`).
- Đổi cấu hình không tính lại kết quả đã chốt (`PASSED` giữ nguyên). Các kết quả chưa chốt tính theo cấu hình mới.

## Related Code Files

(`L` = learning package)

- Sửa: `L/application/usecase/ApplyAssessmentResultUseCase.java` (`applyTopicGate`, `applyCourseGate`),
  `L/domain/service/TopicStatusDeriver.java`, `L/domain/vo/TopicStatus.java`, `L/domain/aggregate/LearnerCurriculum.java`,
  `L/application/usecase/AssignCourseTestUseCase.java`, `L/infrastructure/persistence/JdbcLearnerCurriculumRepository.java`,
  `L/domain/aggregate/CourseProgress.java`
- Tạo: `L/domain/service/CourseScoreCalculator.java`, `L/domain/aggregate/{CourseScoreConfig, CourseOutcome,
  RemediationFlag}.java`, repository, use case, `L/api/controller/CourseScoreController.java`,
  `ScoreConfigAdminController.java`
- Test:
  - `CourseScoreCalculatorTest`: đủ thành phần; thiếu thành phần của học viên = 0; course không có practice test → chia
    lại trọng số; biên 0.6999 / 0.70;
  - `TopicStatusDeriverTest`: `COMPLETED` mở topic sau;
  - `CourseOutcomeIntegrationTest`: thi topic 50% → topic sau mở và có cờ `MID_COURSE`; thi hết topic → course test mở;
    tổng 65% → `REMEDIATION` → làm xong bài ôn → `RETEST_READY` → thi lại 80% → `PASSED`;
  - `ScoreConfigAdminControllerTest`: chỉ ADMIN; tổng khác 1 → 400.

## Implementation Steps

1. **Đỏ trước:** `CourseScoreCalculatorTest`, `TopicStatusDeriverTest`.
2. Migration và seed cấu hình mặc định.
3. Đổi luật topic và course test. Sửa các test cũ đang kỳ vọng "topic sau chỉ mở khi đạt 70%" (`CoursePathIntegrationTest`
   và test liên quan) theo luật mới.
4. Tính điểm, kết quả khóa, cờ bổ trợ.
5. API học viên và admin. Contract `docs/contracts/course-score-v1.md`.
6. `mvn -q -pl services/learning-service -am test`.

## Success Criteria

- [ ] Kịch bản trong `CourseOutcomeIntegrationTest` chạy đúng.
- [ ] Admin đổi trọng số → điểm tổng của kết quả chưa chốt thay đổi theo.
- [ ] Không học viên nào bị khóa thi cuối khóa vì điểm giữa khóa.

## Risk Assessment

- Đây là thay đổi hành vi lớn (bỏ cổng 70% của topic). Cần cập nhật SRS, tài liệu luồng và FE.
- Essay của course test giờ chờ giám khảo (phase 10), nên kết quả khóa có thể chậm vài ngày. FE hiện trạng thái "Đang
  chờ chấm".

## Bàn giao FE

- Trang điểm khóa: 3 thành phần, tổng, ngưỡng, nhóm lỗi, nút thi lại.
- Topic hiện `COMPLETED` hoặc `PASSED`.
- Màn cấu hình điểm cho Admin.
