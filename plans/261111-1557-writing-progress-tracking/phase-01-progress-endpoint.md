---
phase: 1
title: "Progress endpoint"
status: done
priority: P2
dependencies: []
---

# Phase 1: Endpoint tổng hợp tiến bộ Writing (learning)

## Overview
Một endpoint chỉ đọc gom các bài Writing đã chấm gần nhất của học viên, trả xu hướng band, lỗi hay gặp và lỗi đã khắc
phục. Mọi phép tính là hàm thuần trên dữ liệu đã lưu; không gọi LLM.

## Requirements
- `GET /api/learning/writing/progress?task=TASK_1|TASK_2&limit=10` (`limit` 3 đến 20, mặc định 10; thiếu `task` thì trả
  cả hai task, mỗi task một khối). Danh tính từ `CurrentUserProvider`.
- Nguồn: `lesson_writing_submissions` của user, `status = 'GRADED'`, `revision_no = 1` (chỉ lần nộp đầu), sắp
  `graded_at DESC`, giới hạn `limit` mỗi task. Mọi `source` (lesson, Practice, tự do), kể cả bài do EXAMINER chấm nếu
  có `result` cùng hình dạng.
- Response mỗi task:
  - `points[]`: `{submissionId, gradedAt, source, overallBand, criteria: {TA|TR, CC, LR, GRA}, promptVersion}`, sắp tăng
    theo thời gian.
  - `recurringErrors[]`: `{errorCode, errorName, fixHint, practiceKnowledgePointId, essaysWithError, essaysTotal,
    lastSeenAt}`, chỉ `certainty = ERROR` (thiếu thì coi là `ERROR`); sắp `essaysWithError` giảm rồi `severity` giảm; mã
    dự phòng cuối; tối đa 10.
  - `fixedErrors[]`: `{errorCode, errorName, lastSeenAt, cleanEssaysSince}`; mã có ở một bài và vắng ở N bài liền sau
    (N = 2, cấu hình `learning.writing.progress.fixed-after`); không tính mã dự phòng.
  - `insufficient`: `true` khi dưới 3 bài.
- Tên, `fixHint`, `severity`, KP: lấy từ JSON bài chấm; mã nào thiếu (bài cũ) thì gom lại và tra **một lần** qua
  `ErrorTypeTexts`; Content lỗi thì để trống tên, không lỗi cả request.
- Migration Learning mới: `CREATE INDEX ... ON lesson_writing_submissions (user_id, graded_at DESC) WHERE status = 'GRADED'`
  (kiểm số version lúc code).

## Related Code Files
(tương đối `services/learning-service/src/main/java/com/ieltspath/learning/`)
- Create: `api/controller/WritingProgressController.java` (hoặc thêm vào `WritingController` nếu gọn hơn),
  `api/dto/response/WritingProgressResponse.java`, `application/usecase/GetWritingProgressUseCase.java`,
  `application/service/WritingProgressCalculator.java` (thuần), port đọc trong `domain/repository` hoặc `application/port`
  theo cách `WritingSubmissionRepository` đang làm, migration index
- Modify: `infrastructure/persistence/JdbcWritingSubmissionRepository.java` (truy vấn mới)
- Docs: `docs/contracts/writing-progress-v1.md`; `docs/system-architecture.md` nếu có bảng endpoint
- Tests: calculator (đếm theo có/không, `SUGGESTION` bị loại, định nghĩa "đã khắc phục" ở biên, mã dự phòng, dưới 3 bài);
  Testcontainers cho truy vấn (chỉ bài của user, chỉ `GRADED`, chỉ lần nộp đầu, đúng giới hạn); `@WebMvcTest` cho
  controller (auth, tham số sai trả 400)

## Implementation Steps
1. Baseline `mvn -q -pl services/learning-service -am test`.
2. Chốt JSON mẫu với frontend (một học viên 1 bài, 5 bài, có đổi prompt v1 → v2).
3. Test đỏ calculator → code.
4. Test đỏ truy vấn và controller → code, migration index.
5. Viết contract, chạy lại module.

## Success Criteria
- [ ] Một request = một truy vấn DB và tối đa một lời gọi Content.
- [ ] Bài v1 (tối đa 10 lỗi) và v2 (mọi lỗi) không làm "lỗi hay gặp" nhảy vọt giả.
- [ ] Không trả bài của user khác; không trả `essayText` hay `chartFacts`.

## Risk Assessment
- JSON `result` của bài cũ thiếu trường: đọc an toàn, thiếu thì bỏ qua điểm đó chứ không lỗi request.
- Định nghĩa "đã khắc phục" là ước lượng: contract ghi rõ đây là tín hiệu, không phải đánh giá mastery.
