---
phase: 6
title: "Huy hiệu"
status: completed
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

- [x] Streak 7 ngày cấp `STREAK_7` một lần. (test: `BadgeIntegrationTest.aSevenDayStreakAwardsTheBadgeOnceWithItsPoints`)
- [x] Course đạt qua event cấp `COURSE_PASSED` kể cả khi event giao lại. (test: `aRedeliveredCoursePassAwardsTheBadgeOnlyOnce`)

## Tiến độ (2026-10-18)

- Xong: V30 (`badges` có seed 9 huy hiệu, `learner_badges`), `BadgeRules` (4 test), `BadgeService`, `BadgesUseCase`,
  `GET /api/learning/me/badges` (CUSTOMER) và `PATCH /api/learning/admin/badges/{code}` (ADMIN, bật/tắt).
- Mỗi sự kiện chỉ gọi đúng nhóm huy hiệu của nó: streak (`StreakRecorder`, chỉ ở câu đầu của ngày), số KP MAINTAIN
  (`MasteryProfileUpdater`, chỉ khi có KP vừa MAINTAIN), cấp độ (`GamificationRecorder`), nhiệm vụ (`QuestService`), topic test
  (`CourseOutcomeService.afterTopicTest`), course đạt (`CourseOutcomeService.afterCourseTest`, nhánh đạt).
- Điểm thưởng huy hiệu do tôi chọn (bảng điều kiện không nêu): STREAK_7 5, STREAK_30 15, STREAK_100 50, KP_MAINTAIN_10 5,
  KP_MAINTAIN_50 20, FIRST_TOPIC_TEST 3, COURSE_PASSED 20, LEVEL_10 20, QUESTS_30 10. Đi qua `reward_grants` (lý do `BADGE`).
- Huy hiệu bị tắt không được cấp mới nhưng người đã có vẫn thấy. Danh sách chỉ hiện huy hiệu đang bật (và huy hiệu đã có).
- Chưa làm: test HTTP `BadgeController` (vai trò ADMIN/CUSTOMER); thông báo trong app khi vừa đạt (việc của FE/thông báo).

## Bàn giao FE

Màn huy hiệu (đã đạt sáng màu, chưa đạt mờ kèm tiến độ); thông báo trong app khi vừa đạt.
