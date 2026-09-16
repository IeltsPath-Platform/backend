---
phase: 4
title: "Provisioning profile bằng transactional outbox"
status: pending
priority: P1
dependencies: [2, 3]
---

# Phase 4: Provisioning profile bằng transactional outbox

## Overview

Thay HTTP call trong transaction bằng outbox PostgreSQL + worker idempotent.
Không thêm Kafka/RabbitMQ vì database polling đáp ứng scope hiện tại.

## Architecture

```text
register -> save user + outbox row (one DB transaction)
         -> commit
worker   -> claim one outbox row -> call profile service with idempotency key
         -> success: mark processed / activate user per policy
         -> retryable failure: schedule retry
         -> exhausted: dead state + metric/alert, never silently succeed
```

## Related Code Files

- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\application\usecase\RegisterUseCase.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\application\usecase\ProfileProvisioningClient.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\infrastructure\profile\ProfileProvisioningRestClient.java`
- Modify if policy selected: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\domain\vo\UserStatus.java`
- Create entity/repository/adapter/worker under: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\infrastructure\profile\outbox\`
- Create: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\resources\db\migration\V8__create_profile_provisioning_outbox.sql`
- Create/Modify tests under: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\test\java\com\group01\user\infrastructure\profile\`

## Implementation Steps

1. Chốt product policy `PENDING_PROFILE`: khuyến nghị disable login cho tới khi
   profile provider xác nhận success. Nếu không chấp nhận, expose provisioning
   state rõ ràng; tuyệt đối không nuốt lỗi và trả success im lặng.
2. Trong `RegisterUseCase`, lưu user và outbox command trong cùng transaction;
   loại synchronous doctor/patient call khỏi request path.
3. Thêm outbox schema gồm user ID, profile type, dedupe/idempotency key, status,
   attempt count, next-attempt time, last error (đã sanitize), timestamps.
4. Worker claim rows an toàn nhiều instance (`FOR UPDATE SKIP LOCKED` hoặc
   atomic status transition), giới hạn batch/concurrency, exponential backoff.
5. `RestClient` đặt connect/read timeout, map 404/4xx/5xx riêng; gửi correlation
   ID và idempotency key. Service credential chỉ lấy từ secret config.
6. Đánh dấu processed sau response idempotent success; exhausted rows vào DEAD
   và metric/health indicator để vận hành xử lý, không retry vô hạn.

## Todo List

- [ ] Patient và doctor failure có cùng semantics quan sát được.
- [ ] Restart worker không tạo profile trùng.
- [ ] Concurrent worker không xử lý cùng outbox row.
- [ ] Remote timeout/5xx/4xx có retry policy khác nhau.

## Success Criteria

- [ ] Test DB xác nhận user + outbox atomically persist/rollback.
- [ ] HTTP contract test xác nhận idempotency/correlation/timeout behavior.
- [ ] Integration test worker retry rồi success; exhausted failure thành DEAD.
- [ ] Không còn remote HTTP call trong registration transaction.

## Risk Assessment

- Outbox là eventual consistency; dashboard/support phải hiểu trạng thái chờ.
- Provider phải hỗ trợ idempotency key hoặc lookup theo user ID; nếu không, cần
  contract thay đổi với owner service trước rollout.
