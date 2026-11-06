---
title: "Examiner Writing review drives mastery and reviews"
description: "Kết quả examiner chấm Writing (band tiêu chí + mã lỗi) ghi evidence, cập nhật mastery và tạo bài ôn không chặn, như luồng LLM"
status: pending
priority: P2
branch: "feat/examiner-review-mastery"
tags: [learning-service, assessment-service, content-service, frontend, writing, mastery, review, tdd]
blockedBy: []
blocks: []
created: "2026-11-04T17:18:59.674Z"
createdBy: "ck:plan"
source: skill
---

# Examiner Writing review drives mastery and reviews

Nguồn: [brainstorm report](../reports/brainstorm-261105-0015-examiner-review-mastery-report.md). Khi lệch, **plan này đúng**.
Chế độ `--tdd`: mỗi phase viết test trước, chạy thấy đỏ đúng lý do, rồi mới code.

## Overview

Hôm nay examiner chấm xong chỉ là feedback (`human-review-v1.md`: "no evidence, no reviews"). Plan này cho Learning dùng kết
quả examiner theo đúng đường của luồng LLM và luồng bẫy mã lỗi:

1. Assessment ghi event mới `assessment.human-review-completed.v1` (band tiêu chí + mã lỗi, không có văn bản) cùng transaction với lúc lưu bài chấm.
2. Learning nghe event, rồi:
   - band tiêu chí → evidence nguồn mới `human_review` cho KP tiêu chí, **thay** evidence AI của chính bài đó;
   - mã lỗi → evidence `error_trap` cho KP con bẫy (`TRAP_<mã>`);
   - tạo bài ôn không chặn: `ERROR_PATTERN` (bộ luyện có câu bẫy của mã) và `WRITING_CRITERION` (tiêu chí trượt), tổng tối đa 2;
   - lưu "điểm cần ôn" để trang kết quả hiển thị.
3. Content gắn các mã lỗi ngữ pháp chưa có KP (`ARTICLE`, `SUBJECT_VERB_AGREEMENT`…) vào KP V84 và tạo KP con bẫy — **chỉ sau khi chuyên gia duyệt mapping**.
4. Frontend hiện khối "Cần ôn" trên trang kết quả examiner.

## Phases

| Phase | Name | Repo | Depends on | Status |
|-------|------|------|------------|--------|
| 1 | [Shared event contract](./phase-01-shared-event-contract.md) | backend (common-events, docs) | - | Pending |
| 2 | [Assessment publishes the review result](./phase-02-assessment-publishes-the-review-result.md) | backend | 1 | Pending |
| 3 | [Content maps grammar error codes](./phase-03-content-maps-grammar-error-codes.md) | backend | expert approval | Pending |
| 4 | [Learning evidence model and rule](./phase-04-learning-evidence-model-and-rule.md) | backend | - | Pending |
| 5 | [Learning applies the result and creates reviews](./phase-05-learning-applies-the-result-and-creates-reviews.md) | backend | 1, 4 | Pending |
| 6 | [Frontend study points on the examiner result](./phase-06-frontend-study-points-on-the-examiner-result.md) | frontend | 5 (JSON chốt ở phase 5) | Pending |
| 7 | [Docs and full verification](./phase-07-docs-and-full-verification.md) | backend + frontend | 1-6 | Pending |

Thứ tự: 1 → 2; 4 → 5 (5 cần 1); 6 sau khi 5 chốt JSON. Phase 3 độc lập, **chặn bởi duyệt nội dung**, không chặn phase khác:
code phase 5 đúng cả khi mapping chưa có (mã không có KP con thì bỏ qua). Phase 2 và 5 deploy cùng lúc là an toàn theo cả hai
thứ tự: event chưa có người nghe thì nằm trong queue (topology khai báo ở cả hai service).

## Quyết định đã chốt (không đảo; muốn đổi phải hỏi người dùng)

1. Tín hiệu: **band 4 tiêu chí** + **mã lỗi** examiner đánh dấu. Examiner không chọn KP con trực tiếp (để sau).
2. Event **mới**, không sửa `grading-completed.v1` (Notification từ chối event sai contract). Không chở essay, comment, excerpt, suggestion,
   `source`, `task` (Learning lấy `source`/`task` từ bài viết).
