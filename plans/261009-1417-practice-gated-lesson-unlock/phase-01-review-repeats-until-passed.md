---
phase: 1
title: "Review repeats until passed"
status: pending
priority: P1
dependencies: []
---

# Phase 1: Review repeats until passed

## Overview

Bài ôn không còn bị bỏ qua sau 2 set trượt: mỗi set trượt đưa về bước theory rồi sang set mới, đến khi đạt (DONE) hoặc
Content hết package chưa lộ (SKIPPED, đường có sẵn trong `GetReviewUseCase`).

## Requirements

- `ReviewItem.recordSetResult`: đạt → DONE; trượt → `failedSets++`; **chỉ ở lần trượt đầu** (`failedSets == 1`) chuyển
  `stage = THEORY`, `theoryReason = LOW_SCORE` nếu tỉ lệ đúng < `ReviewRule.LOW_SCORE` (0.40), ngược lại `SECOND_FAIL`;
  các lần trượt sau giữ `stage = PRACTICE` (vào thẳng set mới; theory vẫn trả trong `GET /reviews/{id}` để xem);
  **không** SKIPPED. Bài ôn bắt đầu ở THEORY (`startWithTheory`) vẫn đi qua THEORY sau lần trượt đầu như luật trên.
<!-- Updated: Validation Session 1 - chỉ bắt đọc theory ở lần trượt đầu -->
- `ReviewItem.skip()` giữ nguyên (hết package).
- Bỏ `ReviewRule.MAX_FAILED_REVIEW_SETS`.
- `ReviewResult.maxFailedSets` / `ReviewResponse.maxFailedSets`: `int` → `Integer`, luôn `null` (contract: không giới hạn).
- Không migration (`theory_reason` CHECK đã có `SECOND_FAIL`, `LOW_SCORE`).

## Architecture

```text
recordSetResult(setId, requestId, correct, total)
  passed  -> DONE
  failed  -> failedSets++;
             failedSets == 1 ? stage THEORY, theoryReason (LOW_SCORE | SECOND_FAIL)
                             : stage PRACTICE (set mới ngay)                         // lặp đến khi đạt
GetReviewUseCase: open set rỗng + không còn package chưa lộ -> skip() -> SKIPPED (giữ nguyên)
SubmitReviewUseCase: refreshPassForLesson chỉ khi status != PENDING (giữ nguyên; giờ chỉ DONE)
```

## Related Code Files

(`...` = `services/learning-service/src/main/java/com/ieltspath/learning`, test mirror dưới `src/test/java`)

- Modify: `.../domain/aggregate/ReviewItem.java` (luật + Javadoc), `.../domain/service/ReviewRule.java` (bỏ hằng, sửa
  comment `LOW_SCORE`), `.../application/usecase/GetReviewUseCase.java` (truyền `null`),
  `.../application/result/ReviewResult.java`, `.../api/dto/response/ReviewResponse.java`
- Test: `domain/aggregate/ReviewItemTest.java`, `domain/service/ReviewRuleTest.java` (bỏ assert hằng),
  `infrastructure/persistence/RemediationLadderIntegrationTest.java` (L154 đang kỳ vọng SKIPPED sau 2 set trượt)

## Implementation Steps

1. **Đỏ trước:** sửa `ReviewItemTest`: trượt set 1 → THEORY; hoàn thành theory → PRACTICE; trượt set 2 → vẫn PENDING,
   **PRACTICE** (nhận set mới ngay), `failedSets == 2`; trượt set 3 → vẫn PENDING, PRACTICE; đạt set 4 → DONE. Review
   bắt đầu ở THEORY: theory → set trượt → THEORY một lần nữa → các lần sau PRACTICE. `skip()` khi PENDING → SKIPPED
   (giữ). `theoryReason` = LOW_SCORE khi < 40%, SECOND_FAIL khi ≥ 40% (lần trượt đầu). Chạy test → đỏ.
2. Sửa `ReviewItem.recordSetResult`, bỏ hằng trong `ReviewRule`, sửa `ReviewRuleTest`.
3. `ReviewResult`/`ReviewResponse`: `Integer maxFailedSets`; `GetReviewUseCase` truyền `null`. Kiểm `@WebMvcTest` nếu có
   assert `maxFailedSets`.
4. Sửa `RemediationLadderIntegrationTest`: thay kịch bản "2 set trượt → SKIPPED" bằng "trượt nhiều lần vẫn PENDING, đạt
   → DONE"; thêm kịch bản hết package → SKIPPED.
5. `mvn -q -pl services/learning-service -am test -Dtest='ReviewItemTest,ReviewRuleTest,RemediationLadderIntegrationTest'`
   (kèm `-Dsurefire.failIfNoSpecifiedTests=false` nếu cần).

## Success Criteria

- [ ] Test mới đỏ trước khi sửa, xanh sau khi sửa.
- [ ] Không còn tham chiếu `MAX_FAILED_REVIEW_SETS` trong `src/`.
- [ ] Response bài ôn trả `maxFailedSets: null` (do `@JsonInclude` của DTO, kiểm field có được giữ hay bị bỏ; contract
      ghi đúng hành vi thực).

## Risk Assessment

- Học viên lặp bài ôn nhiều lượt; mỗi lượt đọc theory. Không kẹt vì hết package → SKIPPED.
- `failedSets` vẫn đếm từ `review_sets` (adapter L35), không đổi.
