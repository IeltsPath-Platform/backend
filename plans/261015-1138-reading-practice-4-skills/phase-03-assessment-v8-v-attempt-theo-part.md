---
phase: 3
title: "Assessment V8 và attempt theo part"
status: pending
priority: P1
effort: "2d"
dependencies: [2]
---

# Phase 3: Assessment V8 và attempt theo part

## Overview
Hai việc ở assessment-service, một commit cho migration + một commit cho code (hoặc một commit nếu nhỏ):
1. Migration `V8__practice_attempt_scope.sql`: `package_id`, `scope`, `skill` ở `assessment_attempts`, `part_no` ở `attempt_sections`, ràng buộc và index.
2. Bắt đầu attempt `OFFICIAL_PRACTICE` ở hai phạm vi `FULL`/`PART` qua `sectionIds`, bật `TIMED` thật, nộp sau hạn vẫn chấm.

**Thiết kế gốc (đọc trước, làm theo):**
- DDL: [`../261014-1021-practice-test-data-model/phase-02-assessment-schema-migration.md`](../261014-1021-practice-test-data-model/phase-02-assessment-schema-migration.md)
- Code: [`../261014-1002-practice-test-library/phase-03-assessment-attempt-scope.md`](../261014-1002-practice-test-library/phase-03-assessment-attempt-scope.md)

## Lệch / bổ sung so với phase thiết kế
- Gộp hai phase thiết kế vì `V8` chưa tồn tại (plan data-model phase 2 chưa làm). Làm migration **trước** trong phase này, test migration đỏ trước.
- **Bỏ** `idx_assessment_attempts_package_status` khỏi `V8` (không có `stats`). Giữ `idx_assessment_attempts_user_package` (phục vụ progress ở phase 4).
- `V8` đúng như DDL nháp ở phase data-model gồm `chk_assessment_attempts_practice_identity ... NOT VALID`. Không `UPDATE`/`DELETE`, không seed.
- Q5 (TIMED thiếu `timeLimitSeconds` → 422) và Q7 (`OFFICIAL_PRACTICE` nộp sau hạn vẫn chấm, lưu đáp án sau hạn bị từ chối) giữ mặc định như phase thiết kế.
  Dữ liệu demo đều có `time_limit_seconds = 1200`, nên `TIMED` làm được ngay với mọi gói demo.
- Test tích hợp bắt buộc thêm (Testcontainers cho assessment, gọi payload qua stub của `ContentPackageClient`, **không** gọi content thật): dựng payload giống cấu trúc `PT-READING-ISP-01`
  (3 section, 5 câu mỗi section, nhóm) rồi: bắt đầu `PART` section 2 → chỉ 5 `attempt_items`; bắt đầu `FULL` → 15; `TIMED` FULL → `expiresAt = now + 3600s` (sai số 5s);
  snapshot không chứa `answerSpec`; attempt cũ (chèn bằng SQL không có cột mới) đọc ra `scope = FULL`.
- `AttemptType.forContentPackageType("PRACTICE_TEST") = OFFICIAL_PRACTICE`; `PRACTICE_SET` vẫn bị từ chối.

## Related Code Files
Như hai phase thiết kế. Thêm: `services/assessment-service/src/main/resources/db/migration/V8__practice_attempt_scope.sql`,
`.../infrastructure/persistence/PracticeAttemptScopeMigrationTest.java`.
Kiểm test đếm phiên bản Flyway/số bảng của assessment (nếu có) và sửa theo luật số 7 của `plan.md`.

## Implementation Steps
1. Test migration đỏ → viết `V8` (bỏ index `package_status`) → xanh.
2. Test đỏ phần code theo phase thiết kế gốc + test tích hợp bổ sung.
3. Domain (`AttemptScope`, `AssessmentAttempt.start` thêm `packageId`/`scope`/`skill`, luật nộp sau hạn), port + client, use case, `AttemptCreator`, persistence, DTO.
4. `mvn -q -pl services/assessment-service -am test`.

## Success Criteria
- [ ] `flyway_schema_history` assessment có `8`; số dòng bảng cũ không đổi.
- [ ] Mọi tiêu chí của hai phase thiết kế (trừ index `package_status`).
- [ ] Attempt `PART` của Passage 2 `PT-READING-ISP-01` (khi chạy thật ở phase 6) chỉ chứa 5 câu; `scope = PART`, `skill = READING`.
- [ ] Không còn nơi dựng `AssessmentAttemptResult` bằng constructor dài ngoài `from`.

## Risk Assessment
Như hai phase thiết kế. Thêm: DB assessment local đang ở `V7` có thể đã có dòng `OFFICIAL_PRACTICE` cũ (dự kiến không có); ràng buộc `NOT VALID` bảo vệ trường hợp đó.
Thứ tự deploy: content (phase 2) trước assessment; `ContentPackageClient` chịu được payload cũ.