3. Tiêu chí: map `taskResponse`→`TR` (TASK_2) / `TA` (TASK_1), `coherence`→`CC`, `lexical`→`LR`, `grammar`→`GRA`; KP tiêu chí lấy từ rubric Content;
   đúng khi band ≥ `passBand` của đề. Không có `passBand` hoặc tiêu chí không map được → bỏ phần đó, không lỗi.
4. Nguồn evidence mới `human_review`, trọng số cấu hình `learning.mastery.mp1.source-weights.human-review`, mặc định **0.75**
   (cao hơn AI 0.5, thấp hơn bài thi 1.0). Chỉ cấu hình qua file/Config Server; không thêm khóa `learning_rule_configs`. <!-- Updated: Validation Session 1 - weight 0.75 -->
4a. Bài ôn theo mã lỗi tạo **ngay**, không chờ KP con bẫy xuống `PRIORITY`; tổng tối đa **2** mỗi lần chấm. <!-- Updated: Validation Session 1 - confirmed -->
4b. Examiner **xóa evidence AI** của KP tiêu chí trên cùng bài rồi ghi evidence của mình (không cộng đôi). <!-- Updated: Validation Session 1 - confirmed -->
5. Examiner **thay** AI: với KP tiêu chí của chính bài, xóa evidence AI của bài đó (ref xác định được; SQL lọc cả `user_id` và
   `source IN ('lesson_writing','practice_set')`) rồi ghi evidence examiner, dựng lại profile.
   Evidence AI cho KP câu hỏi và KP con giữ nguyên. Evidence examiner ghi kể cả khi block đã pass (bài chấm người là hành động trả phí, có chủ đích).
6. Mã lỗi → KP con bẫy (`ErrorTrapCatalog.trapKpByCode`), một evidence `error_trap` **sai** mỗi mã mỗi review (trọng số 0.3). Không ghi KP cha từ mã lỗi.
   Chỉ mã **đã duyệt** (`activeErrorTypes()`) được dùng cho evidence, bài ôn và `studyPoints`, như luồng AI bỏ correction chưa duyệt.
7. Bài ôn: không chặn, tổng tối đa `learning.human-review.review-limit` (mặc định 2) mỗi review. Mã lỗi trước (nhiều lần nhất, rồi mã theo ABC) →
   `ERROR_PATTERN` cho KP cha, bộ ≥3 câu và ≥2 câu bẫy, **không chờ** KP con xuống `PRIORITY`; rồi tiêu chí trượt (band thấp nhất) →
   `WRITING_CRITERION` (cần `lessonId` của bài). KP đã có review PENDING bị bỏ qua. Không thêm `trigger_kind` mới.
8. Idempotent theo `reviewSubmissionId` (bảng outcome) và ref evidence; event lặp không thêm gì.
9. Mapping mã ngữ pháp (phase 3) phải được chuyên gia duyệt; **không tự merge** mapping đề xuất.

## Dữ kiện đã kiểm chứng (2026-11-05)

