---
phase: 1
title: "Points: cộng thưởng và loại giao dịch mới"
status: completed
priority: P1
dependencies: []
---

# Phase 1: Points: cộng thưởng và loại giao dịch mới

## Overview

`point_transactions.transaction_type` hiện chỉ có `KEY_CREDIT`, `AI_GRADING_DEBIT`, `AI_GRADING_REFUND`,
`ADMIN_ADJUSTMENT`. Nội bộ có `POST /internal/access/points/debit` (có `idempotencyKey`) và refund. Gem gộp vào points
(G2) nên access cần: cộng thưởng từ học, trừ cho hồi tim và đóng băng streak, trần kiếm mỗi ngày.

## Requirements

- **Migration access `V4__learning_points.sql`:** mở rộng CHECK `transaction_type` với `LEARNING_REWARD`,
  `HEART_REFILL_DEBIT`, `STREAK_FREEZE_DEBIT`.
- **Cộng thưởng** `POST /internal/access/points/credit` (chỉ internal token):
  - body `{ userId, amount, reason, referenceId, idempotencyKey }`; `reason` thuộc tập cố định (`STREAK_MILESTONE`,
    `KP_MASTERED`, `DAILY_QUEST`, `BADGE`);
  - cùng `idempotencyKey` trả lại kết quả cũ, không cộng hai lần;
  - **trần ngày** `access.points.daily-earn-limit` (mặc định 20, theo ngày UTC; lý do `BADGE` được miễn trần, chốt 2026-10-18, chờ Codex): vượt thì cộng phần còn lại, trả
    `granted` và `capped = true`; đã hết trần thì `granted = 0` (vẫn 200, không lỗi, để relay không thử lại mãi).
- **Trừ points** dùng endpoint debit có sẵn với `referenceType` mới `HEART_REFILL` / `STREAK_FREEZE`, map sang loại giao
  dịch tương ứng. Thiếu points → mã lỗi hiện có (`INSUFFICIENT_POINTS` hoặc mã tương đương đang dùng; kiểm trước khi
  code).
- Lịch sử points của học viên (endpoint hiện có nếu có) hiện đúng các loại mới.

## Related Code Files

- Sửa: `InternalPointsController`, ledger domain + adapter, `PointLedgerRepositoryAdapter`
- Tạo: `CreditPointsRequest`, use case cộng thưởng, migration, properties trần ngày
- Test: use case (idempotent; trần ngày cắt đúng phần còn lại), `@WebMvcTest` (chỉ internal token), integration
  Testcontainers (CHECK mới, cộng song song cùng key chỉ một lần)

## Success Criteria

- [x] Cộng thưởng cùng key song song chỉ cộng một lần; yêu cầu bị chặn hết hạn mức vẫn phát lại cùng kết quả. (test: `PointCreditIntegrationTest.concurrentCreditWithSameKeyAppliesTheDailyCapOnlyOnce`)
- [x] Ngày đã kiếm 18, cộng 5 → nhận 2, `capped = true`. (test: `PointCreditIntegrationTest.concurrentCreditWithSameKeyAppliesTheDailyCapOnlyOnce`)
- [x] Trừ cho hồi tim và đóng băng ghi đúng loại giao dịch; loại giao dịch không thuộc CHECK bị từ chối. (test: `PointCreditIntegrationTest.debitMapsNewReferencesToTheirLedgerTypesAndMigrationRejectsUnknownType`)
- [x] Chỉ internal JWT truy cập được endpoint credit; request không xác thực nhận 401. (test: `PointCreditIntegrationTest.creditRejectsRequestsWithoutInternalJwt`)
- Validation (test: `mvn -B -ntp -pl services/access-service test`; 54 tests, 0 failures, 0 errors, 0 skipped; PostgreSQL Testcontainers ran.)

## Bàn giao FE

Lịch sử points hiện nhãn: Thưởng học tập, Hồi tim, Đóng băng streak.
