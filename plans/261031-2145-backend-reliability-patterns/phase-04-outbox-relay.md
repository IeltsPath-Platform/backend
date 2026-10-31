---
phase: 4
title: "Outbox relay"
status: pending
priority: P2
effort: "M"
dependencies: [1, 2]
---

# Phase 4: Outbox relay

## Overview

Đóng lỗ hổng "outbox chỉ ghi". Access có relay thật và một consumer thật (notification báo kích hoạt Premium). Game giữ outbox làm nhật ký có hạn lưu. Content xóa code outbox chết.

## Hiện trạng (đã kiểm)

- access: ghi `SubscriptionGranted`, `SubscriptionChanged`, `PointDebited`, `HumanGradingCreditConsumed` vào `outbox_events` (status `PENDING|PUBLISHED|FAILED`, `retry_count`, `published_at`). `findPendingEvents` không ai gọi. Không consumer.
- game: ghi 8 loại event (`GameRoomCreated`, `GameMatchStarted`, ...). Không consumer.
- content: có entity, repository, mapper outbox nhưng **không chỗ nào ghi**.
- Mẫu relay tốt: `assessment/infrastructure/messaging/{OutboxRelay,OutboxRelayScheduler,RabbitOutboxEventPublisher}.java` (claim `SKIP LOCKED`, `max-attempts`, publisher confirm).

## Requirements

- access relay: claim theo lô bằng `FOR UPDATE SKIP LOCKED`, publish với publisher confirm + mandatory, đánh `PUBLISHED`; lỗi tăng `retry_count`, quá `max-attempts` → `FAILED` + log + metric.
- Chỉ relay `SubscriptionGranted` (đủ một luồng thật). <!-- Updated: Validation Session 1 - chốt cách đánh dấu --> Migration mở rộng CHECK của `outbox_events.status` thêm `NOT_RELAYED`; writer ghi `NOT_RELAYED` cho loại event không nằm trong danh sách relay (danh sách là hằng trong code relay); cùng migration chuyển hàng `PENDING` cũ sang `NOT_RELAYED`. Bảng vẫn là nhật ký đầy đủ.
- Contract `SubscriptionGrantedV1` trong `shared/common-events` (`eventId`, `userId`, `planCode`, `startsAt`, `endsAt`, `occurredAt`), exchange/queue/routing key + retry/DLQ theo `RetryTopology` có sẵn.
- notification consume: dedupe theo `eventId`, tạo thông báo "Premium đã kích hoạt đến {endsAt}", push qua `PushBus` (Redis pub/sub, phase 1), lỗi tạm → retry, sai hợp đồng → DLQ.
- game: job dọn outbox hàng cũ hơn N ngày (mặc định 30), advisory lock (phase 1).
- content: xóa entity/repository/mapper/aggregate/adapter outbox không dùng. **Giữ bảng** `outbox_events` (không migration), tránh rủi ro dữ liệu.
- Metric `outbox_pending`, `outbox_oldest_age_seconds` cho access (module phase 2).

## Related Code Files

- Create: `shared/common-events/src/main/java/com/ieltspath/events/access/{SubscriptionGrantedV1,AccessMessaging}.java`
- Create: `services/access-service/.../infrastructure/messaging/{AccessOutboxRelay,AccessOutboxRelayScheduler,RabbitAccessEventPublisher}.java`; `services/access-service/pom.xml` (đã có amqp)
- Modify: `services/access-service/.../infrastructure/persistence/{repository/OutboxEventJpaRepository,adapter/OutboxEventRepositoryAdapter}.java` (claim SKIP LOCKED)
- Create: migration access (CHECK `status` thêm `NOT_RELAYED`; chuyển hàng `PENDING` cũ sang `NOT_RELAYED`; chỉ mục cho claim nếu cần)
- Create: `services/notification-service/.../infrastructure/{SubscriptionGrantedMessagingConfig,SubscriptionGrantedListener}.java` (theo mẫu `GradingCompletedListener`)
- Create: `services/game-service/.../infrastructure/scheduler/OutboxRetentionJob.java`
- Delete: 6 file outbox của content (danh sách trong "Khung code cho Codex")
- Modify: `infra/config-server/config-repo/{access-service,notification-service,game-service}.yaml`

## Implementation Steps

### Test viết trước

