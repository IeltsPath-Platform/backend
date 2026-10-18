---
phase: 5
title: "Assessment điểm và lời giải"
status: pending
priority: P1
effort: "1d"
dependencies: [3]
---

# Phase 5: Assessment điểm và lời giải

## Overview
Kết quả attempt `OFFICIAL_PRACTICE`: ghi điểm thô theo skill khi chấm tự động, hiện lời giải ngay sau khi nộp, và đảm bảo Learning nhận event loại này mà không vào DLQ.

**Thiết kế gốc (đọc trước, làm theo): [`../261014-1002-practice-test-library/phase-05-assessment-practice-results.md`](../261014-1002-practice-test-library/phase-05-assessment-practice-results.md).**

## Lệch so với phase thiết kế
- **Không có `RawScoreBandConverter`**, không bảng quy đổi (Q4 chưa được duyệt). `skill_scores` ghi `{skill, raw_score, band = null, grading_source = AUTO}` **luôn** `band = null`.
  Đề demo có 31 câu chia nhỏ, không có đề 40 câu, nên cũng không có ca nào đáng tính band.
- `assessment_results.overall_band` giữ null cho đề luyện (event `AssessmentCompleted.v2` không đổi).
- Lời giải (Q3): `GetAssessmentResultUseCase` trả `solutions`/`sectionSolutions` ngay khi nộp nếu `attemptType = OFFICIAL_PRACTICE`; các loại khác giữ nguyên quy tắc 70%.
  Test cũ về ngưỡng 70% phải xanh nguyên văn.
- Event của attempt `OFFICIAL_PRACTICE` **không có KP** vì câu hỏi đề luyện không ánh xạ KP (đã kiểm `V30`: không có `question_knowledge_points`). Kiểm bằng
  `OfficialPracticeEventCompatibilityTest` rằng `AssessmentCompletedParser` của Learning nhận được. Nếu Learning từ chối: `BLOCKED`, không sửa Learning trong plan này.
- Test thêm: nộp đủ 5 câu đúng Passage 2 → `raw_score = 5`; nộp 1/5 → `raw_score = 1` và vẫn có lời giải; attempt `TOPIC_GATE` không phát sinh `skill_scores` mới.

## Related Code Files
Như phase thiết kế, trừ `RawScoreBandConverter` và test của nó (không tạo).

## Implementation Steps
1. Test đỏ (`AutoGradeAttemptServiceTest`, `GetAssessmentResultUseCaseTest`, `OfficialPracticeEventCompatibilityTest`).
2. `AutoGradeAttemptService` ghi `skill_scores` cho `OFFICIAL_PRACTICE`; `GetAssessmentResultUseCase` mở lời giải.
3. `mvn -q -pl services/assessment-service,services/learning-service -am test`.

## Success Criteria
- [ ] Test mới đỏ rồi xanh; ca cũ về 70% xanh nguyên văn.
- [ ] `skill_scores` đúng cho `OFFICIAL_PRACTICE`, `band` luôn null; loại khác không đổi.
- [ ] Event được Learning parse không lỗi.
- [ ] Không log đáp án, câu trả lời hay lời giải.

## Risk Assessment
Như phase thiết kế. Ghi vào Verification quan sát thực tế: nộp đề luyện có làm Learning đánh giá lại bài ôn không (`REVIEWED_TYPES`), vì chủ dự án cần biết hiệu ứng này.
