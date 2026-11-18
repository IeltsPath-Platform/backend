---
phase: 3
title: "Giỏ ôn và nhất quán mastery"
status: pending
items: [8, 9, 10, 11]
---

# Phase 3: Giỏ ôn và nhất quán mastery

<!-- Updated: Red Team Session 1 - trigger_kind CHECK, định nghĩa DECAYED bằng cột mới, reserve + paywall cho start mới, badge, nơi gọi ReviewReevaluation -->

## Ngữ cảnh

- `application/usecase/GetReviewBasketUseCase.java`:
  - ứng viên = `CONSOLIDATE` ∪ `MAINTAIN` tới hạn ∪ KP có bài ôn tùy chọn (dòng 80–83);
  - `reason` = `triggerKind` của bài ôn tùy chọn, hoặc `DUE`/`CONSOLIDATE` (103);
  - sắp `due` theo `dueAt`, không xử lý null (118).
- `application/usecase/StartBasketReviewUseCase.java`:
  - từ chối `PRIORITY`, `INSUFFICIENT`, `MAINTAIN` chưa tới hạn (76);
  - cần bài học đã học dạy KP (81–82, `409 NO_LESSON_FOR_KNOWLEDGE_POINT`);
  - không còn set → `409 NO_UNREVEALED_SET` (92–95);
  - ghi `trigger_kind = profile.tier().name()` (99), nhưng CHECK chỉ cho `CONSOLIDATE`/`MAINTAIN` và các trigger khác
    (`V48__speaking_criterion_reviews.sql:4-6`).
- `application/usecase/StartFreePracticeUseCase.java`: mẫu kiểm paywall (82), mức dự trữ `unseen <= reserve` (94), lọc
  `requiredFeatureKey` (101–106).
- `application/usecase/GetReviewBasketSummaryUseCase.java:40`: badge = `required` + `profiles.countDue` (chỉ `MAINTAIN` tới
  hạn, `JdbcMasteryProfileRepository.java:64-65`).
- `MasteryProfile.evidenceCount` là **tổng** số dòng `kp_evidence` của KP (`JdbcKnowledgeEvidenceRepository.java:125`), không
  phải cửa sổ MP1. `confidence` được job decay cập nhật (`MasteryProfileUpdater.java:109-111,151`).
- `ReviewReevaluation` chỉ có **một** nơi gọi: `ApplyAssessmentResultUseCase.java:164`, chạy sau `recordEvidence` (153).

## Migration

`V57__mastery_decayed_from_tier.sql`: cột `kp_mastery_profiles.decayed_from_tier VARCHAR(20)` null, và chỉ mục phục vụ đếm
`due`. (CHECK `trigger_kind` thêm `PRIORITY`, `DECAYED` nằm ở V56, phase 2.)

## Mục 8: KP `PRIORITY` không còn bài ôn

**Trước:** KP `PRIORITY` chỉ được nhắc qua bài ôn bắt buộc. Bài ôn `SKIPPED` (hết gói) hoặc `DONE` mà điểm vẫn < 0.6 thì KP
không hiện ở đâu; `StartBasketReview` trả `409 NOT_IN_REVIEW_BASKET`.

**Sau:**

- `GetReviewBasketUseCase`: ứng viên thêm KP `tier == PRIORITY` không có bài ôn đang mở, **chỉ khi bắt đầu được**:
  - có bài học đã học dạy KP (`lessonLocator.teaching`);
  - số set chưa xem > `learning.free-practice.reserve` (dùng `availability` đã tính ở dòng 128–136).
  
  Xếp vào `weak`, `reason = "PRIORITY"`.
- `StartBasketReviewUseCase` cho phép `PRIORITY`. Riêng hai loại mới (`PRIORITY`, `DECAYED`):
  - kiểm paywall KP và lọc set có `requiredFeatureKey` khi học viên không có quyền, giống `StartFreePracticeUseCase`;
  - từ chối khi số set chưa xem ≤ reserve (`409 NO_UNREVEALED_SET`);
  - ghi `trigger_kind = 'PRIORITY'`.
  
  Các lần bắt đầu `CONSOLIDATE`/`MAINTAIN` giữ nguyên (chọn lọc premium chỉ ở đường mới; người dùng chốt).
