---
phase: 5
title: Learning review selection and trap reviews
status: completed
priority: P1
effort: 1.5d
dependencies:
  - 4
---

# Phase 5: Learning review selection and trap reviews

## Overview
**Đơn vị ôn luôn là KP cha.** KP con chỉ quyết định ôn bằng bộ nào (bẫy của lỗi chính). Thêm trigger `ERROR_PATTERN`: KP
con yếu khi KP cha chưa có bài ôn → bài ôn tùy chọn **của KP cha**. Bài ôn KP cha có lỗi chính trả tên lỗi + `fixHint`.

## Requirements
1. **Lỗi chính** của KP cha P = mã của KP con yếu nhất của P: trong các KP con có `parent_kp_id = P` và hồ sơ tier khác
   `INSUFFICIENT`, chọn mastery thấp nhất; hòa → mã nhỏ hơn theo chữ cái. Không có → không có lỗi chính.
   Đọc KP con từ danh mục (phase 4), hồ sơ qua `findErrorTrapProfiles`, không gọi HTTP. <!-- red-team PH3 -->
2. **Chọn bộ** cho mọi bài ôn KP cha: truyền `trapErrorCode` = lỗi chính khi tìm bộ. Hai chỗ gọi:
   `GetReviewUseCase.nextPackage` (dòng ~151, giao bộ cho bài ôn chặn và tùy chọn) và `StartBasketReviewUseCase` (dòng ~52,
   kiểm còn bộ). Lỗi chính tính lại mỗi lần, không lưu vào bài ôn. Luyện tự do (`StartFreePracticeUseCase`) và chẩn đoán
   (`DiagnosticFlow`) **không** truyền.
3. **Trigger `ERROR_PATTERN`** — `ErrorTrapReviews.create(userId, submissionLessonId, trapKpsWithNewEvidence)`, gọi ngay sau
   `masteryProfiles.recordEvidence(...)` ở 5 đường chấm (mẫu: `SubmitPracticeAttemptUseCase.java:217` → `criterionReviews.create`
   ở dòng 221). Thứ tự bắt buộc: ghi bằng chứng → `recordEvidence` → `ErrorTrapReviews.create` (đọc tier sau cập nhật). <!-- red-team PH5 -->

   ```text
   created = 0
   pendingKps = { review.knowledgePointId | review ∈ reviews.findPending(userId) }   // một lần
   for trapKp in trapKpsWithNewEvidence (sắp theo mastery tăng dần, rồi mã):
       if created == limit: break                                  // learning.error-trap.review-limit, mặc định 1
       profile = hồ sơ trapKp; if profile.tier != PRIORITY: continue
       parent = parentByTrapKp[trapKp]; if parent ∈ pendingKps: continue
       lessonId = submissionLessonId nếu lesson đó dạy parent, ngược lại CompletedLessonLocator.teaching(userId, parent)
       if lessonId rỗng: continue
       set = content.searchPracticeSets(parent, excluded, 3, 1, lessonId, errorCodeOf(trapKp))   // excluded như nextPackage
       if set rỗng hoặc set.trapQuestionCount < 2: continue
       insertPending(userId, [ReviewCandidate(parent, lessonId, skillOf(parent))], false, "ERROR_PATTERN")
       pendingKps.add(parent); created++
   ```

   Lỗi gọi content trong bước này → bỏ qua tạo bài ôn, ghi log id + mã lỗi, **không** làm hỏng lần nộp.
4. **Giỏ ôn:** sau phase 4 `profiles.findAll` đã loại KP con nên giỏ ôn chỉ có KP cha. `StartBasketReviewUseCase` với KP con
   (danh mục có `parent_kp_id`) → `409 NOT_IN_REVIEW_BASKET`. Bài ôn `ERROR_PATTERN` hiện theo cơ chế sẵn có
   (`GetReviewBasketUseCase.java:91`: nhóm `weak`, `reason = triggerKind`).
5. **`errorTrap` khi mở bài ôn KP cha** có lỗi chính: `{code, name, fixHint}`, cả khi bài ôn quay lại lý thuyết ở lần trượt
   đầu. `name`/`fixHint` lấy qua `ErrorTypeTexts` (mới, dùng lại ở phase 6): bọc `LearningContentClient.activeErrorTypes()`,
   gọi tối đa một lần mỗi request, lỗi → trả `Map.of()` và response không có `errorTrap`.

