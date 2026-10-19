# Giai đoạn 3: `MessageRetry` và topology retry dùng chung

Xem [plan.md](./plan.md). Làm sau giai đoạn 1 và 2.

## Phạm vi (đọc kỹ)

Chỉ áp dụng cho **hai queue grant của points**, vì chúng có cùng hình dạng và hai bản `MessageRetry` giống nhau từng dòng:

| Queue | Chủ | Hiện khai báo ở |
| --- | --- | --- |
| `access.points-grant` (+ `.retry`, `.dlq`, DLX `access.points-grant.dlx`) | access | `LearningPointsMessagingConfig` (các `@Bean Queue`/`Binding`) |
| `learning.points-grant-settled` (+ `.retry`, `.dlq`, DLX `learning.points-grant-settled.dlx`) | learning | `RewardGrantMessagingConfig.pointsGrantTopology` (`Declarables`) |

**Không áp dụng** cho `learning.assessment-completed.v2` (retry exchange + DLX riêng, header `x-learning-failure`) và
`community.notifications.v1` (header `community-retry-count`, retry theo từng routing key): hình dạng khác, giữ nguyên.

## 3.1 Dependency

`common-events/pom.xml` thêm (không ghi version, do BOM quản lý):

```xml
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-amqp</artifactId>
</dependency>
```

Test: `org.mockito:mockito-core` (test scope) cho `MessageRetryTest`. **Không** thêm `spring-boot-starter-amqp`,
`spring-rabbit` ở compile (chỉ cần `RabbitTemplate` cho `MessageRetry`: nếu thiếu thì thêm `org.springframework.amqp:spring-rabbit`,
scope compile, và ghi rõ lý do trong commit).

## 3.2 `com.ieltspath.events.messaging.MessageRetry`

Sao chép **nguyên văn** từ `access-service/.../infrastructure/messaging/MessageRetry.java` (hai bản giống hệt), đổi:

- `public class MessageRetry` giữ nguyên chữ ký `MessageRetry(RabbitTemplate rabbit, String retryQueue, int maxRetries)`,
  `retryLater(Message, RuntimeException)`, và `static int retries(Message)` thành `public static int retries(Message)`.
- `ATTEMPTS_HEADER` → dùng `MessageHeaders.RETRY_COUNT` (xóa hằng cục bộ).

## 3.3 `com.ieltspath.events.messaging.RetryTopology`

Một hàm tĩnh dựng đúng các thứ mà `LearningPointsMessagingConfig`/`RewardGrantMessagingConfig` đang dựng, **cùng tham số broker**:

```java
public final class RetryTopology {
    public record Names(String queue, String retryQueue, String deadLetterQueue, String deadLetterExchange) { }

    /** The main queue dead-letters to the DLQ through its own direct DLX; the retry queue holds a message for
     *  {@code retryDelayMs}, then hands it back to the main queue through the default exchange. */
    public static Declarables of(Names n, long retryDelayMs, Exchange sourceExchange, String routingKey) { ... }
}
```

Hành vi phải khớp từng tham số với code hiện tại (đây là chỗ dễ lệch và gây `PRECONDITION_FAILED`):

- Main queue: `QueueBuilder.durable(queue).deadLetterExchange(deadLetterExchange).deadLetterRoutingKey(deadLetterQueue)`.
- Retry queue: `QueueBuilder.durable(retryQueue).ttl((int) retryDelayMs).deadLetterExchange("").deadLetterRoutingKey(queue)`.
- DLQ: `QueueBuilder.durable(deadLetterQueue)`; DLX: `new DirectExchange(deadLetterExchange, true, false)`; binding DLQ→DLX với key = tên DLQ.
- Binding main queue → `sourceExchange` với `routingKey`.
- `sourceExchange` (`learning.points`, `new TopicExchange(name, true, false)`) do **service tạo** và truyền vào, vì cả hai bên cùng khai báo nó (phải giống nhau).

## 3.4 Thay thế

| Service | Việc |
| --- | --- |
| access `LearningPointsMessagingConfig` | Dùng `RetryTopology.of(...)` cho 4 queue/exchange/binding của grant; giữ `@Bean` `MessageConverter`, container factory, `MessageRetry` bean. Xóa các `@Bean Queue`/`Binding` lẻ. Lưu ý lỗi từng gặp: nhiều `Queue` bean gây mơ hồ khi inject; dùng `Declarables` như learning là cách tránh. |
| access | **Xóa** `MessageRetry.java` và `MessageRetryTest.java`; import `com.ieltspath.events.messaging.MessageRetry`. Cập nhật `LearningPointsGrantListener`, `LearningPointsGrantListenerFailureTest`. |
| learning `RewardGrantMessagingConfig` | Dùng `RetryTopology.of(...)` cho settled queue. Giữ container factory và `MessageRetry` bean. |
| learning | **Xóa** `MessageRetry.java` và `MessageRetryTest.java`; import bản chung. Cập nhật `PointsGrantSettledListener`, `PointsGrantSettledListenerTest`, `PointsGrantSettledDeadLetterBrokerTest`, `PointsGrantUnroutedBrokerTest`. |

## 3.5 Test

- `common-events`: `MessageRetryTest` = hợp nhất hai test cũ: tăng header `x-retry-count`, gửi vào queue retry qua exchange mặc định `""`,
  giữ `messageId` và body, ném `AmqpRejectAndDontRequeueException` sau lần cuối, lần đầu header coi là 0.
- `common-events`: `RetryTopologyTest` khẳng định từng `Queue` do helper dựng có đúng `getArguments()`:
  `x-dead-letter-exchange`, `x-dead-letter-routing-key`, `x-message-ttl` = 30000 cho queue retry; durable, không auto-delete, không exclusive.
  Đối chiếu bằng giá trị literal của hai queue grant (so với code cũ trước khi xóa: chạy test này **trước** khi xóa code cũ).
- Giữ nguyên và chạy xanh (Docker, không skip): `PointsGrantDeadLetterBrokerTest` (access), `PointsGrantSettledDeadLetterBrokerTest`,
  `PointsGrantUnroutedBrokerTest`, `PointsGrantBrokerRoundTripTest` (learning). Chúng chứng minh topology và hành vi retry không đổi với broker thật.
- Thủ công, nếu có broker dev đã khai báo sẵn các queue grant: khởi động access và learning với broker đó và xác nhận **không** có
  `PRECONDITION_FAILED` trong log (bằng chứng tham số queue không lệch so với bản cũ). Ghi kết quả vào PR; nếu không có broker dev thì
  nói rõ chưa kiểm tra bước này.
- `RETRY_DELAY_MS = 30_000` và `MAX_RETRIES = 5` đã ở `PointsGrantMessaging` từ giai đoạn 1 (cùng giá trị cho cả hai bên) và được ghim trong
  `EventNamesContractTest`. `RetryTopology` nhận `retryDelayMs` làm tham số; hai service truyền `PointsGrantMessaging.RETRY_DELAY_MS`.

## 3.6 Commit

1. `feat(events): add the shared message retry and retry topology` (module + `MessageRetryTest` + `RetryTopologyTest`).
2. `refactor(access): use the shared message retry and topology`.
3. `refactor(learning): use the shared message retry and topology`.

## Nghiệm thu giai đoạn 3

- Còn đúng một `MessageRetry` (`grep -rn "class MessageRetry" services shared`).
- Test broker thật xanh, không `PRECONDITION_FAILED` khi khởi động hai service với broker đã có queue; số test của access/learning giảm đúng số test `MessageRetryTest` đã chuyển.
