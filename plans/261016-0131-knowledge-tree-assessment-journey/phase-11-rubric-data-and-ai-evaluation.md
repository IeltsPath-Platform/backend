---
phase: 11
title: "Rubric dạng dữ liệu và bộ test nghiệm thu AI"
status: pending
priority: P2
dependencies: [3, 10]
---

# Phase 11: Rubric dạng dữ liệu và bộ test nghiệm thu AI

## Overview

Board yêu cầu rubric có tiêu chí, mô tả từng mức và ví dụ chuẩn, cùng bộ test nghiệm thu AI (gold set, accuracy, đồng
thuận với giáo viên, false-positive). Mentor: "AI phải lập bộ test… pass qua rồi mới cắm vào".

Hiện tại rubric là hằng số một dòng cho mỗi tiêu chí, và bị chép ở hai nơi: `EssayGrader` (learning) và
`OpenAiCompatibleEssayGrader` (assessment). Phase này:
- chuyển rubric thành dữ liệu do R&D quản lý;
- hai bộ chấm cùng dựng prompt từ dữ liệu đó, cùng phiên bản;
- thêm bộ đánh giá AI có ngưỡng bật phiên bản.

## Requirements

- **Migration content `V33__rubrics.sql`:**
  - `rubrics`: id, code, task `TASK_1` / `TASK_2`, version, status `DRAFT` / `ACTIVE` / `RETIRED`, `source_note`.
    Mỗi task chỉ có một rubric `ACTIVE`.
  - `rubric_criteria`: code `TR` / `TA` / `CC` / `LR` / `GRA`, name, mô tả chung, thứ tự.
  - `rubric_band_descriptors`: (criterion, band 0..9, descriptor).
  - `rubric_examples`: (rubric, band, prompt, essay, commentary).
  - Seed rubric `TASK_1`, `TASK_2`. Mô tả band 4–9 diễn giải lại band descriptor công khai, ghi nguồn trong
    `source_note`; bài mẫu tự viết (D14). Nội dung do AI soạn nháp rồi người hiểu IELTS duyệt (theo ý mentor).
- **API rubric:**
  - `ADMIN`, `CONTENT_AUTHOR`: tạo bản nháp, sửa, kích hoạt.
  - `GET /api/content/rubrics?task=`: thêm `EXAMINER` được đọc.
  - API nội bộ `GET /internal/learning-content/rubrics/active?task=`.
- **Bộ chấm:**
  - Learning `EssayGrader` và assessment `OpenAiCompatibleEssayGrader` đọc rubric `ACTIVE` qua API nội bộ và dựng system
    prompt từ tiêu chí cùng mô tả band.
  - Prompt yêu cầu mỗi chỗ sửa có `errorCode` thuộc danh mục của phase 3. Danh mục đưa vào prompt, đầu ra validate,
    mã lạ thì bỏ mã đó.
  - `prompt_version = "<rubricCode>@<rubricVersion>+tpl<templateVersion>"`, lưu kèm mọi lần chấm LLM: assessment ở
    `grading_jobs`, learning ở `lesson_writing_submissions` và essay Practice.
  - Không đọc được rubric → assessment chuyển giám khảo; learning báo lỗi tạm như khi LLM lỗi.
  - Template prompt nằm trong mỗi service nhưng mang chung một `templateVersion`. Test so khớp chuỗi template giữa hai
    service bằng file vector chung trong `docs/contracts/essay-prompt-template-v1.txt`.
- **Migration assessment `V10__ai_evaluation.sql`:**
  - `ai_gold_essays`: id, task, prompt, chart_facts, essay, examiner_band, examiner_criteria jsonb,
    examiner_error_codes[], source `COMPARISON` / `MANUAL`, created_by, created_at.
  - `ai_eval_runs`: id, model, prompt_version, status `RUNNING` / `DONE` / `FAILED`, n, `within_half_band` (tỷ lệ lệch
    ≤ 0.5), mae, qwk, `per_criterion` jsonb, `error_code_false_positive`, `passed`, started_at, finished_at.
  - `ai_eval_items`: run, gold, llm_band, llm_criteria, llm_error_codes, diff.
  - `ai_prompt_activations`: task, prompt_version, run_id, activated_by, activated_at.
