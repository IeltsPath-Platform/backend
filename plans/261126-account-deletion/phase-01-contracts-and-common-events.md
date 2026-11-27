# Phase 1: Contract `user.deleted.v1` và `shared/common-events`

## Mục tiêu
Định nghĩa contract sự kiện `user.deleted.v1` và bổ sung hằng số + payload record vào `shared/common-events` để producer (`user-service`) và tất cả consumer dùng chung, không chứa PII.

## Chi tiết thay đổi

### 1. Contract `docs/contracts/user-deleted-v1.md`
Theo mẫu `docs/contracts/user-password-changed-v1.md`:
- **Exchange:** `user.events` (`UserMessaging.EXCHANGE`, TopicExchange, durable).
- **Routing key / `eventType`:** `user.deleted.v1` (`UserMessaging.USER_DELETED_ROUTING_KEY`).
- **Payload JSON:**

| Field | Type | Meaning |
| --- | --- | --- |
| `eventId` | UUID | Unique event id (dùng làm khóa idempotent ở consumer). |
| `eventType` | string | `user.deleted.v1`. |
| `occurredAt` | ISO-8601 instant | Thời điểm tài khoản bị xóa. |
| `userId` | UUID | ID tài khoản đã bị xóa. |

- Không mang `email`, `displayName`, `phoneNumber` hay bất kỳ PII nào.
- Correlation & tracing đi qua AMQP header (`x-correlation-id`, `traceparent`), không đưa vào payload (AGENTS.md §3.9).

### 2. `shared/common-events`
File sửa/thêm trong `shared/common-events/src/main/java/com/ieltspath/events/user/`:
- `UserMessaging.java`: thêm `public static final String USER_DELETED_ROUTING_KEY = "user.deleted.v1";`
- `UserDeletedV1.java`:
  ```java
  public record UserDeletedV1(UUID eventId, String eventType, Instant occurredAt, UUID userId) {
      public static final String EVENT_TYPE = UserMessaging.USER_DELETED_ROUTING_KEY;
  }
  ```
- Không thêm tên queue/retry/DLQ vào `common-events` (AGENTS.md §3.5: mỗi consumer sở hữu queue, retry queue, DLQ riêng).

## Kiểm chứng
- `mvn -q -pl shared/common-events test`
