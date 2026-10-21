---
phase: 3
title: "Learning: ghi bằng chứng KP và mã lỗi cho essay lesson và Practice"
status: pending
priority: P1
effort: "1d"
dependencies: [2]
---

# Phase 3: Learning: ghi bằng chứng KP và mã lỗi cho essay lesson và Practice

## Overview

Thay chỗ ghi "một kết quả đạt/chưa đạt cho KP của đề" bằng "một kết quả cho mỗi tiêu chí", ở cả hai đường: essay lesson và essay Practice. Quy tắc nằm trong một domain service thuần để hai use case dùng chung và test được không cần DB.

## Requirements

- Áp quyết định D2 đến D6 trong `plan.md`.
- Không đổi: bài nào được tính là bằng chứng (`firstAttempt` của essay lesson; `counted` của bộ Practice), trọng số nguồn, khóa idempotent theo `(submission, kp)` / `(requestId, question, kp)`, `reviewNeeds` (Writing vẫn không sinh bài ôn), XP/streak/nhiệm vụ.
- Bài chưa có `knowledgePointId` (bài cũ) rơi về hành vi cũ: `passed` cho KP của đề.
- Không log nội dung bài viết, prompt hay `corrections`; chỉ id và mã lỗi.

## Architecture

```
WritingEvidenceRule (domain/service, thuần)
  vào : passBand, overallPassed, criteria[(code, band, kpId)], promptKpIds, corrections[errorCode]
  ra  : outcomes: Map<kpId, correct>   // tiêu chí thay passed với cùng KP; KP của đề ngoài ánh xạ giữ passed
        failedCriterionKps: Set<kpId>
        dominantErrorCode: String|null // nhiều nhất, hòa theo chữ cái, bỏ mã trống

Essay lesson : SubmitLessonEssayUseCase.finish()            -> event + KnowledgeEvidence(LESSON_WRITING)
Essay Practice: SubmitPracticeAttemptUseCase.execute() loop -> event + KnowledgeEvidence(PRACTICE_SET)
```

Event của essay vẫn là 1 dòng (khóa unique `user, source, source_reference_id, question_version_id`): `correct = passed`; `knowledge_point_ids = KP của đề + failedCriterionKps`; `error_code = dominantErrorCode`.

## Related Code Files

- Create: `services/learning-service/.../domain/service/WritingEvidenceRule.java` (+ record kết quả)
- Modify: `.../application/usecase/SubmitLessonEssayUseCase.java` (thay đoạn dựng event và `essayEvidence` trong `finish`, giữ `firstAttempt` và bước lọc `access.knownKnowledgePoints`)
- Modify: `.../application/usecase/SubmitPracticeAttemptUseCase.java`:
  - `gradedEssays` trả thêm dữ liệu chấm (`Map<UUID, EssayResult>` gồm `passed` và `WritingEvidenceRule` output) thay vì chỉ `Boolean`; chỗ gọi `ItemGrading.grade(...)` vẫn nhận `Map<UUID,Boolean>` suy ra từ đó
  - Trong vòng lặp item: với item essay, giữ `kps` hiện có cho `PracticeReviewRule.ItemOutcome` (để `reviewNeeds` không đổi), nhưng đổi `QuestionAttemptEvent` (KP + `errorCode`) và `firstAnswers` (bằng chứng theo `outcomes`) sang kết quả của rule
- Không đổi: `ItemGrading`, `EssayGrader`, `MasteryProfileUpdater`, `Mp1MasteryCalculator`, `LessonEvidenceReference`

## Implementation Steps

1. Viết `WritingEvidenceRule` và test đơn vị trước (xem phase 5): đạt/không đạt từng tiêu chí; ngưỡng bằng `passBand` đúng; hai tiêu chí cùng KP -> AND; KP của đề trùng KP tiêu chí -> kết quả tiêu chí thắng; KP của đề ngoài ánh xạ -> `passed`; `knowledgePointId == null` -> bỏ tiêu chí đó; mã lỗi hòa, trống, không có `corrections`.
2. Sửa `SubmitLessonEssayUseCase.finish()`: lấy `submission.grade().criteria()` và `submission.prompt()`; gọi rule; dựng event và bằng chứng; lọc KP không thuộc danh mục học viên bằng `access.knownKnowledgePoints` như hiện nay (cảnh báo log chỉ gồm id).
3. Sửa `SubmitPracticeAttemptUseCase`: mở rộng `gradedEssays`; áp rule cho từng item essay; dùng `LessonEvidenceReference.forPracticeSet(requestId, questionVersionId, kpId)` cho từng KP tiêu chí; áp bộ lọc KP đã biết giống đường lesson (kiểm tra `access` có cung cấp `knownKnowledgePoints` ở đây, nếu không thì dùng cùng thành phần đã inject).
4. Kiểm chứng điểm bất định đầu tiên: bằng chứng gửi vào KP `INACTIVE` hoặc KP không có trong danh mục học viên có bị loại không. Đọc `JdbcKnowledgeEvidenceRepository.histories`, `RefreshLearningTopicsUseCase` và `LessonAccess.catalog`; ghi kết quả vào mô tả test. Nếu 5 KP của D1 không vào được danh mục, báo lại để đổi ánh xạ trước khi viết tiếp.
5. Xác nhận khóa idempotent của `kp_evidence` cho phép nhiều dòng cùng `submission` khác `kp` (`forWriting` đã gồm `kpId`).

## Success Criteria

- [ ] Essay Task 1 chấm xong -> 4 bằng chứng (TA/CC/LR/GRA) vào 4 KP, đúng/sai theo `band >= passBand`; Task 2 dùng TR.
- [ ] Event của essay có `error_code` = mã gặp nhiều nhất; `null` khi không có `corrections`; vẫn đúng 1 dòng mỗi essay.
- [ ] Essay lesson: chỉ các bài nộp trước khi block đạt tạo bằng chứng (không đổi); Practice: chỉ khi `counted` (không đổi).
- [ ] Bài chưa có `knowledgePointId` -> ghi `passed` cho KP của đề như cũ.
- [ ] `reviewNeeds` và kết quả `PracticeSubmission` (đạt/không đạt, review tạo ra) không đổi trên bộ test hiện có.
- [ ] Thống kê lỗi theo KP (`statsByKnowledgePoint`) trả mã lỗi essay cho KP tiêu chí đã sai.

## Risk Assessment

- KP tiêu chí không vào danh mục học viên: bằng chứng im lặng bị bỏ. Giảm bằng bước 4 và test với dữ liệu seed thật.
- Mã lỗi đơn lẻ lan sang mọi KP trong mảng của event: chấp nhận ở phiên bản này; nếu nhiễu, chuyển sang ánh xạ theo `category` của mã lỗi (câu hỏi mở 2 trong `plan.md`).
- XP mốc ("rises") cho KP Practice: essay Practice vốn là nguồn `PRACTICE_SET` nên các KP tiêu chí mới cũng có thể nhận XP mốc như KP của đề trước đây; đây là hành vi nhất quán, nhưng cần xác nhận trong review.
- Transaction: không gọi HTTP mới bên trong `finish()` hay `execute()`; dữ liệu KP đã nằm trong bài chấm.
- Rollback: tắt bằng cách để Content không trả `knowledgePointId` (tất cả `null`) -> toàn bộ rơi về hành vi cũ.
