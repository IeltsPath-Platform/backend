---
phase: 11
title: "Rubric dạng dữ liệu và bộ test nghiệm thu AI"
status: tooling_complete
priority: P2
dependencies: [3, 10]
---

> **Trạng thái (2026-10-18): công cụ xong, chưa nghiệm thu.** Chưa có gold set do chuyên gia IELTS chấm và chưa có lần chạy đánh giá thật nên chưa có bằng chứng độ chính xác. Không bật mặc định quyết định nào dựa trên kết quả đánh giá AI.


# Phase 11: Rubric dạng dữ liệu và bộ test nghiệm thu AI

## Overview

Board yêu cầu rubric có tiêu chí, mô tả từng mức và ví dụ chuẩn, cùng bộ test nghiệm thu AI (gold set, accuracy, đồng
thuận với giáo viên, false-positive). Mentor: "AI phải lập bộ test… pass qua rồi mới cắm vào".

Rubric được quản lý thành dữ liệu trong Content; `EssayGrader` (Learning) và `OpenAiCompatibleEssayGrader`
(Assessment) cùng dựng prompt từ rubric và template chung. Phase này:
- chuyển rubric thành dữ liệu do R&D quản lý;
- hai bộ chấm cùng dựng prompt từ dữ liệu đó, cùng phiên bản;
- thêm bộ đánh giá AI có ngưỡng bật phiên bản.

Trạng thái ngày 2026-10-17: đang triển khai. Seed được bật để bootstrap theo quyết định của người dùng, nhưng vẫn
là bản nháp do AI soạn, chưa được chuyên gia IELTS duyệt. **Tiêu chí nghiệm thu còn mở:** cần gold set do người có
chuyên môn chấm và ít nhất một lần đánh giá thật có chỉ số lưu lại; test tự động không thay thế bằng chứng này.

## Requirements

- **Migration content `V39__rubrics.sql`:**
  - `rubrics`: id, code, task `TASK_1` / `TASK_2`, version, status `DRAFT` / `ACTIVE` / `RETIRED`, `source_note`.
    Mỗi task chỉ có một rubric `ACTIVE`.
  - `rubric_criteria`: code `TR` / `TA` / `CC` / `LR` / `GRA`, name, mô tả chung, thứ tự.
  - `rubric_band_descriptors`: (criterion, band 0..9, descriptor).
  - `rubric_examples`: (rubric, band, prompt, essay, commentary).
  - Seed rubric `TASK_1`, `TASK_2` ở trạng thái `ACTIVE` để bootstrap. Mô tả band 4–9 diễn giải lại band descriptor
    công khai, ghi nguồn trong `source_note`; bài mẫu tự viết (D14). `source_note` phải ghi rõ đây là bản nháp do AI
    soạn, chưa được chuyên gia IELTS duyệt. Trạng thái `ACTIVE` không chứng nhận đã được chuyên gia duyệt.
- **API rubric:**
  - `ADMIN`, `CONTENT_AUTHOR`: tạo bản nháp, sửa, kích hoạt.
  - `GET /api/content/rubrics?task=`: thêm `EXAMINER` được đọc.
  - API nội bộ `GET /internal/learning-content/rubrics/active?task=`.
  - API nội bộ `GET /internal/learning-content/rubrics/{code}/{version}` trả bản `ACTIVE` hoặc `RETIRED`, không trả `DRAFT`.
- **Bộ chấm:**
  - Trước khi task có dòng đầu tiên trong `ai_prompt_activations`, Learning `EssayGrader` và Assessment
    `GradeGateEssayJobUseCase` dùng rubric `ACTIVE` của Content; không trả `PROMPT_NOT_ACTIVATED` chỉ vì chưa có dòng.
  - Sau lần kích hoạt đầu tiên của task, cả hai chỉ dùng `promptVersion` đã kích hoạt, kể cả khi rubric đã `RETIRED`.
    Đổi rubric `ACTIVE` trong Content không đổi phiên bản đang chấm. Lỗi đọc activation/rubric không cho phép quay về bootstrap.
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
    rubric `ACTIVE` hiện tại của task, độc lập phiên bản runtime đã kích hoạt. Dùng hạn mức riêng
    `assessment.ai-eval.daily-limit`, không ăn vào quota của học viên.
  - `GET /api/assessments/admin/ai-evaluations/{id}`.
  - `POST /api/assessments/admin/ai-prompt-activations`: chỉ kích hoạt `prompt_version` có lần chạy `passed = true`,
    nếu không → `409 EVALUATION_NOT_PASSED`.
- **Tra cứu activation nội bộ:** `GET /internal/assessments/ai-prompt-activations?task=TASK_1` yêu cầu internal JWT đã
  xác thực; trả `{"task":"TASK_1","promptVersion":null}` khi chưa có dòng, hoặc `promptVersion` là chuỗi đã kích hoạt.
  Learning gọi Assessment qua `learning.assessment.base-url` (mặc định `http://localhost:8083`), rồi gọi API rubric
  lịch sử của Content khi có phiên bản. Lỗi dịch vụ không được hiểu thành chưa có activation.
