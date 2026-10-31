# Brainstorm: 10 bước reliability cho backend (B1–B10) và nền nhiều instance

Ngày: 2026-10-31. Phạm vi: `backend/` (9 service Spring Boot, `ai-learning-service` rỗng). Chế độ: markdown, không HTML, không wiki.

## 1. Vấn đề và yêu cầu

- Người dùng muốn implement 10 bước reliability (B1 Idempotency → B10 Failure Testing), mục tiêu **cả hai**: sửa rủi ro thật và có minh họa chạy được, có test cho từng pattern.
- Hệ thống **sẽ chạy nhiều instance** cho một service. Hiện compose chạy 1 instance mỗi service.
- Thư viện được phép: Resilience4j, Micrometer Prometheus, Logstash JSON encoder, Redis (cho rate limit). Không cần Bucket4j.
- Phạm vi: một plan cho cả 10 bước, tổ chức theo rủi ro, có bảng đối chiếu B1–B10.

## 2. Hiện trạng (kết quả quét code)

| Bước | Người dùng tự ghi | Thực tế | Bằng chứng |
|---|---|---|---|
| B1 | Có một phần | `Idempotency-Key` HTTP chỉ ở mở khóa khóa học; consumer dedupe theo messageId ở access, community, learning; nộp bài dùng `submissionKey` | `learning/api/controller/CourseController.java:48`, `UnlockCourseUseCase.java` |
| B2 | Có ở một số luồng | Relay thật: assessment, community. **Chỉ ghi, không relay:** access, content, game; `findPendingEvents` của access không ai gọi; không service nào consume các event này | `assessment/.../OutboxRelay.java`, `community/.../CommunityOutboxRelay.java`; access `OutboxEventRepository.java:13` |
| B3 | Có, cần kiểm thử | Mạnh: retry topology và DLX chung (`MessageRetry`, `RetryTopology`), manual ACK, publisher confirm + returns, tên event có version (V1/V2), test broker thật | `shared/common-events`, `RabbitPointsGrantPublisher.java`, `PointsGrantDeadLetterBrokerTest` |
| B4 | Có bù trừ, cần rà | Mở khóa khóa học: intent `PENDING` + reconciler + debit idempotent; tín dụng chấm người thật: consume/refund | `UnlockCourseUseCase.java`, `RefundPointsUseCase.java`, `ConsumeHumanGradingCreditUseCase.java` |
| B5 | Timeout đã có | Timeout ở ~24 client; retry chỉ ở client LLM; không circuit breaker, không bulkhead | `requestFactory.setReadTimeout(...)` |
| B6 | Có ở một số luồng | Mạnh: ~57 file dùng `@Version`, `FOR UPDATE SKIP LOCKED`, advisory lock | `PostgresAdvisoryLock.java`, `JdbcLearnerLock.java`, `AdvisoryUserLock.java` |
| B7 | Có nền tảng | RestClient có timeout và exception "Unavailable"; chỉ assessment chuyển tiếp `X-Correlation-Id`; không contract test | `assessment/.../GatewayRequestContext.java` |
| B8 | Cần rà | Mọi service có `GlobalExceptionHandler` + `@Valid`; rate limit chỉ in-memory cho upload avatar và ảnh community; **gateway không rate limit, login không chống dò mật khẩu** | `InMemoryAvatarUploadRateLimiter.java`, `InMemoryMediaRateLimiter.java` |
| B9 | Có một phần | Gateway tạo correlation ID nhưng **0 chỗ `MDC.put`**; log text; actuator chỉ `health,info,refresh`; 0 metric tự định nghĩa | `apigateway/filter/CorrelationIdFilter.java`, `config-repo/*.yaml` |
| B10 | Cần bổ sung | 179 lớp test Testcontainers, CI bắt buộc Docker; có test lỗi broker; gần như không có test lỗi HTTP downstream hay relay crash | `.github/workflows/ci.yml` |

### Phát hiện thêm khi chạy nhiều instance (ngoài B1–B10)

