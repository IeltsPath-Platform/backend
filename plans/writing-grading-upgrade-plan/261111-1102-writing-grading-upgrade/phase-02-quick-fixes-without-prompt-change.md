---
phase: 2
title: "Quick fixes without prompt change"
status: done (metric llm.call.duration và llm.tokens chờ phase 0b)
priority: P1
dependencies: [0]
---

# Phase 2: Sửa nhỏ không đổi prompt (learning)

## Overview
Bốn việc độc lập, không đụng tới prompt hay band: khớp response với contract, trả lại hạn mức khi chấm lỗi, ghi lại
model đã chấm, thêm metric. Sau phase này frontend đã gom lỗi theo dạng và hiện quy tắc được.

## Requirements
- **Correction trong response:** thêm `errorCode` (contract `lesson-writing-v1.md` đã ghi nhưng
  `WritingSubmissionResponse.Correction` chưa trả), `errorName`, `fixHint`, `severity`, `practiceKnowledgePointId`.
  - Bốn trường sau lấy từ danh mục lỗi đã tải lúc chấm (`severity` 1 đến 3 có sẵn trong danh mục, không do LLM cho) và
    **lưu vào JSON bài chấm**, không gọi Content khi đọc.
  - Bài cũ: các trường mới vắng mặt (`NON_NULL`), không backfill.
  - `practiceKnowledgePointId` chỉ có khi mã lỗi gắn KP luyện được; theo cách `StudyPoint.practiceKnowledgePointId` đang tính.
- **Hạn mức (quyết định 10):** `EssaySubmissionFlow.grade` gọi `quota.release(userId, day, USAGE_KIND)` **chỉ** khi lỗi
  phía hệ thống: `LLM_UNAVAILABLE`, `RUBRIC_UNAVAILABLE`, `PROMPT_VERSION_UNAVAILABLE`, và `AI_BUSY` (bulkhead đầy; hiện ném
  `LearningRequestException` đi thẳng qua, cần bắt riêng). `INVALID_GRADE` **vẫn tính lượt**: output hỏng có thể do chính
  bài nộp gây ra, trả lại thì một học viên gọi LLM được vô hạn. Dùng đúng `day` đã `tryConsume`; lỗi khi `release` chỉ log
  cảnh báo như `SpeakingGradingJob.releaseQuota`, không đổi mã lỗi trả về.
- **Model:** JSON bài chấm lưu `model` (giá trị `learning.llm.model`). Không phát ra API học viên, không log.
- **Metric (Micrometer, qua `common-observability`):**
  - counter `learning.writing.grading` nhãn `outcome` (`GRADED`, `LLM_UNAVAILABLE`, `INVALID_GRADE`, `AI_BUSY`,
    `DAILY_LIMIT_REACHED`, `RUBRIC_UNAVAILABLE`) và `source` (lesson, practice, free);
  - thời gian gọi LLM và token: dùng `llm.call.duration` và `llm.tokens` của `common-llm` (phase 0b) với
    `purpose=writing-grading`, không tạo timer riêng;
  - distribution `learning.writing.corrections`.
  - Không có nhãn user, submission hay nội dung.

## Related Code Files
(tương đối `services/learning-service/src/main/java/com/ieltspath/learning/`)
- Modify: `domain/vo/WritingGrade.java` (`Correction` thêm trường, giữ constructor cũ), `application/service/EssayGrader.java`,
  `application/service/EssaySubmissionFlow.java`, `api/dto/response/WritingSubmissionResponse.java`,
  mapper JSON của bài chấm trong `infrastructure/persistence/JdbcWritingSubmissionRepository.java`
- Docs: `docs/contracts/lesson-writing-v1.md` (trường mới của correction; ghi rõ lỗi phía hệ thống không tính hạn mức,
  `INVALID_GRADE` thì có)
- Tests: `EssaySubmissionFlowTest`, `EssayGraderTest`, test controller của Writing, test đọc JSON bài chấm cũ

## Implementation Steps
1. Baseline `mvn -q -pl services/learning-service -am test`; ghi lỗi nền.
2. Test đỏ: `LLM_UNAVAILABLE`, `AI_BUSY`, `RUBRIC_UNAVAILABLE`, `PROMPT_VERSION_UNAVAILABLE` → `release` đúng một lần;
   `INVALID_GRADE` → không gọi; chấm thành công → không gọi; hết hạn mức → không gọi.
3. Test đỏ: response có `errorCode`, `errorName`, `fixHint`; JSON bài chấm cũ (không có trường mới) vẫn đọc được.
4. Test đỏ: counter tăng đúng `outcome` (dùng `SimpleMeterRegistry`).
5. Code, chạy lại module, cập nhật contract.

## Success Criteria
- [ ] Response khớp contract; bài cũ không lỗi.
- [ ] Lỗi phía hệ thống không làm giảm hạn mức ngày; `INVALID_GRADE` vẫn tính lượt.
- [ ] Metric thấy được ở `/actuator/prometheus` khi chạy local.

## Risk Assessment
- `release` chạy sau một lần `tryConsume` bị từ chối sẽ trừ nhầm lượt của lần khác: chỉ gọi khi lần `tryConsume` của
  chính request này đã thành công.
- Thêm trường vào response là thay đổi cộng thêm; frontend cũ bỏ qua được.
