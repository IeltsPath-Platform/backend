---
phase: 5
title: "Assessment điểm và lời giải cho đề luyện"
status: pending
priority: P1
dependencies: [3]
---

# Phase 5: Assessment điểm và lời giải cho đề luyện

## Overview
Hoàn thiện kết quả của attempt `OFFICIAL_PRACTICE` (D1, Q3, Q4): ghi điểm thô và band theo skill khi chấm tự động, hiện lời giải ngay sau khi nộp, và
xác nhận Learning nhận event của loại attempt này mà không vào DLQ.

## Requirements
- Functional:
  - Chấm tự động (`AutoGradeAttemptService.grade`) cho attempt `OFFICIAL_PRACTICE` ghi thêm một dòng `skill_scores`
    `{skill, raw_score = tổng điểm, band, grading_source = AUTO}`. Hiện auto-grade **không** ghi `skill_scores` (chỉ người chấm mới ghi).
    - `skill` lấy từ `assessment_attempts.skill` (phase 3).
    - `band` chỉ tính khi **cả ba** đúng: `scope = FULL`, skill là `LISTENING` hoặc `READING`, và tổng điểm tối đa của attempt bằng 40; ngược lại `band = null`
      (part lẻ hoặc đề không đủ 40 câu chỉ có điểm thô). Không nội suy, không làm tròn kiểu khác.
    - `assessment_results.overall_band` **giữ null** cho đề luyện: contract `AssessmentCompleted.v2` định nghĩa `overall_band` là band của người chấm.
      Event hiện không mang `skill_scores` nên ghi thêm dòng này không đổi event.
  - Bảng quy đổi điểm thô → band là hàm thuần trong `domain/service` (cạnh `AnswerSpecGrader`, `PlacementBandCalculator`), không phải bảng DB, nhận bảng
    từ chủ dự án (Q4). **Chưa có bảng được xác nhận thì không viết bộ quy đổi**, chỉ ghi `raw_score` và `band = null`.
    Giá trị ứng viên để chủ dự án đối chiếu với nguồn chính thức (chưa xác minh, không dùng khi chưa duyệt):
    - Listening: 39-40→9; 37-38→8.5; 35-36→8; 32-34→7.5; 30-31→7; 26-29→6.5; 23-25→6; 18-22→5.5; 16-17→5; 13-15→4.5; 10-12→4; 8-9→3.5; 6-7→3; 4-5→2.5.
    - Reading Academic: 39-40→9; 37-38→8.5; 35-36→8; 33-34→7.5; 30-32→7; 27-29→6.5; 23-26→6; 19-22→5.5; 15-18→5; 13-14→4.5; 10-12→4; 8-9→3.5; 6-7→3; 4-5→2.5.
    Dưới 4 điểm thô: không có band (null).
  - Lời giải (Q3): `GetAssessmentResultUseCase` hiện chỉ trả `solutions` và `sectionSolutions` khi `percent >= SOLUTIONS_PERCENT` (70). Với attempt
    `OFFICIAL_PRACTICE` trả chúng **ngay sau khi nộp** bất kể điểm; mọi loại attempt khác giữ đúng quy tắc 70% (không đổi hành vi cũ).
  - Learning: attempt `OFFICIAL_PRACTICE` đã được parser whitelist và nằm trong `REVIEWED_TYPES`. Câu hỏi đề luyện có thể **không có** ánh xạ KP.
    Xác nhận event dựng bởi `AssessmentCompletedEventFactory` cho attempt `OFFICIAL_PRACTICE` không có KP vẫn qua được `AssessmentCompletedParser`
    của Learning (không vào DLQ). Nếu parser từ chối: **dừng và ghi `BLOCKED`**, không sửa Learning trong plan này.
- Non-functional: ghi `skill_scores` nằm trong cùng transaction với kết quả (hàm `grade` đã `MANDATORY`); không log đáp án, câu trả lời hay lời giải.

