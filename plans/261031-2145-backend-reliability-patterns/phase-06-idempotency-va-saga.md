---
phase: 6
title: "Idempotency va saga"
status: pending
priority: P2
effort: "M"
dependencies: [2, 5]
---

# Phase 6: Idempotency và saga

## Overview

Đóng lỗ hổng idempotency còn lại và chứng minh bằng test rằng hai saga có tiền và tín dụng chịu được lỗi ở mọi bước.

## Hiện trạng (đã kiểm)

- Đã có idempotency: access `POST /points/debit`, `/points/refund`, `/users/{id}/consume-human-grading` (`referenceId`, `idempotencyKey`), `POST /keys/activate`, `POST /points/adjust`; learning `POST` mở khóa khóa học (`Idempotency-Key` header, kiểm tái dùng khóa).
- Khóa tự nhiên: tín dụng chấm theo `submissionId`; yêu cầu chấm người thật theo `attemptId` + `itemId`.
- **Thiếu:** `POST /api/access/admin/subscriptions/grant` (`AdminAccessController`) không có khóa; bấm hai lần cấp hai lần.
- Consumer dedupe: access (grant id), community/notification (messageId), learning (assessment completed); cần rà mọi listener còn lại.
- Saga: mở khóa khóa học (`UnlockCourseUseCase`: intent `PENDING` → debit qua access → `ACTIVE`; `InsufficientPoints` → cancel; reconciler xử lý `PENDING` treo). Tín dụng chấm người thật (assessment `RequestHumanGradingUseCase` → access consume; lỗi sau đó → refund).

## Requirements

- Cấp gói admin nhận `Idempotency-Key` (header, bắt buộc); cùng khóa + cùng nội dung → trả kết quả cũ; cùng khóa + nội dung khác → 409 `IDEMPOTENCY_KEY_REUSED` (theo mẫu learning). Frontend không gọi endpoint này nên không cần sửa.
- Mọi `@RabbitListener` có dedupe theo `messageId`/`eventId` (bảng hoặc ràng buộc unique tự nhiên); listener nào thiếu thì thêm.
- Tài liệu máy trạng thái hai saga (Mermaid trong `docs/`), liệt kê từng cửa sổ lỗi và cách hệ thống tự hồi phục.
- Mỗi cửa sổ lỗi có một test.

## Cửa sổ lỗi cần test

| Saga | Lỗi tại | Kỳ vọng |
|---|---|---|
| Mở khóa | access timeout khi debit (không biết đã trừ chưa) | intent giữ `PENDING`; reconciler hỏi access theo `requestId`; đã trừ → `ACTIVE`, chưa → thử lại hoặc `CANCELLED`; không trừ hai lần |
| Mở khóa | debit thành công, crash trước `activate` | reconciler chuyển `ACTIVE` dùng ledger hiện có |
| Mở khóa | gửi lại cùng `Idempotency-Key` khi đang `PENDING` | 503 hiện có, không tạo intent mới |
| Mở khóa | breaker access mở (phase 5) | 503 ngay, intent không tạo hoặc giữ `PENDING` rõ ràng |
| Tín dụng chấm | consume thành công, tạo job chấm lỗi | refund đúng một lần |
| Tín dụng chấm | refund lỗi mạng | <!-- Updated: khung Codex - theo thiết kế hiện có của RequestHumanGradingUseCase --> tín dụng vẫn ở trạng thái đã tiêu; người học gửi lại cùng bài → `consume` theo `submissionId` không trừ lần hai, chỉ tạo job; không mất tín dụng, không hoàn hai lần |
| Tín dụng chấm | job có thể đã tạo nhưng không chắc (lỗi sau khi ghi) | không hoàn (tránh chấm miễn phí), như Javadoc hiện có |
| Cấp gói | gửi lại cùng khóa | một subscription, một event outbox |

## Related Code Files

