# Giai đoạn 1: khung module và hằng số

Xem [plan.md](./plan.md) (quy tắc bắt buộc áp dụng cho cả giai đoạn này).

## 1.1 Tạo module `shared/common-events`

Tạo `shared/common-events/pom.xml` theo mẫu của `shared/common-security/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.ieltspath</groupId>
        <artifactId>code-base</artifactId>
        <version>1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-events</artifactId>
    <name>common-events</name>
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

(Giai đoạn 1 **không có dependency compile nào**. Phiên bản JUnit do Boot BOM quản lý.) Nếu `common-security` khai báo thêm
plugin Surefire/compiler thì sao chép đúng cấu hình đó; không phát minh cấu hình mới.

Sửa `pom.xml` gốc: thêm `<module>shared/common-events</module>` ngay **trước** `shared/common-security` (hoặc ngay sau; miễn là trước
các service). Không cần `spring-boot-maven-plugin` (đây là thư viện, không phải app).

Tạo `shared/common-events/src/main/java/com/ieltspath/events/package-info.java`:

```java
/** The names and shapes of the events services exchange over the message broker; no behaviour lives here. */
package com.ieltspath.events;
```

## 1.2 Các lớp hằng số

Mọi lớp: `public final class`, constructor `private` rỗng, chỉ có `public static final`. Javadoc ngắn ghi **ai publish, ai
consume**, và liên kết tới contract trong `docs/contracts/`.

### `com.ieltspath.events.messaging.MessageHeaders`

```java
public final class MessageHeaders {
    /** How many times a message has been parked on a delayed retry queue; set by the consumer that retries it. */
    public static final String RETRY_COUNT = "x-retry-count";
    private MessageHeaders() { }
}
```

### `com.ieltspath.events.points.PointsGrantMessaging`

Giá trị **chính xác** (đang chạy ở access `LearningPointsMessagingConfig` và learning `RewardGrantMessagingConfig`):

```java
public static final String EXCHANGE = "learning.points";                       // topic, durable, không auto-delete
public static final String REQUESTED_ROUTING_KEY = "points.grant.requested";   // learning -> access
public static final String SETTLED_ROUTING_KEY = "points.grant.settled";       // access -> learning
/** Both sides park a failed message for this long before trying again, and give up after MAX_RETRIES. */
public static final long RETRY_DELAY_MS = 30_000;
public static final int MAX_RETRIES = 5;
```

### `com.ieltspath.events.assessment.AssessmentMessaging`

```java
public static final String EXCHANGE = "assessment.events";                     // topic, durable
public static final String COMPLETED_ROUTING_KEY = "assessment.completed.v2";
```

### `com.ieltspath.events.community.CommunityMessaging`

```java
public static final String EXCHANGE = "community.events";                      // topic, durable
```

> `community.events.retry` và `community.events.dlx` chỉ notification dùng (community chỉ publish), nên theo quy tắc số 2 **không đưa**
> vào `common-events`; chúng ở lại `NotificationMessagingConfig`. Chỉ đưa `EXCHANGE`.

### `com.ieltspath.events.community.CommunityEventTypes`

Hiện bị chép ở `community-service/.../application/port/CommunityEventTypes.java` **và**
`notification-service/.../application/NotificationEventTypes.java`. Gộp thành một:

```java
public static final String POST_COMMENTED   = "community.post-commented.v1";
public static final String COMMENT_REPLIED  = "community.comment-replied.v1";
public static final String COMMENT_MENTIONED = "community.comment-mentioned.v1";
public static final String POST_REACTED     = "community.post-reacted.v1";
public static final String USER_FOLLOWED    = "community.user-followed.v1";
/** In the order notification preferences list them. */
public static final List<String> ORDERED = List.of(POST_COMMENTED, COMMENT_REPLIED, COMMENT_MENTIONED, POST_REACTED, USER_FOLLOWED);
public static final Set<String> ALL = Set.copyOf(ORDERED);
```

`eventType` bằng routing key (xem `docs/contracts/community-events-v1.md`), nên một chuỗi dùng cho cả hai; không tạo hằng số routing key riêng.

## 1.3 Thay thế ở từng service

Với mỗi service: thêm dependency (`com.ieltspath:common-events:${project.version}`) vào `pom.xml` của service, đổi import, **giữ nguyên
tên và giá trị queue của service**.

| Service | File cần sửa (main) | Đổi gì |
| --- | --- | --- |
| access | `infrastructure/messaging/LearningPointsMessagingConfig.java` | Xóa `EXCHANGE`, `GRANT_ROUTING_KEY`, `SETTLED_ROUTING_KEY`; dùng `PointsGrantMessaging.*`. Giữ `GRANT_QUEUE`, `GRANT_RETRY_QUEUE`, `GRANT_DLQ`, `DEAD_LETTER_EXCHANGE`, `CONTAINER_FACTORY`. Xóa `RETRY_DELAY_MS`, `MAX_RETRIES` (dùng bản trong `PointsGrantMessaging`). |
| access | `LearningPointsGrantListener.java` | Đổi tham chiếu `EXCHANGE`/`SETTLED_ROUTING_KEY` sang `PointsGrantMessaging`. |
| access | `MessageRetry.java` | `ATTEMPTS_HEADER` = `MessageHeaders.RETRY_COUNT` (xong hẳn ở giai đoạn 3). |
| learning | `infrastructure/messaging/RewardGrantMessagingConfig.java` | Xóa `EXCHANGE`, `REQUESTED_ROUTING_KEY`, `SETTLED_ROUTING_KEY`; dùng `PointsGrantMessaging.*`. Xóa `RETRY_DELAY_MS`, `MAX_RETRIES` (dùng bản trong `PointsGrantMessaging`). Giữ queue/DLQ/DLX/factory. |
| learning | `RabbitPointsGrantPublisher.java`, `PointsGrantSettledListener.java` | Đổi tham chiếu hằng số. |
| learning | `AssessmentMessagingConfig.java` | `EXCHANGE`, `ROUTING_KEY` → `AssessmentMessaging.EXCHANGE`, `COMPLETED_ROUTING_KEY`. Giữ `QUEUE`, `RETRY_*`, `DEAD_LETTER_*`. |
| learning | `MessageRetry.java` | Như access (giai đoạn 3). |
| assessment | `infrastructure/messaging/AssessmentMessagingConfiguration.java` | Default của `${assessment.messaging.exchange:...}` lấy từ hằng: `@Value("${assessment.messaging.exchange:" + AssessmentMessaging.EXCHANGE + "}")`. |
| assessment | `infrastructure/messaging/RabbitOutboxEventPublisher.java` | Cùng cách cho `@Value`; `ROUTING_KEYS = Map.of(AssessmentCompletedV2.EVENT_TYPE, AssessmentMessaging.COMPLETED_ROUTING_KEY)`. |
| community | `infrastructure/messaging/CommunityMessagingConfig.java` | `EXCHANGE` → `CommunityMessaging.EXCHANGE`; `@Value` default dùng hằng. |
| community | `application/port/CommunityEventTypes.java` | **Xóa**; thay bằng `com.ieltspath.events.community.CommunityEventTypes`. Sửa import ở `AddReactionUseCase`, `CreateCommentUseCase`, `ManageSocialUseCase`, `CommunityEventOutboxAdapter`, `CommunityOutboxRelay`. |
| notification | `application/NotificationEventTypes.java` | **Xóa**; dùng `CommunityEventTypes`. Sửa `NotificationPreferenceService`, `RecordCommunityEventUseCase`. |
| notification | `infrastructure/NotificationMessagingConfig.java` | Thay literal `"community.events"` (exchange), 5 literal trong mảng `routes` bằng `CommunityMessaging.EXCHANGE` và `CommunityEventTypes.ORDERED`. Giữ literal queue/retry/DLX. |
| notification | `infrastructure/CommunityEventConsumer.java` | Không có literal event type; **không đổi** `"community.events.dlx"`, `"community.events.retry"`, `"community.notifications.v1*"`. Chỉ đổi nếu grep (giai đoạn 4) báo còn literal exchange `community.events`. |

Sửa các chỗ dùng `RETRY_DELAY_MS`/`MAX_RETRIES` trong `LearningPointsMessagingConfig` và `RewardGrantMessagingConfig` (bean `MessageRetry`, TTL queue retry).
Mỗi file sửa xong phải `grep` lại literal cũ (lệnh ở giai đoạn 4) để chắc không sót.

Test (src/test) phải đổi import theo, không đổi nội dung kiểm tra:
`LearningPointsGrantListenerFailureTest`, `LearningPointsGrantListenerIntegrationTest`, `PointsGrantDeadLetterBrokerTest` (access);
`AssessmentCompletedListenerTest`, `PointsGrantSettledDeadLetterBrokerTest`, `PointsGrantSettledListenerTest`,
`PointsGrantUnroutedBrokerTest`, `PointsGrantBrokerRoundTripTest` (learning); `CommunityEventRoutingContractTest`,
`CommunityOutboxRelayIntegrationTest` (community); `NotificationPreferenceServiceTest`, `CommunityEventConsumerIntegrationTest` (notification).

## 1.4 Test cho module

`shared/common-events/src/test/java/com/ieltspath/events/EventNamesContractTest.java`: **ghim từng giá trị bằng literal** (đây là
test thay cho hai `PointsGrantTopologyContractTest`):

```java
assertEquals("learning.points", PointsGrantMessaging.EXCHANGE);
assertEquals("points.grant.requested", PointsGrantMessaging.REQUESTED_ROUTING_KEY);
assertEquals("points.grant.settled", PointsGrantMessaging.SETTLED_ROUTING_KEY);
assertEquals("assessment.events", AssessmentMessaging.EXCHANGE);
assertEquals("assessment.completed.v2", AssessmentMessaging.COMPLETED_ROUTING_KEY);
assertEquals("community.events", CommunityMessaging.EXCHANGE);
assertEquals("x-retry-count", MessageHeaders.RETRY_COUNT);
assertEquals(30_000L, PointsGrantMessaging.RETRY_DELAY_MS);
assertEquals(5, PointsGrantMessaging.MAX_RETRIES);
assertEquals(List.of("community.post-commented.v1", "community.comment-replied.v1", "community.comment-mentioned.v1",
        "community.post-reacted.v1", "community.user-followed.v1"), CommunityEventTypes.ORDERED);
```

Sau khi test này xanh: **xóa** `PointsGrantTopologyContractTest` ở access và learning (test đó cũng ghim `RETRY_DELAY_MS`,
`MAX_RETRIES` và header; nay đã có trong `EventNamesContractTest`). Cập nhật `NotificationPreferenceServiceTest` nếu nó từng test `NotificationEventTypes`.

## 1.5 Commit

1. `feat(events): add the shared module holding event names` (module + test).
2. Một commit `refactor(<service>): ...` cho mỗi service, mô tả "read exchange and routing key names from the shared module".
3. `test: drop the duplicated topology contract tests` (xóa hai test, sau khi test mới đã có).

## Nghiệm thu giai đoạn 1

- `mvn -q -pl shared/common-events test` xanh; test của 5 service xanh; số test giảm đúng 2 (hai test bị thay) và tăng 1.
- Test broker thật chạy (không skip). `grep` không còn literal exchange/routing key ngoài `common-events`.
