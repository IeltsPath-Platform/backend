---
phase: 5
title: "Learning placement recommendation"
status: pending
priority: P2
dependencies: []
---

<!-- Updated: Validation Session 1 - bỏ level tự khai; placement chỉ lưu band ước tính để gợi ý course (D10) -->

# Phase 5: Learning placement recommendation

## Overview
Learning lưu band ước tính từ kết quả `PLACEMENT` mới nhất để phase 6 gợi ý course (D10). Không có API tự khai, và
không đổi trạng thái topic nào.

## Requirements
- Functional:
  - Bảng `learner_placements(user_id UUID PK, band NUMERIC(2,1) NOT NULL CHECK (band BETWEEN 0 AND 9 AND band*2 =
    trunc(band*2)), attempt_id UUID NOT NULL, completed_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)`.
  - `AssessmentCompletedParser` đọc `overall_band` (optional, null được, vắng mặt ở event cũ) vào
    `AssessmentResult.overallBand`. Ngoài 0–9 hoặc không phải bước 0.5 → `ContractViolationException`.
  - `ApplyAssessmentResultUseCase`, nhánh `PLACEMENT`: sau `recordVersion`, nếu `overallBand != null` thì
    `LearnerPlacement.record(band, attemptId, completedAt)`. Placement mới hơn (`completedAt` sau) hoặc cùng attempt
    (regrade) thì ghi đè; placement cũ hơn thì bỏ qua. Vẫn không ghi mastery evidence.
  - Chạy trong transaction sẵn có của consumer (đã mở đầu bằng `LearnerLock.lock`).
- Non-functional: log chỉ `eventId`/`attemptId`, không log band.

## Architecture
```
domain/vo/BandLevel (record: 0–9, bước 0.5)
domain/aggregate/LearnerPlacement — record(band, attemptId, completedAt): bỏ qua nếu cũ hơn
domain/repository/LearnerPlacementRepository  ←  infrastructure/persistence/JdbcLearnerPlacementRepository
```
Không có controller ở phase này; phase 6 đọc placement để đánh dấu `recommended`.

## Related Code Files
- Create: `services/learning-service/src/main/resources/db/migration/V6__learner_placements.sql`,
  `domain/vo/BandLevel.java`, `domain/aggregate/LearnerPlacement.java`, `domain/repository/LearnerPlacementRepository.java`,
  `infrastructure/persistence/JdbcLearnerPlacementRepository.java` (gốc `services/learning-service/src/main/java/com/group01/learning/`)
- Modify: `application/command/AssessmentResult.java` (+`BigDecimal overallBand`),
  `infrastructure/messaging/AssessmentCompletedParser.java`, `application/usecase/ApplyAssessmentResultUseCase.java`
- Tests: `domain/aggregate/LearnerPlacementTest.java`, `domain/vo/BandLevelTest.java`,
  `infrastructure/messaging/AssessmentCompletedParserTest.java`, `infrastructure/persistence/AssessmentResultIntegrationTest.java`

## Implementation Steps
1. **Test đỏ**:
   - `BandLevelTest`: 6.25 và 9.5 bị từ chối; 0.0, 9.0 hợp lệ;
   - `LearnerPlacementTest`: placement mới ghi đè; placement cũ hơn bị bỏ qua; regrade cùng attempt ghi đè;
   - parser: `overall_band` có, null, vắng mặt đều parse được; 9.5 → `ContractViolationException`;
   - integration: event PLACEMENT `overall_band=6.0` → có dòng 6.0; replay cùng version không đổi; `overall_band=null`
     → không có dòng; không có `kp_evidence` nào được ghi.
2. Migration V6, VO, aggregate, repository JDBC.
3. Parser + `ApplyAssessmentResultUseCase`.
4. `mvn -q -pl services/learning-service -am test`.

## Success Criteria
- [ ] Test mới đỏ trước, xanh sau; `AssessmentResultIntegrationTest` cũ xanh.
- [ ] Event cũ không có `overall_band` vẫn xử lý bình thường.

## Risk Assessment
- Placement tự chấm hiện ra `overall_band = null` (assessment chỉ đặt band khi EXAMINER chấm tay) nên gợi ý chỉ xuất
  hiện sau khi có người chấm. Plan sau: assessment tự quy điểm ra band.