- **API đánh giá** (`ADMIN`):
  - `POST /api/assessments/admin/ai-gold-essays`: nhập tay bài GV đã chấm.
  - `POST /api/assessments/admin/ai-gold-essays/import-comparisons`: lấy từ `essay_grade_comparisons` đã đủ điểm
    giám khảo.
  - `POST /api/assessments/admin/ai-evaluations`: chạy bất đồng bộ trên gold set bằng `EssayGradingPort` với model và
    prompt hiện tại. Dùng hạn mức riêng `assessment.ai-eval.daily-limit`, không ăn vào quota của học viên.
  - `GET /api/assessments/admin/ai-evaluations/{id}`.
  - `POST /api/assessments/admin/ai-prompt-activations`: chỉ kích hoạt `prompt_version` có lần chạy `passed = true`,
    nếu không → `409 EVALUATION_NOT_PASSED`.
- **Chỉ số:**
  - `within_half_band ≥ 0.80` và `qwk ≥ 0.60`, cấu hình ở `assessment.ai-eval.min-*`;
  - QWK tính trên band làm tròn 0.5;
  - false-positive mã lỗi = mã LLM báo mà giám khảo không gắn / tổng mã LLM báo, chỉ tính trên gold có mã của giám
    khảo;
  - false-positive cờ bất thường tính ở phase 12.
- **Gate trong bộ chấm:** `GradeGateEssayJobUseCase` chỉ dùng `prompt_version` đang được kích hoạt. Rubric đổi mà chưa có
  lần chạy đạt thì tiếp tục dùng bản đã kích hoạt trước đó.

## Related Code Files

- content: migration V33, aggregate `Rubric`, use case, `RubricController`, `InternalLearningContentController`
- learning: `application/service/EssayGrader.java`, client đọc rubric, lưu `prompt_version`
- assessment: `infrastructure/client/OpenAiCompatibleEssayGrader.java`, `application/usecase/GradeGateEssayJobUseCase.java`,
  migration V10, use case và controller đánh giá, domain service `AgreementMetrics` (QWK, MAE, tỷ lệ lệch ≤ 0.5)
- Test:
  - `AgreementMetricsTest`: QWK với bộ số tính tay; trường hợp hoàn toàn trùng → 1;
  - `RubricPromptBuilderTest` ở hai service: chuỗi template khớp file vector;
  - `AiEvaluationIntegrationTest`: LLM giả trả band cố định → chỉ số đúng; kích hoạt khi chưa đạt → 409.

## Implementation Steps

1. Soạn rubric nháp (AI hỗ trợ), người hiểu IELTS duyệt, ghi nguồn.
2. **Đỏ trước:** `AgreementMetricsTest`, `RubricPromptBuilderTest`.
3. Content: bảng, API, seed.
4. Hai bộ chấm đọc rubric, thêm `errorCode`, lưu `prompt_version`.
5. Assessment: gold set, chạy đánh giá, kích hoạt.
6. Nhập gold set khởi đầu: vài chục bài do người có chuyên môn chấm tay. Chạy lần đầu, ghi kết quả vào báo cáo plan.
7. `mvn -q -pl services/content-service,services/learning-service,services/assessment-service -am test`.

## Success Criteria

- [ ] Sửa rubric trên API → prompt của cả hai bộ chấm đổi theo sau khi kích hoạt.
- [ ] Có ít nhất một lần chạy đánh giá với chỉ số được lưu, dùng làm bằng chứng trình hội đồng.
- [ ] Không kích hoạt được prompt chưa đạt ngưỡng.

## Risk Assessment

- Gold set nhỏ thì chỉ số dao động. Báo cáo kèm `n` và khoảng tin cậy đơn giản.
- Chạy đánh giá tốn quota LLM. Hạn mức riêng, chạy thủ công.

## Bàn giao FE

- Màn rubric cho R&D.
- Màn gold set và kết quả đánh giá cho Admin: bảng chỉ số, so sánh từng bài.
