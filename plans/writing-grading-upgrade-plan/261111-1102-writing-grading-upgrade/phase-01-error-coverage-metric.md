---
phase: 1
title: "Error coverage metric"
status: code done, số nền v1 đã chạy trên 8 bài (xem reports/phase-01-prompt-v1-baseline-numbers-261111-2145.md); chờ xác nhận bộ bài và mã dự phòng
priority: P1
dependencies: [0]
---

# Phase 1: Đo độ bao phủ lỗi (assessment)

## Overview
Bộ đo của Assessment đã có band (within-half-band, MAE, QWK) và tỷ lệ báo sai mã lỗi, nhưng chưa đo AI bắt được bao nhiêu
lỗi mà examiner đã đánh dấu. Phase này thêm chỉ số đó và lấy số nền với prompt v1, trước mọi thay đổi khác.

## Requirements
- Migration `V23__gold_essay_examiner_errors.sql` (kiểm V23 còn trống lúc code):
  - `ai_gold_essays.examiner_errors JSONB NOT NULL DEFAULT '[]'`; phần tử `{excerpt, errorCode, kind}` với `kind` là
    `LANGUAGE` hoặc `DATA`. `examiner_error_codes` giữ nguyên.
  - `ai_eval_runs`: `error_recall NUMERIC(6,5)`, `data_error_recall NUMERIC(6,5)`, `corrections_per_essay NUMERIC(6,2)`,
    `p95_latency_ms INTEGER`, `invalid_grade_rate NUMERIC(6,5)`, `reasoning_effort VARCHAR(20)` (đều nullable; run cũ để null).
  - `ai_eval_items.llm_corrections JSONB`, `latency_ms INTEGER`, `failure_code VARCHAR(40)` để xem lại từng bài.
- **Bài lỗi không làm hỏng cả run (quyết định 11):** hiện `evaluate` bắt exception ngoài vòng lặp nên một bài lỗi là cả
  run `FAILED`. Đổi thành bắt theo từng bài: bài lỗi ghi `failure_code`, đếm vào `invalid_grade_rate`, bỏ khỏi các phép
  tính khác. `passed` thêm điều kiện `invalid_grade_rate ≤ assessment.ai-eval.max-invalid-rate` (mặc định 0.05). Lỗi
  không thuộc về bài (thiếu cấu hình, rubric, DB) vẫn fail cả run như cũ.
- **Grader của Assessment khớp Learning (quyết định 8)**, để số nền và p95 đo cùng đường chấm với học viên:
  - Cách gửi request (`max_tokens`/`max_completion_tokens`, `reasoning_effort`) đã giống Learning nhờ `common-llm`
    ở phase 0b; phase này không chép thêm code dựng request.
  - Eval cắt mỗi bài theo `assessment.ai-eval.grading-timeout-seconds`, đặt bằng giá trị của Learning; quá giờ là bài lỗi
    `LLM_TIMEOUT`. Read timeout 45 giây của chấm gate giữ nguyên.
  - Run ghi `model` (đã có) và `reasoning_effort`. API activation trả thêm hai giá trị này. Learning so với
    `learning.llm.model` và `reasoning-effort` của mình; lệch thì ghi metric `learning.writing.activation_mismatch` và log
    cảnh báo, **không** chặn chấm (chặn sẽ làm ngừng chấm vì một lỗi cấu hình). Quy định vận hành ghi vào `docs/ai-policy.md`.
- `POST /api/assessments/admin/ai-gold-essays` nhận thêm `examinerErrors` (tùy chọn). `excerpt` phải là chuỗi con nguyên
  văn của `essay`, sai thì `400`.
- Phép tính (hàm thuần, có test riêng): một lỗi của examiner được tính là **bắt được** khi có correction của AI có
  khoảng ký tự chồng lên `excerpt` của lỗi đó. `error_recall` tính trên lỗi `LANGUAGE`, `data_error_recall` trên lỗi
  `DATA`; bài không có `examiner_errors` không vào mẫu số.
- `EssayGradingPort.EssayGrade` mang thêm danh sách correction (excerpt + mã) để tính; không đổi nội dung chấm.
- **Frontend khu quản trị:** form "Thêm bài chuẩn" có danh sách lỗi nhập được từng dòng: đoạn sai (dán nguyên văn từ
  bài), mã lỗi, loại `LANGUAGE`/`DATA` (chỉ hiện `DATA` với Task 1); báo ngay khi đoạn sai không có trong bài, trước khi
  gửi. Ô "Mã lỗi" cũ giữ nguyên. Trang kết quả eval hiện thêm `error_recall`, `data_error_recall`, `invalid_grade_rate`,
  `p95_latency_ms`, model và `reasoning_effort`; run cũ thiếu số thì hiện "—". Không có chữ giải thích dưới trường.
