---
phase: 4
title: "Regression verification"
status: in_progress
effort: ""
---

# Phase 4: Regression verification

## Overview

Dependencies: phase 1–3. Kiểm chứng xuyên API → persistence → listener → worker và bảo đảm các contract inbox/WebSocket cũ không đổi. Các test hẹp vẫn thực hiện ngay ở phase tạo hành vi, không đợi tới phase này.

## Related Code Files

Create tests dưới `services/notification-service/src/test/java/com/ieltspath/notification/` theo nhóm api/application/infrastructure:

- Device controller security/validation, device PostgreSQL persistence/concurrency.
- Push delivery atomicity integration cho sáu Record* và redelivery.
- PushDeliveryScheduler/claim/lease recovery integration và FcmPushSender fake transport tests.
- Push metrics/redaction/configuration isolation tests.

Giữ/chạy lại: `NotificationSecurityTest`, `NotificationReadIntegrationTest`, `CommunityEventConsumerIntegrationTest`, `CommunityReplayBrokerIntegrationTest`, các `Grading*InboxIntegrationTest`, `SubscriptionGrantedBrokerIntegrationTest`, `NotificationPrometheusSecurityIntegrationTest`. Recheck tên file thực tế trước chọn lệnh `-Dtest`.

## Test matrix bắt buộc

| Nhóm | Tình huống / kết quả |
| --- | --- |
| Auth/IDOR | JWT thiếu/sai; body userId không được tin; DELETE user khác không effect/no owner leak |
| Registration | Repeat PUT, token refresh, reinstall, 2 device/user, same token 2 installation, account switch, concurrent PUT/DELETE |
| Version | Repeat không hủy job; refresh/transfer/logout làm job cũ cancel; old UNREGISTERED không disable token mới |
| Atomicity | Ép lỗi job insert rollback notification; retry commit 1 inbox; crash sau commit trước ACK/job send không mất job |
| Dedupe | Source event redelivery, comment mention dedupe, source consumer retry không thêm inbox/unread/delivery |
| Fan-out | Hai active device → hai jobs; inactive/foreign device → không job; device mới không nhận lịch sử |
| Policy | Community disabled không lưu/WS/push; grading/subscription vẫn theo chính sách hiện tại |
| Multi-instance | Hai worker, SKIP LOCKED; expired lease; stale claim update; attempts vẫn tăng qua crash; batch budget/lease constraint |
| Provider | accepted, timeout, 429+Retry-After, 5xx, UNREGISTERED, INVALID_ARGUMENT payload, auth/project mismatch |
| Flags/config | 4 tổ hợp flags; Firebase thiếu credentials; worker pause không burn budget; restart với backlog hết TTL |
| Privacy | DTO/error/log/trace/metric không chứa token, request body hay credential; labels không cardinality theo user/device |
| Regression | List/read/read-all/unread/WS auth+Redis relay, source retry/DLQ/replay, actuator access không đổi |

## Implementation Steps

1. Chạy unit/controller tests hẹp, sau đó PostgreSQL Testcontainers; không thay bằng H2 cho lock/partial index.
2. Chạy broker integration theo setup hiện có để kiểm tra ACK/retry/DLQ và WebSocket vẫn hoạt động khi fake FCM hỏng.
3. Fake provider ghi nhận calls để đối chiếu jobs, recipient và registration version; không log token trong assertion failure/snapshot.
4. Test shutdown/restart giữa claim/send/finalize, lease reclaim và duplicate window; không assert exactly-once khi timeout mơ hồ.
5. Chạy notification module cùng dependencies; chỉ chạy Gateway/common-resilience suite nếu thực sự sửa hoặc thêm wiring phụ thuộc chúng.
6. Lưu kết quả thực chạy, skip, điều kiện Docker/credentials và failure vào reports; sửa regression thay vì hạ assertions.

## Verification commands dự kiến

Từ backend root, dùng Maven hiện có. Ví dụ selected test sau khi test đó tồn tại:

```powershell
mvn -q -pl services/notification-service -am "-Dtest=NotificationDeviceControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -q -pl services/notification-service -am test
# Chỉ nếu Gateway thay đổi hoặc cần kiểm tra ingress mới:
mvn -q -pl infra/api-gateway -am test
# Chỉ nếu shared resilience bị sửa:
mvn -q -pl shared/common-resilience -am test
```

Không `clean` khi service có thể đang chạy từ IDE. Chỉ thêm command Docker nếu daemon khả dụng. Thiếu Docker phải ghi integration tests chưa chạy, không kết luận migration/race đã đạt. Chưa cần Firebase/Android để đạt automated gate; credentials thật chỉ dùng staging manual.

## Success Criteria

- [ ] Toàn bộ matrix có coverage tương ứng và test reports thực chạy.
- [ ] Không test nào gửi notification thật hoặc đọc credential thật.
- [ ] Các suite inbox/WS/listener cũ đạt, không đổi status/schema hoặc unread semantics.
- [ ] Kiểm chứng PostgreSQL concurrency/upgrade không bị skip khi chuẩn bị nghiệm thu.

## Risk Assessment / rollback

Testcontainers/Broker cần môi trường chạy; không bỏ annotation hoặc nới timeout để che race. Performance test dùng dữ liệu synthetic và bounded device/job volumes. Nếu regression ở inbox/WS, dừng rollout FCM và trả flags false; không sửa source events/payload cũ để làm test xanh.