- `notification-service` dùng `enableSimpleBroker("/queue")`: push chỉ tới người nối vào cùng instance (REST vẫn đồng bộ lại).
- `game-service`: `GameWebSocketHandler` và `InMemoryWebSocketTicketStore` giữ phiên và vé trong bộ nhớ; phòng game vỡ khi nhiều instance.
- Hai rate limiter in-memory bị chia theo instance.
- 16 `@Scheduled` job; 7 có khóa trong file; 9 job còn lại (OutboxRelayScheduler, GateEssayGradingScheduler, GradingDeadlineScheduler, SpeakingRecordingRetentionScheduler, CommunityOutboxRelayScheduler, AiDraftScheduler, RocketRoundScheduler, SoloTimeoutScheduler, LoginHistoryPurgeScheduler) cần rà xem store bên dưới có `SKIP LOCKED` không.

## 3. Phương án đã xét

| Phương án | Ưu | Nhược | Kết luận |
|---|---|---|---|
| A. Theo rủi ro, module dùng chung | Đo được trước khi sửa (observability trước); mỗi service chỉ đụng một lần cho mỗi module | Thứ tự không khớp B1–B10 | **Chọn**, kèm bảng đối chiếu |
| B. Đúng thứ tự B1–B10 | Khớp tiêu chí trình bày | Observability cuối; đụng service nhiều lần; nền nhiều instance không có chỗ | Loại |
| C. Tách 2 plan | Plan nhỏ | Phụ thuộc chéo (rate limit cần Redis) | Loại |

## 4. Quyết định đã chốt

| # | Quyết định |
|---|---|
| 1 | Mục tiêu: sửa rủi ro thật và minh họa đủ pattern có test |
| 2 | Sẽ chạy nhiều instance |
| 3 | Thêm Redis vào compose |
| 4 | Thư viện: Resilience4j, Micrometer Prometheus, Logstash JSON encoder; rate limit bằng gateway `RequestRateLimiter` + Redis (không Bucket4j) |
| 5 | Tổ chức plan theo rủi ro (phương án A), một plan, kèm bảng đối chiếu B1–B10 |
| 6 | Outbox chỉ ghi: access thêm relay và consumer thật (notification báo kích hoạt Premium); game giữ làm nhật ký + job dọn; content rà rồi quyết |
| 7 | ~~WebSocket: notification chuyển STOMP broker relay trên RabbitMQ; game: vé WebSocket lưu Redis, gateway sticky theo phòng~~ **Thay sau red-team (plan `261031-2145`):** game và notification phát qua Redis pub/sub; vé game lưu Redis; bỏ sticky (WebSocket game không có `roomId` trong đường dẫn) và STOMP relay |
| 8 | Redis chết: rate limit cho qua (có metric cảnh báo), riêng login chặn |
| 9 | Ngoài phạm vi: Kubernetes/autoscale, tracing phân tán (OpenTelemetry), Redis HA, đồng bộ trạng thái game giữa instance, schema registry |

## 5. Giải pháp đề xuất: 8 phase