- Migration baseline: learning **V45**, assessment **V22**, content **V117** trên main (sau khi merge Speaking AI). Nhánh dùng learning V46, content V118 cho mapping grammar và V119 cho bộ luyện grammar; giữ nguyên Content V113–V117 và Library V36–V38 từ main để tương thích DB đã chạy chúng.
- Examiner gửi `criteria[{criterion, band, comment}]` với `criterion` ∈ `taskResponse|coherence|lexical|grammar` (`gradingLabels.ts`), `errorCodes`, `annotations[].errorCode`.
- `JdbcHumanReviewStore.complete` trả `Completed` khi thật sự lưu lần đầu; `GradingCompletedAnnouncer` ghi outbox `Propagation.MANDATORY`.
- `human_review_submissions.source_submission_id` = id bài viết trong Learning (`lesson_writing_submissions.id`).
- `lesson_writing_submissions.source` ∈ `LESSON_BLOCK` (có `lesson_id`, `block_id`), `PRACTICE_ITEM` (có `lesson_id`, `practice_attempt_id`), `FREE_WRITING` (không lesson) — V12.
- Ref evidence AI: lesson `LessonEvidenceReference.forWriting(submissionId, kp)`; practice `forPracticeSet(practice_attempts.request_id, questionVersionId, kp)` (`request_id UNIQUE`, V4).
- `KnowledgeEvidenceRepository.removeAssessmentEvidence` + `MasteryProfileUpdater.rebuild` là tiền lệ xóa-và-dựng-lại.
- `kp_evidence_source_check` và `review_items_trigger_kind_check` khai báo ở learning V36.
- Bộ luyện bẫy Writing: `LINKER_MISUSE` 6, `WORD_CHOICE` 4, `COLLOCATION` 4, `UNDERDEVELOPED_IDEA` 4 (V80, V83). Mã ngữ pháp chưa có bộ.
- KP ngữ pháp V84 ACTIVE: `G55_ARTICLES`, `G55_SUBJECT_VERB_AGREEMENT`, `G60_PERFECT_TENSES`, `G55_COMPLEX_SENTENCES`…

## Hướng dẫn cho người làm (đọc trước)

- Đọc `backend/AGENTS.md` (layer, không log essay/payload, không sửa migration cũ, test không gọi LLM thật, không N+1) và `backend/CLAUDE.md`.
- Mỗi phase: (1) chạy baseline module, ghi test đỏ sẵn có; (2) viết test mới, thấy đỏ đúng lý do; (3) code; (4) chạy lại toàn module.
- Testcontainers tự bỏ qua khi không có Docker: ghi "chưa chạy", không coi là xanh. Kiểm `docker ps` trước.
- Không commit/push trừ khi người dùng yêu cầu. Không đưa mã plan/phase vào comment, tên test, migration, commit message.
- Dừng và hỏi nếu: cần đổi quyết định ở trên; số migration đã bị chiếm khác nội dung; file nhắc tới không tồn tại hoặc khác hẳn mô tả.

## Dependencies (cross-plan)

- `261101-2358-first-attempt-suggested-review` (pending): nâng review `ERROR_PATTERN` PENDING thành chặn khi KP yếu lại trong
  Practice có tính điểm. Review `ERROR_PATTERN` do examiner tạo cũng sẽ được nâng — **chấp nhận** (cùng ngữ nghĩa). Không chặn nhau.
- `261021-2016-writing-criterion-sub-kp-judgments` (in_progress, chỉ còn gate kiểm chứng): cùng dùng `WritingEvidenceRule`; plan này
  không sửa luật AI, chỉ thêm đường examiner.
- `261022-1150-examiner-selection-and-queue` (in-progress): cùng chạm `HumanReviewService`; phase 2 chỉ thêm một lời gọi announcer.

## Tiêu chí nghiệm thu

1. Examiner chấm CC < passBand cho bài lesson → `DEMO_WRITING_CRITERION_CC` có 1 evidence `human_review` sai; evidence `lesson_writing` của cùng bài cho KP đó bị xóa; profile dựng lại.
2. Examiner đánh dấu `LINKER_MISUSE` (2 chỗ) → `TRAP_LINKER_MISUSE` có đúng 1 evidence `error_trap` sai; review `ERROR_PATTERN` không chặn cho `DEMO_WRITING_LINKING`.
3. Cùng event giao lại → không thêm evidence, review, outcome.
4. Mã không có KP con, tiêu chí không map được, đề không có `passBand` → bỏ qua phần đó, listener vẫn ack.
5. Bài free-writing (không lesson) → chỉ lưu study points; không evidence, không đổi mastery, không review (quyết định sau khi kế hoạch viết: luyện tự do không ảnh hưởng mastery như course, giống bài AI chấm).
6. Tối đa 2 review mỗi examiner review; KP có review PENDING bị bỏ qua.
7. `GET /api/learning/writing-submissions/{id}/human-review` trả `studyPoints` (mảng, `[]` khi chưa áp hoặc chưa `DONE`); điểm có review PENDING mang `reviewItemId`.
7a. Mã chưa duyệt không tạo evidence, review, hay xuất hiện trong `studyPoints`.
7b. Event park vào DLQ rồi đẩy lại sau khi Content hồi phục → áp đúng một lần.
8. Trang kết quả examiner hiện "Cần ôn" với Ôn ngay / Luyện tập.
9. Event không chứa essay/comment/excerpt/suggestion; log không chứa payload.
10. Test sẵn có của Assessment, Learning, Content, frontend vẫn xanh (trừ lỗi nền đã ghi).

