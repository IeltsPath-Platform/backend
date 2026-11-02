---
phase: 1
title: "Correlation id qua outbox"
status: pending
priority: P1
effort: "M"
dependencies: [old-2, old-4]
---

# Phase 1: Correlation id qua outbox

Người làm: Claude. Không đụng file của Codex (phase 3 cũ), nên làm ngay được.

## Overview

Bảo đảm một request HTTP có `X-Correlation-Id` để lại cùng id đó trong log của consumer, kể cả khi message đi qua bảng outbox và relay chạy ở thread khác.

## Hiện trạng (đã kiểm)

- Không file service nào đọc MDC; không bảng outbox nào có cột correlation. Publisher chỉ gắn `event_type`, `aggregate_id` (`RabbitOutboxEventPublisher`, `CommunityOutboxRelay`, `RabbitAccessEventPublisher`).
- Relay chạy ở thread timer hoặc `outbox-relay-nudge` (`OutboxRelayNudge`): MDC rỗng, nên bước đóng dấu của `CorrelationRabbitBeanPostProcessor` không có gì để gắn.
- Consumer đọc header và khôi phục MDC đúng sau fix `5221e81c` (advice đọc `Message` ở mọi vị trí tham số). Fix này chưa nằm trên `feat/reliability-patterns`.
- Test hiện có chỉ phủ publish trực tiếp trong một thread; không test nào đi qua outbox.
- Learning: `reward_grants` là outbox kiểu khác (relay `RewardGrantRelayJob`), cùng lỗi.

## Thiết kế

Lưu id **một lần, tại điểm ghi duy nhất của mỗi service**, và gắn lại ở publisher:

```
request thread: adapter.save(outboxRow) ──► cột correlation_id = MDC["correlationId"] (có thể null)
relay thread:   publisher đọc cột ──────────► header x-correlation-id (bỏ qua nếu null)
consumer:       CorrelationListenerAdvice ───► MDC (đã có), xóa sau khi xử lý (đã có)
```

Không sửa từng use case; không đưa id vào payload (tránh đổi contract sự kiện). Id chạy qua `CorrelationIdFilter.safeOrNew` ở consumer nên giá trị lạ không làm hỏng log.

## Requirements

- Helper `com.ieltspath.commonobservability.Correlation` với `static String current()` (MDC hoặc `null`) và hằng `HEADER`. Không thêm gì khác.
- Cột `correlation_id VARCHAR(100) NULL` trên: assessment `outbox_events` (V21), community `outbox_events` (V9), access `outbox_events` (V8; V7 dành cho idempotency của phase 6 cũ), learning `reward_grants` (V41). Hàng cũ giữ `NULL`, không backfill.
- Mỗi publisher thêm header `x-correlation-id` khi cột có giá trị.
- Mỗi service ghi cột tại một chỗ: `OutboxEventRepositoryAdapter.save` (assessment, access), `CommunityEventOutboxAdapter` (INSERT), `JdbcRewardGrantRepository.enqueue`.

## Related Code Files

- Create: `shared/common-observability/.../Correlation.java` + test
- Create: 4 migration như trên (tên: `outbox_correlation_id`, `reward_grant_correlation_id`)
- Modify: assessment `domain/entity/OutboxEvent.java`, `infrastructure/persistence/{entity/OutboxEventJpaEntity,adapter/OutboxEventRepositoryAdapter}.java`, `infrastructure/messaging/RabbitOutboxEventPublisher.java`
- Modify: access `domain/entity/OutboxEvent.java`, `infrastructure/persistence/{entity/OutboxEventJpaEntity,adapter/OutboxEventRepositoryAdapter,mapper/AccessPersistenceMapper}.java`, `infrastructure/messaging/RabbitAccessEventPublisher.java`
- Modify: community `infrastructure/messaging/{CommunityEventOutboxAdapter,CommunityOutboxRelay}.java`
- Modify: learning `infrastructure/persistence/JdbcRewardGrantRepository.java`, `domain/repository/RewardGrantRepository.java` (record `PendingGrant`), `infrastructure/messaging/RabbitPointsGrantPublisher.java`
- Điều kiện nhánh: đưa `5221e81c` vào `feat/reliability-patterns` trước khi chạy test consumer.

## Implementation Steps

### Test viết trước (phải đỏ)

1. `CorrelationTest`: `currentReturnsMdcValue`, `currentIsNullOutsideRequest`.
2. Mỗi service, test persistence (Testcontainers Postgres): `storesCorrelationIdOfTheSavingThread` (MDC `req-1` → cột `req-1`) và `storesNullWithoutCorrelationId`.
3. Mỗi publisher, test đơn vị: `sendsHeaderWhenRowHasCorrelationId`, `omitsHeaderWhenRowHasNone`.
4. Access, mở rộng `AccessOutboxRelayIntegrationTest` (Postgres + RabbitMQ thật): `relayOnAnotherThreadKeepsTheRequestCorrelationId` — ghi hàng với MDC `req-9` ở thread test, gọi `publishPendingBatch` từ thread khác, message nhận được có `x-correlation-id = req-9`.
5. Notification, test broker thật: `listenerSeesTheCorrelationIdOfTheMessageAndClearsIt` — gửi message có header, use case giả ghi lại `MDC.get`, khẳng định bằng id đó và `null` sau khi xử lý.

### Code

6. Tạo `Correlation`; thêm migration; sửa adapter, entity, mapper, publisher theo bảng trên.
7. Tài liệu: mục "Correlation id" trong `docs/system-architecture.md` (đường đi, thread nào giữ id, vì sao không đưa vào payload).

## Success Criteria

- [ ] Test 1–5 xanh; test cũ của các service giữ nguyên số và xanh.
- [ ] Thử tay: `curl -H "X-Correlation-Id: t-1"` qua gateway cho một thao tác phát sự kiện (cấp gói Premium) → `docker compose logs notification-service | grep t-1` có dòng của consumer.
- [ ] Tiêu chí "event → listener" của phase 2 cũ đạt thật.

## Risk Assessment

- **Hàng đang chờ lúc triển khai không có id:** header bỏ qua, consumer tự sinh id mới. Chấp nhận.
- **Sự kiện do scheduler sinh ra (không có request):** `NULL`, consumer tự sinh id. Đúng ý nghĩa: không có request gốc.
- **Rollback:** cột nullable, không ràng buộc; bỏ code ghi/đọc là đủ, không cần xóa cột.
