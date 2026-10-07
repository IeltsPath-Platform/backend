---
phase: 6
title: "Assessment LLM gate grading"
status: pending
priority: P1
dependencies: [1]
---

# Phase 6: Assessment LLM gate grading

## Overview
Đề `TOPIC_GATE`/`COURSE_GATE` có essay: assessment chấm khách quan + LLM chấm essay ngoài transaction nộp bài,
không trừ điểm, có hạn mức (M8). Lỗi thì chuyển EXAMINER (M9). `MOCK` không đổi.

## Requirements
- Functional:
  - Nộp attempt gate có essay:
    - `AutoGradeAttemptService` không tự chấm được (như hiện tại);
    - essay được học viên nộp trước qua API sẵn có `POST /api/assessments/submissions` (`learner_submissions`,
      `attempt_item_id`). Trong transaction nộp attempt: item essay có submission `SUBMITTED` → tạo `grading_jobs`
      `grading_mode=AI`, `point_cost_snapshot=0`, `status=QUEUED`; item essay chưa có submission → 0 điểm, không tạo
      job (M14). Mọi essay đều thiếu → chấm xong ngay trong transaction như tự chấm;
<!-- Updated: Validation Session 1 - nguồn essay từ learner_submissions; thiếu thì 0 điểm -->
    - `idempotency_key = attemptItemId` để nộp lại không tạo job trùng.
  - `GateEssayGradingScheduler` (`@Scheduled`, cấu hình `assessment.llm-grading.poll-interval`):
    - lấy job AI `QUEUED` của attempt `TOPIC_GATE`/`COURSE_GATE` (giới hạn batch, `FOR UPDATE SKIP LOCKED`), chuyển
      `PROCESSING` và commit;
    - **ngoài transaction**: kiểm `AssessmentLlmQuota` (theo user, theo ngày, giới hạn `ASSESSMENT_LLM_DAILY_LIMIT`),
      gọi `EssayGradingPort.grade(prompt, essay)`;
    - thành công: transaction mới ghi band vào job (`COMPLETED`); khi **mọi** job của attempt `COMPLETED` → tạo result
      version 1:
      - câu khách quan chấm bằng `AnswerSpecGrader`;
      - essay `correct = band ≥ passBand`, `score = correct ? maxScore : 0` (M10);
      - gọi `AssessmentResultCompleter.complete` → outbox `AssessmentCompleted.v2` như luồng tự chấm.
    - thất bại (LLM lỗi, chưa cấu hình, hết hạn mức, JSON sai): job chuyển `FAILED`; tạo job `HUMAN` mới cho
      submission đó (hàng chờ EXAMINER). EXAMINER chấm qua `GradingController` / `SaveAssessmentResultDetailsUseCase`
      sẵn có.
    - job `PROCESSING` quá `assessment.llm-grading.stuck-after` (service chết giữa chừng) → về `QUEUED`.
  - `EssayGradingPort` ở `application/port`; adapter `OpenAiCompatibleEssayGrader` ở `infrastructure/client`:
    - prompt band descriptors chép từ `services/learning-service/.../application/service/EssayGrader.java` (giữ nguyên
      cách tính overall band và kiểm tiêu chí);
    - cấu hình `ASSESSMENT_LLM_BASE_URL`, `ASSESSMENT_LLM_API_KEY`, `ASSESSMENT_LLM_MODEL` (thiếu thì port báo
      `unavailable` → chuyển HUMAN ngay).
  - Bảng `llm_usage_daily(user_id, usage_date, count, PRIMARY KEY(user_id, usage_date))` (migration
    `V6__gate_llm_grading.sql`, cùng với cột `grading_jobs.llm_band NUMERIC(2,1)` để lưu band LLM trả về và index job
    theo `(status, grading_mode)`).
  - `MOCK` và attempt không phải gate: không tạo job AI; giữ luồng EXAMINER như cũ.
