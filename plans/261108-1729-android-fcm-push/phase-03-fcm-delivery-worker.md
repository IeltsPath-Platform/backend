---
phase: 3
title: "FCM delivery worker"
status: complete
effort: ""
---

# Phase 3: FCM delivery worker

## Overview

Dependencies: phase 1–2. Thêm Firebase adapter và worker delivery có lease/fencing, gọi mạng ngoài transaction và giữ khả năng vận hành inbox khi Firebase lỗi.

## Requirements

- Dùng Firebase Admin Java SDK cho HTTP v1, ADC/workload identity; secret JSON chỉ mount khi môi trường bắt buộc. Không tải credentials từ mobile, commit vào repo hoặc tạo Firebase Database.
- `worker-enabled=false` không khởi tạo Firebase hoặc tìm ADC; service/inbox khởi động được khi không có cấu hình Firebase.
- Bật worker nhưng thiếu/sai credentials/project: báo lỗi cấu hình push đã redact, pause sender, giữ jobs đến hạn/expiry; inbox/WS và readiness phục vụ chúng không phụ thuộc availability của FCM. Không có fake sender trả thành công trong production.
- Claim jobs bằng transaction ngắn, gửi ngoài transaction/lock, ghi kết quả có điều kiện claim ID. Hai instance không cùng claim còn hạn; timeout/crash vẫn có thể tạo push nhận lặp.

## Related Code Files

Create dưới notification package:

- `application/port/PushSender.java`, result/failure type trung lập provider; có thể nhận recipient/device/version snapshot và notification references, không expose SDK vào application/domain.
- `application/ProcessPushDeliveryUseCase.java`, xử lý state transition và điều kiện owner/version.
- `infrastructure/push/FcmPushSender.java`, payload mapper, cấu hình Firebase lifecycle/properties.
- `infrastructure/PushDeliveryScheduler.java` (hoặc package scheduler theo convention lúc triển khai).
- Mở rộng `PushDeliveryRepositoryAdapter` với claim/finalize/reclaim/expiry SQL.

Modify `services/notification-service/pom.xml`: thêm Firebase Admin version pin đã kiểm tra với Java 21/Spring Boot hiện có và `common-resilience` nếu chưa có. Modify **đúng file** `infra/config-server/config-repo/notification-service.yaml` với placeholders không có secret fallback; không sửa Docker/global security nếu chưa có nhu cầu cụ thể. Bổ sung worker vào bảng Scheduler và nhiều instance trong `docs/system-architecture.md`.

## Architecture / vòng đời job

```text
PENDING/RETRY, next_attempt_at <= now, chưa hết TTL
  -> claim: IN_FLIGHT, claim_id mới, lease_until, attempt_count + 1
  -> kiểm tra binding active + user/version đúng + TTL còn hạn
     -> không đúng: CANCELLED
     -> gửi ngoài transaction
        -> FCM accepted: SENT
        -> lỗi tạm: RETRY(next_attempt_at)
        -> token unregistered: conditional disable + CANCELLED
        -> lỗi vĩnh viễn/hết budget: PARKED

IN_FLIGHT lease hết -> reclaim claim_id mới hoặc PARKED nếu hết budget
Job hết TTL -> CANCELLED(EXPIRED); không gọi Firebase
```

- Dùng `FOR UPDATE SKIP LOCKED` + update claim trong một transaction; commit trước external call. Tham khảo CommunityOutboxRelay lease, không copy đường publish giữ transaction của relay khác.
- Claim/finalize bằng ID/version/claim ID. Stale worker không thể ghi SENT/RETRY hoặc vô hiệu hóa token sau khi job được reclaim/registration thay đổi.
- Tăng attempt count bền vững khi claim để crash liên tục không bypass retry budget. Tách metric actual send attempts nếu claim bị cancel trước gửi.
- Worker lấy token hiện tại từ device ngay trước send, kiểm tra `active && user_id == recipient_id && registration_version == snapshot`. Không giữ plaintext trong queue/database delivery.
- Bất cứ update invalid token nào phải WHERE device ID + owner + registration_version + token hash đã gửi; old UNREGISTERED response không disable token mới vừa refresh.
- Không giữ DB lock trong suốt HTTP để cố bảo đảm logout tuyệt đối; payload generic và mobile refetch REST là lớp bảo vệ khi request đang bay.

## Bounds và retry đề xuất

Giá trị dưới đây là cấu hình ban đầu để load-test, không phải SLA hoặc yêu cầu có sẵn:

| Config | Giá trị đề xuất |
| --- | --- |
| enqueue-enabled / worker-enabled | false / false |
| batch-size | 20 job/lượt, không tải hàng đợi không giới hạn |
| max-concurrency | 4 send trong mỗi instance; cấu hình bulkhead `fcm` rõ ràng |
| max-dispatch-per-second | 20/instance, limiter hoặc bounded dispatch theo tick; tính tổng khi scale |
| connect/read/overall send budget | 3s / 10s / tối đa 15s gồm retry nội bộ đã kiểm soát |
| lease | 120s; phải lớn hơn `(ceil(batch/concurrency) × worst-send-budget) + margin` |
| max-attempts | 8 claim attempts |
| retry base / cap | 30s / 1h, exponential backoff + jitter |
| TTL | 24h từ thời điểm notification được enqueue; type reminder có thể chốt TTL ngắn hơn |