## Ngoài phạm vi

Examiner chọn KP con PASS/FAIL; bài ôn sinh từ đoạn sai của learner; bài thi course/mock (`assessment-completed-v2`); Speaking;
soạn bộ luyện bẫy cho mã ngữ pháp; hiển thị trigger "examiner" khác biệt trong giỏ ôn; **backfill** các review đã `DONE` trước khi phát hành.

## Câu hỏi còn mở

1. Mapping mã ngữ pháp (phase 3) — giữ cả 5 dòng đề xuất, chờ chuyên gia IELTS duyệt từng dòng (đặc biệt `TENSE`, `SENTENCE_FRAGMENT`, `PLURAL_FORM`).

(Trọng số `human_review` đã chốt 0.75 ở Validation Session 1.)

## Red Team Review

### Session 1 - 2026-11-05
Lenses: Security Adversary, Assumption Destroyer, Failure Mode Analyst, Scope & Complexity Critic, chạy inline bởi controller
(không spawn subagent, theo tiền lệ plan `261101-2358`). Finding không có `file:line` bị loại.

**Findings:** 11 (2 High, 3 Medium, 2 Low, 4 bác). Accepted: 7. Rejected: 4. Người dùng chọn áp dụng cả 7.

| # | Severity | Finding | Evidence | Disposition |
|---|----------|---------|----------|-------------|
| 1 | High | Mã chưa duyệt lọt vào evidence/`studyPoints` | `HumanReviewService.java:122-129`, `:63-76`; `lesson-writing-v1.md:88-90` | Accept: chỉ mã APPROVED (phase 4, 5) |
| 2 | High | Không lấy được `reviewItemId` | `JdbcReviewItemRepository.java:166-172` (`void`, `ON CONFLICT DO NOTHING`) | Accept: đọc lại `findPending` một lần (phase 5) |
| 3 | Medium | `source`/`task` trong event không đáng tin, thừa | `HumanReviewUseCase.java:63`; `HumanReviewStore.java:27` | Accept: bỏ khỏi event (phase 1, 2, 5) |
| 4 | Medium | Xóa evidence chỉ theo ref | `JdbcKnowledgeEvidenceRepository.java:71` (tiền lệ lọc user + source) | Accept: lọc user + nguồn (phase 4) |
| 5 | Medium | Event DLQ là mất vĩnh viễn | `AssessmentCompletedListener.java:38-40` | Accept: test replay, log id, docs replay (phase 5, 7) |
| 6 | Low | Xóa ref practice bỏ cả kết quả câu của KP trùng | `SubmitPracticeAttemptUseCase.java:170-181` | Accept: ghi rõ, chấp nhận (phase 5) |
| 7 | Low | Review DONE trước release không được áp | `GradingCompletedAnnouncer` chỉ báo lúc hoàn tất | Accept: ngoài phạm vi, ghi docs (plan, phase 7) |

Rejected: examiner xấu kéo mastery (người chấm tin cậy, giới hạn 2 review, có QA sample); ghi evidence sau khi block pass cho phép
"cày" (mỗi lần tốn 1 lượt, `HumanReviewUseCase.java:59`); áp trùng khi song song (`pg_advisory_xact_lock` trong `LearnerLock`, outcome
kiểm trong lock); gọi Content khi giữ lock (tiền lệ `ErrorTrapReviews.java:99`).

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01 … phase-07
- Decision deltas checked: 5 (event không có `source`/`task`; chỉ mã APPROVED; `removeBySourceReferences` → `removeWritingEvidence` lọc user + nguồn; `reviewItemId` qua `findPending`; không backfill)
- Reconciled stale references: tên phương thức xóa ở phase 4 (Requirements, Implementation Steps) và phase 5 (bước 6); payload phase 1; `Completed` phase 2; nguồn `task` phase 5 bước 3
- Unresolved contradictions: 0

