---
phase: 4
title: Learning trap evidence
status: completed
priority: P1
effort: 2d
dependencies:
  - 3
---

# Phase 4: Learning trap evidence

## Overview
Danh mục KP ở learning biết KP con; KP con nhận bằng chứng theo đáp án đã chọn (nguồn `error_trap`); KP con bị loại khỏi
mọi phép tính tổng hợp.

## Requirements
1. **Danh mục:** `knowledge_point_catalog` thêm `parent_kp_id UUID NULL`, `error_code VARCHAR(60) NULL`.
   `RefreshLearningTopicsUseCase` gọi `topic-sequence?includeErrorTraps=true` và lưu hai cột. <!-- red-team PH1 -->
2. **Bằng chứng KP con** — với mỗi câu có `answerSpec.distractors` (map khóa → mã), xét các mã có KP con trong danh mục:

   | Điều kiện | KP con của mã X |
   | --- | --- |
   | Câu đúng | ✓ cho **mỗi mã khác nhau** trong `distractors` |
   | Câu sai và mã của đáp án đã chọn (`errorCode` từ `ErrorTagger`) = X | ✗ cho X |
   | Còn lại (sai bẫy khác, sai không mã, bỏ trống, câu điền) | không ghi |

   Ghi ở đúng chỗ và đúng điều kiện bằng chứng chính đang ghi; **không** ghi ở `ApplyAssessmentResultUseCase`.
3. **MP-1.0:** `EvidenceSource.ERROR_TRAP("error_trap")`; trọng số mặc định 0.3, cấu hình
   `learning.mastery.mp1.source-weights.error-trap`; **không** tính vào `maxPracticeInWindow` (giữ `Mp1Params.isPractice`
   như cũ). Luật "cùng câu trong 7 ngày tính một lần" áp như cũ nhờ `forQuestion(...)`.
4. **Không XP mốc:** mọi KP con có bằng chứng trong lần nộp đi vào tập `withoutMilestone` của
   `MasteryProfileUpdater.recordEvidence(userId, evidence, withoutMilestone)` (`MasteryProfileUpdater.java:85`).
5. **Không gọi HTTP khi ghi bằng chứng** (red-team PH3): map mã → KP con đọc từ danh mục trong `learning_db`.
6. **Loại KP con khỏi phép tính tổng hợp** (red-team PH1). Danh sách chỗ đọc (đã grep):

   | Chỗ đọc | Xử lý |
   | --- | --- |
   | `QuestService.java:157` `profiles.findAll` | loại KP con |
   | `GetLearnerReportUseCase.java:84` `profiles.findAll` (→ `BandEstimator`, `skills`, `focus`) | loại KP con |
   | `GetReviewBasketUseCase.java:69` `profiles.findAll` | loại KP con (giỏ ôn chỉ có KP cha) |
   | `GetSuggestionsUseCase.java:83` `profiles.findAll` | loại KP con |
   | `StaffInsightUseCase.java:94` `profiles.findAll` | loại KP con |
   | `SnapshotService.java:129` `catalog.findAll` | bỏ mục có `parentKnowledgePointId` |
   | `LessonAccess.java:163`, `ReviewReevaluation.java:54`, `GetMasteryUseCase.java:21`, `SubmitPracticeAttemptUseCase.java:189` `findMasteryHistories(userId)` | loại KP con |
   | `MasteryProfileUpdater.java:126` `findMasteryHistories(userId, kpIds)` | **giữ** KP con (phải tính mastery KP con) |
   | SQL trong `JdbcBadgeRepository`, `JdbcSnapshotRepository`, `JdbcStaffInsightRepository`, `JdbcReviewItemRepository`, `JdbcKnowledgeEvidenceRepository`, `JdbcMasteryProfileRepository` | đọc từng câu; câu nào đếm/tổng hợp theo KP thì thêm điều kiện loại KP con (`parent_kp_id IS NULL` qua join danh mục) |

   Cách làm khuyến nghị: `MasteryProfileRepository.findAll(userId)` và `KnowledgeEvidenceRepository.findMasteryHistories(userId)`
   **mặc định loại KP con** ở SQL; thêm `findErrorTrapProfiles(userId)` cho phase 5–6. Kiểm job rebuild/decay mastery
   (`MasteryDecayJob`, rebuild trong `MasteryProfileUpdater`) vẫn tính cả KP con: nếu chúng đi qua method mặc định thì đổi sang
   method có KP con. Ghi rõ trong báo cáo phase từng chỗ đã đổi.