- Bài ôn tùy chọn này chỉ được nâng thành bắt buộc khi đề thi đánh giá lại (`ReviewReevaluation`) hoặc chẩn đoán kết luận
  KP hổng. Practice **không** nâng nó (`promoteSuggestionsToBlocking` chỉ nhận `PRACTICE`, `ERROR_PATTERN`,
  `JdbcReviewItemRepository.java:235`).
- Bài ôn bắt buộc đã hạ cấp (phase 2, mục 4) hiện ở `weak` với `reason = "UNBLOCKED"` (nhận ra qua `unblocked_at`), thay vì
  `triggerKind`.

## Mục 9: KP bị decay

**Trước:** KP `MAINTAIN` lâu không làm → confidence giảm theo half-life 30 ngày → dưới `minConfidence` → `INSUFFICIENT`,
`nextReviewAt = null` → không bao giờ "due". Ví dụ: 3 câu luyện tập trọng số 0.6 → tổng 1.8, sau 60 ngày còn 0.45 < 1.0.

**Sau:**

- **Định nghĩa bằng dữ liệu, không suy từ `evidenceCount`:** `MasteryProfileUpdater` ở chế độ DECAY ghi
  `decayed_from_tier = <tier cũ>` khi job chuyển một KP từ tier đủ dữ liệu (`CONSOLIDATE`/`MAINTAIN`) sang `INSUFFICIENT`.
  Xóa về null khi KP đủ dữ liệu trở lại. KP mới có ít câu, hay nhiều câu trọng số thấp, không bao giờ có cột này.
- `GetReviewBasketUseCase`: KP có `decayed_from_tier` vào nhóm `due`, `reason = "DECAYED"`. **Khác bản đầu:** nhóm này không
  bị lọc theo "bắt đầu được" như `PRIORITY`; nó hiện như các mục `due` khác, nút "Bắt đầu ôn" tắt kèm ghi chú khi hết bộ đề
  (`hasUnrevealedSet = false`), và lúc start vẫn kiểm bài học, paywall, reserve rồi trả 409. Nhờ vậy badge `due` và nhóm `due`
  đếm cùng một tập. Tối đa **10** KP decay mỗi lần, ưu tiên KP có bằng chứng cuối cũ nhất. Sắp `due` bằng
  `Comparator.nullsFirst` (KP decay lên đầu). <!-- Updated: Validation Session 1 - giới hạn 10 KP DECAYED -->
- Chỉ ghi `decayed_from_tier` từ lúc deploy; không backfill KP đã decay trước đó (người dùng chốt).
  <!-- Updated: Validation Session 1 - không backfill -->
- `StartBasketReviewUseCase`: cho phép KP decay, với cùng kiểm paywall, `requiredFeatureKey` và reserve như mục 8.
  Ghi `trigger_kind = 'DECAYED'`.
- `countDue` (`JdbcMasteryProfileRepository.java:64-65`) đếm thêm KP decay đủ điều kiện (tối đa 10), để badge khớp nhóm `due`.

## Mục 10: một công thức mastery

**Trước:**

| Nơi | Công thức |
|-----|-----------|
| Tier, lịch ôn, giỏ ôn (`kp_mastery_profiles`) | MP1: 8 câu, trọng số nguồn, decay 30 ngày |
| `GET /api/learning/mastery` | DeepTutor: 5 câu gần nhất, không trọng số |
| `ReviewReevaluation` (quyết định tạo bài ôn bắt buộc sau đề thi) | DeepTutor |

**Sau (đã chốt, thu hẹp):** chỉ `ReviewReevaluation` đọc `profile.mastery()` từ `kp_mastery_profiles`. Hồ sơ tính theo
`learning.mastery.policy` đang cấu hình, nên đổi policy về DeepTutor vẫn đúng. Không cần chạy lại hồ sơ.

- `ReviewReevaluation`: thay `masteryCalculator.compute(history.correctness())` bằng điểm hồ sơ; vẫn dùng
  `evidence.findMasteryHistories` cho skill và cờ Practice. KP chưa có hồ sơ có điểm 0 (không có bằng chứng). Dùng `mastery`
  kể cả khi tier `INSUFFICIENT`, để giữ hành vi "sai ở đề thi → có bài ôn" với KP mới. Thứ tự ghi hồ sơ trước đã đúng
  (`ApplyAssessmentResultUseCase.java:153` trước `:164`), không cần đổi.
