---
phase: 6
title: "Huy hiệu"
status: pending
priority: P3
dependencies: [2, 4]
---

# Phase 6: Huy hiệu

## Overview

03 US-039: huy hiệu thành tích. Danh mục cố định, cấp tự động khi đạt điều kiện, không gỡ.

## Requirements

- **Bảng** `badges(code PK, name, description, reward_points, active)` (seed) và `learner_badges(user_id, badge_code,
  awarded_at, PRIMARY KEY(user_id, badge_code))`.
- **Danh mục ban đầu** (seed, Admin chỉ bật/tắt):

  | Mã | Điều kiện |
  |---|---|
  | `STREAK_7`, `STREAK_30`, `STREAK_100` | streak server đạt mốc |
  | `KP_MAINTAIN_10`, `KP_MAINTAIN_50` | số KP đang `MAINTAIN` |
  | `FIRST_TOPIC_TEST` | thi topic test đầu tiên |
  | `COURSE_PASSED` | course đạt (`PASSED`) |
  | `LEVEL_10` | đạt cấp 10 |
  | `QUESTS_30` | hoàn thành 30 nhiệm vụ ngày |

- **Kiểm tra** trong `GamificationRecorder` và nơi ghi kết quả course (consumer `AssessmentCompleted.v2`, không gọi
  HTTP; thưởng points đi qua `reward_grants`). Mỗi loại sự kiện chỉ kiểm các huy hiệu liên quan (không quét cả danh mục).
- **API** `GET /api/learning/me/badges` (đã có + chưa có, kèm tiến độ nếu đếm được); `PATCH
  /api/learning/admin/badges/{code}` (`ADMIN`, bật/tắt).

## Related Code Files

- Tạo: migration + seed, `L/domain/service/BadgeRules.java`, repository, use case
- Sửa: `GamificationRecorder`, `ApplyAssessmentResultUseCase` (huy hiệu thi, course), controller
- Test: `BadgeRulesTest`, integration (cấp đúng một lần; consumer chạy lại không cấp lại)

## Success Criteria

- [ ] Streak 7 ngày cấp `STREAK_7` một lần.
- [ ] Course đạt qua event cấp `COURSE_PASSED` kể cả khi event giao lại.

## Bàn giao FE

Màn huy hiệu (đã đạt sáng màu, chưa đạt mờ kèm tiến độ); thông báo trong app khi vừa đạt.