1. Access: test cấp gói ghi đúng 1 hàng outbox `PENDING` cùng transaction (rollback thì không có hàng).
2. Access relay (Testcontainers Postgres + RabbitMQ): hàng `PENDING` → message tới queue, hàng `PUBLISHED`; broker dừng → hàng vẫn `PENDING`, `retry_count` tăng; broker sống lại → gửi; 2 relay song song không gửi trùng.
3. Notification (Testcontainers): message hợp lệ → 1 thông báo; gửi lại cùng `eventId` → vẫn 1; JSON hỏng → vào DLQ; lỗi DB tạm → retry rồi thành công.
4. Game: job dọn xóa hàng cũ, giữ hàng mới.
5. Content: build và test xanh sau khi xóa code chết.

### Code

6. Contract trong common-events; topology.
7. Relay access theo mẫu assessment.
8. Consumer notification.
9. Job dọn game.
10. Xóa code chết content.
11. Tài liệu: bảng event trong `docs/system-architecture.md` (producer, consumer, queue, DLQ).

## Success Criteria

- [ ] Admin cấp gói → người dùng nhận thông báo (thử tay qua frontend hoặc STOMP client).
- [ ] Dừng RabbitMQ 1 phút khi cấp gói → thông báo đến sau khi RabbitMQ sống lại.
- [ ] Không có hàng `PENDING` tồn đọng của loại event không relay.
- [ ] Test 1–5 xanh.

## Risk Assessment

- **Hàng `PENDING` cũ tích lũy trước khi có relay:** <!-- Updated: Validation Session 1 - đã chốt --> đã chốt: migration dữ liệu chuyển mọi hàng `PENDING` tạo trước thời điểm migration chạy sang trạng thái không relay; không gửi thông báo cũ cho người dùng. Test: hàng cũ không được relay, hàng mới được relay.
- **Thông báo trùng:** dedupe theo `eventId` ở consumer (bắt buộc test).
- **Đổi contract sau này:** theo quy tắc version ở phase 7.

## Khung code cho Codex

Mẫu bắt buộc đọc trước: `services/assessment-service/.../infrastructure/messaging/{OutboxRelay,OutboxRelayScheduler,RabbitOutboxEventPublisher}.java`, `.../persistence/repository/OutboxEventJpaRepository.java` (claim `SKIP LOCKED`), `shared/common-events/.../assessment/{GradingCompletedV1,GradingCompletedTopology}.java`, `services/notification-service/.../infrastructure/{GradingCompletedMessagingConfig,GradingCompletedListener}.java`, `.../application/RecordGradingCompletedUseCase.java`. Làm **giống hệt** quy ước của các mẫu này: routing key = `eventType`, `messageId` = `eventId`, dedupe bằng `insertIfAbsent(eventId, ...)`.

### common-events

```java
package com.ieltspath.events.access;

public final class AccessMessaging {
    public static final String EXCHANGE = "access.events";            // TopicExchange, durable
    public static final String SUBSCRIPTION_GRANTED_ROUTING_KEY = SubscriptionGrantedV1.EVENT_TYPE;
    private AccessMessaging() { }
}

/** Access báo một gói Premium vừa được cấp (admin cấp). docs/contracts/subscription-granted-v1.md. */
public record SubscriptionGrantedV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                    UUID subscriptionId, String planCode, Instant endsAt) {
    public static final String EVENT_TYPE = "access.subscription-granted.v1";
}

public final class SubscriptionGrantedTopology {
    public static final String QUEUE = "notification.subscription-granted.v1";
    public static final String RETRY_QUEUE = "notification.subscription-granted.v1.retry";
    public static final String DLQ = "notification.subscription-granted.v1.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.subscription-granted.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;
    public static Declarables declarables(String exchangeName) { /* như GradingCompletedTopology */ }
}
```

### access-service

Migration `services/access-service/src/main/resources/db/migration/V6__outbox_relay_status.sql`:

```sql
ALTER TABLE outbox_events DROP CONSTRAINT outbox_events_status_check;
ALTER TABLE outbox_events ADD CONSTRAINT outbox_events_status_check
    CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED', 'NOT_RELAYED'));
ALTER TABLE outbox_events ADD COLUMN last_error VARCHAR(500);
UPDATE outbox_events SET status = 'NOT_RELAYED' WHERE status = 'PENDING';
CREATE INDEX idx_access_outbox_pending ON outbox_events (created_at) WHERE status = 'PENDING';
```

