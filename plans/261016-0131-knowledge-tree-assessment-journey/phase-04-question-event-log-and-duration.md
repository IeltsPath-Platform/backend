---
phase: 4
title: "Event log từng câu và thời gian làm"
status: pending
priority: P1
dependencies: [3]
---

# Phase 4: Event log từng câu và thời gian làm

## Overview

Board: "tất cả đều phải trỏ ra Event Log câu hỏi". Hiện `kp_evidence` ghi đúng/sai theo KP và đã có `source`, nhưng
thiếu mã câu hỏi, thời gian làm và mã lỗi. Mentor cần thời gian để nhận ra kiểu học viên "chăm nhưng sai liên tục,
làm lâu". Phase này thêm bảng event theo câu ở learning, nhận `durationMs` từ client, và mang thời gian từ assessment
qua event `AssessmentCompleted.v2` (D9).

## Requirements

- **Migration learning `V11__question_attempt_events.sql`:**

  ```sql
  CREATE TABLE question_attempt_events (
      id UUID PRIMARY KEY,
      ordinal BIGINT GENERATED ALWAYS AS IDENTITY,
      user_id UUID NOT NULL,
      question_version_id UUID NOT NULL,
      knowledge_point_ids UUID[] NOT NULL DEFAULT '{}',
      correct BOOLEAN,
      score NUMERIC(8,2),
      max_score NUMERIC(8,2),
      duration_ms BIGINT CHECK (duration_ms IS NULL OR duration_ms BETWEEN 0 AND 7200000),
      error_code VARCHAR(60),
      source VARCHAR(30) NOT NULL CHECK (source IN
          ('lesson_exercise','practice_set','review_set','lesson_writing','assessment','diagnostic_probe')),
      source_reference_id UUID NOT NULL,
      first_submission BOOLEAN NOT NULL,
      occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      UNIQUE (user_id, source, source_reference_id, question_version_id)
  );
  CREATE INDEX idx_qae_user_time ON question_attempt_events (user_id, occurred_at);
  CREATE INDEX idx_qae_question ON question_attempt_events (question_version_id);
  CREATE INDEX idx_qae_user_kp ON question_attempt_events USING GIN (knowledge_point_ids);
  ```

  - `diagnostic_probe` để sẵn cho phase 7.
  - `first_submission`: lần làm đầu (đếm vào evidence) hay làm lại (D7 của plan 261002).
- **Request learning:**
  - `SubmitExerciseRequest.Answer` thêm `@PositiveOrZero Long durationMs` (tùy chọn). Cả lesson, Practice và set ôn
    đều dùng record này.
  - Server kẹp giá trị trong khoảng [0, 2 giờ] và không tin để chấm điểm.
- **Ghi event** trong cùng transaction với `kp_evidence`, ở:
  - `SubmitLessonExerciseUseCase`, `SubmitPracticeAttemptUseCase`, `SubmitReviewUseCase`;
  - `SubmitLessonEssayUseCase`, `SubmitPracticeEssayUseCase` (`duration_ms` null, `error_code` là mã lỗi nặng nhất
    nếu bộ chấm trả);
  - `ApplyAssessmentResultUseCase`.

  Viết port `QuestionEventLog` và adapter `JdbcQuestionEventLog`.
- **Assessment:**
  - Migration `V8__attempt_item_elapsed.sql`: `ALTER TABLE attempt_items ADD COLUMN elapsed_ms BIGINT CHECK
    (elapsed_ms IS NULL OR elapsed_ms >= 0)`.
  - `SaveAttemptResponseRequest` thêm `Long elapsedMs` (tùy chọn): tổng thời gian học viên đã ở câu đó, client cộng
    dồn. Server lưu `max(cũ, mới)`, kẹp 2 giờ.
  - Khi chấm tự động, chép vào `item_results.duration_milliseconds` nếu cột đang null.
  - `AssessmentCompletedEventFactory`: mỗi `item_results[]` thêm `duration_ms` (number hoặc null) và `error_code` lấy
    từ `error_analysis_items`. `knowledge_point_mappings[].error_type` giữ nguyên.
- **Learning parser:**
  - `AssessmentCompletedParser` đọc `duration_ms` và `error_code` nếu có. Thiếu hoặc null vẫn hợp lệ.
  - Giá trị sai kiểu → `ContractViolationException`.
- **Contract:** cập nhật `docs/contracts/assessment-completed-v2.md` (field tùy chọn, tương thích ngược) và
  `lesson-learning-v1.md`, `practice-v1.md` (`durationMs`).

## Related Code Files

(`L` = learning package, `A` = assessment package)

- L: `api/dto/request/SubmitExerciseRequest.java`, `application/command/SubmitExerciseCommand.java`, các use case ở
  trên, `application/port/QuestionEventLog.java` (mới), `infrastructure/persistence/JdbcQuestionEventLog.java` (mới),
  `infrastructure/messaging/AssessmentCompletedParser.java`
- A: `api/dto/request/SaveAttemptResponseRequest.java`, use case lưu câu trả lời, `AutoGradeAttemptService`,
  `application/event/AssessmentCompletedEventFactory.java`, record `AssessmentCompletedV2`
- Test:
  - L: `QuestionEventLogIntegrationTest` (mỗi nguồn ghi đúng 1 dòng/câu, nộp lại cùng `requestId` không nhân đôi),
    `AssessmentCompletedParserTest` (có và không có field mới);
  - A: `AssessmentCompletedEventFactoryTest`, test lưu `elapsedMs`.

## Implementation Steps

1. **Đỏ trước:** test parser với payload có `duration_ms` và `error_code`; payload cũ không có hai field vẫn hợp lệ.
2. Migration và adapter learning; nối vào 6 use case.
3. Assessment: migration, request, factory, chấm tự động.
4. Cập nhật contract.
5. Tính tương thích: assessment deploy trước hay learning trước đều không lỗi, vì parser bỏ qua field lạ.
6. `mvn -q -pl services/learning-service,services/assessment-service -am test`.

## Success Criteria

- [ ] Nộp Practice 5 câu kèm `durationMs` → 5 dòng event có `duration_ms`, `first_submission=true`.
- [ ] Làm lại cùng gói → event `first_submission=false`, không thêm `kp_evidence`.
- [ ] Bài thi có câu chọn phương án gắn mã → event có `error_code`.

## Risk Assessment

- Client gửi thời gian sai (treo tab). Kẹp 2 giờ; báo cáo dùng trung vị, không dùng trung bình.
- Bảng tăng nhanh. Đã có index theo user và câu. Lưu trữ dài hạn để sau.

## Bàn giao FE

- Web và mobile đo thời gian mỗi câu và gửi `durationMs` khi nộp lesson, Practice, set ôn.
- Gửi `elapsedMs` khi lưu câu trả lời bài thi.