## Architecture

```java
// learning/domain/service/TrapEvidenceRule.java (thuần)
public static List<TrapOutcome> outcomes(Map<String, Object> answerSpec, boolean correct, String chosenErrorCode,
                                         Map<String, UUID> trapKpByCode)
public record TrapOutcome(UUID trapKnowledgePointId, boolean correct) {}
// distractors = answerSpec.get("distractors") nếu là Map, ngược lại rỗng
// correct → mỗi mã khác nhau trong distractors.values() có trong trapKpByCode → TrapOutcome(kp, true)
// !correct && chosenErrorCode != null && distractors.values() chứa chosenErrorCode && trapKpByCode có → TrapOutcome(kp, false)
// còn lại → List.of()
```

```java
// learning/application/service/ErrorTrapCatalog.java
public ErrorTraps load()   // một truy vấn: SELECT kp_id, parent_kp_id, error_code FROM knowledge_point_catalog WHERE parent_kp_id IS NOT NULL
public record ErrorTraps(Map<String, UUID> trapKpByCode, Map<UUID, UUID> parentByTrapKp,
                         Map<UUID, List<UUID>> trapKpsByParent) {}
```

**Chỗ cắm** — mẫu theo `SubmitPracticeAttemptUseCase` (dòng 129 `firstAnswers`, 162 thêm bằng chứng, 216 `evidence.append`,
217 `recordEvidence(userId, firstAnswers, criterionOnly)`): trong vòng lặp câu, ngay sau dòng thêm bằng chứng chính và cùng
điều kiện `counted`:

```java
for (var trap : TrapEvidenceRule.outcomes(item.answerSpec(), grade.correct(), errorCode, traps.trapKpByCode())) {
    firstAnswers.add(KnowledgeEvidence.of(trap.trapKnowledgePointId(), trap.correct(), EvidenceSource.ERROR_TRAP,
            LessonEvidenceReference.forPracticeSet(command.requestId(), item.questionVersionId(), trap.trapKnowledgePointId()))
            .forQuestion(item.questionVersionId()));
    withoutMilestone.add(trap.trapKnowledgePointId());   // gộp với criterionOnly hiện có
}
```

Làm tương tự ở `SubmitLessonExerciseUseCase` (mã lỗi tính ở dòng 123), `SubmitReviewUseCase`, `FreePracticeSubmission`,
`SubmitDiagnosticProbeUseCase`, dùng hàm tạo tham chiếu mà mỗi đường đang dùng cho bằng chứng chính. Gom phần lặp vào một
helper (ví dụ `ErrorTrapEvidence.append(...)`) để 5 chỗ chỉ gọi một dòng. `traps = errorTrapCatalog.load()` gọi một lần
mỗi lần nộp, chỉ khi có câu có `distractors`.

**Migration** `V36__error_trap_evidence.sql` (kiểm số trước; hiện tới V35):

```sql
ALTER TABLE knowledge_point_catalog ADD COLUMN parent_kp_id UUID, ADD COLUMN error_code VARCHAR(60);
ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check CHECK (source IN (<danh sách hiện hành>, 'error_trap'));
ALTER TABLE review_items DROP CONSTRAINT review_items_trigger_kind_check;
ALTER TABLE review_items ADD CONSTRAINT review_items_trigger_kind_check CHECK (trigger_kind IN
    ('PRACTICE', 'ASSESSMENT', 'CONSOLIDATE', 'MAINTAIN', 'FREE_PRACTICE_TEST', 'DIAGNOSTIC', 'WRITING_CRITERION', 'ERROR_PATTERN'));
```

`<danh sách hiện hành>`: lấy từ migration mới nhất đổi `kp_evidence_source_check` (`grep -n kp_evidence_source_check
src/main/resources/db/migration/*.sql`); phải khớp mọi giá trị của `EvidenceSource` hiện có. Trigger `ERROR_PATTERN` dùng ở
phase 5 nhưng thêm ở đây để chỉ có một migration.