| Phase | Nội dung | Bước | Tiêu chí xong |
|---|---|---|---|
| P0 Nền nhiều instance | Redis trong compose; rà 16 scheduler, bọc advisory lock cho job chưa khóa; notification STOMP relay qua RabbitMQ (plugin stomp); game: vé Redis, load balancer sticky theo phòng ở gateway; compose chạy được 2 bản sao | B6 | 2 instance notification/learning/game: push tới đúng người; mỗi job chạy 1 lần mỗi chu kỳ; phòng game chơi được |
| P1 Observability | `shared/common-observability`: filter MDC (`correlationId`, `userId`), interceptor RestClient + RabbitMQ chuyển tiếp header; log JSON theo profile; Micrometer Prometheus `/actuator/prometheus` (không qua gateway); metric outbox chờ, DLQ, rate limit | B9 | Một request có cùng `correlationId` ở gateway, 2 service và consumer; Prometheus có metric HTTP và `outbox_pending` |
| P2 API | Gateway `RequestRateLimiter` Redis, khóa theo userId hoặc IP, mức riêng cho auth/upload/AI; login giới hạn theo tài khoản + IP, 429 + `Retry-After`; bỏ 2 limiter in-memory; thống nhất định dạng lỗi; rà validation community và notification | B8 | 2 gateway dùng chung giới hạn; login sai lần thứ N trả 429; cùng định dạng lỗi |
| P3 Outbox | Relay access theo mẫu assessment (`SKIP LOCKED`, retry, terminal failure); `SubscriptionGrantedV1` qua common-events; notification consume; game dọn outbox theo hạn; rà content | B2, B3 | Cấp gói → thông báo; RabbitMQ chết thì chờ, sống lại thì gửi; consumer không nhận trùng |
| P4 Resilience + gọi service | Resilience4j dùng chung: breaker + timeout cho LLM và learning→content/access, assessment→content/access/user; retry chỉ GET; bulkhead cho LLM; ánh xạ lỗi downstream thống nhất | B5, B7 | Content chết: learning trả 503 nhanh khi breaker mở; tự hồi phục |
| P5 Idempotency + saga | `Idempotency-Key` cho kích hoạt key, yêu cầu chấm người thật, trừ điểm nội bộ; rà dedupe mọi listener; máy trạng thái + test cửa sổ lỗi cho mở khóa và tín dụng chấm | B1, B4 | Gửi lại cùng request → cùng kết quả, không trừ hai lần; mỗi cửa sổ lỗi có test |
| P6 Rà messaging + concurrency | Mọi listener dùng retry/DLQ chung; quy tắc version event; rà `@Version` ở aggregate tranh chấp | B3, B6 | Có tài liệu quy tắc; không listener tự làm retry riêng |
| P7 Test lỗi tổng | Downstream chậm/5xx, RabbitMQ mất kết nối, Redis chết, 2 instance tranh job | B10 | Chạy trong CI có Docker |

### Bảng đối chiếu B1–B10

| Bước | Phase |
|---|---|
| B1 Idempotency | P5 |
| B2 Outbox | P3 |
| B3 Reliable Messaging | P3, P6 |
| B4 Saga | P5 |
| B5 Resilience | P4 |
| B6 Concurrency | P0, P6 |
| B7 Service-to-service | P1 (correlation), P4 |
| B8 API Reliability | P2 |
| B9 Observability | P1 |
| B10 Failure Testing | mọi phase + P7 |

## 6. Rủi ro và lưu ý triển khai

- **Sticky session cho game:** load balancer mặc định của gateway với Eureka không sticky; cần load balancer riêng (hash theo roomId). Phần khó nhất của P0.
- **Compose nhiều bản sao:** service có `container_name` hay cổng host cố định sẽ xung đột khi scale; cần rà.
- **STOMP relay:** cần bật `rabbitmq_stomp` trong image RabbitMQ; đổi cấu hình phía client WebSocket có thể ảnh hưởng frontend.
- **Redis chết:** cho qua với API thường, chặn với login; cần metric cảnh báo.
- **Test lỗi:** P7 có thể cần Toxiproxy (Testcontainers) hoặc WireMock; kiểm repo khi lập plan.
- **Khối lượng:** 8 phase chạm cả 9 service; mỗi phase một PR.
- Không đặt mã phase hay mã plan trong code, tên test, migration, commit (quy tắc repo).

## 7. Tiêu chí thành công tổng

- Chạy 2 instance cho mỗi service quan trọng mà không mất thông báo, không chạy job trùng, game chơi được.
- Mọi bước B1–B10 có ít nhất một chỗ áp dụng thật và một test minh họa.
- Lỗi downstream, broker hay Redis không làm hệ thống treo; có metric để thấy.

## 8. Bước tiếp theo

- Lập plan bằng `/ck:plan --tdd` (đổi hành vi hiện có ở nhiều service, có bộ test lớn cần giữ) với báo cáo này làm đầu vào.

## Câu hỏi chưa giải quyết

- Outbox của content ghi loại event nào và có cần relay không (rà trong P3).
- Mức rate limit cụ thể (số request mỗi phút) cho từng nhóm route.
- Toxiproxy hay WireMock cho P7.
