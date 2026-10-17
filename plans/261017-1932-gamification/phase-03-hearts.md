---
phase: 3
title: "Tim"
status: pending
priority: P1
dependencies: [1, 2]
---

# Phase 3: Tim

## Overview

Theo G1 và 03 §C.6, §E.4: 5 tim (PREMIUM 7), sai 1 câu mất 1 tim ở bài tập lesson, Practice set của lesson và luyện tự
do. Hồi +1 mỗi 5 giờ, ngừng đếm khi đầy. Trả points để hồi đầy. Lên cấp hồi đầy. Hết tim thì chặn nộp các bài có trừ tim.
Luyện tự do phụ thuộc plan `261017-1931` phase 3.

## Requirements

- **Bảng** `learner_hearts(user_id PK, hearts SMALLINT, max_hearts SMALLINT, last_regen_at TIMESTAMPTZ, updated_at)`.
  Tạo lười ở lần đọc/nộp đầu tiên với `hearts = max`.
- **Domain** `Hearts` (aggregate, thuần Java):
  - `regen(now)`: cộng `floor((now - last_regen_at) / 5h)` tim, tối đa `max`; đang đầy thì `last_regen_at = now`
    (đồng hồ dừng khi đầy);
  - `lose(n)`: trừ, không dưới 0; nếu trước đó đầy thì `last_regen_at = now`;
  - `refillFull()`, `setMax(m)` (đổi gói: hạ max thì cắt hearts về max).
- **Max theo gói:** PREMIUM → 7, còn lại 5. Đọc quyền qua `AccessClient` có sẵn (feature hiện dùng cho premium),
  một lần mỗi bài nộp; lỗi gọi access thì dùng max đã lưu.
- **Cổng tim** ở đầu use case nộp (sau khóa advisory, trước khi chấm) cho 3 loại bài trên:
  - `regen(now)`; `hearts == 0` → `403 HEARTS_DEPLETED` kèm `nextHeartAt`, `refillCost`;
  - còn ≥ 1 tim thì cho nộp; sau khi chấm, trừ số câu khách quan sai (câu essay không trừ). Bài nộp có thể kéo về 0.
  - Không áp cho bài ôn, quick-check, thăm dò, practice test, thi, placement, essay.
  - `StartFreePracticeUseCase` và bắt đầu Practice cũng kiểm `hearts == 0` để báo sớm (cùng mã).
- **Hồi đầy bằng points** `POST /api/learning/me/hearts/refill` body `{ requestId }`:
  - đầy rồi → `409 HEARTS_FULL`;
  - trừ `learning.gamification.hearts.refill-cost` (mặc định 5) qua debit access với `referenceType = HEART_REFILL`,
    `idempotencyKey = requestId`; trừ xong mới hồi; thiếu points → mã lỗi của access chuyển tiếp;
  - nếu transaction hồi tim lỗi sau khi đã trừ points thì gọi refund (cùng mẫu với chấm AI hiện có).
- **Lên cấp** (phase 2) → `refillFull()` trong cùng transaction.
- **API** `GET /api/learning/me/hearts`: `hearts`, `maxHearts`, `nextHeartAt`, `refillCost`.
- Khối `gamification` trong response nộp thêm `heartsLost`, `heartsRemaining`, `nextHeartAt`.
- **Contract:** `403 HEARTS_DEPLETED` là hành vi mới của endpoint có sẵn (đã được người dùng duyệt, G1); ghi vào contract
  bài học và Practice.

## Related Code Files

- Tạo: migration, `L/domain/aggregate/Hearts.java`, repository + adapter, use case refill, mã lỗi
- Sửa: `SubmitLessonExerciseUseCase`, `SubmitPracticeAttemptUseCase`, `StartPracticeAttemptUseCase`,
  `StartFreePracticeUseCase`, `GlobalExceptionHandler`, `GamificationController`
- Test: `HeartsTest` (hồi theo 5h, dừng khi đầy, không âm, hạ max), integration (hết tim → 403 ở Practice nhưng bài ôn
  vẫn nộp được; hồi bằng points idempotent; lỗi sau khi trừ thì refund; lên cấp hồi đầy), `@WebMvcTest`

## Success Criteria

- [ ] Sai 5 câu ở Practice → 0 tim; nộp tiếp → `403 HEARTS_DEPLETED` với `nextHeartAt` = lần mất + 5h.
- [ ] Bài ôn chặn vẫn làm được khi hết tim.
- [ ] Hồi đầy trừ đúng 5 points một lần dù gửi lại `requestId`.
- [ ] Học viên PREMIUM có 7 tim.

## Risk Assessment

- Hết tim chặn Practice bắt buộc nên chặn mở lesson sau (G1, đã chấp nhận). Ghi rõ trong màn hết tim: thời gian chờ và
  lựa chọn trả points.
- Gọi access mỗi bài nộp: chỉ một cuộc gọi, có fallback max đã lưu.

## Bàn giao FE

Tim ở header; màn "Hết tim" với đồng hồ đếm ngược và nút "Hồi đầy (5 points)"; hiệu ứng mất tim khi sai.
