---
phase: 10
title: "Hàng chờ giám khảo và chính sách AI"
status: completed
priority: P1
dependencies: [3]
---

# Phase 10: Hàng chờ giám khảo và chính sách AI

## Overview

Chính sách AI trên board: "không tự quyết pass/fail". Hiện tại band LLM của essay quyết định kết quả gate
(`GateResultAssembler.java:60`). Ngoài ra, assessment có bảng `grading_jobs` nhưng **không có API liệt kê việc chờ
chấm**, nên EXAMINER không biết có bài nào đang chờ. Phase này:
- cho EXAMINER một hàng chờ dùng được;
- chuyển essay course test sang giám khảo chấm mù, LLM chấm song song (D6);
- lưu so sánh để làm gold set ở phase 11;
- viết tài liệu chính sách AI.

## Requirements

- **Migration assessment `V9__examiner_queue.sql`:**

  ```sql
  ALTER TABLE grading_jobs ADD COLUMN assigned_examiner_id UUID;
  ALTER TABLE grading_jobs ADD COLUMN claimed_at TIMESTAMPTZ;
  ALTER TABLE grading_jobs ADD COLUMN reason VARCHAR(30) CHECK (reason IN
      ('LLM_FAILED','LLM_QUOTA','LLM_NOT_CONFIGURED','COURSE_GATE_POLICY','HUMAN_CREDIT','QA_SAMPLE'));
  CREATE TABLE essay_grade_comparisons (
      submission_id UUID PRIMARY KEY REFERENCES learner_submissions(id),
      attempt_type VARCHAR(40) NOT NULL,
      task VARCHAR(10) NOT NULL,
      llm_band NUMERIC(2,1), llm_criteria JSONB, llm_prompt_version VARCHAR(40),
      examiner_band NUMERIC(2,1), examiner_criteria JSONB, examiner_error_codes VARCHAR(60)[],
      examiner_id UUID, examiner_graded_at TIMESTAMPTZ
  );
  ```

  Giá trị `QA_SAMPLE` của `reason` dùng ở phase 12.
- **Hàng chờ** (`EXAMINER`, `ADMIN`):
  - `GET /api/assessments/grading-jobs?mode=HUMAN&status=QUEUED&page=&size=`: loại attempt, kỹ năng, task, thời gian
    chờ, lý do, người đang nhận. Không trả band LLM (chấm mù).
  - `POST /api/assessments/grading-jobs/{id}/claim`: nhận việc. Việc đã có người nhận → `409 JOB_ALREADY_CLAIMED`.
    Nhận quá 24 giờ chưa xong thì người khác nhận lại được.
  - `GET /api/assessments/grading-jobs/{id}/essay`: đề, essay, task, rubric đang áp dụng (phase 11). Không kèm điểm LLM.
  - Chấm và chốt dùng luồng có sẵn: `GradingController` `/results`, `/details`, `/finalize`.
- **Course test luôn có người chấm (D6):**
  - Khi tạo job essay cho attempt `COURSE_GATE`: vẫn `enqueueAi` (để có so sánh và nhận xét tạm), đồng thời
    `enqueueHuman` với `reason = COURSE_GATE_POLICY`.
  - `GateResultAssembler.completeIfGraded` bỏ qua attempt `COURSE_GATE`. Kết quả chỉ tạo khi giám khảo `finalize`.
    Event `AssessmentCompleted.v2` phát như luồng chấm người hiện nay.
  - Học viên đọc được nhận xét tạm qua `GET /api/assessments/attempts/{id}/provisional-feedback`: chỉ có nhận xét chữ,
    không có band, ghi rõ "chờ giám khảo".
- **So sánh:**
  - Job AI hoàn thành → ghi phần `llm_*` vào `essay_grade_comparisons`.
  - Giám khảo `finalize` một essay → ghi phần `examiner_*`.
  - Áp dụng cho mọi essay có cả hai điểm: course test, và topic test khi LLM lỗi xong được chấm lại (hiếm).
- **Mã lỗi:** khi giám khảo lưu chi tiết, `error_type` phải có trong danh mục (phase 3).
- **Tài liệu `docs/ai-policy.md`:**
  - AI được làm gì: chấm tạm, nhận xét theo rubric, gợi ý bài từ kho đã duyệt, gắn mã lỗi trong danh mục, phát hiện cờ.
  - AI không được làm gì: quyết định đạt/trượt khóa; sinh kiến thức ngoài syllabus; đưa câu hỏi tự sinh tới học viên khi
    chưa duyệt; ghi essay hay prompt vào log.
  - Mỗi quy tắc trỏ tới điểm kiểm tra trong code và test tương ứng.