## Validation Log

### Session 1 - 2026-11-05
**Trigger:** `/ck:plan validate` sau red-team. **Questions asked:** 5.

#### Verification Results
- Tier: Full (7 phases), giới hạn theo guard (đã có Red Team Review có bằng chứng): chỉ kiểm khẳng định chưa xác minh.
- Claims checked: 8 | Verified: 7 | Failed: 0 | Gap: 1
- Verified: Content `/error-types` chỉ trả mã đã duyệt (`InternalLearningContentController.java:83,99`); `EssayStudyPoints.tsx` có
  (`frontend/src/features/learning-path/components/`); route `reviews/:reviewId` (`App.tsx:131`); `teachingSubmissionLesson` private
  (`ErrorTrapReviews.java:117`); `AssessmentMessagingConfig` (Learning) và `AssessmentMessagingConfiguration` (Assessment) có;
  `HumanReviewController.review` trả thẳng `HumanReviewClient.Review` (`HumanReviewController.java:40`).
- Gap: trọng số error-trap ghi đè được qua `learning_rule_configs` (`MasteryFormulaSettings.java:23,75`); plan chưa nói `human_review` → hỏi ở Q1.

#### Questions & Answers
1. **[Tradeoffs]** Trọng số `human_review`? Options: 1.0 + admin ghi đè | 1.0 chỉ config | 0.75.
   **Answer:** 0.75. **Rationale:** cao hơn AI chấm (0.5), thấp hơn bài thi; một essay chỉ là một mẫu. Chỉ config (không khóa `learning_rule_configs`).
2. **[Architecture]** Evidence AI của KP tiêu chí trên cùng bài? Options: xóa AI, ghi examiner | cộng thêm | bỏ examiner nếu AI đã có.
   **Answer:** Xóa AI, ghi examiner. **Rationale:** không đếm đôi; có tiền lệ chấm lại bài thi.
3. **[Assumptions]** Bài ôn theo mã lỗi: tạo ngay hay chờ PRIORITY? **Answer:** Tạo ngay. **Rationale:** examiner đã thấy lỗi; chờ PRIORITY thì gần như không bao giờ có bài ôn.
4. **[Scope]** Giới hạn bài ôn mỗi lần chấm? **Answer:** 2.
5. **[Scope]** Phase 3 mapping? Options: chỉ 2 dòng tốt | cả 5 chờ duyệt | hoãn. **Answer:** Cả 5 dòng, chờ chuyên gia duyệt từng dòng.

#### Confirmed Decisions
- Trọng số `human_review` 0.75, chỉ cấu hình file/Config Server.
- Xóa evidence AI của KP tiêu chí trên cùng bài, ghi evidence examiner.
- Bài ôn theo mã lỗi tạo ngay; tối đa 2 mỗi lần chấm.
- Phase 3 giữ cả 5 mapping đề xuất, chuyên gia duyệt từng dòng trước merge.

#### Impact on Phases
- Phase 3: ghi rõ giữ cả 5 dòng chờ duyệt.
- Phase 4: trọng số mặc định 0.75, chỉ `@Value`; test trọng số theo 0.75.
- Phase 5: DTO response mới bọc review + `studyPoints`; `createForMarkedCodes` trong `ErrorTrapReviews` dùng helper private sẵn có.

### Whole-Plan Consistency Sweep (Validation Session 1)
- Files reread: plan.md, phase-01 … phase-07
- Decision deltas checked: 5 (trọng số 0.75 config-only; xóa AI xác nhận; tạo ngay xác nhận; giới hạn 2 xác nhận; mapping 5 dòng chờ duyệt)
- Reconciled stale references: trọng số "1.0" ở plan.md (quyết định 4, câu hỏi mở 2) và phase 4 (Requirements, bước 3); grep `1\.0` chỉ còn câu so sánh với bài thi
- Unresolved contradictions: 0