- `GetMasteryUseCase` **giữ nguyên**: `GetMasteryUseCaseTest` ghi SRS FT-26 (5 kết quả gần nhất), `learner-report-v1.md:65`
  ghi `GET /api/learning/mastery is unchanged`, và frontend không gọi endpoint này.

## Mục 11: luyện tự do và bài ôn

**Trước:** `FreePracticeSubmission:120` gọi `errorTrapEvidence.createReviews(userId, null, firstAnswers)`; javadoc của lớp và
`docs/contracts/free-practice-v1.md` nói "không tạo bài ôn". Nhưng lời gọi này tạo bài ôn **tùy chọn** `ERROR_PATTERN` khi KP
bẫy lỗi vào `PRIORITY`, và `FreePracticeIntegrationTest:181` kiểm đúng điều đó.

**Sau (đã chốt, đổi so với bản đầu):** giữ code. Sửa javadoc và contract thành "luyện tự do không tạo bài ôn chặn; chỉ có thể
thêm bài ôn tùy chọn `ERROR_PATTERN`".

## File sửa

- `application/usecase/GetReviewBasketUseCase.java`, `StartBasketReviewUseCase.java`, `GetReviewBasketSummaryUseCase.java`
- `application/service/ReviewReevaluation.java`, `MasteryProfileUpdater.java`; `FreePracticeSubmission.java` (chỉ javadoc)
- `docs/contracts/free-practice-v1.md`
- `domain/aggregate/MasteryProfile.java` (trường `decayedFromTier`)
- `infrastructure/persistence/JdbcMasteryProfileRepository.java`
- `src/main/resources/db/migration/V57__mastery_decayed_from_tier.sql`

## Test

- Bài ôn bắt buộc `SKIPPED`, KP còn `PRIORITY`, có bài học dạy KP, còn set > reserve → giỏ ôn có KP ở `weak`, lý do
  `PRIORITY`; start tạo bài ôn tùy chọn `trigger_kind = PRIORITY` (Testcontainers, để bắt lỗi CHECK).
- KP `PRIORITY` hết set hoặc chưa học bài nào dạy nó → không hiện trong giỏ ôn và badge.
- Học viên miễn phí start KP `PRIORITY`/`DECAYED` → chỉ nhận set không có `requiredFeatureKey`, hoặc 409.
- Start `PRIORITY` lặp lại tới khi chỉ còn reserve → `409`, reserve còn nguyên cho bài ôn bắt buộc.
- Job decay chuyển KP `MAINTAIN` → `INSUFFICIENT` → `decayed_from_tier = MAINTAIN` → giỏ ôn có KP ở `due`, lý do `DECAYED`,
  không NPE khi sắp; start được với `trigger_kind = DECAYED`.
- KP mới với 3 câu bẫy lỗi (confidence 0.9) → không hiện `DECAYED`.
- 15 KP decay → giỏ ôn hiện 10 KP có bằng chứng cuối cũ nhất; badge `due` đếm đúng 10.
- Badge `due` (`/review-basket/summary`) = số mục nhóm `due` của giỏ ôn.
- Bài ôn đã hạ cấp → `weak`, lý do `UNBLOCKED`.
- `ReviewReevaluation`: cùng bằng chứng (3 câu sai gần nhất), điểm hồ sơ 0.65 (trên ngưỡng) → không có bài ôn; hạ điểm hồ sơ
  xuống 0.2 → một bài ôn bắt buộc. Test `aReviewIsRaisedFromTheStoredMasteryScoreNotFromTheLatestAnswersAlone`.
- Nộp luyện tự do sai câu bẫy → có đúng một bài ôn tùy chọn `ERROR_PATTERN`, không có bài ôn chặn
  (`FreePracticeIntegrationTest`, giữ nguyên).

## Rủi ro

- Mục 9 làm tăng số mục `due` của học viên học lâu; đã giới hạn 10 KP `DECAYED` mỗi lần.
- Mục 10 đổi KP nào nhận bài ôn bắt buộc sau đề thi. Ghi trong mô tả PR.
- KP đã decay trước khi deploy không có `decayed_from_tier` và không hiện là `DECAYED`; chỉ hiện lại khi học viên làm bài
  có KP đó. Chấp nhận (người dùng chốt, không backfill).
