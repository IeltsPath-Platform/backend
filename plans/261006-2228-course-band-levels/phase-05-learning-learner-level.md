---
phase: 5
title: "Learning placement recommendation"
status: completed
priority: P2
dependencies: []
---

<!-- Updated: Validation Session 1 - bỏ level tự khai; placement chỉ lưu band ước tính để gợi ý course (D10) -->

# Phase 5: Learning placement recommendation

Execution resumed 2026-10-07 after phase 3 completed. The authorized migration-version and table-count expectations were updated, and the focused and full Docker-backed learning suites pass. See the Verification section below and plan.md.

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
- [x] Test mới đỏ trước, xanh sau; `AssessmentResultIntegrationTest` cũ xanh.
- [x] Event cũ không có `overall_band` vẫn xử lý bình thường.

## Verification

- Status: `DONE`.
- TDD red before implementation: the focused compile exited 1 because the new `BandLevel` type did not exist; 0 tests executed and 0 skipped in that run. The partial implementation was then written.
- Focused green after implementation: `mvn -q -pl services/learning-service -am test '-Dtest=BandLevelTest,LearnerPlacementTest,ApplyPlacementResultUseCaseTest,AssessmentCompletedParserTest,AssessmentResultIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'` exited 0. The five selected learning test classes executed 19 tests: 19 passed, 0 failures, 0 errors, 0 skipped. `AssessmentResultIntegrationTest` ran with Docker/Testcontainers and applied Flyway V1–V6.
- Full suite after the authorized update: `mvn -q -pl services/learning-service -am test` exited 0. Learning Service ran 228 tests: 228 passed, 0 failures, 0 errors, 0 skipped. Shared `common-security` ran 8 tests: 8 passed, 0 failures, 0 errors, 0 skipped. Docker/Testcontainers executed.
- Authorized old expectation change: `LearningServiceApplicationTests.contextStartsWithMigratedSchemaAndPublicHealth` changed only Flyway version `5` to `6` and table count `14` to `15`. Migration inventory is `14` baseline tables + `1` table in V6 (`learner_placements`) = `15`. Health check unchanged.
- No other old expectation changed. No real LLM calls. RabbitMQ connection retries appeared in test contexts without causing skipped tests or failures.
- No real LLM calls. RabbitMQ listener connection retries appeared during test context startup; they did not cause the reported failure.

## Risk Assessment
- Placement tự chấm hiện ra `overall_band = null` (assessment chỉ đặt band khi EXAMINER chấm tay) nên gợi ý chỉ xuất
  hiện sau khi có người chấm. Plan sau: assessment tự quy điểm ra band.