- Modify: `services/access-service/.../api/controller/AdminAccessController.java`, `.../application/usecase/GrantSubscriptionUseCase.java`, request DTO
- Create: migration access (bảng hoặc cột lưu `idempotency_key` + hash nội dung cho cấp gói, unique)
- Modify (nếu thiếu dedupe): listener trong notification, learning, access
- Create: `docs/saga-course-unlock-and-human-grading.md` (máy trạng thái + bảng cửa sổ lỗi)
- Frontend: **không sửa**. <!-- Updated: khung Codex - đã kiểm frontend/src không gọi subscriptions/grant --> Đã kiểm `frontend/src`: không có màn hình hay API client nào gọi cấp gói admin (endpoint chỉ dùng qua Swagger/API). Ghi header bắt buộc vào mô tả OpenAPI của endpoint.
- Test: `services/learning-service/src/test/.../UnlockCourseFailureWindowTest.java` (tên mô tả hành vi, không mã phase), `services/assessment-service/src/test/.../HumanGradingCreditFailureWindowTest.java`

## Implementation Steps

### Test viết trước

1. Cấp gói: hai request cùng khóa → một subscription; khác nội dung → 409; thiếu khóa → 400.
2. Viết test cho từng dòng bảng cửa sổ lỗi (Testcontainers Postgres + `MockRestServiceServer` cho access). Test nào đỏ là lỗi thật: ghi lại trước khi sửa.
3. Rà listener: mỗi listener một test gửi cùng message hai lần → side-effect một lần.

### Code

4. Idempotency cấp gói.
5. Sửa các cửa sổ lỗi đỏ ở bước 2 (không viết lại saga).
6. Thêm dedupe cho listener thiếu.
7. Tài liệu máy trạng thái.

## Success Criteria

- [ ] Mọi dòng bảng cửa sổ lỗi có test xanh.
- [ ] Cấp gói gửi lại không tạo trùng.
- [ ] Mọi listener có test gửi trùng.

## Risk Assessment

- **Frontend admin chưa gửi khóa:** <!-- Updated: khung Codex - frontend không có màn hình này --> không áp dụng: frontend không gọi endpoint này (đã kiểm `frontend/src`). Người dùng Swagger/API phải gửi header; ghi trong OpenAPI.
- **Sửa saga gây hồi quy:** bước 2 khóa hành vi trước; chỉ sửa test đỏ.

## Khung code cho Codex

Điều kiện bắt đầu: phase 4 (migration access `V6`) và phase 5 (`ResilientCall`) đã có.

### Idempotency cấp gói admin (access)

Migration `services/access-service/src/main/resources/db/migration/V7__admin_grant_idempotency.sql`:

```sql
CREATE TABLE admin_grant_requests (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    request_hash    CHAR(64)     NOT NULL,
    subscription_id UUID         NOT NULL REFERENCES subscriptions(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

(Kiểm tên bảng subscription thật trong `V1__create_access_tables.sql`; khác thì dùng tên thật, ghi báo cáo.)

```java
// api/controller/AdminAccessController.java — chỉ đổi chữ ký method grantSubscription
public SubscriptionResponse grantSubscription(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                              @Valid @RequestBody GrantSubscriptionRequest request) { }

// application/command/GrantSubscriptionCommand.java — thêm field cuối
public record GrantSubscriptionCommand(UUID userId, UUID planId, int durationDays, int humanGradingCredits,
                                       String idempotencyKey) { }

