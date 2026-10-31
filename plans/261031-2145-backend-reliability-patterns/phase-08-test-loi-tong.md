---
phase: 8
title: "Test loi tong"
status: pending
priority: P2
effort: "M"
dependencies: [1, 2, 3, 4, 5, 6, 7]
---

# Phase 8: Test lỗi tổng

## Overview

Gom thành một bộ test lỗi đầu-cuối chạy trong CI, chứng minh hệ thống chịu được các lỗi hạ tầng chính khi chạy nhiều instance. Các phase trước đã có test lỗi cục bộ; phase này test xuyên service.

## Requirements

Kịch bản bắt buộc (mỗi kịch bản một test, tên mô tả hành vi):

| Kịch bản | Kỳ vọng | Bước liên quan |
|---|---|---|
| Downstream chậm hơn timeout | caller trả 503 sau timeout; breaker mở; lời gọi sau nhanh | B5, B7 |
| Downstream 5xx liên tiếp rồi hồi phục | breaker mở rồi đóng; không treo luồng | B5 |
| RabbitMQ mất kết nối khi có event | outbox giữ `PENDING`; gửi sau khi có lại; consumer không nhận trùng | B2, B3, B1 |
| Message sai hợp đồng | vào DLQ, không chặn message sau | B3 |
| Redis chết | API thường vẫn qua; login trả 503; push game và thông báo thời gian thực dừng nhưng REST vẫn đúng | B8 |
| 2 instance tranh cùng job | mỗi đơn vị việc 1 lần | B6 |
| Gửi lại request có tiền | không trừ hai lần | B1, B4 |
| Một request đi qua 3 service + 1 consumer | cùng `correlationId` ở mọi log | B9 |
| Downstream treo sau gateway | gateway trả 504 theo định dạng lỗi chung | B5 |
| Provider đổi hợp đồng API nội bộ | contract test provider đỏ | B7 |
| Message vào DLQ rồi được đưa lại sau khi sửa nguyên nhân | xử lý đúng một lần | B3 |

## Architecture

- <!-- Updated: khung Codex - chốt vị trí test --> Mỗi kịch bản đặt trong service sở hữu hành vi (không tạo module Maven mới). Kịch bản đã có test ở phase trước thì chỉ tham chiếu.
- Hạ tầng: Testcontainers Postgres, RabbitMQ, Redis. <!-- Updated: Validation Session 1 - Toxiproxy được phép khi cần --> Cắt mạng thật bằng Toxiproxy (đã duyệt, chỉ scope test) (`org.testcontainers:toxiproxy`) chỉ cho kịch bản "downstream chậm" và "RabbitMQ mất kết nối" nếu `MockRestServiceServer` và dừng container không đủ.
- Không dựng toàn bộ compose trong test (quá chậm); khởi động đúng các context Spring cần thiết.
- CI: chạy trong `mvn verify` hiện có; nếu tổng thời gian tăng > 5 phút, tách job riêng chạy song song trong `.github/workflows/ci.yml`.

## Related Code Files

- Create: test trong service tương ứng (bảng trong "Khung code cho Codex")
- Modify: `.github/workflows/ci.yml` (chỉ nếu tách job)
- Modify: `docs/system-architecture.md` (mục "Kiểm chứng reliability": bảng kịch bản → test)

## Implementation Steps

1. Liệt kê test lỗi đã có từ phase 1–7; kịch bản nào đã được phủ thì chỉ tham chiếu, không viết lại.
2. Viết test cho kịch bản còn thiếu.
3. Đo thời gian CI; tách job nếu cần.
4. Viết bảng kịch bản → tên test trong tài liệu (dùng để trình bày B10).

## Success Criteria

- [ ] Mỗi kịch bản trong bảng có test xanh trong CI có Docker.
- [ ] Bảng đối chiếu kịch bản → test có trong `docs/system-architecture.md`.
- [ ] Thời gian CI tăng có kiểm soát (ghi số trước/sau).

## Risk Assessment

- **Test chập chờn do thời gian:** dùng Awaitility với giới hạn rõ, không `sleep` cố định.
- **CI chậm:** tái dùng container (`@Container static`), tách job song song.
- **Thêm phụ thuộc Toxiproxy:** chỉ scope test.

## Khung code cho Codex

