---
phase: 7
title: "Ra messaging va concurrency"
status: pending
priority: P3
effort: "M"
dependencies: [4]
---

# Phase 7: Rà messaging và concurrency

## Overview

Messaging và concurrency đã mạnh. Phase này rà cho đồng nhất, viết quy tắc version event, thêm công cụ đưa message từ DLQ về xử lý lại, và lấp các chỗ lệch nhỏ; không thêm hạ tầng mới.

## Hiện trạng (đã kiểm)

- Chung: `shared/common-events` có `MessageRetry`, `RetryTopology`, `MessageHeaders`; contract có hậu tố version (`GradingCompletedV1`, `AssessmentCompletedV2`).
- Listener dùng helper chung: notification (grading ×4), access (`LearningPointsGrantListener`), learning (`PointsGrantSettledListener`).
- Listener tự làm retry/park riêng: `notification/.../CommunityEventConsumer` (header `community-retry-count`, park có confirm) và `learning/.../AssessmentCompletedListener` (ACK/NACK thủ công).
- Concurrency: `@Version` ở `SubscriptionJpaEntity`, `PointWalletJpaEntity`, `PostJpaEntity`, `CommentJpaEntity`, `AssessmentAttemptJpaEntity`, `AttemptResponseJpaEntity`; advisory lock ở content, learning, user; `SKIP LOCKED` ở relay và store.

## Requirements

- Mỗi listener thuộc một trong hai kiểu được ghi rõ: (a) helper chung `MessageRetry`, hoặc (b) tự xử lý có lý do ghi trong Javadoc. Chuyển `CommunityEventConsumer` và `AssessmentCompletedListener` sang (a) **chỉ khi** test hiện có giữ xanh và hành vi không đổi; nếu không, giữ (b) và ghi lý do.
- Quy tắc version event (tài liệu ngắn trong `shared/common-events/README.md`):
  - Thêm field tùy chọn: giữ version, consumer bỏ qua field lạ (`FAIL_ON_UNKNOWN_PROPERTIES=false`, test bắt buộc).
  - Đổi nghĩa, đổi tên, xóa field, đổi kiểu: tạo `...V{n+1}`, routing key mới, publish song song đến khi mọi consumer chuyển.
  - Mỗi contract có test JSON mẫu (golden) để phát hiện thay đổi vô ý.
- Concurrency: rà aggregate bị nhiều request cùng sửa mà chưa có `@Version` hay khóa (đề cử: `learning` tiến độ bài học, `library` bộ thẻ, `game` phòng). Thêm `@Version` hoặc khóa hàng khi có test chứng minh mất cập nhật; không thêm vô căn cứ.
- Không thêm lock phân tán ngoài Postgres (đã chốt).
- <!-- Updated: bổ sung sau validate - đưa message từ DLQ về xử lý lại --> **Đưa lại từ DLQ:** hiện chỉ có hướng dẫn tay cho một event (`docs/contracts/assessment-completed-v2.md:172`). Thêm công cụ chung: admin xem số message trong mỗi DLQ của service và chuyển tối đa N message về queue chính (đặt lại bộ đếm retry), có publisher confirm, chỉ ack message DLQ sau khi broker xác nhận. Chỉ cho các cặp DLQ → queue khai báo sẵn (không nhận tên queue tùy ý). Áp cho 3 service có listener: notification, access, learning.

## Related Code Files

- Modify: `shared/common-events/README.md`; test golden JSON trong `shared/common-events/src/test`
- Modify (có điều kiện): `services/notification-service/.../CommunityEventConsumer.java`, `services/learning-service/.../AssessmentCompletedListener.java`
- Modify (có điều kiện): entity được chứng minh mất cập nhật

## Implementation Steps

### Test viết trước

1. Golden JSON cho mọi contract trong common-events (serialize → so file mẫu; deserialize mẫu có thêm field lạ → không lỗi).
2. Với mỗi aggregate đề cử: test hai transaction song song cùng sửa → khẳng định có hay không mất cập nhật. Chỉ aggregate đỏ mới sửa.
3. Chạy lại test listener hiện có làm mốc trước khi đổi kiểu listener.