- **Chỉ số:**
  - `within_half_band ≥ 0.80` và `qwk ≥ 0.60`, cấu hình ở `assessment.ai-eval.min-*`;
  - QWK tính trên band làm tròn 0.5;
  - false-positive mã lỗi = mã LLM báo mà giám khảo không gắn / tổng mã LLM báo, chỉ tính trên gold có mã của giám
    khảo;
  - false-positive cờ bất thường tính ở phase 12.
- **Gate trong bộ chấm:** bootstrap chỉ khi task chưa có dòng activation; sau đó cả hai bộ chấm chỉ dùng phiên bản đã
  kích hoạt. Rubric đổi mà chưa có lần chạy đạt và được kích hoạt thì tiếp tục dùng phiên bản trước đó. Quyết định D6
  giữ nguyên: essay course test do EXAMINER chấm mù để chốt kết quả, LLM chỉ trả nhận xét tạm và lưu so sánh.

## Related Code Files

- content: migration V39, aggregate `Rubric`, use case, `RubricController`, `InternalRubricController`
- learning: `application/service/EssayGrader.java`, client đọc rubric, lưu `prompt_version`
- assessment: `infrastructure/client/OpenAiCompatibleEssayGrader.java`, `application/usecase/GradeGateEssayJobUseCase.java`,
  migration V10, use case và controller đánh giá, domain service `AgreementMetrics` (QWK, MAE, tỷ lệ lệch ≤ 0.5)
- Test:
  - `AgreementMetricsTest`: QWK với bộ số tính tay; trường hợp hoàn toàn trùng → 1;
  - `RubricPromptBuilderTest` ở hai service: chuỗi template khớp file vector;
  - `AiEvaluationIntegrationTest`: LLM giả trả band cố định → chỉ số đúng; kích hoạt khi chưa đạt → 409.

## Implementation Steps

1. Seed rubric AI nháp ở trạng thái `ACTIVE` để bootstrap; ghi nguồn và tình trạng chưa duyệt. Chuyên gia IELTS duyệt
   là công việc nghiệm thu còn mở, không được suy ra từ trạng thái `ACTIVE`.
2. **Đỏ trước:** `AgreementMetricsTest`, `RubricPromptBuilderTest`.
3. Content: bảng, API, seed.
4. Hai bộ chấm đọc rubric, thêm `errorCode`, lưu `prompt_version`.
5. Assessment: gold set, chạy đánh giá, kích hoạt.
6. Nhập gold set khởi đầu: vài chục bài do người có chuyên môn chấm tay. Chạy lần đầu, ghi kết quả vào báo cáo plan.
7. `mvn -q -pl services/content-service,services/learning-service,services/assessment-service -am test`.

## Success Criteria

- [x] Trước activation đầu tiên của task, cả hai bộ chấm dùng rubric `ACTIVE`; sau activation chỉ dùng phiên bản đã
  kích hoạt, kể cả `RETIRED`, và lỗi dịch vụ không quay về bootstrap.
- [ ] Sửa rubric trên API → prompt của cả hai bộ chấm đổi theo sau khi lần đánh giá đạt được kích hoạt.
- [ ] **Còn mở:** có gold set được chuyên gia chấm và ít nhất một lần chạy đánh giá thật với chỉ số được lưu, dùng làm
  bằng chứng trình hội đồng. Chưa có bằng chứng hoàn thành; không dùng điểm chuyên gia giả lập để đóng tiêu chí.
- [x] Không kích hoạt được prompt chưa đạt ngưỡng.

Bằng chứng kiểm tra tự động: [báo cáo xác minh](./reports/phase-11-verification.md). Content, Learning và Assessment
chạy đủ 1.074 test, không fail, error hoặc skip.

**Quyết định (2026-10-17): phase kết thúc ở mức công cụ, chưa có bằng chứng nghiệm thu.** Rubric dạng dữ liệu, bộ chấm,
đánh giá AI và cổng kích hoạt đều chạy và có test. Chưa có gold set do chuyên gia IELTS chấm và chưa có lần chạy đánh giá
thật, nên **không được nói hệ thống đã chứng minh độ chính xác của AI** khi trình hội đồng; rubric hạt giống và các chỉ
số hiện có chỉ là bản nháp chưa chuyên gia duyệt. Hai tiêu chí còn mở ở trên cần dữ liệu và lần chạy nghiệm thu thực tế;
khi có người chấm thì làm theo `docs/demo/knowledge-tree-demo.md` bước 7.

## Risk Assessment

- Gold set nhỏ thì chỉ số dao động. Báo cáo kèm `n` và khoảng tin cậy đơn giản.
- Chạy đánh giá tốn quota LLM. Hạn mức riêng, chạy thủ công.

## Bàn giao FE

- Màn rubric cho R&D.
- Màn gold set và kết quả đánh giá cho Admin: bảng chỉ số, so sánh từng bài.