// domain/repository/AdminGrantRequestRepository.java
public interface AdminGrantRequestRepository {
    /** INSERT ... ON CONFLICT (idempotency_key) DO NOTHING; true nếu chèn được. */
    boolean reserve(String idempotencyKey, String requestHash, UUID subscriptionId);
    Optional<AdminGrantRequest> find(String idempotencyKey);
}
public record AdminGrantRequest(String idempotencyKey, String requestHash, UUID subscriptionId) { }
```

Luồng `GrantSubscriptionUseCase.execute` (một transaction):

1. Khóa rỗng/blank hoặc > 100 ký tự → `InvalidIdempotencyKeyException` → 400 `IDEMPOTENCY_KEY_REQUIRED`.
2. `requestHash = sha256(userId|planId|durationDays|humanGradingCredits)` (hex thường).
3. `find(key)`: có và hash khác → 409 `IDEMPOTENCY_KEY_REUSED`; có và hash trùng → trả `SubscriptionResult` của `subscriptionId` đã lưu, **không** extend, **không** ghi outbox.
4. Không có → chạy luồng cấp như cũ → `reserve(key, hash, saved.getId())`; `reserve` trả false (request song song thắng trước) → ném exception để rollback, rồi caller nhận 409 `IDEMPOTENCY_KEY_IN_PROGRESS` (client thử lại sẽ rơi vào bước 3).
5. Thiếu header → `MissingRequestHeaderException` → `GlobalExceptionHandler` access trả 400 `IDEMPOTENCY_KEY_REQUIRED` (thêm handler nếu chưa có, chỉ cho header tên `Idempotency-Key`).
6. OpenAPI: `@Parameter(in = HEADER, name = "Idempotency-Key", required = true, description = "UUID mới cho mỗi lần cấp; gửi lại cùng khóa khi thử lại")`.

### Dedupe listener

Danh sách phải có test gửi trùng (không thêm listener khác):

| Service | Listener | Khóa dedupe hiện có / cần có |
|---|---|---|
| notification | `GradingCompletedListener`, `GradingRequestedListener`, `GradingOverdueListener`, `GradingDueSoonListener`, `SubscriptionGrantedListener` | `insertIfAbsent(eventId)` |
| notification | `CommunityEventConsumer` | `messageId` qua use case record |
| access | `LearningPointsGrantListener` | grant id (unique) |
| learning | `PointsGrantSettledListener`, `AssessmentCompletedListener` | kiểm trong code; thiếu thì thêm ràng buộc unique theo id event (migration learning `V41__...`) |

### Test cửa sổ lỗi saga

Lớp học dùng thật: learning `UnlockCourseUseCase`, `PendingCourseUnlockWriter`, `ReconcilePendingCourseUnlocks`, `LearnerCourseUnlockRepository`; assessment `RequestHumanGradingUseCase`. Access giả bằng `MockRestServiceServer` (learning) hoặc port giả (assessment). Postgres Testcontainers thật.

### File được sửa

| Hành động | File |
|---|---|
| Tạo | access: migration `V7__admin_grant_idempotency.sql`; `domain/repository/AdminGrantRequestRepository.java`; `domain/entity/AdminGrantRequest.java`; `infrastructure/persistence/adapter/JdbcAdminGrantRequestRepository.java`; `application/exception/InvalidIdempotencyKeyException.java` + test |
| Sửa | access: `api/controller/AdminAccessController.java`, `application/command/GrantSubscriptionCommand.java`, `application/usecase/GrantSubscriptionUseCase.java`, `api/exception/GlobalExceptionHandler.java` |
| Sửa (có điều kiện) | learning `PointsGrantSettledListener`/`AssessmentCompletedListener` + migration `V41__...` nếu thiếu dedupe |
| Tạo | test saga ở learning và assessment (tên bên dưới); `docs/saga-course-unlock-and-human-grading.md` |

### Tên test bắt buộc

- access `GrantSubscriptionIdempotencyTest`: `sameKeySamePayloadReturnsFirstResultWithoutExtending`, `sameKeyDifferentPayloadIsConflict`, `missingKeyIsBadRequest`, `concurrentSameKeyGrantsOnce`, `replayWritesNoSecondOutboxEvent`.
- learning `UnlockCourseFailureWindowTest`: `debitTimeoutKeepsIntentPendingAndReconcilerActivatesWhenDebited`, `debitTimeoutAndNotDebitedIsCancelledOrRetried`, `crashAfterDebitBeforeActivateIsActivatedByReconciler`, `retryWithSameKeyWhilePendingDoesNotCreateIntent`, `accessBreakerOpenFailsFastWithoutDoubleDebit`.
- assessment `HumanGradingCreditFailureWindowTest`: `jobCreationFailureRefundsOnce`, `refundFailureKeepsCreditSpentAndRetryDoesNotChargeAgain`, `uncertainJobKeepsCredit`.
- Mỗi listener trong bảng dedupe: `<Listener>DuplicateDeliveryTest.secondDeliveryHasNoSideEffect`.

Test nào đỏ vì lỗi thật (không phải vì test sai): dừng, báo tên test + hành vi thực tế, **không** tự sửa saga khi chưa có Claude duyệt.

### Lệnh kiểm chứng

```
mvn -B -ntp -pl services/access-service,services/learning-service,services/assessment-service,services/notification-service -am verify
```

### Kiểm tay (Claude)

Gọi cấp gói qua gateway hai lần cùng `Idempotency-Key` → cùng response, `subscriptions.ends_at` chỉ tăng một lần, `outbox_events` một hàng `SubscriptionGranted`.

### Dừng và hỏi

- Test cửa sổ lỗi đỏ vì hành vi thật khác bảng.
- Learning listener không có khóa tự nhiên để dedupe.