Điều kiện bắt đầu: phase 1–7 đã qua cổng duyệt của Claude.

### Bảng kịch bản → test (đúng tên, đúng chỗ)

| Kịch bản | Test | Chỗ đặt | Có sẵn từ phase |
|---|---|---|---|
| Downstream chậm hơn timeout | `DownstreamTimeoutReliabilityTest.slowContentMakesLearningFailFastAfterBreakerOpens` | learning (Toxiproxy trước một HTTP server giả trong Testcontainers, hoặc `MockRestServiceServer` + delay nếu đủ) | mới |
| Downstream 5xx rồi hồi phục | `RestLearningContentClientResilienceTest.returnsUnavailableQuicklyOnceBreakerOpens` + thêm `recoversAfterHalfOpen` | learning | 5 (thêm 1 ca) |
| RabbitMQ mất kết nối | `AccessOutboxRelayIntegrationTest.keepsEventPendingWhileBrokerIsDownThenPublishes` | access | 4 |
| Message sai hợp đồng | `SubscriptionGrantedListenerIntegrationTest.contractViolationGoesToDeadLetterQueue` + `followingValidMessageStillProcessed` | notification | 4 (thêm 1 ca) |
| Redis chết | `ClientRateLimitGatewayFilterFactoryTest.letsRequestsThroughWhenRedisIsDown`, `RedisLoginAttemptLimiterTest.failsClosedWhenRedisIsDown`, `RoomEventBusRedisDownTest.restStillWorksWhenPushFails` | gateway, user, game | 3; game mới |
| 2 instance tranh job | test song song phase 1 (9 job) | từng service | 1 |
| Gửi lại request có tiền | `GrantSubscriptionIdempotencyTest.concurrentSameKeyGrantsOnce`, `UnlockCourseFailureWindowTest.retryWithSameKeyWhilePendingDoesNotCreateIntent` | access, learning | 6 |
| Correlation qua 3 service + consumer | `CorrelationAcrossServicesReliabilityTest.sameCorrelationIdInHttpAndConsumerLogs` | learning (content và assessment giả qua HTTP, RabbitMQ thật, log capture) | mới |
| Downstream treo sau gateway | `GatewayTimeoutTest.slowDownstreamReturnsGatewayTimeout` | gateway | 5 |
| Provider đổi hợp đồng | 3 `*ContractProviderTest` | content, access | 5 |
| Đưa lại từ DLQ | `ReplayedMessageIsProcessedTest` | notification | 7 |

Chỉ viết test **mới** đánh dấu "mới" và các ca "(thêm 1 ca)"; không viết lại test có sẵn.

### Toxiproxy

- Phụ thuộc `org.testcontainers:toxiproxy` scope test, version theo BOM Testcontainers hiện có; chỉ thêm vào `services/learning-service/pom.xml`. Không cần → không thêm, ghi lý do.

### Tài liệu

`docs/system-architecture.md` mục "Kiểm chứng reliability": bảng trên (kịch bản → test → bước B1–B10). Đây là bảng dùng để trình bày.

### File được sửa

| Hành động | File |
|---|---|
| Tạo | `services/learning-service/src/test/.../DownstreamTimeoutReliabilityTest.java`, `.../CorrelationAcrossServicesReliabilityTest.java`; `services/game-service/src/test/.../RoomEventBusRedisDownTest.java` |
| Sửa (thêm ca) | `RestLearningContentClientResilienceTest`, `SubscriptionGrantedListenerIntegrationTest` |
| Sửa | `services/learning-service/pom.xml` (Toxiproxy, nếu dùng); `.github/workflows/ci.yml` (chỉ nếu thời gian CI tăng > 5 phút); `docs/system-architecture.md` |

### Lệnh kiểm chứng

```
mvn -B -ntp verify
```

Ghi thời gian CI trước và sau (lấy từ run GitHub Actions gần nhất trên `main` và run trên nhánh).

### Kiểm tay (Claude)

Đọc bảng trong `docs/system-architecture.md`, chạy từng test trong bảng một lần riêng lẻ, xác nhận mỗi test thật sự kiểm đúng kịch bản (không chỉ xanh).

### Dừng và hỏi

- Một kịch bản không thể kiểm trong một service mà không dựng nhiều service thật.
- Thời gian CI tăng > 10 phút dù đã dùng container tĩnh.