## Related Code Files
- Create: `learning/src/main/resources/db/migration/V36__error_trap_evidence.sql`
- Create: `learning/domain/service/TrapEvidenceRule.java`, `learning/application/service/ErrorTrapCatalog.java`, helper ghi bằng chứng KP con (+ test)
- Modify: `learning/domain/vo/EvidenceSource.java`, `learning/domain/vo/Mp1Params.java` (`SourceWeights` thêm `errorTrap`;
  `of(ERROR_TRAP)`; mặc định 0.3), `learning/application/service/MasteryFormulaSettings.java` (`@Value` mới + đọc ghi đè
  Admin trong `learning_rule_configs` với mặc định), mọi `switch` trên `EvidenceSource` (`grep -rn "case LESSON_WRITING"`)
- Modify: `infra/config-server/config-repo/learning-service.yaml` (`learning.mastery.mp1.source-weights.error-trap: 0.3`)
- Modify: `learning/domain/vo/KnowledgePointCatalogEntry.java` (+ `parentKnowledgePointId`, `errorCode`),
  `JdbcKnowledgePointCatalogRepository.java`, `RefreshLearningTopicsUseCase.java`, `LearningContentClient`/`RestLearningContentClient`
  (cờ `includeErrorTraps`, trường KP con)
- Modify: 5 đường chấm ở trên; các chỗ đọc ở bảng mục 6 của Requirements

## Implementation Steps
1. Baseline: `mvn -q -pl services/learning-service -am test`; ghi đỏ có sẵn.
2. **Đỏ trước:**
   - `TrapEvidenceRuleTest`: đúng (2 mã khác nhau → 2 ✓); đúng với hai đáp án cùng mã → 1 ✓; chọn bẫy X → 1 ✗ X; chọn bẫy Y
     → 1 ✗ Y, không gì cho X; sai không mã → rỗng; bỏ trống → rỗng; không `distractors` → rỗng; mã không có KP con → bỏ.
   - `Mp1ParamsTest`/`MasteryFormulaSettingsTest`: `of(ERROR_TRAP) = 0.3`; `isPractice(ERROR_TRAP) = false`; ghi đè cấu hình.
   - Testcontainers: mỗi đường chấm (lesson, Practice `counted`, ôn, luyện tự do, chẩn đoán) ghi đúng số dòng `kp_evidence`
     `source = 'error_trap'`; Practice không `counted` không ghi; bài thi (`ApplyAssessmentResultUseCase`) không ghi;
     ví dụ "5 câu có bẫy X: X, đúng, X, Y, đúng" → KP con X có 4 bằng chứng (✗ ✓ ✗ ✓).
   - Không XP mốc: KP con vượt 60%/80% không tạo dòng `xp_ledger` mốc.
   - Danh mục không có KP con (content cũ) → không ghi, không lỗi.
   - Không HTTP: client content giả ném lỗi ở mọi method trừ phần đã có trong luồng → lần nộp vẫn thành công và có bằng chứng KP con.
   - Tổng hợp không đổi: thêm KP con có bằng chứng cho hồ sơ mẫu → `BandEstimator`, `skills`/`focus`, bản chụp, nhiệm vụ ngày,
     thống kê staff, huy hiệu bằng đúng giá trị khi không có KP con.
3. Migration + enum + trọng số + config → danh mục → `TrapEvidenceRule` + catalog → 5 đường chấm → loại KP con khỏi tổng hợp.
4. `mvn -q -pl services/learning-service -am test`.

## Success Criteria
- [ ] Mọi test ở bước 2 xanh; test cũ xanh (trừ đỏ có sẵn).
- [ ] Báo cáo phase liệt kê từng chỗ đọc ở bảng mục 6 đã đổi hay giữ và lý do.

## Risk Assessment
- Bỏ sót một chỗ tổng hợp → số liệu lệch: test "tổng hợp không đổi" là chốt chặn; người nghiệm thu grep lại.
- Một câu sai ảnh hưởng hai KP: trọng số 0.3, KP con không bao giờ là đơn vị ôn (phase 5).