Nếu tên ràng buộc thật khác `outbox_events_status_check` (kiểm bằng Testcontainers: `\d outbox_events`) → dùng tên thật, ghi trong báo cáo.

Domain (`domain/entity/OutboxEvent.java`):

```java
public static final Set<String> RELAYED_EVENT_TYPES = Set.of("SubscriptionGranted");
/** Giữ chữ ký cũ; status = PENDING nếu eventType thuộc RELAYED_EVENT_TYPES, ngược lại NOT_RELAYED. */
public static OutboxEvent create(String aggregateType, UUID aggregateId, String eventType, String payload) { }
/** Như trên nhưng id do caller cấp (id này là eventId trong payload). */
public static OutboxEvent create(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload) { }
```

Repository (`domain/repository/OutboxEventRepository.java`, thêm; giữ method cũ):

```java
List<OutboxEvent> claimPending(int limit, int maxAttempts);   // native SQL FOR UPDATE SKIP LOCKED, status='PENDING' AND retry_count < maxAttempts, ORDER BY created_at
void markPublished(UUID id, Instant publishedAt);
void recordFailure(UUID id, String error, int maxAttempts);   // retry_count+1; >= maxAttempts → status 'FAILED'; last_error cắt 500 ký tự
```

Messaging (`infrastructure/messaging/`):

```java
@Component public class AccessOutboxRelay {
    public AccessOutboxRelay(OutboxEventRepository outbox, AccessEventPublisher publisher,
                             @Value("${access.outbox.relay.batch-size:50}") int batchSize,
                             @Value("${access.outbox.relay.max-attempts:20}") int maxAttempts) { }
    @Transactional public int publishPendingBatch() { }   // y hệt assessment OutboxRelay
}
@Component public class AccessOutboxRelayScheduler {
    @Scheduled(fixedDelayString = "${access.outbox.relay.poll-interval:PT2S}") void relay() { } // bắt mọi exception, log warn
}
public interface AccessEventPublisher { void publish(OutboxEvent event); }
@Component public class RabbitAccessEventPublisher implements AccessEventPublisher {
    // eventType "SubscriptionGranted" → routing key AccessMessaging.SUBSCRIPTION_GRANTED_ROUTING_KEY; loại khác → IllegalStateException
    // body = payload đã lưu; messageId = event.id(); content-type JSON; PERSISTENT; mandatory; chờ confirm (timeout 5s); bị trả về → exception
}
@Configuration public class AccessMessagingConfig { /* TopicExchange AccessMessaging.EXCHANGE durable */ }
```

`GrantSubscriptionUseCase`: tạo `UUID eventId = UUID.randomUUID()`; payload = JSON của `SubscriptionGrantedV1(eventId, EVENT_TYPE, now, userId, saved.getId(), plan.getCode(), saved.getEndsAt())` (inject `ObjectMapper`); `OutboxEvent.create(eventId, "Subscription", saved.getId(), "SubscriptionGranted", payload)`. Không đổi giá trị trả về.

`config-repo/access-service.yaml`: thêm `spring.rabbitmq.publisher-confirm-type: correlated`, `publisher-returns: true` (giống `assessment-service.yaml:22-23`), và khối `access.outbox.relay.*`.

### notification-service

```java
package com.ieltspath.notification.infrastructure;
@Configuration public class SubscriptionGrantedMessagingConfig { /* chép GradingCompletedMessagingConfig, đổi sang SubscriptionGrantedTopology và AccessMessaging.EXCHANGE */ }
@Component public class SubscriptionGrantedListener { /* chép GradingCompletedListener: đọc lỗi → DLQ, IllegalArgumentException → DLQ, lỗi khác → retry; push qua PushBus của phase 1 */ }

package com.ieltspath.notification.application;
@Service public class RecordSubscriptionGrantedUseCase {
    /** Kiểm hợp đồng như RecordGradingCompletedUseCase; Notification(eventId, userId, userId, eventType, null, null, null, occurredAt, null); insertIfAbsent. */
    @Transactional public Optional<Notification> execute(SubscriptionGrantedV1 event, String routingKey, String messageId) { }
}
```

Nếu bảng `notifications` có CHECK giới hạn `event_type` → migration `V5__subscription_granted_notice.sql` thêm `access.subscription-granted.v1`. Frontend hiển thị loại thông báo mới: ngoài phạm vi backend, ghi trong báo cáo.

### game-service

