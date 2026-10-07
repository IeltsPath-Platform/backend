---
phase: 4
title: "Assessment course gate"
status: completed
priority: P2
dependencies: [2, 6]
---

# Phase 4: Assessment course gate

## Overview
Học viên làm package `COURSE_TEST` như một attempt `COURSE_GATE`; event `AssessmentCompleted.v2` mang
`assessment_type = "COURSE_GATE"` (D9). Code nhỏ, nhưng là thay đổi contract event, nên **commit sau phase 6 trong cùng
PR** (D12). Mọi service chạy cùng commit nên learning luôn nhận được `COURSE_GATE`.
<!-- Updated: Validation Session 1 - một nhánh một PR -->


## Requirements
- Functional:
  - `AttemptType.COURSE_GATE`; `forContentPackageType("COURSE_TEST") → COURSE_GATE`.
  - Migration `V5__course_gate_attempt_type.sql`: thay CHECK của `assessment_attempts.attempt_type` thêm `'COURSE_GATE'`.
    Constraint inline ở V1 có tên tự sinh: tìm tên trong `pg_constraint` và drop trong một khối `DO $$ … $$`, rồi add lại
    với tên tường minh `chk_assessment_attempts_type`.
  - Event factory không đổi (đã phát `attempt.getAttemptType().name()`).
- Non-functional: contract `docs/contracts/assessment-completed-v2.md` thêm `COURSE_GATE` vào bảng `assessment_type`,
  ghi rõ consumer phải chấp nhận trước khi producer phát và Learning dùng nó cho thi cuối course.

## Related Code Files
- Modify: `services/assessment-service/src/main/java/com/group01/assessment/domain/vo/AttemptType.java`
- Create: `services/assessment-service/src/main/resources/db/migration/V5__course_gate_attempt_type.sql`
- Docs: `docs/contracts/assessment-completed-v2.md`
- Tests: unit test cho `AttemptType` (domain), Testcontainers persistence test lưu attempt `COURSE_GATE`, test event
  factory có `assessment_type=COURSE_GATE`

## Implementation Steps
1. **Test đỏ**: `forContentPackageType("COURSE_TEST")` trả `COURSE_GATE`; lưu attempt `COURSE_GATE` không vi phạm CHECK;
   event JSON có `"assessment_type":"COURSE_GATE"`.
2. Enum + migration.
3. Contract doc.
4. `mvn -q -pl services/assessment-service -am test`.

## Success Criteria
- [ ] Test mới xanh, test assessment cũ xanh.
- [ ] Contract doc nêu thứ tự deploy.

## Verification

- TDD red: the first focused run failed compilation because `AttemptType.COURSE_GATE` did not exist; 0 tests executed, 0 skipped. After adding the enum mapping but before V5, PostgreSQL rejected the new type under the existing `assessment_attempts_attempt_type_check` constraint as expected (10 selected tests, 9 passed, 1 expected error, 0 skipped).
- Focused green: `mvn -q -pl services/assessment-service -am test '-Dtest=AssessmentAttemptTest,AutoGradingIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'` exited 0: 10 executed, 10 passed, 0 failures, 0 errors, 0 skipped. Docker/Testcontainers applied V5; the integration case persisted `COURSE_GATE` and asserted the emitted `AssessmentCompleted.v2` JSON type.
- Full suite: `mvn -q -pl services/assessment-service -am test` exited 0. Assessment Service: 118 executed, 118 passed, 0 failures, 0 errors, 0 skipped. Shared `common-security`: 8 executed, 8 passed, 0 failures, 0 errors, 0 skipped. PostgreSQL/Testcontainers schema and outbox cases executed.
- Updated the event contract and Assessment README with the new type and deploy order: Learning accepts `COURSE_GATE` before Assessment emits it. `git diff --check` passed. No existing test expectation changed.
- Commit subject: `feat(assessment): support course gate attempts`.

## Risk Assessment
- Chạy assessment mới với learning cũ (ví dụ chỉ restart assessment khi dev local) thì event `COURSE_GATE` vào DLQ
  của learning, vì parser cũ từ chối type lạ. Cách xử lý: restart learning, rồi replay từ DLQ.
