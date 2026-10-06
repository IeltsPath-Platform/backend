---
phase: 4
title: "Assessment course gate"
status: pending
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

## Risk Assessment
- Chạy assessment mới với learning cũ (ví dụ chỉ restart assessment khi dev local) thì event `COURSE_GATE` vào DLQ
  của learning, vì parser cũ từ chối type lạ. Cách xử lý: restart learning, rồi replay từ DLQ.