## Architecture
```
SubmitAssessmentAttemptUseCase ── AutoGradeAttemptService.grade
        ├─ item_results (như cũ)
        ├─ skill_scores (mới, chỉ OFFICIAL_PRACTICE) ── domain/service/RawScoreBandConverter (khi có bảng đã duyệt)
        └─ AssessmentResultCompleter.complete (outbox AssessmentCompleted.v2 như cũ)
GetAssessmentResultUseCase: điều kiện hiện lời giải = percent >= 70 || attemptType == OFFICIAL_PRACTICE
```

## Related Code Files
Gốc: `services/assessment-service/src/main/java/com/ieltspath/assessment/`
- Create (chỉ khi Q4 có bảng được duyệt): `domain/service/RawScoreBandConverter.java`
- Modify: `application/usecase/AutoGradeAttemptService.java`, `application/usecase/GetAssessmentResultUseCase.java`
- Tests (gốc `src/test/java/com/ieltspath/assessment/`): `application/usecase/AutoGradeAttemptServiceTest.java`,
  `application/usecase/GetAssessmentResultUseCaseTest.java`, `domain/service/RawScoreBandConverterTest.java` (khi có bộ quy đổi),
  `application/event/OfficialPracticeEventCompatibilityTest.java`; Learning: kiểm bằng test hiện có của `AssessmentCompletedParser`, chỉ **thêm** ca
  `OFFICIAL_PRACTICE` không KP nếu chưa có (không sửa code Learning)

## Implementation Steps
1. **Test đỏ**:
   - `AutoGradeAttemptServiceTest`: `OFFICIAL_PRACTICE` ghi đúng một `skill_scores` với tổng điểm thô; attempt `PART` và đề 30 câu có `band = null`; attempt loại khác (gate, mock) **không** có `skill_scores` mới;
     `overall_band` vẫn null.
   - `GetAssessmentResultUseCaseTest`: `OFFICIAL_PRACTICE` 20% có `solutions` và `sectionSolutions`; `TOPIC_GATE` 20% vẫn null; `TOPIC_GATE` 80% vẫn có.
   - `RawScoreBandConverterTest` (khi có bảng): mọi ngưỡng biên (39, 37, 4), dưới 4 → null, tổng tối đa khác 40 → null.
   - `OfficialPracticeEventCompatibilityTest`: dựng event cho attempt `OFFICIAL_PRACTICE` không KP, parse bằng cùng hợp đồng của Learning.
2. Cài `AutoGradeAttemptService` và `GetAssessmentResultUseCase`.
3. Nếu Q4 đã có bảng: thêm `RawScoreBandConverter` và nối vào auto-grade.
4. `mvn -q -pl services/assessment-service,services/learning-service -am test`.

## Success Criteria
- [ ] Test mới đỏ rồi xanh; test cũ về quy tắc 70% vẫn xanh nguyên văn (chỉ thêm ca mới, không đổi ca cũ).
- [ ] Attempt `OFFICIAL_PRACTICE` nộp xong có `skill_scores` đúng; band chỉ có khi đủ ba điều kiện và có bảng được duyệt.
- [ ] Event của attempt `OFFICIAL_PRACTICE` được Learning parse không lỗi.
- [ ] Loại attempt khác không đổi hành vi (không `skill_scores` mới, không đổi lời giải).

## Risk Assessment
- Hiện lời giải cho đề luyện là thay đổi hành vi **có chủ ý** của một loại attempt (Q3): chỉ áp cho `OFFICIAL_PRACTICE`, loại khác giữ nguyên; ghi rõ trong docs để frontend không giả định 70% cho thư viện đề.
- Band sai gây hại (học viên tin điểm giả): vì vậy mặc định là không tính band khi chưa có bảng được xác nhận.
- Learning có thể đánh giá lại bài ôn khi nhận kết quả đề luyện (`REVIEWED_TYPES`), nghĩa là làm đề luyện có thể tác động tiến độ bài ôn. Đây là hành vi đã có trong Learning, nhưng
  hiệu ứng với thư viện đề cần chủ dự án biết; ghi vào Verification kết quả quan sát thực tế.
- Rollback: chỉ thay đổi logic và dòng `skill_scores` mới; không đổi schema.
