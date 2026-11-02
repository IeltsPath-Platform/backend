---
phase: 4
title: "Graceful shutdown và draining"
status: pending
priority: P2
effort: "M"
dependencies: [old-3]
---

# Phase 4: Graceful shutdown và draining

Người làm: Claude. Bắt đầu sau khi phase 3 cũ qua cổng, vì phase này sửa `docker-compose.yml` và `config-repo/application.yaml` mà Codex vừa sửa. Phần Retry của gateway nằm ở [phase 3](./phase-03-thiet-ke-lai-resilience.md), không lặp lại ở đây.

## Overview

Khi một replica nhận SIGTERM: request đang xử lý hoàn tất, replica thôi nhận request mới càng sớm càng tốt, và gateway thôi gửi tới nó trong thời gian ngắn. Docker Compose không bảo đảm zero-downtime; mục tiêu là **thu hẹp cửa sổ lỗi và đo được nó**.

## Hiện trạng (đã kiểm)

- Spring Boot 3.5 mặc định `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=30s`. Config-repo không đặt rõ nên hành vi phụ thuộc mặc định.
- `Dockerfile.spring-service` dùng `ENTRYPOINT ["java","-jar","app.jar"]` dạng exec: SIGTERM tới thẳng JVM. Đúng.
- `docker-compose.yml` không có `stop_grace_period` (mặc định 10 s): Docker gửi SIGKILL trước khi hết 30 s drain, cắt request dài (LLM, upload).
- `spring.task.scheduling.shutdown.await-termination` chưa đặt (mặc định `false`): job đang chạy bị ngắt khi tắt.
- Gateway giữ instance cũ trong bộ nhớ đệm: Eureka server cache phản hồi 30 s, client fetch registry 30 s, Spring Cloud LoadBalancer cache 35 s; cộng dồn có thể vài chục giây sau khi instance đã từ chối kết nối.
- Gọi giữa service dùng hostname Docker; JVM cache DNS (mặc định 30 s khi không có security manager).
- Healthcheck compose gọi `/actuator/health` (có kiểm DB), dùng cho thứ tự khởi động. Probe liveness/readiness đã bật nhưng chưa dùng.
- Không phase nào có test "dừng một replica khi replica khác còn chạy".

## Thiết kế

### Backend (config và code, làm trong phase này)

| Mục | Giá trị | Chỗ đặt |
|---|---|---|
| Graceful rõ ràng | `server.shutdown: graceful`, `spring.lifecycle.timeout-per-shutdown-phase: 30s` | `config-repo/application.yaml` |
| Scheduler đợi job xong | `spring.task.scheduling.shutdown.await-termination: true`, `await-termination-period: 20s` | `config-repo/application.yaml` |
| Thời gian Docker chờ | `stop_grace_period: 40s` (lớn hơn 30 s drain) | anchor `x-spring-service` trong compose |
| Eureka server bớt cache | `response-cache-update-interval-ms: 3000`, `eviction-interval-timer-in-ms: 5000` | `config-repo/eureka-server.yaml` |
| Client thấy thay đổi nhanh | `eureka.client.registry-fetch-interval-seconds: 5`; `eureka.instance.lease-renewal-interval-in-seconds: 10`, `lease-expiration-duration-in-seconds: 30` | `config-repo/application.yaml` |
| Cache loadbalancer của gateway | `spring.cloud.loadbalancer.cache.ttl: 5s` | `config-repo/api-gateway.yaml` |
| DNS ngắn cho gọi giữa service | thêm `-Dsun.net.inetaddr.ttl=5` vào `JAVA_TOOL_OPTIONS` | anchor `x-spring-env` |

Không đổi healthcheck compose sang readiness: nó cần kiểm DB để chặn thứ tự khởi động. Readiness vẫn có cho đầu dò bên ngoài.

### Để giai đoạn deploy VPS (ghi vào tài liệu, không làm ở đây)

