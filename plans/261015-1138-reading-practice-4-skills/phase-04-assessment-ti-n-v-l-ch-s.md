---
phase: 4
title: "Assessment tiến độ và lịch sử"
status: pending
priority: P1
effort: "1d"
dependencies: [3]
---

# Phase 4: Assessment tiến độ và lịch sử

## Overview
Dữ liệu người học cho thư viện đề: `GET /api/assessments/practice/progress` (đã làm part/đề nào, để frontend ghép với catalog qua `only`/`exclude`)
và `GET /api/assessments/practice/history` (lịch sử phân trang).

**Thiết kế gốc (đọc trước, làm theo): [`../261014-1002-practice-test-library/phase-04-assessment-history-and-stats.md`](../261014-1002-practice-test-library/phase-04-assessment-history-and-stats.md).**

## Lệch so với phase thiết kế
- **Bỏ `POST /stats`** cùng `GetPracticeStatsUseCase`, `PracticeStatsRequest/Response`, `PracticeStatsResult` và test của chúng (YAGNI: màn hình đích không hiện lượt làm; `V8` đã bỏ index tương ứng).
- Giữ nguyên `progress` và `history`: ngữ nghĩa "đã làm" = đã **nộp** (`SUBMITTED`/`GRADED`/hoàn tất, không tính `IN_PROGRESS`/`EXPIRED`/`CANCELLED`), tính theo `package_id` + `part_no`, không phụ thuộc phiên bản.
- Skill lọc ở DB bằng cột `assessment_attempts.skill` (do `V8`), không đọc snapshot JSON.
- Kịch bản bắt buộc có test (Testcontainers, tự `INSERT` bằng `JdbcTemplate`) khớp dữ liệu demo: học viên làm Passage 2 của gói A (`PART`) → progress `PART` có đúng khóa `(A, 2)`, Passage 1 và 3 không;
  rồi làm `FULL` gói A → progress `FULL` có `(A, null)` và progress `PART` có cả ba part; học viên khác không lẫn vào; đề ra version 2 vẫn tính "đã làm".

## Related Code Files
Như phase thiết kế, trừ các file của `stats`. `PracticeLibraryController` chỉ có `GET /progress` và `GET /history`.
`springdoc.paths-to-match` của assessment-service: thêm `/api/assessments/practice/**` nếu file đang liệt kê route.

## Implementation Steps
1. Test đỏ (reader Testcontainers, use case, WebMvcTest quyền/422).
2. `PracticeAttemptReader` (port) + `JdbcPracticeAttemptReader` + use case + controller + DTO.
3. `mvn -q -pl services/assessment-service -am test`.

## Success Criteria
- [ ] Test mới đỏ rồi xanh; test cũ xanh.
- [ ] Không có endpoint `stats`, không có chữ `stats` trong code mới.
- [ ] Mỗi endpoint số query cố định, lọc và phân trang ở DB, không lộ dữ liệu học viên khác.
- [ ] `percent` lấy từ `result_version` lớn nhất có `status = COMPLETED`.

## Risk Assessment
Như phase thiết kế. Giới hạn 500 khóa của `progress` đủ cho giai đoạn đầu; vượt thì ghi vào Verification.
