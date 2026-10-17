---
phase: 5
title: "Bản chụp tuần, xu hướng, bản tháng"
status: pending
priority: P2
dependencies: [4]
---

# Phase 5: Bản chụp tuần, xu hướng, bản tháng

## Overview

Tài liệu mentor có báo cáo tuần (01 §6.1) và tháng (01 §6.2), xu hướng 4 tuần, thời gian học, dự kiến khi nào đạt band
mục tiêu (03 §C.3). Plan này chỉ làm phần dữ liệu và API xem trong app; không email, không PDF (M6).

## Requirements

- **Migration learning `learner_weekly_snapshots`:** `user_id`, `week_start` (thứ Hai theo múi giờ hệ thống, cấu hình),
  `skills JSONB` (mastery theo 6 khối), `tier_counts JSONB`, `answered`, `correct`, `active_minutes` (tổng `duration_ms`
  của `question_attempt_events` trong tuần, mỗi câu chặn 10 phút để bỏ thời gian treo), `focus_kp_ids UUID[]`,
  `estimated_band NUMERIC(2,1)`, `created_at`; khóa chính `(user_id, week_start)`.
- **Migration `learner_monthly_snapshots`:** cùng cột theo `month_start`, gom từ dữ liệu tháng (không từ bản tuần, vì
  tuần cắt ngang tháng).
- **Job** `SnapshotJob` (khóa advisory, chạy 00:30 thứ Hai và ngày 1 hằng tháng):
  - chỉ học viên có hoạt động trong 14 ngày gần nhất (01 §A.8);
  - tính theo lô, query gom theo tập user; idempotent (upsert theo khóa chính) để chạy lại an toàn.
- **Giữ lâu:** bản tuần 12 tuần, bản tháng 36 tháng (03 §G.2); job dọn chạy cùng lịch.
- **API** (`CUSTOMER`, chỉ dữ liệu của chính mình):
  - `GET /api/learning/report/weekly?weekStart=` (mặc định tuần trước): bản chụp + so với tuần trước (mũi tên tăng/giảm
    mỗi kỹ năng) + `focus` + gợi ý tuần tới (dùng `SuggestionRules`);
  - `GET /api/learning/report/history?weeks=4..12`: chuỗi xu hướng mastery, độ chính xác, phút học;
  - `GET /api/learning/report/monthly?monthStart=`.
- **Dự kiến ngày đạt mục tiêu:** hồi quy tuyến tính `estimated_band` trên 4 bản tuần gần nhất; ít hơn 4 tuần hoặc độ dốc
  ≤ 0 thì `projection = null` với lý do `NOT_ENOUGH_DATA` / `NO_PROGRESS`. Thêm vào `target` của báo cáo phase 4.
- Admin xem bản chụp của một học viên qua hồ sơ 360° (thêm khối `weekly` vào overview).

## Related Code Files

- Tạo: migration, `L/domain/aggregate/WeeklySnapshot.java` (hoặc record), repository + adapter JDBC,
  `L/infrastructure/scheduler/SnapshotJob.java`, `L/application/usecase/GetWeeklyReportUseCase.java`,
  `L/domain/service/BandProjection.java`
- Sửa: `ReportController`, `StaffInsightUseCase` (khối weekly)
- Test: `BandProjectionTest` (4 điểm tăng đều; < 4 tuần; dốc âm), integration (job idempotent, chỉ học viên hoạt động,
  dọn bản cũ), `@WebMvcTest`

## Success Criteria

- [ ] Chạy job hai lần cho cùng tuần không nhân đôi dữ liệu.
- [ ] Học viên xem được báo cáo tuần trước, xu hướng 4 tuần và dự kiến ngày đạt mục tiêu (khi đủ dữ liệu).
- [ ] Phút học không bị đội lên bởi câu treo lâu.

## Risk Assessment

- Job nặng khi nhiều học viên: lô 500, ghi log số học viên và thời gian, không log nội dung bài.

## Bàn giao FE

Màn "Tuần của bạn" như 03 §F.3 (bỏ XP/tim nếu plan gamification chưa xong), màn tháng có 4 biểu đồ xu hướng.
