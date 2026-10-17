---
phase: 7
title: "Cờ vắng học và thống kê hoạt động"
status: pending
priority: P2
dependencies: []
---

# Phase 7: Cờ vắng học và thống kê hoạt động

## Overview

US-074 (03 §B.6): danh sách học viên có nguy cơ, không học từ 3 ngày. Hiện cờ chỉ có `TOO_FAST_HIGH_SCORE`,
`BURST_SUBMISSIONS`, `DUPLICATE_ESSAY`. US-106: Admin xem người dùng hoạt động và tỉ lệ quay lại; hiện chỉ có thống kê
câu, cây và giám khảo. Không làm lớp hay thống kê lớp (M1).

## Requirements

- **Cờ `INACTIVE`:**
  - migration learning: thêm `INACTIVE` vào CHECK `learner_flags.kind`;
  - `InactivityFlagJob` (mỗi ngày, khóa advisory): học viên đã từng học (có `question_attempt_events`) mà lần nộp cuối
    cũ hơn `learning.flags.inactive-days` (mặc định 3) và đang có course chưa hoàn tất → mở cờ; đã có cờ `OPEN` thì
    không mở thêm;
  - học viên nộp bài lại thì cờ `OPEN` tự chuyển `DISMISSED` (trong transaction ghi bài, một câu UPDATE theo user);
  - cờ sinh work item `LEARNER_FLAG` cho `EXAMINER` như các cờ khác (`WorkItemSync`).
- **Thống kê hoạt động** `GET /api/learning/admin/activity-stats?from=&to=` (`ADMIN`):
  - DAU/WAU/MAU theo ngày có ít nhất một lần nộp bài (`question_attempt_events`), không theo lượt đăng nhập (dữ liệu
    đăng nhập ở user-service);
  - giữ chân theo tuần: nhóm học viên theo tuần nộp bài đầu tiên, % còn hoạt động ở tuần 1, 2, 4, 8;
  - một query gom ở database cho mỗi chỉ số; giới hạn khoảng `from..to` tối đa 180 ngày.
- Không đụng doanh thu/chuyển đổi gói (dữ liệu ở access, để sau).

## Related Code Files

- Tạo: migration, `L/infrastructure/scheduler/InactivityFlagJob.java`, `L/application/usecase/GetActivityStatsUseCase.java`
- Sửa: `L/domain/service/AnomalyRules.java` (luật vắng học), `FlagDetector`, các use case nộp bài (tắt cờ), `StaffAdminController`
- Test: `AnomalyRulesTest` (biên 3 ngày), integration (job không mở trùng; nộp bài tắt cờ; thống kê đúng trên dữ liệu
  dựng sẵn), `@WebMvcTest` (chỉ ADMIN)

## Success Criteria

- [ ] Học viên 3 ngày không nộp bài xuất hiện trong hàng việc EXAMINER với cờ `INACTIVE`.
- [ ] Học viên quay lại học thì cờ tự đóng.
- [ ] Admin xem được DAU/WAU/MAU và bảng giữ chân.

## Risk Assessment

- "Có course chưa hoàn tất" phải suy ra không N+1: một query gom.

## Bàn giao FE

Hàng việc EXAMINER có lọc theo loại cờ; màn Admin "Hoạt động" có biểu đồ DAU và bảng giữ chân theo tuần.
