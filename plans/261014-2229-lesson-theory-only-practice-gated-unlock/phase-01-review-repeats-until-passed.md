---
phase: 1
title: Review repeats until passed
status: completed
priority: P1
dependencies: []
---

# Phase 1: Review repeats until passed

## Overview

Bài ôn không còn bị bỏ qua sau 2 set trượt. Mỗi set trượt dẫn sang set mới cho đến khi đạt (DONE) hoặc Content hết
package chưa lộ (SKIPPED, đường có sẵn trong `GetReviewUseCase`). Chỉ lần trượt đầu bắt đọc lại summary.

## Luồng tạo bài ôn hiện tại (giữ nguyên, chỉ để đối chiếu)

`SubmitPracticeAttemptUseCase` L98-119:
1. Chỉ lần làm đầu của một bộ chưa lộ mới tính (`counted`); làm lại bộ đã lộ không tạo bài ôn, không ghi bằng chứng.
2. Bộ dưới 70%: xét từng skill chưa đạt (Writing không bao giờ sinh bài ôn), trong skill đó từng KP < 70%.
3. KP chỉ được tạo bài ôn khi Content còn bộ khác chưa lộ có ≥ 3 câu và KP chưa có bài ôn PENDING.
4. KP < 40% (`ReviewRule.LOW_SCORE`) bắt đầu ở THEORY (đọc summary), ngược lại vào thẳng PRACTICE.

Phase này chỉ đổi điều xảy ra **sau khi** bài ôn đã tồn tại.

## Requirements

- `ReviewItem.recordSetResult`: đạt → DONE; trượt → `failedSets++`;
  - **chỉ lần trượt đầu** (`failedSets == 1`): `stage = THEORY`, `theoryReason = LOW_SCORE` nếu tỉ lệ đúng
    < `ReviewRule.LOW_SCORE`, ngược lại `SECOND_FAIL`;
  - các lần trượt sau: giữ `stage = PRACTICE` (lấy set mới ngay; summary vẫn trả trong `GET /reviews/{id}` để xem lại);
  - **không** còn SKIPPED do trượt.
  - Bài ôn bắt đầu ở THEORY (KP < 40%) vẫn qua THEORY thêm một lần ở lần trượt đầu, theo cùng luật.
- `ReviewItem.skip()` giữ nguyên (hết package).
- Bỏ `ReviewRule.MAX_FAILED_REVIEW_SETS`; sửa Javadoc `ReviewItem` (L16-20) và comment hằng `LOW_SCORE`.
- `ReviewResult.maxFailedSets`, `ReviewResponse.maxFailedSets`: `int` → `Integer`, luôn `null` (không giới hạn).
  `GetReviewUseCase` L102 truyền `null`.
- Không migration (`theory_reason` CHECK đã có `SECOND_FAIL`, `LOW_SCORE`).

## Architecture

```text
recordSetResult(setId, requestId, correct, total)
  passed  -> DONE
  failed  -> failedSets++
             failedSets == 1 ? stage THEORY, theoryReason (LOW_SCORE | SECOND_FAIL)
                             : stage PRACTICE          // set mới ngay, lặp đến khi đạt
GetReviewUseCase: open set rỗng + không còn package chưa lộ -> skip() -> SKIPPED (giữ)
SubmitReviewUseCase L119: refresh Practice khi status != PENDING (giữ; phase 3 đổi lời gọi)
```

## Related Code Files

(`...` = `services/learning-service/src/main/java/com/ieltspath/learning`, test mirror dưới `src/test/java`)

- Modify: `.../domain/aggregate/ReviewItem.java`, `.../domain/service/ReviewRule.java`,
  `.../application/usecase/GetReviewUseCase.java`, `.../application/result/ReviewResult.java`,
  `.../api/dto/response/ReviewResponse.java`
- Test: `domain/aggregate/ReviewItemTest.java`, `domain/service/ReviewRuleTest.java`,
  `infrastructure/persistence/RemediationLadderIntegrationTest.java` (đang kỳ vọng SKIPPED sau 2 set trượt)

## Implementation Steps

1. **Đỏ trước** — `ReviewItemTest`:
   - trượt set 1 → THEORY; hoàn thành theory → PRACTICE; trượt set 2 → PENDING, PRACTICE, `failedSets == 2`; trượt
     set 3 → PENDING, PRACTICE; đạt set 4 → DONE;
   - bài ôn bắt đầu ở THEORY: theory → trượt → THEORY → các lần sau PRACTICE;
   - `theoryReason`: LOW_SCORE khi < 40%, SECOND_FAIL khi ≥ 40% (lần trượt đầu);
   - `skip()` khi PENDING → SKIPPED (giữ).
   Chạy → đỏ.
2. Sửa `ReviewItem.recordSetResult`; bỏ hằng trong `ReviewRule`; sửa `ReviewRuleTest` (bỏ assert hằng).
3. `Integer maxFailedSets` ở result/response; `GetReviewUseCase` truyền `null`. Kiểm các `@WebMvcTest` có assert
   `maxFailedSets`.
4. `RemediationLadderIntegrationTest`: thay "2 set trượt → SKIPPED" bằng "trượt nhiều lần vẫn PENDING, đạt → DONE";
   thêm kịch bản hết package → SKIPPED.
5. `mvn -q -pl services/learning-service -am test -Dtest='ReviewItemTest,ReviewRuleTest,RemediationLadderIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false`

## Success Criteria

- [ ] Test mới đỏ trước, xanh sau.
- [ ] Không còn `MAX_FAILED_REVIEW_SETS` trong `src/`.
- [ ] `GET /reviews/{id}` trả `maxFailedSets: null` (DTO không có `@JsonInclude` cấp record nên field vẫn xuất hiện).

## Risk Assessment

- Học viên có thể lặp bài ôn nhiều lượt. Không kẹt vĩnh viễn: hết package → SKIPPED.
- `failedSets` vẫn đếm từ `review_sets`, không đổi.
