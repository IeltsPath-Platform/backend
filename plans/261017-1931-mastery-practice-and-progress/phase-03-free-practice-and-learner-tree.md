---
phase: 3
title: "Luyện tự do theo KP và cây của học viên"
status: pending
priority: P1
dependencies: [2]
---

# Phase 3: Luyện tự do theo KP và cây của học viên

## Overview

US-010 (03 §C.1): học viên bấm "Practice" trên một KID bất kỳ để luyện điểm yếu. Hiện chỉ có
`POST /api/learning/review-basket/{kpId}/start`, chỉ nhận KP `CONSOLIDATE` hoặc `MAINTAIN` đến hạn
(`NOT_IN_REVIEW_BASKET`). Tài liệu cũng có màn cây kiến thức tô màu theo trạng thái (02 §B.6).

## Requirements

- **Cây của học viên** `GET /api/learning/tree?courseId=` (`CUSTOMER`):
  - mỗi KP: `id`, `code`, `name`, `skill`, `kind`, `cefrLevel`, `mastery`, `tier`, `evidenceCount`, `nextReviewAt`,
    `prerequisiteIds`, `practiceAvailable` (còn gói luyện tự do được hay không);
  - một lần gọi content lấy cây và cạnh của course, một query profile theo tập KP; không gọi theo từng KP.
- **Bắt đầu luyện tự do** `POST /api/learning/knowledge-points/{kpId}/free-practice`:
  - KP phải `ACTIVE`, thuộc một course mà học viên vào được (mọi course đang mở; gói `PREMIUM` cần feature
    `PREMIUM_CONTENT` như Practice hiện tại);
  - chọn một gói `PRACTICE_SET` gắn KP, chưa lộ với học viên, không cao hơn band của course chứa KP;
  - **dự trữ cho bài ôn (M5):** chỉ giao khi số gói chưa lộ của KP lớn hơn `learning.free-practice.reserve` (mặc định
    2), ngược lại `409 FREE_PRACTICE_EXHAUSTED`;
  - KP đang có bài ôn chặn → `409 REVIEW_REQUIRED` kèm `reviewId` (làm bài ôn trước);
  - trả attempt như Practice (`/practice-attempts/{id}`), nộp bằng endpoint nộp Practice có sẵn.
- **Kết quả luyện tự do:**
  - ghi bằng chứng nguồn `free_practice` (MP-1.0 tính như luyện, trọng số 0.6); event log từng câu như Practice;
  - **không chặn gì**, không sinh bài ôn chặn, không tính vào điểm Process của course;
  - `explanation` có ngay sau khi nộp (như practice attempt, AGENTS.md §3.8);
  - gói đã nộp coi như đã lộ, không giao lại cho bài ôn.
- **Mã lỗi** thêm vào `GlobalExceptionHandler`: `FREE_PRACTICE_EXHAUSTED`, giữ `REVIEW_REQUIRED` có sẵn.
- Mọi lượt ghi trong một `@Transactional` mở đầu bằng khóa advisory theo user.

## Related Code Files

(`L` = `learning-service/src/main/java/com/ieltspath/learning`)

- Tạo: `L/application/usecase/GetLearnerTreeUseCase.java`, `L/application/usecase/StartFreePracticeUseCase.java`,
  `L/api/controller/LearnerTreeController.java`, DTO, mã lỗi
- Sửa: `StartPracticeAttemptUseCase` hoặc phần chọn gói dùng chung (`PackageRotation`), `SubmitPracticeAttemptUseCase`
  (nguồn `free_practice`, không sinh bài ôn chặn, không vào Process), `CourseScoreCalculator` (loại `free_practice`),
  migration nếu attempt cần cột `origin`
- Test: use case (dự trữ 2 gói; KP có bài ôn chặn; premium), `@WebMvcTest`, integration (luyện tự do không sinh bài ôn
  chặn, không đổi điểm course, gói đã nộp không được giao làm set ôn)

## Implementation Steps

1. Đọc cách Practice chọn gói và ghi kết quả; chốt cột phân biệt `origin = LESSON | FREE`.
2. **Đỏ trước:** test dự trữ gói và test không sinh bài ôn chặn.
3. Use case bắt đầu, nhánh nộp, mã lỗi.
4. Cây của học viên.
5. `mvn -q -pl services/learning-service -am test`.

## Success Criteria

- [ ] Học viên chọn KP bất kỳ của course mở và luyện được khi còn > 2 gói chưa lộ.
- [ ] Luyện tự do trượt không sinh bài ôn chặn, không khóa lesson.
- [ ] Điểm course không đổi sau luyện tự do.
- [ ] Cây trả đúng tier cho mọi KP trong 2 query (content + profile).

## Risk Assessment

- Kho gói mỏng: dự trữ giữ cho bài ôn; báo `CONTENT_GAP` nếu KP thường xuyên `FREE_PRACTICE_EXHAUSTED` (dùng work item có sẵn).

## Bàn giao FE

Màn cây theo course, tô màu theo tier; nút "Luyện" trên mỗi KP còn gói. Kết thúc phiên hiển thị độ chính xác và mastery
trước/sau.
