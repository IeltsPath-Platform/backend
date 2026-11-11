---
phase: 2
title: "Dead outbox cleanup"
status: pending
priority: P2
dependencies: []
---

# Phase 2: Dọn outbox không có người nhận (game, content)

## Overview
Game ghi event vào `outbox_events` ở 10 use case nhưng không có relay và không service nào nhận. Content có bảng
`outbox_events` mà không code nào ghi. Cả hai khiến người đọc tưởng có event đang chạy. Theo YAGNI: bỏ, không làm relay
(đổi hướng nếu người dùng chọn làm relay ở `plan.md`).

## Requirements
- **Game:**
  - Bỏ lời gọi `OutboxWriter.append` ở các use case (`AbandonGameSession`, `ChangeGameRoomMembership`, `CreateGameRoom`,
    `CreateWordlyRoom`, `LobbySeat`, `StartGameMatch`, `StartGameSession`, `StartRocketMatch`, `SubmitGameAnswer`…; liệt
    kê lại bằng `grep` lúc code).
  - Xóa `application/port/OutboxWriter`, `OutboxWriterAdapter`, `OutboxEventJpaEntity`, `OutboxEventJpaRepository`,
    `infrastructure/scheduler/OutboxRetentionJob` và cấu hình của job nếu có.
  - Migration Game mới: `DROP TABLE IF EXISTS outbox_events`.
  - Hành vi nghiệp vụ của game không đổi (phòng, trận, điểm, WebSocket).
- **Content:** migration Content mới: `DROP TABLE IF EXISTS outbox_events` (và index của nó).
- **Tài liệu:** sửa mọi chỗ ghi "outbox của content và game chỉ ghi, chưa có relay": `AGENTS.md` §7, `CLAUDE.md` §5,
  `docs/system-architecture.md` (bảng outbox, §11), `.sdd/database/DATABASE_V5.md` nếu liệt kê bảng.
- Khi sau này Game hoặc Content cần phát event: làm đúng pattern §3.9 từ đầu (outbox có `correlation_id`,
  `trace_context`, relay, tên trong `common-events`).

## Related Code Files
- Game: các use case trên, port/adapter/entity/repository/job outbox, migration mới, test của use case có mock `OutboxWriter`
- Content: migration mới
- Docs: như trên

## Implementation Steps
1. Baseline `mvn -q -pl services/game-service,services/content-service -am test`.
2. Xác nhận lần cuối không consumer nào: `grep` exchange/routing key của game trong mọi service và `common-events`.
3. Bỏ code Game, sửa test (bỏ verify `OutboxWriter`).
4. Hai migration; test Testcontainers: migration áp được trên DB đã có bảng.
5. Sửa tài liệu.

## Success Criteria
- [ ] `grep -ri outbox services/game-service/src/main services/content-service/src/main` chỉ còn migration.
- [ ] Test hai service xanh; game chơi được như trước (test hiện có).
- [ ] Tài liệu không còn câu "outbox content/game chỉ ghi".

## Risk Assessment
- Xóa bảng là thay đổi schema phá hủy: chỉ làm khi người dùng đã duyệt ở `plan.md`.
- Có thể có báo cáo/admin đọc `outbox_events` của game: kiểm bằng `grep` trước khi xóa.