## Related Code Files

(`A` = assessment package)

- Sửa: `A/application/usecase/GateResultAssembler.java`, `A/application/usecase/GradeGateEssayJobUseCase.java`, nơi
  gọi `enqueueAi` cho gate attempt, `A/application/port/GateEssayJobStore.java`,
  `A/infrastructure/persistence/adapter/JdbcGateEssayJobStore.java`, `A/api/controller/GradingJobController.java`,
  `A/application/usecase/SaveAssessmentResultDetailsUseCase.java`, use case finalize
- Tạo: `A/application/usecase/{ListGradingQueueUseCase, ClaimGradingJobUseCase, GetJobEssayUseCase,
  GetProvisionalFeedbackUseCase}.java`, repository so sánh, `docs/ai-policy.md`
- Test:
  - `GradingQueueIntegrationTest`: liệt kê, nhận việc, tranh nhận → 409, quá 24 giờ thì nhận lại được;
  - `CourseGatePolicyIntegrationTest`: LLM xong nhưng không có kết quả, giám khảo finalize xong mới có kết quả và event,
    so sánh đủ hai điểm;
  - `GradingJobControllerTest`: CUSTOMER → 403; response không có band LLM.

## Implementation Steps

1. **Đỏ trước:** `CourseGatePolicyIntegrationTest`, `GradingQueueIntegrationTest`.
2. Migration; mở rộng store và adapter.
3. API hàng chờ, nhận việc, đọc essay.
4. Đổi luồng course test; nhận xét tạm.
5. Ghi so sánh ở hai đầu.
6. Viết `docs/ai-policy.md`; cập nhật contract `assessment-grading-v1.md` (mới) và `assessment-completed-v2.md` (ghi rõ
   course gate luôn có người chấm).
7. `mvn -q -pl services/assessment-service -am test`.

## Success Criteria

- [x] Essay course test không bao giờ được chốt bởi LLM.
- [x] EXAMINER thấy đủ việc chờ và không chấm trùng.
- [x] Mỗi essay course test đã chấm có một dòng so sánh; đủ hai điểm khi LLM chấm thành công. Khi LLM lỗi, phần LLM để null thay vì giả lập band.

## Risk Assessment

- Kết quả khóa chậm vì phụ thuộc giám khảo. Hàng chờ hiện thời gian chờ; phase 12 cảnh báo khi việc chờ quá 48 giờ.
- Demo cần tài khoản EXAMINER. Kiểm `DEMO_DATA_ENABLED` có seed sẵn tài khoản này.

## Bàn giao FE

- Màn hàng chờ chấm: lọc, nhận việc, chấm theo tiêu chí, chọn mã lỗi.
- Học viên thấy "Đang chờ giám khảo" và nhận xét tạm.


## Kết quả triển khai — 2026-10-16

- Migration V9 đã được kiểm tra trên PostgreSQL thật, cả database sạch lẫn nâng cấp từ V8 có job cũ trùng.
- Hàng chờ phân trang, nhận việc, tranh nhận đồng thời, nhận lại sau hơn 24 giờ và API essay chấm mù đã hoàn tất.
- Một giám khảo giữ các HUMAN job còn chờ của cùng attempt; mở phiên chấm lại sẽ đưa job đã hoàn tất về QUEUED chưa có người nhận.
- COURSE_GATE dùng cùng ánh xạ submission đã chốt cho hai job. Essay bỏ trống được ghi rõ omitted:true và chỉ tạo job người chấm để không mất khỏi hàng chờ.
- Lưu so sánh theo submission của assignment đã khóa; LLM đến trước/sau đều giữ phần điểm còn lại. Band giám khảo của từng essay lấy từ feedbackSnapshot.band (0–9, bước 0.5).
- Nhận xét tạm chỉ chứa summary, comments, focus; không trả band. Rubric trả null cho tới tính năng rubric.
- Topic, practice và placement giữ chính sách chấm hiện có. Schema AssessmentCompleted.v2 không đổi.
- Kiểm chứng cuối: mvn -B -ntp -pl services/assessment-service -am test — assessment 268, common-security 8; 0 fail/error/skip. Rà soát độc lập đã không còn blocker.
- Báo cáo: [from-codex-to-claude-phase-10-examiner-queue-report.md](plans/reports/from-codex-to-claude-phase-10-examiner-queue-report.md).
