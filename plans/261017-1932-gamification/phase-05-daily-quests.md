---
phase: 5
title: "Nhiệm vụ ngày"
status: completed
priority: P2
dependencies: [2, 4]
---

# Phase 5: Nhiệm vụ ngày

## Overview

01 §5.4, 03 US-035: mỗi ngày 3 nhiệm vụ, làm mới lúc 00:00 theo giờ học viên. Nhiệm vụ "luyện KP yếu" nối gamification
với mastery.

## Requirements

- **Bảng** `daily_quests(id, user_id, local_date, type, target_kp_id NULL, required, progress, completed_at NULL,
  xp_reward, points_reward)`, `UNIQUE(user_id, local_date, type)`.
- **Sinh lười** ở lần đọc hoặc nộp bài đầu tiên trong ngày (không cần job):

  | Loại | Điều kiện xong | Thưởng |
  |---|---|---|
  | `PRACTICE` | trả lời 5 câu khách quan (lesson, Practice, luyện tự do, ôn) | +20 XP, +5 points |
  | `WEAKNESS` | trả lời 3 câu của KP mục tiêu ở luyện tự do hoặc giỏ ôn | +30 XP |
  | `STREAK` | có hoạt động hôm nay (streak server) | +10 XP |

  - KP mục tiêu của `WEAKNESS`: KP `PRIORITY` hoặc `CONSOLIDATE` có mastery thấp nhất mà còn gói luyện được; KP có bài ôn
    chặn ưu tiên giỏ ôn của chính nó. Không có KP phù hợp → thay bằng `PRACTICE_PLUS` (trả lời 10 câu, +30 XP).
  - Tham số trong `learning.gamification.quests.*`.
- **Tiến độ** cập nhật trong `GamificationRecorder` cùng transaction nộp bài; xong thì tự trao thưởng (XP qua sổ XP,
  points qua `reward_grants`), không cần endpoint nhận thưởng.
- **API** `GET /api/learning/me/quests`: 3 nhiệm vụ hôm nay, tiến độ, thưởng, KP mục tiêu (mã, tên).

## Related Code Files

- Tạo: migration, `L/domain/service/QuestRules.java`, repository + adapter, use case đọc
- Sửa: `GamificationRecorder`, `GamificationController`
- Test: `QuestRulesTest` (chọn KP yếu, thay `PRACTICE_PLUS`), integration (qua ngày theo múi giờ sinh bộ mới; xong một
  lần chỉ thưởng một lần; nộp bài ôn tính vào `WEAKNESS` khi đúng KP)

## Success Criteria

- [x] Đầu ngày học viên có đúng 3 nhiệm vụ. (test: `QuestIntegrationTest.theDayStartsWithThreeQuestsAndALongerPracticeWhenNoPointIsWeak`)
- [x] Trả lời 3 câu của KP mục tiêu → `WEAKNESS` xong, +30 XP một lần. (test: `threeAnswersOnTheTargetPointCompleteTheWeaknessQuest`: 30 + 10 streak = 40)
- [x] Không có KP yếu → nhận `PRACTICE_PLUS`. (test: `aWeakPointNobodyCanPracticeLeavesNoTarget`, `QuestRulesTest`)

## Tiến độ (2026-10-18)

- Xong: V29 (`daily_quests`, nguồn XP `QUEST`), `QuestRules` (7 test), `QuestService` (sinh lười, tiến độ, thưởng một lần),
  `QuestRecorder` (BEFORE_COMMIT trên `QuestionEventsAppended`), `GetQuestsUseCase`, `GET /api/learning/me/quests`,
  khóa `learning.gamification.quests.*`.
- `QuestionEventsAppended` mang thêm danh sách câu MỚI của lần ghi (lần nộp lặp không có câu mới nên không cộng tiến độ).
- Chỉ đếm câu khách quan có chấm đúng/sai, lần nộp đầu, từ lesson, Practice, luyện tự do, ôn. Thi, thăm dò chẩn đoán và
  essay không tính. `WEAKNESS` chỉ đếm câu của KP mục tiêu ở luyện tự do hoặc ôn. `STREAK` xong khi có bất kỳ câu mới nào.
- Thưởng XP qua sổ XP khóa theo mã nhiệm vụ (không bị trần 100/ngày, như mốc KP); points qua `reward_grants` (lý do
  `DAILY_QUEST`), chỉ nhiệm vụ `PRACTICE` có points (+5).
- Chọn KP mục tiêu cần Content (KP nào còn gói luyện): Content lỗi thì thay bằng `PRACTICE_PLUS`, không làm hỏng lần nộp.
- Chưa làm: test HTTP `QuestController`; nhiệm vụ ngày không tính bài essay (không phát câu hỏi).

## Bàn giao FE

Khối "Nhiệm vụ hôm nay (1/3)" trên dashboard; `WEAKNESS` có nút đi thẳng tới luyện KP mục tiêu.