```java
package com.ieltspath.game.infrastructure.scheduler;
@Component public class OutboxRetentionJob {
    // @Scheduled(cron = "${game.outbox.retention-cron:0 15 4 * * *}"), xóa outbox_events có created_at < now() - ${game.outbox.retention-days:30} ngày
    // bọc khóa advisory theo mẫu job dọn ở phase 1
}
```

### content-service

Xóa (đã xác nhận không chỗ nào ghi): `domain/aggregate/OutboxEvent.java`, `domain/repository/OutboxEventRepository.java`, `infrastructure/persistence/entity/OutboxEventJpaEntity.java`, `infrastructure/persistence/repository/OutboxEventJpaRepository.java`, `infrastructure/persistence/mapper/OutboxEventPersistenceMapper.java`, `infrastructure/persistence/adapter/OutboxEventRepositoryAdapter.java`. Không migration. Nếu xóa làm hỏng biên dịch ở chỗ khác → dừng hỏi.

### File được sửa

| Hành động | File |
|---|---|
| Tạo | `shared/common-events/src/main/java/com/ieltspath/events/access/{AccessMessaging,SubscriptionGrantedV1,SubscriptionGrantedTopology}.java` + test |
| Tạo | `docs/contracts/subscription-granted-v1.md` (theo mẫu `grading-completed-v1.md`) |
| Tạo | access: migration `V6__outbox_relay_status.sql`; `infrastructure/messaging/{AccessOutboxRelay,AccessOutboxRelayScheduler,AccessEventPublisher,RabbitAccessEventPublisher,AccessMessagingConfig}.java` + test |
| Sửa | access: `domain/entity/OutboxEvent.java`, `domain/repository/OutboxEventRepository.java`, `infrastructure/persistence/{adapter/OutboxEventRepositoryAdapter,repository/OutboxEventJpaRepository,entity/OutboxEventJpaEntity,mapper/AccessPersistenceMapper}.java`, `application/usecase/GrantSubscriptionUseCase.java` |
| Sửa | `infra/config-server/config-repo/{access-service,notification-service,game-service}.yaml` |
| Tạo | notification: `infrastructure/{SubscriptionGrantedMessagingConfig,SubscriptionGrantedListener}.java`, `application/RecordSubscriptionGrantedUseCase.java` + test; migration `V5` nếu cần |
| Tạo | game: `infrastructure/scheduler/OutboxRetentionJob.java` + test |
| Xóa | 6 file outbox của content (liệt kê ở trên) |

### Tên test bắt buộc

- access `GrantSubscriptionUseCaseTest`: `writesPendingSubscriptionGrantedEventInSameTransaction`, `payloadMatchesSubscriptionGrantedV1`, `rollbackLeavesNoOutboxRow`.
- access `OutboxEventTest`: `nonRelayedTypesAreStoredAsNotRelayed`.
- access `AccessOutboxRelayIntegrationTest` (Postgres + RabbitMQ Testcontainers): `publishesPendingEventAndMarksPublished`, `keepsEventPendingWhileBrokerIsDownThenPublishes`, `twoRelaysDoNotPublishTheSameEventTwice`, `marksFailedAfterMaxAttempts`, `neverRelaysNotRelayedRows`.
- access `OutboxRelayStatusMigrationTest`: `movesExistingPendingRowsToNotRelayed`.
- notification `SubscriptionGrantedListenerIntegrationTest`: `storesOneNoticeAndPushes`, `duplicateEventIdStoresOnce`, `unreadablePayloadGoesToDeadLetterQueue`, `contractViolationGoesToDeadLetterQueue`, `temporaryFailureIsRetriedThenSucceeds`.
- game `OutboxRetentionJobTest`: `deletesRowsOlderThanRetention`, `keepsRecentRows`.

### Lệnh kiểm chứng

```
mvn -B -ntp -pl shared/common-events,services/access-service,services/notification-service,services/game-service,services/content-service -am verify
```

### Kiểm tay (Claude)

Stack thật: admin cấp gói → người dùng thấy thông báo; dừng RabbitMQ, cấp gói, bật lại → thông báo đến; bảng `outbox_events` của access không còn hàng `PENDING` cũ.

### Dừng và hỏi

- `ActivateKeyUseCase` cũng cấp Premium nhưng ghi loại event khác `SubscriptionGranted`: báo, không tự thêm vào danh sách relay.
- Bảng `notifications` hoặc frontend không chấp nhận `event_type` mới mà không đổi thêm chỗ khác.
