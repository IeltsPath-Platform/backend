---
phase: 14
title: "Học viên sửa essay và nộp lại"
status: pending
priority: P2
dependencies: [4, 5]
---

# Phase 14: Học viên sửa essay và nộp lại

## Overview

Board có nhánh: nhận feedback Writing theo rubric → **học viên sửa bài** → attempt mới → cập nhật Mastery Profile.

**Hiện trạng (đã kiểm với code):** học viên đã nộp lại được essay của một block lesson. Mỗi lần nộp là một bản ghi riêng
trong `lesson_writing_submissions` (khác `requestId`), và `SubmitLessonEssayUseCase` ghi bằng chứng `kp_evidence` cho mọi
bản nộp **trước khi** học viên đạt block đó; sau khi đạt thì không ghi nữa. Thứ còn thiếu là: các bản **không nối với
nhau**, không so sánh được bản trước với bản sau, không giới hạn số lần nộp, và Practice essay chưa có chuỗi tương tự.

Phase này thêm chuỗi bản, so sánh và giới hạn lần sửa. **Không đổi luật bằng chứng hiện tại.**

## Requirements

- **Migration learning `V18__essay_revisions.sql`:**

  ```sql
  ALTER TABLE lesson_writing_submissions ADD COLUMN previous_submission_id UUID REFERENCES lesson_writing_submissions(id);
  ALTER TABLE lesson_writing_submissions ADD COLUMN revision_no INTEGER NOT NULL DEFAULT 1 CHECK (revision_no BETWEEN 1 AND 5);
  CREATE INDEX idx_lesson_writing_previous ON lesson_writing_submissions (previous_submission_id);
  ```

  Bản đầu có `previous_submission_id` null và `revision_no = 1`.
- **API:** `POST /api/learning/writing-submissions/{id}/revisions` (`CUSTOMER`, của chính mình), body giống nộp essay
  (`requestId`, `essayText`):
  - chỉ nộp lại khi bản trước ở trạng thái `GRADED` và là bản **mới nhất** của chuỗi (không rẽ nhánh);
  - tối đa 3 lần sửa (`revision_no` ≤ 4) cho mỗi essay gốc; vượt → `409 REVISION_LIMIT`;
  - dùng lại nguyên luồng nộp, chấm LLM, hạn mức ngày và trừ điểm của essay hiện có (không viết đường chấm riêng);
  - prompt chấm giữ nguyên đề và rubric; **không** gửi bản trước hay nhận xét trước cho LLM (tránh chấm lệch).
- **So sánh:** `GET /api/learning/writing-submissions/{id}/history` trả chuỗi các bản: band từng tiêu chí, band tổng,
  số từ, mã lỗi (phase 3) của từng bản, và `delta` so với bản trước; mã lỗi đã hết và còn lại giữa hai bản gần nhất.
- **Mastery (đã chốt với người dùng 2026-10-16): giữ luật hiện tại, không đổi.** Bản sửa vẫn theo đúng luật đang chạy:
  mỗi bản nộp trước khi đạt block ghi bằng chứng, sau khi đạt thì không. Phase này không thêm và không bớt bằng chứng; chỉ
  ghi thêm `question_attempt_events` (phase 4) cho mỗi bản kèm `revision_no`. Báo cáo (phase 5) thêm `revisionDelta` ở
  khối `evidence`. Khớp nhánh "HV sửa bài → cập nhật mastery" trên board và không phải sửa AGENTS.md.
- **Cổng và bài ôn:** không đổi so với hiện tại.

## Related Code Files

(`L` = learning package)

- Sửa: `L/domain/aggregate/WritingSubmission.java` (liên kết bản trước, số lần sửa), `L/application/service/EssaySubmissionFlow.java`,
  `L/infrastructure/persistence/JdbcWritingSubmissionRepository.java`, `L/api/controller/WritingController.java`
- Tạo: `L/application/usecase/{SubmitEssayRevisionUseCase, GetEssayHistoryUseCase}.java`, DTO
- Test: `WritingSubmissionTest` (giới hạn lần sửa, chỉ sửa bản mới nhất), `EssayRevisionIntegrationTest` (Testcontainers,
  LLM giả: nộp, sửa, so sánh, bằng chứng ghi đúng như luật hiện tại, trừ điểm đúng), `WritingControllerTest` (chỉ chủ bài)

## Implementation Steps

1. **Đỏ trước:** `WritingSubmissionTest`.
2. Migration, aggregate, repository.
3. Use case sửa, dùng lại `EssaySubmissionFlow`; use case lịch sử.
4. Cập nhật báo cáo phase 5 (`revisionDelta`).
5. `mvn -q -pl services/learning-service -am test`.

## Success Criteria

- [ ] Nộp lại tạo bản mới nối với bản trước; lịch sử trả delta đúng.
- [ ] Luật bằng chứng không đổi: test hiện có của `SubmitLessonEssayUseCase` vẫn xanh nguyên vẹn.
- [ ] Vượt 3 lần sửa → 409; không rẽ nhánh từ bản cũ.

## Risk Assessment

- Mỗi bản sửa tốn điểm và một lượt LLM như essay thường, nên học viên và hạn mức đều phải chịu thêm. Chưa giảm giá bản
  sửa; nếu cần ưu đãi thì là quyết định kinh doanh riêng.
- LLM chấm không ổn định giữa hai lần nên `delta` có nhiễu. Hiển thị theo tiêu chí và ghi chú đây là chấm tự động.

## Bàn giao FE

Nút "Sửa và nộp lại" ở màn kết quả essay, màn so sánh hai bản cạnh nhau, biểu đồ band theo lần nộp.
