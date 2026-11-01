---
phase: 2
title: "Observability"
status: pending
priority: P1
effort: "M"
dependencies: [1]
---

# Phase 2: Observability

## Overview

Một request có cùng `correlationId` trong log của gateway, mọi service nó đi qua và consumer RabbitMQ. Log dạng JSON. Mỗi service có metric Prometheus. Đây là công cụ để kiểm chứng các phase sau.

## Requirements

- MDC: `correlationId` (header `SecurityHeaders.CORRELATION_ID`, gateway đã tạo trong `CorrelationIdFilter`), `userId` (nếu có), `service`.
- Chuyển tiếp header ở mọi RestClient đi ra (22 client) và mọi message RabbitMQ publish; listener khôi phục MDC.
  <!-- Updated: rà soát 2026-11-01 - đóng dấu theo MDC không đủ cho outbox -->
  - **Qua outbox:** relay chạy ở thread khác không có MDC, nên đóng dấu theo MDC không mang được id của request gốc. Lưu `correlation_id` vào hàng outbox tại điểm ghi duy nhất của mỗi service (assessment, community, access, và `reward_grants` của learning) rồi publisher gắn header từ cột đó. Spec: [đính chính, phase 1](../261101-0930-reliability-followups/phase-01-correlation-qua-outbox.md) (gói B2). Tiêu chí "event assessment → learning listener" ở dưới chỉ đạt sau khi làm xong phần này.
- Log JSON khi chạy trong docker (profile `json-logs`); log text khi chạy local.
- `/actuator/prometheus` mở trong mạng nội bộ, **không** định tuyến qua gateway.
  <!-- Updated: Red Team Review - security chung chặn actuator ngoài health/info; exposure bị ghi đè ở từng service -->
  - `shared/common-security/.../CommonSecurityAutoConfiguration.java:56` chỉ `permitAll` cho `health`, `info` → phải thêm `GET /actuator/prometheus`, nếu không sẽ 401.
  - Exposure khai báo ở `config-repo/application.yaml:12` **và** ghi đè ở từng service (ví dụ `learning-service.yaml:155`); hiện 12 file trong config-repo khai báo exposure. <!-- Updated: Validation Session 1 - số file theo thực tế --> Ưu tiên xóa ghi đè để dùng giá trị chung; file nào cần khác thì thêm `prometheus`.
- Prometheus trong compose, profile `observability` (không chạy mặc định), lấy danh sách instance qua Eureka (`eureka_sd_configs`) để thấy đủ bản sao khi scale; file `infra/prometheus/prometheus.yml`.
- Metric riêng: `outbox_pending{service}`, `outbox_oldest_age_seconds{service}`, `dead_letter_messages{queue}` (gauge số message đang nằm trong từng DLQ của service có `DeadLetterReplayer`, đọc bằng `counts()`, `NaN` khi broker lỗi; thay cho counter `messages_dead_lettered_total` vì đo được tồn đọng thật mà không phải chèn vào mọi nơi từ chối message), `rate_limit_rejected_total{route}` (phase 3 ghi), metric Resilience4j tự có (phase 5).

## Architecture

- Module mới `shared/common-observability` (theo mẫu `common-storage`, auto-configuration):
  - `CorrelationIdServletFilter` (order cao nhất): đọc header hoặc sinh UUID, đặt MDC, trả lại header trong response, xóa MDC ở `finally`.
  - `CorrelationRestClientCustomizer` (`RestClientCustomizer`): interceptor thêm header từ MDC.
  - `CorrelationMessagePostProcessor` cho `RabbitTemplate` (đặt header `x-correlation-id`) và advice cho listener container (đọc header → MDC → xóa sau khi xử lý).
  - Không phụ thuộc security; `userId` đặt bởi một hook nhỏ sau khi xác thực (mỗi service đã có `CurrentUserProvider`).