- `error_recall` **chưa** vào điều kiện `passed`. Ngưỡng tối thiểu đặt sau khi có số nền trên cả bộ bài (ghi vào phase 4).
- **Bộ bài đo (quyết định 9):** examiner đánh dấu lỗi cho 5 đến 10 bài, gồm Task 1 và Task 2, trải nhiều mức band (ít
  nhất một bài band 6.5 trở lên để đo báo sai). Bài 24 lỗi là một trong số đó.

## Related Code Files
- Modify: `services/assessment-service/.../application/service/AiEvaluationService.java`, `AiEvaluationReadService.java`,
  `api/controller/AiEvaluationController.java`, `api/dto/request/CreateAiEvaluationRequest.java` (và `CreateAiGoldEssayRequest`),
  `application/port/EssayGradingPort.java`, `infrastructure/client/OpenAiCompatibleEssayGrader.java`,
  `infra/config-server/config-repo/assessment-service.yaml`
- Learning: `infrastructure/client/RestEssayPromptActivationClient.java` (đọc `model`, `reasoningEffort`; so và báo lệch)
- Frontend (khu quản trị, theo `frontend/.sdd/constraints/admin-ui.md`; form tạo mới là drawer phải):
  `src/features/admin/ai-evaluation/components/GoldEssayForm.tsx`, `types.ts`, `api/evaluationApi.ts`,
  `pages/AiEvaluationPage.tsx`, `pages/AiEvaluationPages.test.tsx`
- Create: migration V23, lớp tính recall trong `application/service/`
- Docs: contract admin của AI evaluation trong `docs/contracts/` (tìm file hiện có; chưa có thì thêm mục vào contract gần nhất)

## Implementation Steps
1. Baseline `mvn -q -pl services/assessment-service -am test`; ghi lỗi nền.
2. Test đỏ cho phép tính recall: trùng khít, chồng một phần, không chồng, cụm lặp nhiều lần trong bài, bài không có lỗi examiner.
3. Test đỏ Testcontainers: migration áp trên DB có V1 đến V22; run lưu đủ cột mới; gold essay từ chối `excerpt` không có trong bài.
4. Test đỏ: một bài ném lỗi → run vẫn `DONE`, `invalid_grade_rate` đúng; vượt `max-invalid-rate` → `passed = false`;
   thiếu cấu hình → run `FAILED` như cũ.
5. Test đỏ: eval cắt bài quá `grading-timeout-seconds` thành `LLM_TIMEOUT`; run ghi `reasoning_effort`; Learning báo
   lệch model khi activation khác cấu hình.
6. Code, chạy lại hai module.
7. Frontend: test đỏ form (thêm, xóa dòng lỗi; đoạn sai không có trong bài bị chặn; Task 2 không có `DATA`) và trang kết
   quả (chỉ số mới, run cũ hiện "—") → code; `npm run lint`, `npm run typecheck`, `npm run build`.
8. Nhập bộ 5 đến 10 bài có `examinerErrors` qua màn quản trị trên môi trường thử. Văn bản bài và danh sách lỗi **không**
   commit vào repo.
9. Chạy eval với prompt v1 và model, `reasoning_effort` giống Learning; ghi số nền vào `reports/` của plan này (chỉ số
   liệu, không có essay).

## Success Criteria
- [ ] Run mới có `error_recall`, `data_error_recall`, `p95_latency_ms`, `invalid_grade_rate`, `reasoning_effort`.
- [ ] Một bài lỗi không làm hỏng cả run.
- [ ] Có số nền v1 trên bộ 5 đến 10 bài.
- [ ] Learning báo được khi model của run kích hoạt khác model của mình.
- [ ] Run và gold essay cũ vẫn đọc được.

## Risk Assessment
- Eval gọi LLM thật và có hạn mức `assessment.ai-eval.daily-limit`; test tự động chỉ dùng grader giả.
- Bộ bài cần công đánh dấu của examiner: code làm trước được, số nền chờ đủ bộ. Chưa đủ 5 bài thì chưa đặt ngưỡng.
- Đổi cách gửi request của grader Assessment đã làm ở phase 0b, kèm run eval v1 trước và sau; run "sau" dùng làm số nền
  ở đây khi bộ bài đã đủ.