- Không chỉ thêm timeout chờ Future nếu SDK request vẫn chạy ngầm không giới hạn. Kiểm tra transport timeouts, SDK retries, bounded executor và shutdown; bảo đảm tổng budget phù hợp lease. Không cho config vi phạm bất đẳng thức lease/batch/concurrency chạy production.
- Rate limit sender là giới hạn riêng với rate limit API ở Gateway. Claim không vượt capacity executor/rate budget; tick trước còn chạy không tạo một batch khác trong cùng instance. Khi scheduler pause/bulkhead đầy không claim liên tục để burn attempts.
- `429`/quota: tôn trọng Retry-After, backoff tối thiểu 60s cho quota; `5xx`/network/timeout retry có jitter. Không retry loop ngay trong consumer nguồn.
- `UNREGISTERED`: disable đúng binding đã gửi. `INVALID_ARGUMENT`: park payload lỗi, không tự disable token trừ khi đã xác minh chắc chắn token invalid và payload hợp lệ; default không deactivate theo mã mơ hồ.
- Sender/project mismatch, permission/auth/config: pause/alert, không vô hiệu hóa hàng loạt device. Circuit-open/bulkhead-full trì hoãn jobs thay vì bỏ chúng; khi configuration pause không claim liên tục làm cạn attempt budget.
- Hết lượt: PARKED bền vững với mã lỗi đã chuẩn hóa; manual replay chỉ job còn hạn và binding còn hợp lệ, tạo claim mới có audit, không dựng lại inbox. Không thêm public replay/send endpoint nếu chưa cần.

## Resilience và observability

`ResilientCall.call("fcm", CallKind.WRITE, ...)` bảo vệ boundary: send không phải idempotent nên không dùng IDEMPOTENT_WRITE để nhân đôi retry. Durable worker sở hữu retry budget. SDK có retry riêng phải được cấu hình/đo và tính vào overall budget; một job attempt không nhất thiết là một HTTP request.

Wrapper hiện nhận diện lỗi HTTP/network kiểu Spring, không tự hiểu FirebaseMessagingException: adapter cần chuẩn hóa transient failure sang exception đã redact được wrapper nhận diện, giữ retry metadata cần thiết. Test chứng minh breaker đếm lỗi FCM và bulkhead có hiệu lực; không đổi predicate toàn cục như side effect.

Micrometer: queued/sent/failed/retried/deactivated/parked/cancelled, pending count, oldest pending age, latency; labels chỉ provider/event family/result/error category hữu hạn. Notification/device IDs chỉ trong structured logs, không làm metric labels. Propagate correlation/trace từ delivery trong scope ngắn và clear thread-local sau mỗi job. Không log exception SDK/raw payload/token hoặc dùng chúng làm trace attribute.

## Implementation Steps

1. Viết fake PushSender và tests state transitions/Clock trước khi nối SDK.
2. Thêm scheduler/store claim và fencing, recovery lease, expiry. Tests hai worker và process restart.
3. Thêm adapter Firebase và cấu hình conditional lifecycle; xác nhận pin SDK/dependency tree, không tự upgrade Spring/shared dependencies.
4. Tạo title/body generic theo event type. Data chỉ string: `notificationId`, `eventType`, ID điều hướng cần thiết như `actorId` cho follow, `postId`, `commentId`, `gradingJobId`. Không có nội dung bài riêng tư, token hoặc credentials. Kiểm tra giới hạn kích thước payload trước send. AndroidConfig TTL lấy thời gian còn lại tới `expires_at`, không dùng TTL mặc định của provider để kéo dài thông báo cũ; channel ID có cấu hình và thống nhất với Android. Không tự collapse các notification khác nhau làm mất sự kiện.
5. Nối resilience, transport timeout và concurrency; test transient/auth/payload/token errors, version race và stale claim response.
6. Thêm metrics/health diagnostic riêng push và contract cho replay/expiry; test Firebase thiếu cấu hình vẫn phục vụ inbox/WS.

## Verification

Unit fake sender; adapter HTTP/SDK transport fake, không gọi Firebase thật; PostgreSQL integration cho claim/fencing. Đọc tài liệu Firebase chính thức về [khởi tạo Admin SDK](https://firebase.google.com/docs/admin/setup) và [phân loại lỗi](https://firebase.google.com/docs/cloud-messaging/error-codes). Trước khi pin SDK xác minh API timeout/retry của đúng phiên bản thay vì suy từ Node SDK.

## Success Criteria

- [ ] Worker gửi được qua fake provider, không giữ transaction/lock trong HTTP.
- [ ] Lease recovery/fencing/two-instance tests đạt; bounded memory/concurrency/time.
- [ ] Old response không disable binding mới; outage không làm mất job/inbox.
- [ ] Config off/misconfigured không gọi thật hoặc làm hỏng inbox; không false SENT.
- [ ] Lỗi/token/payload được phân loại, retry có budget/expiry và metrics an toàn.

## Risk Assessment / rollback

FCM acceptance trước lúc process chết nhưng trước SENT là vùng không thể loại trừ duplicate display; mobile xử lý tap idempotent theo notificationId. Logout không thu hồi được request đã gửi hoặc notification đang chờ ở provider. Tắt worker để ngừng send; tắt enqueue khi rollback dài; giữ schema/jobs cho điều tra, TTL ngăn backlog cũ phát ra sau restart.
