---
phase: 4
title: "Learning evidence model and rule"
status: pending
priority: P1
dependencies: []
---

# Phase 4: Learning evidence model and rule

## Overview
Thêm nguồn evidence `human_review`, bảng outcome, ref evidence, phương thức xóa evidence theo ref, và một luật thuần (domain)
biến kết quả examiner thành evidence + danh sách điểm yếu. Không có I/O mạng ở phase này.

## Requirements
- `EvidenceSource.HUMAN_REVIEW("human_review")`; `Mp1Params.SourceWeights` thêm `humanReview` (mặc định **0.75**), cấu hình
  `learning.mastery.mp1.source-weights.human-review` trong `MasteryFormulaSettings` (chỉ `@Value`, **không** thêm khóa
  `learning_rule_configs` như `ERROR_TRAP_WEIGHT_KEY`). `isPractice` không đổi (không tính vào trần luyện tập).
  <!-- Updated: Validation Session 1 - weight 0.75, config-only -->
- Constructor `SourceWeights` hiện có giữ tương thích (thêm overload như cách `errorTrap` đã làm).
- `LessonEvidenceReference.forHumanReview(reviewSubmissionId, kpId)` — namespace riêng `ielts-path:human_review`.
- `KnowledgeEvidenceRepository.removeWritingEvidence(userId, Collection<UUID> refs)` → `Set<UUID>` KP bị mất evidence.
  SQL lọc **cả** `user_id`, `source IN ('lesson_writing','practice_set')` và `source_reference_id IN (…)`, theo tiền lệ
  `JdbcKnowledgeEvidenceRepository.java:71` (Red Team #4). Ref của nguồn khác không bao giờ bị xóa dù trùng.
- Luật `HumanReviewEvidenceRule` (domain, thuần):
  - Input: `task`, `passBand` (nullable), `criteria[{criterion, band}]`, KP tiêu chí theo mã rubric (`Map<String, UUID>`),
    `errorCodes[{code, count}]`, `trapKpByCode`, `approvedCodes` (Set mã ACTIVE + APPROVED).
  - Output: `criterionOutcomes` (mã rubric, KP, band, correct), `trapOutcomes` (mã, KP con, count) — chỉ mã **đã duyệt** và có KP con
    (Red Team #1: examiner được ghi cả mã chưa duyệt, `HumanReviewService.java:122-129`, nhưng learner không được thấy và luồng AI bỏ correction chưa duyệt, `lesson-writing-v1.md:88-90`),
    `failedCriteria` (band < passBand, sắp band tăng rồi mã), thứ tự mã lỗi (count giảm, rồi mã).
  - Map tên: `taskResponse`→`TR` (TASK_2) / `TA` (TASK_1); `coherence`→`CC`; `lexical`→`LR`; `grammar`→`GRA`. Tên lạ → bỏ.
  - `passBand == null` → không có `criterionOutcomes`/`failedCriteria`. Hai tiêu chí cùng KP → đúng chỉ khi cả hai đạt (như `WritingEvidenceRule`).
- Migration learning `V46__human_review_evidence.sql`:
  - `kp_evidence_source_check` thêm `'human_review'` (drop + add, giữ đủ giá trị V36).
  - Bảng `human_review_outcomes(review_submission_id UUID PK, user_id UUID NOT NULL, submission_id UUID NOT NULL REFERENCES lesson_writing_submissions(id), study_points JSONB NOT NULL, applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)`.
- Repository `HumanReviewOutcomeRepository` (`insertIfAbsent` trả boolean, `find(reviewSubmissionId)`), JDBC adapter.

## Architecture
- Domain: `domain/service/HumanReviewEvidenceRule.java`, VO kết quả trong `domain/vo/`.
- Application không đổi ở phase này ngoài `MasteryFormulaSettings`.
- Infra: `JdbcKnowledgeEvidenceRepository.removeWritingEvidence`, `JdbcHumanReviewOutcomeRepository`.
- `task` của luật lấy từ bài viết (phase 5), không từ event.

## Related Code Files
- Modify: `domain/vo/EvidenceSource.java`, `domain/vo/Mp1Params.java`, `application/service/MasteryFormulaSettings.java`,
  `application/service/LessonEvidenceReference.java`, `domain/repository/KnowledgeEvidenceRepository.java`,
  `infrastructure/persistence/JdbcKnowledgeEvidenceRepository.java`, `src/main/resources/application*.yml` hoặc config-repo `learning-service.yaml` (trọng số mặc định)
- Create: `domain/service/HumanReviewEvidenceRule.java`, `domain/repository/HumanReviewOutcomeRepository.java`,
  `infrastructure/persistence/JdbcHumanReviewOutcomeRepository.java`, `src/main/resources/db/migration/V46__human_review_evidence.sql`
- Tests: `HumanReviewEvidenceRuleTest` (domain), `Mp1ParamsTest`/`ErrorTrapMp1ParamsTest` mở rộng, integration cho repository (xóa theo ref, outcome idempotent, CHECK nhận `human_review`)

(đường dẫn tương đối `services/learning-service/src/main/java/com/ieltspath/learning/` trừ khi ghi khác)

## Implementation Steps
1. Baseline `mvn -q -pl services/learning-service -am test`; ghi lỗi nền.
2. Test đỏ domain: map tên tiêu chí theo task; band = passBand → đúng; < → sai, vào `failedCriteria`; passBand null; tên lạ; mã không có KP con bị bỏ; **mã chưa duyệt bị bỏ dù có KP con**; thứ tự mã; hai tiêu chí cùng KP.
3. Test đỏ trọng số: `of(HUMAN_REVIEW)` = giá trị cấu hình (mặc định 0.75); `isPractice(HUMAN_REVIEW)` false.
4. Test đỏ persistence: insert evidence `human_review` thành công; `removeWritingEvidence` xóa đúng ref, trả KP; **không xóa** row cùng ref nhưng khác user hoặc khác nguồn (ví dụ `review_set`); outcome `insertIfAbsent` lần 2 trả false.
5. Code + migration V46 (kiểm V46 còn trống).
6. Chạy lại toàn module.

## Success Criteria
- [ ] Domain rule có test cho mọi nhánh trên.
- [ ] Migration áp được trên DB đã có V1–V42; CHECK cũ không mất giá trị nào.
- [ ] Test sẵn có xanh (trừ lỗi nền).

## Risk Assessment
- `switch` trên `EvidenceSource` ở nơi khác (ví dụ `SourceWeights.of`, báo cáo, mapper) sẽ báo lỗi biên dịch khi thêm enum: grep `EvidenceSource` và xử lý đủ.
- Xóa evidence là thao tác phá hủy: chỉ nhận ref do Learning tự tính (phase 5), không nhận từ event.