- Non-functional:
  - không log essay, prompt, API key; log job id và mã lỗi;
  - test dùng `MockRestServiceServer` hoặc port giả;
  - thêm biến vào `config-repo/assessment-service.yaml` với default rỗng; ghi **tên biến** vào README và `.env`
    example nếu có, không ghi giá trị.

## Architecture
```
SubmitAssessmentAttemptUseCase ─(tx)→ AutoGradeAttemptService (objective only) │ else → EnqueueGateEssayGrading (jobs AI)
GateEssayGradingScheduler → ClaimJobs(tx) → quota + EssayGradingPort (no tx) → CompleteJob(tx)
                                                           └─ fail → FailJob(tx) + HUMAN job
CompleteJob: all jobs COMPLETED → GateResultAssembler → AssessmentResultCompleter → outbox
```

## Related Code Files
- Create (gốc `services/assessment-service/src/main/java/com/group01/assessment/`):
  - `application/port/{EssayGradingPort,AssessmentLlmQuota}.java`;
  - `application/usecase/{EnqueueGateEssayGradingService,GradeGateEssayJobUseCase,GateResultAssembler}.java`;
  - `infrastructure/client/OpenAiCompatibleEssayGrader.java`, `infrastructure/persistence/JdbcAssessmentLlmQuota.java`;
  - `infrastructure/scheduler/GateEssayGradingScheduler.java`, `infrastructure/config/AssessmentLlmProperties.java`;
  - `src/main/resources/db/migration/V6__gate_llm_grading.sql`.
- Modify: `application/usecase/SubmitAssessmentAttemptUseCase.java`, `AutoGradeAttemptService.java` (tách phần chấm
  khách quan cho `GateResultAssembler` dùng lại), job entity/repository (thêm `llm_band`; trạng thái `QUEUED/PROCESSING/COMPLETED/FAILED` sẵn có đủ dùng, không thêm);
  `infrastructure/config/...` bật `@EnableScheduling` nếu chưa có (relay outbox đã dùng `@Scheduled`).
- Config: `infra/config-server/config-repo/assessment-service.yaml`.
- Tests: `GradeGateEssayJobUseCaseTest`, `OpenAiCompatibleEssayGraderTest` (MockRestServiceServer),
  `GateEssayGradingIntegrationTest` (Testcontainers: nộp → job → chấm giả → outbox có event; lỗi → job HUMAN).

## Implementation Steps
1. **Test đỏ**:
   - nộp gate có essay đã submit → 1 job AI `QUEUED`, chưa có result;
   - nộp gate mà essay chưa submit → essay 0 điểm, result hoàn thành ngay, không có job;
   - scheduler + port giả band 6.0, `passBand` 5.5 → result hoàn thành, outbox 1 event, item essay `correct=true`;
   - port lỗi → job `FAILED` + job `HUMAN`; EXAMINER chấm → event;
   - hết hạn mức → `HUMAN`;
   - `MOCK` có essay → không có job AI;
   - nộp lại cùng attempt → không job trùng;
   - job kẹt `PROCESSING` → về `QUEUED`.
2. Migration V6; port; quota; adapter (test với MockRestServiceServer).
3. Enqueue trong submit; tách chấm khách quan; use case chấm job; scheduler.
4. Config + README.
5. `mvn -q -pl services/assessment-service -am test`.

## Success Criteria
- [ ] Không gọi LLM trong transaction nào (review code + test bằng port chậm giả).
- [ ] Luồng EXAMINER cũ và tự chấm khách quan không đổi (test cũ xanh).

## Risk Assessment
- Luồng nộp bài essay hiện tại (`learner_submissions`, `grading_jobs`) phải đọc kỹ trước khi sửa: bước đầu của phase là
  ghi lại luồng hiện có vào phần comment của test integration.
- Chép prompt từ learning là nhân bản có chủ ý (AGENTS cấm logic nghiệp vụ trong `shared/`); ghi comment chỉ nguồn để
  hai bản sửa cùng nhau.
- Thêm biến môi trường: báo người dùng cập nhật `.env`.
