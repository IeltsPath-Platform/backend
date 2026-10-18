# Giai đoạn 2: record payload của event

Xem [plan.md](./plan.md). Làm sau giai đoạn 1 (các lớp hằng số đã có).

## Nguyên tắc

- Record là **dữ liệu thuần**, không logic kiểm tra. Kiểm tra giá trị (ví dụ `amount` 1 đến 100, UUID hợp lệ) vẫn ở consumer.
- **JSON trên dây không đổi**: tên field, kiểu, nullability. Chứng minh bằng test (xem từng event).
- **Producer** dựng record thay cho `Map`/record chép tay; **consumer giữ parser tolerant hiện tại** (đọc `JsonNode`, bỏ qua field thừa).
  Không viết lại parser sang bind trực tiếp vào record: parser hiện có quy tắc hợp đồng (trường tùy chọn, field mới thêm sau v2).
  Lợi ích đạt được bằng **test hợp đồng ở phía consumer**: serialize record rồi cho parser/consumer đọc.
- Giai đoạn này thêm dependency compile vào `common-events`:
  - `com.fasterxml.jackson.core:jackson-annotations` (chỉ để giữ `@JsonNaming`, `@JsonInclude` đang có trên record của assessment).
  - Test scope: `com.fasterxml.jackson.core:jackson-databind`, `com.fasterxml.jackson.datatype:jackson-datatype-jsr310`.
  Phiên bản do Boot BOM quản lý, không ghi version. **Không** thêm `jackson-databind` ở scope compile.

## 2.1 Points grant

`com.ieltspath.events.points`:

```java
/** Learning asks Access to credit points; the grant id is also the AMQP message id and the idempotency key. */
public record PointsGrantRequested(UUID grantId, UUID userId, int amount, String reason, String referenceId) { }

/** Access's answer to a grant. */
public record PointsGrantSettled(UUID grantId, String status, String error) {
    public static final String GRANTED = "GRANTED";
    public static final String CAPPED = "CAPPED";
    public static final String FAILED = "FAILED";
    /** The statuses a consumer must accept. */
    public static final Set<String> STATUSES = Set.of(GRANTED, CAPPED, FAILED);
    public static PointsGrantSettled granted(UUID grantId) { return new PointsGrantSettled(grantId, GRANTED, null); }
    public static PointsGrantSettled capped(UUID grantId)  { return new PointsGrantSettled(grantId, CAPPED, null); }
    public static PointsGrantSettled failed(UUID grantId, String error) { return new PointsGrantSettled(grantId, FAILED, error); }
}
```

Việc sửa:

| Nơi | Việc |
| --- | --- |
| access `infrastructure/messaging/PointsGrantSettled.java` | **Xóa**; dùng record trong `common-events`. Cập nhật import ở `LearningPointsGrantListener` và `LearningPointsGrantListenerIntegrationTest`. |
| learning `RabbitPointsGrantPublisher.publish` | Thay `LinkedHashMap` bằng `new PointsGrantRequested(grant.id(), grant.userId(), grant.amount(), grant.reason(), grant.referenceId())` rồi `json.writeValueAsBytes(...)`. Thứ tự field JSON phải là `grantId,userId,amount,reason,referenceId` (thứ tự khai báo của record). |
| learning `RewardGrantService.ANSWERS` | `= PointsGrantSettled.STATUSES`. |
| learning `PointsGrantSettledListener` | Giữ parser `JsonNode`. Chỉ đổi nếu có literal `"GRANTED"`... |

`PointsGrantRequested` **không** thay `private record GrantRequest` trong `LearningPointsGrantListener`: record đó có `LearningRewardReason`
(enum domain của access) và là kết quả sau validate. Giữ nguyên.

Test:
- `common-events`: `PointsGrantJsonTest`: serialize bằng `new ObjectMapper()` ra đúng
  `{"grantId":"...","userId":"...","amount":5,"reason":"DAILY_QUEST","referenceId":"quest:1"}` (so sánh bằng `JsonNode`, không so chuỗi) và
  `{"grantId":"...","status":"GRANTED","error":null}`; đọc ngược được.
- learning: test publisher hiện có (`RabbitPointsGrantPublisherTest`, `PointsGrantBrokerRoundTripTest`) phải xanh; thêm assert body
  parse được bởi parser phía access: dùng chuỗi JSON do record serialize, đưa vào `LearningPointsGrantListener` ở test của access
  nếu thuận tiện, hoặc kiểm tra tên field bằng `JsonNode`.