- Health check chủ động và loại instance khỏi pool ở reverse proxy.
- Khởi động lại từng replica một, chờ replica mới `healthy` rồi mới dừng replica kế.
- Thời gian drain của proxy, giới hạn kết nối WebSocket khi đóng.
- Việc hứa zero-downtime chỉ khi proxy làm health check; Compose một mình không đủ.

## Related Code Files

| Hành động | File |
|---|---|
| Sửa | `infra/config-server/config-repo/{application,eureka-server,api-gateway}.yaml` |
| Sửa | `docker-compose.yml` (anchor `x-spring-service`, `x-spring-env`) |
| Tạo | test `GracefulShutdownTest` trong `services/notification-service` |
| Sửa | `docs/system-architecture.md`: mục "Dừng một instance" (trình tự, giới hạn, phần dành cho VPS, cách đo) |
| Sửa | `plans/261031-2145-backend-reliability-patterns/phase-08-test-loi-tong.md`: thêm kịch bản dừng một replica |

## Implementation Steps

### Test viết trước (phải đỏ)

1. `GracefulShutdownTest` (notification-service, `@SpringBootTest(RANDOM_PORT)`, nạp `server.shutdown` và `timeout-per-shutdown-phase` từ `config-repo/application.yaml`): endpoint thử chậm 1,5 s; gọi bất đồng bộ, 200 ms sau đóng context; khẳng định (a) response vẫn 200 với thân đầy đủ, (b) kết nối mới sau khi bắt đầu đóng bị từ chối. Đỏ nếu cấu hình bị đặt `immediate`.
2. `SchedulerShutdownTest`: một `@Scheduled` giả chạy 1 s; đóng context lúc đang chạy; khẳng định job hoàn tất (cờ `finished`), không bị interrupt. Đỏ khi `await-termination=false`.
3. Kiểm cấu hình compose: `docker compose config` có `stop_grace_period: 40s` cho mọi service Spring (lệnh ghi trong báo cáo).

### Code

4. Sửa yaml và compose theo bảng.
5. Tài liệu như bảng file.

### Đo cửa sổ lỗi (kiểm tay, Claude)

6. Chạy `--scale learning-service=2 --scale content-service=2`; vòng lặp `curl` qua gateway tới một GET đi tới learning, 20 req/s, trong 60 s. Ở giây 10: `docker compose stop -t 40` một replica learning.
7. Ghi lại: số request lỗi, thời gian từ lúc `stop` tới request lỗi cuối cùng, và request đang xử lý lúc `stop` có hoàn tất không. Chạy lại sau khi thêm Retry filter của phase 3 để so sánh.
8. Lặp lại với một replica `content-service` trong lúc learning gọi content (kiểm DNS 5 s).

## Success Criteria

- [ ] Test 1–3 xanh; mọi test cũ giữ nguyên.
- [ ] Request đang xử lý lúc SIGTERM hoàn tất (đo ở bước 7).
- [ ] Cửa sổ lỗi do định tuyến cũ ≤ 15 giây, số đo và lệnh ghi vào `docs/system-architecture.md`. Nếu vượt, ghi nguyên nhân và con số thực, không sửa chỉ tiêu.
- [ ] Tài liệu nói rõ phần nào thuộc backend, phần nào thuộc deploy VPS.

## Risk Assessment

- **Lease và fetch quá ngắn tăng tải lên Eureka:** quy mô hiện tại nhỏ; nếu thấy tải, nới về 10/20 s.
- **`stop_grace_period` dài làm `docker compose down` chậm:** chỉ khi có request đang chạy; chấp nhận.
- **Lease hết hạn quá sớm khi GC dài:** `lease-expiration 30 s` lớn hơn ba lần heartbeat; theo dõi cảnh báo của Eureka.
- **WebSocket bị đóng đột ngột khi tắt:** frontend đã tự nối lại; không đổi.
- **Rollback:** các giá trị đều là cấu hình; hoàn lại bằng cách xóa khỏi yaml và compose.