### Code

4. Viết quy tắc version.
5. Chuyển listener sang helper chung nếu an toàn; nếu không, thêm Javadoc lý do.
6. Thêm `@Version`/khóa cho aggregate đỏ (migration thêm cột `version` nếu cần).

## Success Criteria

- [ ] Có quy tắc version và golden test cho mọi contract.
- [ ] Mọi listener được phân loại (a)/(b) có lý do.
- [ ] Mọi aggregate đề cử có test song song; aggregate có lỗi đã sửa.
- [ ] Admin xem được số message DLQ và đưa lại tối đa N message ở notification, access, learning; message đưa lại được xử lý đúng một lần.

## Risk Assessment

- **Đổi listener đang chạy tốt:** chỉ đổi khi test giữ xanh; mặc định giữ nguyên.
- **Thêm `@Version` làm request đang chạy nhận 409:** map `OptimisticLockException` thành 409 có mã rõ (đã có ở assessment), frontend thử lại.
- **Đưa lại DLQ khi lỗi gốc chưa sửa:** message quay lại DLQ sau vài lần retry; giới hạn N mỗi lần gọi, hướng dẫn ghi rõ "sửa nguyên nhân trước khi đưa lại".

## Khung code cho Codex

Điều kiện bắt đầu: phase 4 (topology `SubscriptionGranted`) đã có.

### Đưa lại từ DLQ (`shared/common-events`)

```java
package com.ieltspath.events.messaging;

/** Cặp được phép: DLQ → queue chính. */
public record DeadLetterRoute(String deadLetterQueue, String queue) { }

public record ReplayResult(String deadLetterQueue, int moved, int remaining) { }
public record DeadLetterCount(String deadLetterQueue, String queue, long messages) { }

public class DeadLetterReplayer {
    public DeadLetterReplayer(RabbitTemplate rabbit, AmqpAdmin admin, List<DeadLetterRoute> routes) { }
    public List<DeadLetterCount> counts() { }
    /**
     * Lấy lần lượt bằng basicGet (không auto-ack) tối đa max message (1..500) từ DLQ thuộc routes;
     * xóa header MessageHeaders.RETRY_COUNT; publish tới exchange "" routing key = queue chính, giữ messageId và body,
     * chờ publisher confirm; confirm OK mới basicAck message DLQ; lỗi → basicNack(requeue=true) và dừng.
     * DLQ không thuộc routes → IllegalArgumentException.
     */
    public ReplayResult replay(String deadLetterQueue, int max) { }
}
```

Mỗi service khai báo routes bằng hằng có sẵn trong Topology (không viết tên queue bằng chuỗi mới):

| Service | Routes |
|---|---|
| notification | `GradingCompletedTopology`, `GradingRequestedTopology`, `GradingOverdueTopology`, `GradingDueSoonTopology`, `SubscriptionGrantedTopology` (DLQ → QUEUE); `community.notifications.v1.dlq` → `community.notifications.v1` (đưa thành hằng trong `CommunityMessaging` nếu chưa có) |
| access | points grant DLQ → queue (hằng trong `PointsGrantMessaging`/`LearningPointsMessagingConfig`) |
| learning | points-grant-settled DLQ, assessment-completed DLQ → queue tương ứng (hằng trong config hiện có) |

Controller mỗi service (cùng hình, khác prefix theo route gateway đã có):

```java
@RestController
@RequestMapping("/api/notifications/admin/dead-letters")   // access: /api/access/admin/dead-letters; learning: /api/learning/admin/dead-letters
@PreAuthorize("hasRole('ADMIN')")
public class DeadLetterAdminController {
    @GetMapping public List<DeadLetterCount> counts() { }
    @PostMapping("/{deadLetterQueue}/replay") public ReplayResult replay(@PathVariable String deadLetterQueue,
                                                                        @RequestParam(defaultValue = "50") int max) { }
    // max ngoài 1..500 → 400; DLQ không khai báo → 404
}
```

Log mỗi lần replay: `deadLetterQueue`, `moved`, `remaining`, userId admin (không log body).