## 2.2 `AssessmentCompleted.v2`

Chuyển nguyên văn `assessment-service/.../application/event/AssessmentCompletedV2.java` sang
`com.ieltspath.events.assessment.AssessmentCompletedV2` (cả record lồng `Data`, `ItemResult`, `KnowledgePointMapping`, hằng
`EVENT_TYPE = "AssessmentCompleted.v2"`, `SOURCE = "assessment-service"`, constructor phụ của `ItemResult`, các annotation
`@JsonNaming(SnakeCaseStrategy)` và `@JsonInclude(ALWAYS)`, và Javadoc). **Không đổi một field nào.**

| Nơi | Việc |
| --- | --- |
| assessment | Xóa file cũ; đổi import ở `AssessmentCompletedEventFactory` và mọi chỗ khác (`grep -rn AssessmentCompletedV2 services/assessment-service`), kể cả test. |
| learning | Giữ `AssessmentCompletedParser`. Thêm test hợp đồng (bên dưới). |

Test:
- `common-events`: `AssessmentCompletedV2JsonTest`: serialize một event mẫu (đủ field, một item có `knowledge_point_mappings`, một item
  `is_correct = null`) và đối chiếu với JSON mẫu trong `docs/contracts/assessment-completed-v2.md` (đặt bản sao ở
  `shared/common-events/src/test/resources/assessment-completed-v2-sample.json`, **lấy từ contract**; so sánh bằng `JsonNode`).
  Cần `jackson-datatype-jsr310` và `WRITE_DATES_AS_TIMESTAMPS` tắt, giống `ObjectMapper` của assessment.
- learning: `AssessmentCompletedContractTest`: tạo `AssessmentCompletedV2`, serialize bằng `ObjectMapper` giống production, rồi đưa
  qua `AssessmentCompletedParser.parse(byte[])`; khẳng định parse thành công và các giá trị khớp. Đây là test chống lệch hình dạng
  giữa producer và consumer mà trước đây không có.
- Test cũ `AssessmentCompletedParserTest`, `AssessmentCompletedListenerTest`, test của `AssessmentCompletedEventFactory` giữ nguyên, phải xanh.

## 2.3 Community event

Hai bản trùng: `CommunityEventOutboxAdapter.CommunityEvent` (community) và `RecordCommunityEventUseCase.CommunityEvent` (notification),
cùng `(UUID eventId, String eventType, Instant occurredAt, UUID actorId, UUID recipientId, UUID postId, UUID commentId)`.
Gộp thành `com.ieltspath.events.community.CommunityEvent` (cùng thứ tự và tên field; không annotation đặt tên, camelCase như contract
`community-events-v1.md`).

| Nơi | Việc |
| --- | --- |
| community `CommunityEventOutboxAdapter` | Xóa record lồng; dùng record chung. |
| notification `RecordCommunityEventUseCase` | Xóa record lồng; dùng record chung. `CommunityEventConsumer` (dòng dựng `new CommunityEvent(...)`) giữ parser tolerant, chỉ đổi import. |

Test:
- `common-events`: `CommunityEventJsonTest` serialize ra đúng các key `eventId, eventType, occurredAt, actorId, recipientId, postId, commentId`
  (null giữ nguyên là `null`, không bỏ key, như hành vi hiện tại của adapter; kiểm tra bằng cách chạy test cũ của community xanh trước khi sửa).
- notification: `CommunityEventConsumerIntegrationTest` và `CommunityEventRoutingContractTest` (community) giữ nguyên và xanh. Thêm một test ở
  notification đưa JSON do record chung serialize vào consumer.

## Thứ tự commit

1. `feat(events): add the points grant payloads` (module + test) → `refactor(learning)`, `refactor(access)`.
2. `feat(events): add the assessment completed event` → `refactor(assessment)` → `test(learning): parse an event built from the shared record`.
3. `feat(events): add the community event` → `refactor(community)`, `refactor(notification)`.

## Nghiệm thu giai đoạn 2

- Các test cũ của 5 service xanh, không sửa kỳ vọng; test mới ở trên xanh; JSON của từng event không đổi so với trước (test đối chiếu).
- Không còn bản `AssessmentCompletedV2`, `PointsGrantSettled`, `CommunityEvent` trùng ở service (`grep -rn "record CommunityEvent\|record AssessmentCompletedV2\|record PointsGrantSettled" services/*/src/main`).