- Thay cách làm riêng của assessment (`GatewayRequestContext`, header thủ công ở 5 client) bằng customizer chung.
- Logback: `logback-spring.xml` dùng chung (đặt trong module, service import) với `LogstashEncoder` cho profile `json-logs`.
- Micrometer: `micrometer-registry-prometheus` trong mọi service; exposure `health,info,refresh,prometheus` trong `config-repo/application.yaml`.

## Related Code Files

- Create: `shared/common-observability/` (pom, auto-config, 3 lớp trên, `logback` mặc định, README)
- Modify: `pom.xml` gốc (module mới, quản lý version `logstash-logback-encoder`), `services/*/pom.xml` (thêm phụ thuộc)
- Modify: `infra/config-server/config-repo/application.yaml` (exposure, tag `application`) và mọi `config-repo/*-service.yaml` có ghi đè `management.endpoints.web.exposure`
- Modify: `shared/common-security/src/main/java/com/ieltspath/commonsecurity/config/CommonSecurityAutoConfiguration.java` (permit `GET /actuator/prometheus`)
- Create: `infra/prometheus/prometheus.yml`; Modify: `docker-compose.yml` (service `prometheus`, profile `observability`)
- Modify: `services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/client/*` (bỏ header thủ công), xóa `GatewayRequestContext.java` nếu không còn dùng
- Create: gauge outbox ở assessment và community (đọc count/oldest từ bảng outbox)
- Modify: `docker-compose.yml` (`SPRING_PROFILES_ACTIVE` thêm `json-logs` cho service)

## Implementation Steps

### Test viết trước

1. Unit test filter: có header → giữ nguyên; không có → sinh; MDC rỗng sau request (kể cả khi controller ném lỗi).
2. Test customizer với `MockRestServiceServer`: request đi ra mang header bằng MDC hiện tại.
3. Test RabbitMQ (Testcontainers): publish trong MDC `abc` → listener thấy MDC `abc` khi xử lý → MDC rỗng sau đó.
4. Test tích hợp một service (learning): gọi endpoint có header `X-Correlation-Id: t-1` → log capture (`OutputCaptureExtension`) chứa `t-1` ở dòng log của request; downstream giả nhận header `t-1`.
5. Test actuator ở mỗi service (dùng security thật, không tắt): `/actuator/prometheus` không token trả 200 và có `http_server_requests_seconds`; ở assessment và community có `outbox_pending`. Test này đỏ trước khi sửa rule security.

### Code

6. Tạo module, auto-configuration đăng ký qua `AutoConfiguration.imports`.
7. Thêm phụ thuộc vào 9 service; chạy test từng service.
8. Bỏ code correlation riêng của assessment.
9. Thêm gauge outbox ở hai service có relay.
10. Cập nhật config-repo và compose.
11. Tài liệu: mục "Log và metric" trong `docs/system-architecture.md` (tên field log, endpoint metric, cách tra một request).

## Success Criteria

- [ ] Một request qua gateway → learning → content: cùng `correlationId` ở log JSON của cả ba (kiểm tay bằng `docker compose logs | grep`).
- [ ] Event assessment → learning listener: log consumer có `correlationId` của request gốc.
- [ ] `/actuator/prometheus` có ở 9 service, không truy cập được qua gateway (`GET :8080/actuator/prometheus` → 404).
- [ ] `docker compose --profile observability up` với `--scale learning-service=2`: Prometheus thấy cả 2 target learning ở trạng thái UP.
- [ ] Toàn bộ test hiện có xanh.

## Risk Assessment

- **Log JSON khó đọc local:** chỉ bật bằng profile trong docker.
- **MDC rò giữa request (thread pool):** luôn xóa trong `finally`; test bắt buộc ở bước 1.
- **Lộ dữ liệu trong log:** không log body request; chỉ id.
- **Prometheus lộ ra ngoài:** gateway không có route `/actuator/**`; thêm test ở gateway.
