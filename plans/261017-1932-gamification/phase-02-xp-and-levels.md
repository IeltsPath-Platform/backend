---
phase: 2
title: "XP, cấp độ, cài đặt múi giờ"
status: pending
priority: P1
dependencies: []
---

# Phase 2: XP, cấp độ, cài đặt múi giờ

## Overview

03 §C.5: XP theo câu đúng nhân độ khó, thưởng khi KP đạt mastery, thưởng nhiệm vụ và streak, trần 100 XP/ngày từ câu
trả lời. Cấp độ theo mốc XP. Các luật theo ngày (trần XP, streak, nhiệm vụ) cần múi giờ của học viên.

## Requirements

- **Cài đặt** `GET/PUT /api/learning/me/settings` body `{ "timezone": "Asia/Ho_Chi_Minh" }` (IANA, kiểm hợp lệ); mặc
  định `learning.gamification.default-timezone`. Bảng `learner_settings(user_id PK, timezone, updated_at)`.
- **Sổ XP** bảng `xp_ledger(id, user_id, amount, source, source_ref, local_date, counted BOOLEAN, created_at)` với
  `UNIQUE(user_id, source, source_ref)` để ghi lại không nhân đôi. Bảng `learner_xp(user_id PK, total_xp, level,
  updated_at)`.
- **Luật XP** (`domain/service/XpRules`, thuần Java, tham số trong `learning.gamification.xp.*`):

  | Sự kiện | XP |
  |---|---|
  | Câu khách quan đúng (lần nộp đầu của khối/set) ở lesson, Practice, luyện tự do, bài ôn | EASY 10, MEDIUM 15, HARD 20 (thiếu độ khó: 10) |
  | KP lần đầu lên ≥ 60% (CONSOLIDATE) | +25 |
  | KP lần đầu lên ≥ 80% (MAINTAIN) | +50 |
  | Nhiệm vụ ngày, streak 7/30 ngày | phase 4, 5 |

  - Trần 100 XP/ngày (theo ngày của học viên) cho dòng "câu đúng"; phần vượt ghi `counted = false` (shadow XP).
    Thưởng KP, nhiệm vụ, streak không tính vào trần.
  - Không XP cho thi (topic/course/practice test), placement, essay.
  - Độ khó lấy bằng batch lookup đã có của content (`question difficulty`), một lần mỗi bài nộp.
- **Điểm nối:** các use case nộp (`SubmitLessonExerciseUseCase`, `SubmitPracticeAttemptUseCase` gồm luyện tự do,
  `SubmitReviewUseCase`) gọi `GamificationRecorder` (application/service) **trong cùng transaction**, sau khi chấm.
  `MasteryProfileUpdater` báo KP đổi tier để cộng thưởng KP (chỉ lần đầu, nhờ UNIQUE).
- **Cấp độ:** `level = 1 + total_xp / learning.gamification.level-xp` (mặc định 1000). Lên cấp → sự kiện cho phase 3
  (hồi đầy tim).
- **Response:** các endpoint nộp thêm khối `gamification { xpEarned, xpCapped, totalXp, level, leveledUp }` (chỉ thêm
  trường).
- **API** `GET /api/learning/me/progress`: `totalXp`, `level`, XP tới cấp sau, XP hôm nay / trần.

## Related Code Files

- Tạo: migration, `L/domain/service/XpRules.java`, `L/application/service/GamificationRecorder.java`, repository + adapter,
  `L/api/controller/GamificationController.java`, DTO
- Sửa: 3 use case nộp, `MasteryProfileUpdater` (báo đổi tier), config-repo `learning-service.yaml`
- Test: `XpRulesTest` (độ khó, trần 100, shadow, thưởng KP một lần), integration (nộp lại cùng khối không thêm XP; trần
  theo ngày của múi giờ học viên; lên cấp), `@WebMvcTest` settings (timezone sai → 400)

## Success Criteria

- [ ] 12 câu HARD đúng trong một ngày cho 100 XP tính + 140 shadow.
- [ ] KP lên MAINTAIN lần hai không cộng thêm.
- [ ] Response nộp bài có khối `gamification`.
