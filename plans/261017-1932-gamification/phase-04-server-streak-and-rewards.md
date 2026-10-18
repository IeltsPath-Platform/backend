---
phase: 4
title: "Streak phía server, đóng băng, thưởng points"
status: in_progress
priority: P1
dependencies: [1, 2]
---

# Phase 4: Streak phía server, đóng băng, thưởng points

## Overview

Streak của user-service do client gửi, không dùng được để thưởng (G4). Phase này tính streak ở learning từ lần nộp bài
thật, thêm đóng băng trả bằng points (03 §E.3), và đường cộng points thưởng sang access (dùng chung cho phase 5, 6).

## Requirements

- **Bảng** `learner_streaks(user_id PK, current_days, longest_days, last_active_date, frozen_dates DATE[],
  freeze_available BOOLEAN, updated_at)`.
- **Ngày có hoạt động:** có ít nhất một lần nộp bài được ghi (lesson, Practice, luyện tự do, ôn, thăm dò, thi, essay)
  theo ngày của múi giờ học viên. Ghi trong transaction nộp bài (`GamificationRecorder`).
- **Luật** (`domain/service/StreakRules`):
  - cùng ngày: không đổi; ngày kế tiếp: +1; cách ngày mà các ngày trống đều được đóng băng: +1, ngày đóng băng không
    cộng ngày nhưng không làm mất chuỗi; ngược lại về 1;
  - đọc streak cũng áp luật theo `today` (không cần job nửa đêm), để hiển thị "Streak đã mất, trước đó 16 ngày".
- **Đóng băng** `POST /api/learning/me/streak/freeze` body `{ requestId }`:
  - mua trước, dùng tự động cho ngày trống kế tiếp; giữ tối đa 1 lượt chưa dùng (`409 FREEZE_ALREADY_OWNED`);
  - giá = 10 × số ngày đóng băng liên tiếp tính cả lượt này (10/20/30/40/50); quá 5 ngày liên tiếp → `409 FREEZE_LIMIT`;
  - trừ points qua debit access (`STREAK_FREEZE`), cùng mẫu refund như hồi tim.
- **Thưởng streak:** mỗi khi `current_days` chạm bội của 7: +100 XP và +10 points; chạm 30: +500 XP. Không thưởng lại
  cùng mốc trong cùng chuỗi (UNIQUE theo `(user, chuỗi bắt đầu, mốc)`).
- **Đường cộng points** (dùng chung):
  - bảng `reward_grants(id, user_id, amount, reason, reference_id, status PENDING|GRANTED|CAPPED|FAILED, attempts,
    created_at, updated_at)`, ghi `PENDING` trong transaction nộp bài (consumer và use case không gọi HTTP giữa
    transaction);
  - `RewardGrantRelayJob` (mỗi phút, khóa advisory, lô 100) gọi `POST /internal/access/points/credit` với
    `idempotencyKey = grant.id`; kết quả `capped` thì `CAPPED`; lỗi thì tăng `attempts`, quá 10 lần thì `FAILED` và ghi
    log mã lỗi;
  - thưởng KP ≥ 90% mastery lần đầu: +5 points (thêm vào luật của phase 2).
- **API** `GET /api/learning/me/streak`: `currentDays`, `longestDays`, `freezeAvailable`, `nextFreezeCost`,
  `activeToday`, `lostStreakDays` (khi vừa mất).
- **user-service:** không sửa. Ghi trong contract rằng streak server là nguồn cho thưởng.

## Related Code Files

- Tạo: migration, `L/domain/service/StreakRules.java`, `L/domain/aggregate/LearnerStreak.java`, `RewardGrant` +
  repository, `L/application/port/AccessClient` (thêm `credit`), `L/infrastructure/scheduler/RewardGrantRelayJob.java`,
  use case freeze
- Sửa: `GamificationRecorder`, `XpRules` (thưởng KP 90%), `GamificationController`
- Test: `StreakRulesTest` (liên tiếp, cách 1 ngày có/không đóng băng, đổi múi giờ, DST không làm mất chuỗi), relay
  (idempotent, capped, lỗi rồi thử lại, FAILED), integration (mốc 7 ngày cộng 100 XP và tạo grant 10 points một lần)

## Success Criteria

