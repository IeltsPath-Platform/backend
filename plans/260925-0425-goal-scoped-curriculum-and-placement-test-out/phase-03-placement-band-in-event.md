---
phase: 3
title: "Placement band in AssessmentCompleted.v2"
status: pending
priority: P1
dependencies: []
effort: "~2h"
---

# Phase 3: Placement band in AssessmentCompleted.v2

## Overview
Luật test-out (a) ở pha 4 cần band tổng của bài placement. `AssessmentCompleted.v2` hiện không mang band.
Pha này thêm trường tùy chọn `overall_band` vào `data`. Đây là thay đổi cộng thêm nên vẫn là v2.

## Context
- `AssessmentCompletedV2.Data`: `userId`, `learningGoalId`, `attemptId`, `resultId`, `resultVersion`,
  `assessmentType`, `status`, `completedAt`, `itemResults`. Không có band.
- `AssessmentResult.overallBand` là band grader chốt (pha 3 của plan trước: band của grader luôn ghi đè band learner).
- `FormalEvidenceAdapter` (Python) chỉ đọc các trường nó biết, nên trường mới không làm vỡ consumer cũ.

## Requirements
- Functional:
  - `data.overall_band`: số 0.0–9.0 hoặc `null`. Là band của result version đang được công bố.
  - Có mặt với mọi `assessment_type`; consumer chỉ dùng nó cho `PLACEMENT`.
  - `FormalAssessmentCommand` có `overall_band: Decimal | None`. Validate: số trong 0–9, bội số 0.5, hoặc null;
    sai kiểu → `ContractError` (DLQ).
  - Contract doc ghi trường mới là tùy chọn và chỉ cộng thêm.
- Non-functional:
  - Event cũ không có `overall_band` vẫn hợp lệ (đọc ra `None`). Các event trong hộp chờ không bị ảnh hưởng.

## Related Code Files
- Modify: `services/assessment-service/.../application/event/AssessmentCompletedV2.java`, `AssessmentCompletedEventFactory.java`
- Modify: `services/ai-learning-service/app/adapters/formal_evidence_adapter.py`
- Modify: `docs/contracts/assessment-completed-v2.md`
- Tests: `AssessmentCompletedEventFactoryTest` (hoặc test finalize hiện có), `AssessmentOutboxIntegrationTest`
  (payload có `overall_band`), `tests/test_formal_evidence_adapter.py`

## Implementation Steps
### Tests Before
1. Viết test **fail trước**:
   - Java: payload của result có band 6.5 chứa `"overall_band":6.5`; result không band chứa `"overall_band":null`.
   - Python: event có `overall_band` 6.5 → `command.overall_band == Decimal("6.5")`; event thiếu trường → `None`;
     `overall_band` = "abc" hoặc 9.5 → `ContractError`.
### Refactor
2. Thêm trường ở producer và adapter.
### Tests After
3. Test outbox Testcontainers và toàn bộ suite Python pass.
### Regression Gate
4. Gate chung; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Event mới mang `overall_band`; event cũ vẫn được consumer chấp nhận.
- [ ] Contract doc cập nhật.

## Risk Assessment
- **Band null ở placement** (grader không nhập band): luật (a) không áp dụng; chỉ còn luật (b) theo từng KP.