## Architecture
- `learning/domain/service/MainErrorRule.java` (thuần):
  `static Optional<String> mainErrorCode(UUID parentKp, Collection<MasteryProfile> trapProfiles, ErrorTraps traps)`.
- `learning/application/service/ErrorTrapReviews.java` (khuôn `WritingCriterionReviews`, `@Value("${learning.error-trap.review-limit:1}")`).
- `learning/application/service/ErrorTypeTexts.java`: `Map<String, Text> byCode()`, `record Text(String name, String fixHint)`.
- `LearningContentClient.searchPracticeSets` thêm overload có `trapErrorCode`; record `PracticeSet` thêm `trapQuestionCount`.

## Related Code Files
- Create: `MainErrorRule.java`, `ErrorTrapReviews.java`, `ErrorTypeTexts.java` (+ test)
- Modify: `GetReviewUseCase.java` (`nextPackage`, response `errorTrap`), `StartBasketReviewUseCase.java`, 5 đường chấm (gọi
  `ErrorTrapReviews` sau `recordEvidence`), `LearningContentClient.java`, `RestLearningContentClient.java`, result/DTO bài ôn
- Modify: `infra/config-server/config-repo/learning-service.yaml` (`learning.error-trap.review-limit: 1`)
- Modify: `docs/contracts/lesson-learning-v1.md` (trigger `ERROR_PATTERN`, `errorTrap`)

## Implementation Steps
1. Baseline test learning.
2. **Đỏ trước:**
   - `MainErrorRuleTest`: không KP con; mọi KP con `INSUFFICIENT`; hai KP con → chọn mastery thấp hơn; hòa → mã nhỏ hơn.
   - `ErrorTrapReviewsTest` (Mockito): mỗi điều kiện chặn (không `PRIORITY`, KP cha có bài ôn chờ, không lesson, không bộ,
     bộ chỉ 1 câu bẫy, vượt giới hạn) → không tạo; đủ → `insertPending(..., false, "ERROR_PATTERN")` cho **KP cha**;
     content ném lỗi → không tạo, không ném.
   - Testcontainers (tái hiện ví dụ):
     - Lan (KP con NG_AS_FALSE yếu nhất) và Minh (FALSE_AS_NG) với PS-A/PS-B/PS-C: bộ ôn KP cha đầu tiên lần lượt PS-A và PS-B.
     - KP cha 85%, KP con xuống `PRIORITY` → một bài ôn `ERROR_PATTERN` của KP cha, bộ giao đầu là bộ nhiều câu bẫy.
     - KP cha đang có bài ôn → không tạo; KP cha ôn xong, lần nộp sau KP con vẫn `PRIORITY` → tạo; lần nộp kế → không thêm.
     - Lần nộp làm KP con từ 65% xuống 55% → tạo ngay trong lần nộp đó.
     - Luyện tự do khi chưa học lesson của KP cha → không tạo.
     - `StartBasketReviewUseCase` với KP con → 409; giỏ ôn không có KP con.
     - Bài ôn KP cha có lỗi chính trả `errorTrap`; content lỗi khi lấy tên lỗi → bài ôn vẫn mở, không có `errorTrap`.
     - KP thường không có KP con: thứ tự bộ, giỏ ôn, bài ôn như cũ (test cũ xanh).
3. Cài đặt: `MainErrorRule` → client/`PracticeSet` → `nextPackage`/`StartBasketReviewUseCase` → `ErrorTypeTexts` →
   `ErrorTrapReviews` + gọi ở 5 đường chấm → response.
4. `mvn -q -pl services/learning-service -am test`.

## Success Criteria
- [ ] Mọi kịch bản ở bước 2 xanh.
- [ ] Không có bài ôn nào mang KP là KP con.
- [ ] Contract `lesson-learning-v1` cập nhật.

## Risk Assessment
- Lý thuyết lesson KP cha có thể không nói về bẫy → `errorTrap.fixHint` bù (nội dung nháp chờ duyệt).
- Mã lỗi trỏ KP cha khác KP của câu (ví dụ `KEYWORD_MATCH_ONLY` → `TR_R_SCANNING`): bài ôn là của KP cha đó; không có bộ
  ≥ 2 câu bẫy thì không tạo.
