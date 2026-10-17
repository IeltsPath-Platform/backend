---
phase: 3
title: "Luyện tự do theo KP và cây của học viên"
status: in_progress
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

- [x] Học viên chọn KP bất kỳ của course mở và luyện được khi còn > 2 gói chưa lộ. (test: `FreePracticeIntegrationTest.freePracticeStopsAtTheReserve...`, `startingAgainReturnsTheOpenAttempt...`)
- [x] Luyện tự do trượt không sinh bài ôn chặn, không khóa lesson. (test: `FreePracticeIntegrationTest.aFreeSetIsRecordedAsFreeEvidence...`, không có `review_items`)
- [ ] Điểm course không đổi sau luyện tự do. Chưa có test điểm course; chỉ đảm bảo luyện tự do không ghi `lesson_id`.
- [x] Cây trả đúng tier cho mọi KP trong 2 query. (test: `theTreeShowsTheTierOfAPointTheLearnerHasEvidenceOn` cho KP có bằng chứng, `theTreeListsThePointsOfTheCourseWithFreePracticeOpen` cho KP chưa có; số lần gọi Content cố định, hồ sơ một query)

Đã làm sau khi Codex trả `band` và `requiredFeatureKey` trên `PracticeSet` (commit `76bb9aa`) và `cefrLevel`, `canDo` trên KP:
- Luyện tự do bỏ gói có `band` cao hơn band của course; bỏ gói có `requiredFeatureKey` nếu học viên không có gói đang hiệu lực.
  Access chỉ trả có/không có gói (`GET /api/access/me/subscription`), không trả từng tính năng, nên mọi gói đang hiệu lực
  được xem là đủ quyền; Access lỗi thì coi như không có gói. Chỉ hỏi Access một lần cho cả danh sách. Test: 5 ca trong
  `FreePracticeIntegrationTest` (band cao hơn, band bằng, cần gói không có gói, có gói, Access lỗi).
- Cây có `cefrLevel` và `canDo` (test: `theTreeCarriesTheCefrLevelAndCanDoContentGives`).
- Contract và AGENTS.md cho luyện tự do và cây: `docs/contracts/free-practice-v1.md`, AGENTS.md §3.8.

Còn mở:
- `practiceAvailable` và số gói chưa xem của cây chưa trừ các gói bị lọc theo band hoặc gói; cây có thể báo còn gói trong khi
  `POST free-practice` trả `FREE_PRACTICE_EXHAUSTED`.
- Chỉ lấy tối đa 5 gói ứng viên từ Content rồi mới lọc band/gói; nếu 5 gói đầu đều bị loại thì báo hết gói dù còn gói khác.
- Điểm course không đổi sau luyện tự do: chưa có test điểm course, chỉ đảm bảo luyện tự do không ghi `lesson_id`.
- Gói có essay bị loại lúc nộp (409 `FREE_PRACTICE_ESSAY_UNSUPPORTED`), không loại lúc chọn gói.

## Risk Assessment

- Kho gói mỏng: dự trữ giữ cho bài ôn; báo `CONTENT_GAP` nếu KP thường xuyên `FREE_PRACTICE_EXHAUSTED` (dùng work item có sẵn).

## Bàn giao FE

Màn cây theo course, tô màu theo tier; nút "Luyện" trên mỗi KP còn gói. Kết thúc phiên hiển thị độ chính xác và mastery
trước/sau.