- [ ] 7 ngày liên tiếp có nộp bài → streak 7, +100 XP, access nhận +10 points đúng một lần.
- [x] Bỏ 1 ngày có đóng băng → chuỗi giữ; không đóng băng → về 1 khi nộp bài lại. (StreakRulesTest)
- [ ] Đóng băng lần thứ 3 liên tiếp giá 30 points.
- [ ] access tắt: grant nằm `PENDING`, bật lại thì được cộng.

## Tiến độ (2026-10-17)

- Xong: `StreakRules` (11 test đơn vị), V26 (`learner_streaks`, `reward_grants`, nguồn XP `STREAK`), `StreakRecorder`
  (ghi trong transaction nộp bài, BEFORE_COMMIT trên `QuestionEventsAppended`), `StreakUseCase`, `StreakController`
  (`GET /api/learning/me/streak`, `POST /api/learning/me/streak/freeze`), `StreakIntegrationTest` (5 test, DB thật).
- Điều chỉnh luật đóng băng: lượt mua ngày D phủ đúng ngày D nếu D còn trống khi được chấm, không thì ngày trống đầu tiên sau
  D. Luật "phủ ngày mai" làm giá tăng theo liên tiếp không bao giờ đạt được: lượt cũ phải được dùng trước khi mua lượt mới,
  mà việc dùng chỉ xảy ra khi ngày đó được chấm, sau ngày mua.
- Quyết định: mua khi chưa có streak trả `409 NO_STREAK_TO_PROTECT`. Ngày trả lời lại trong cùng ngày không thưởng lại.
- Chưa xong: hoàn points khi đóng băng lỗi (access chưa có route nội bộ); bài essay chưa tính là ngày hoạt động (không phát
  sự kiện câu hỏi).
- Thưởng KP ≥ 90% mastery: +5 points một lần cho mỗi KP, chỉ khi bằng chứng là luyện tập (cùng bộ lọc với XP mốc KP).
  `GamificationRecorder.recordRises` xếp hàng grant `KP_MASTERED` với `referenceId` = id KP; hàng đợi bỏ qua lần lặp.
  Ngưỡng và số điểm cấu hình `learning.rewards.kp-mastered.*`. Test: `GamificationIntegrationTest.aPointAtNinetyPercent...`.

### Relay points (2026-10-18)

Người dùng chọn gửi qua RabbitMQ (hợp đồng C6 trong `work-split.md`), không dùng HTTP.

- Phía learning xong: `RewardGrantMessagingConfig` (exchange `learning.points`, queue `learning.points-grant-settled`),
  `RabbitPointsGrantPublisher`, `RewardGrantService` (gửi lô 100, gửi lại sau 5 phút nếu chưa có trả lời, bỏ cuộc sau 10 lần
  với lỗi `NO_SETTLEMENT`), `RewardGrantRelayJob` (mỗi phút, khóa advisory, `FOR UPDATE SKIP LOCKED`),
  `PointsGrantSettledListener` (đổi trạng thái `GRANTED` / `CAPPED` / `FAILED`). Test: `RewardGrantRelayIntegrationTest` 7,
  `PointsGrantSettledListenerTest` 3.
- Tiêu chí "access tắt thì grant nằm `PENDING`, bật lại thì được cộng": phía learning đạt (grant giữ PENDING và được gửi lại).
  Chưa kiểm chứng đầu-cuối: consumer Access đã commit (0ad9d31) nhưng chưa chạy cùng learning.
- **Lệch cần Codex sửa (còn nguyên sau 0ad9d31, đọc code ngày 2026-10-18):** `LearningPointsGrantListener.parse()` chỉ nhận
  `reason = STREAK_MILESTONE`, trong khi learning gửi thêm `DAILY_QUEST` và `BADGE` (và sau này `KP_MASTERED`). Enum
  `LearningRewardReason` của Access đã có đủ bốn giá trị, chỉ listener chặn. Hậu quả: điểm thưởng nhiệm vụ ngày và huy hiệu
  bị trả `FAILED INVALID_PAYLOAD`, relay không gửi lại. Báo cáo của Codex ghi "Concerns: Không" là chưa đúng. Đã ghi vào C6.
- Chưa có route hoàn điểm khi trừ điểm thành công mà thao tác sau đó lỗi (hồi tim, mua freeze); chờ người dùng quyết chính sách.