Tài liệu: `shared/common-events/README.md` mục "Đưa lại message từ DLQ" (khi nào dùng, sửa nguyên nhân trước, ví dụ curl qua gateway). Cập nhật `docs/contracts/assessment-completed-v2.md` mục "Rollout and DLQ replay" trỏ về công cụ mới.

### Quy tắc version và golden test

- `shared/common-events/README.md` mục "Version event": nội dung như phần Requirements.
- Golden file: `shared/common-events/src/test/resources/golden/<EventType>.json` cho mọi record `*V1`, `*V2` trong `com.ieltspath.events`. Test `EventContractGoldenTest`: (1) serialize record mẫu cố định = file golden; (2) deserialize golden có thêm field `"unknownField": 1` không lỗi.

### Listener và concurrency

- `CommunityEventConsumer`, `AssessmentCompletedListener`: **mặc định giữ nguyên**, chỉ thêm Javadoc lý do tự xử lý ACK/retry. Chuyển sang `MessageRetry` chỉ khi mọi test hiện có của listener giữ xanh không sửa test; nếu phải sửa test → giữ nguyên, ghi báo cáo.
- Aggregate đề cử kiểm mất cập nhật: learning tiến độ bài học (bảng do `access.refresh(...)` ghi), library bộ thẻ, game phòng. Test hai transaction song song; chỉ sửa khi test đỏ: thêm cột `version BIGINT NOT NULL DEFAULT 0` + `@Version` (migration số kế tiếp của service đó tại thời điểm làm) và handler 409 `CONCURRENT_UPDATE`.

### File được sửa

| Hành động | File |
|---|---|
| Tạo | `shared/common-events/src/main/java/com/ieltspath/events/messaging/{DeadLetterRoute,ReplayResult,DeadLetterCount,DeadLetterReplayer}.java` + test (RabbitMQ Testcontainers) |
| Tạo | `services/{notification,access,learning}-service/.../api/controller/DeadLetterAdminController.java` + bean routes trong config messaging hiện có + test |
| Tạo | `shared/common-events/src/test/.../EventContractGoldenTest.java`, `src/test/resources/golden/*.json` |
| Sửa | `shared/common-events/README.md`, `docs/contracts/assessment-completed-v2.md` |
| Sửa (Javadoc) | `services/notification-service/.../CommunityEventConsumer.java`, `services/learning-service/.../AssessmentCompletedListener.java` |
| Sửa (có điều kiện) | entity + migration của aggregate có test mất cập nhật đỏ |

### Tên test bắt buộc

- `DeadLetterReplayerIntegrationTest`: `movesUpToMaxMessagesBackAndResetsRetryCount`, `keepsMessageIdAndBody`, `acksDeadLetterOnlyAfterConfirm`, `rejectsUndeclaredQueue`, `reportsCounts`.
- `DeadLetterAdminControllerTest` (mỗi service): `nonAdminIsForbidden`, `maxOutOfRangeIsBadRequest`, `unknownQueueIsNotFound`.
- `ReplayedMessageIsProcessedTest` (notification): message hỏng tạm vào DLQ → sửa điều kiện → replay → thông báo được lưu đúng một lần.
- `EventContractGoldenTest`: `serializesToGolden`, `ignoresUnknownFields` (tham số hóa theo từng record).
- `<Aggregate>ConcurrentUpdateTest` cho mỗi aggregate đề cử.

### Lệnh kiểm chứng

```
mvn -B -ntp -pl shared/common-events,services/notification-service,services/access-service,services/learning-service,services/library-service,services/game-service -am verify
```

### Kiểm tay (Claude)

Đẩy một message JSON hỏng vào queue `notification.subscription-granted.v1` qua RabbitMQ UI → vào DLQ; `GET /api/notifications/admin/dead-letters` thấy 1; replay → quay lại DLQ (vẫn hỏng) và số đếm đúng.

### Dừng và hỏi

- notification-service hoặc access-service chưa bật method security (`@PreAuthorize` không có tác dụng): kiểm bằng test `nonAdminIsForbidden`; nếu xanh không do `@PreAuthorize` → dừng.
- Tên DLQ của listener không có hằng để tham chiếu và phải viết chuỗi mới.
