---
title: "Log tập trung và tracing phân tán (Loki, Tempo, Alloy, Grafana)"
description: "Mở rộng observability của plan độ tin cậy backend: thu log Docker vào Loki, trace OpenTelemetry vào Tempo, xem bằng Grafana, giữ trace xuyên outbox và RabbitMQ."
status: pending
priority: P2
branch: "feat/reliability-patterns"
tags: [observability, tracing, logging, loki, tempo, grafana]
blockedBy: [261031-2145-backend-reliability-patterns]
blocks: []
related: [261101-0930-reliability-followups]
created: "2026-11-01T05:11:00.000Z"
createdBy: "ck:plan"
source: skill
---

# Log tập trung và tracing phân tán

## Overview

Mở rộng ngoài phạm vi ban đầu của [plan độ tin cậy](../261031-2145-backend-reliability-patterns/plan.md), vốn ghi "tracing phân tán ngoài phạm vi". Plan đó đang được Codex chạy nên **plan này tách riêng và chưa sửa gì ở đó**. Bắt đầu sau khi Codex báo xong gói E.

Môi trường: laptop + Docker Compose, nhiều instance, không Kubernetes, chưa VPS. Mọi thứ nằm trong profile `observability`; tracing mặc định tắt.

Hai năng lực mới, giữ Prometheus cho metrics:

1. **Log tập trung:** Alloy đọc stdout container → Loki → Grafana Explore.
2. **Tracing:** Micrometer Tracing + OpenTelemetry (W3C `traceparent`) → Alloy (OTLP) → Tempo → Grafana (waterfall, nhảy hai chiều log ↔ trace), kể cả qua outbox và RabbitMQ.

Không Promtail, không Java Agent, không Kubernetes, không HA.

### Ba định danh, không trộn

| Trường | Mục đích | Loki label? |
|---|---|---|
| `correlationId` | mã hỗ trợ/debug, header `x-correlation-id` (đã có) | Không |
| `traceId` | trace OpenTelemetry xuyên service | Không |
| `spanId` | một bước trong trace | Không |

`traceId` **không** thay `correlationId`; cả hai cùng xuất hiện trong log JSON để tìm và liên kết. Label Loki chỉ gồm `service_name`, `container` (tránh bùng nổ cardinality).

## Đối chiếu đề xuất gốc với repo (điểm lệch đã chỉnh)

| Đề xuất gốc | Thực tế | Xử lý |
|---|---|---|
| "Giữ Grafana hiện có", 3 container mới | `docker-compose.yml` chỉ có `prometheus` (profile `observability`), **không có Grafana** | Thêm `grafana`; **4** container: `loki`, `tempo`, `alloy`, `grafana` |
| `image: ...:${LOKI_VERSION:?...}` | Compose nội suy biến cho **mọi** service kể cả profile không bật → `docker compose up` mặc định báo lỗi | Tag literal đã kiểm, như `prom/prometheus:v2.55.1`. Không `:?`, không `latest` |
| Provisioning Grafana "có thể đã có" | Chưa có `infra/grafana/` | Tạo provisioning datasource bằng file YAML |
| Project `ieltspath`, network `codebase-network` | Đúng (`docker-compose.yml` dòng 5 và 385) | Giữ regex `ieltspath` trong Alloy |
| Tempo 3.x monolithic không cần Kafka | Chưa kiểm trên máy này | Kiểm bản đã pin khởi động được; nếu đòi Kafka thì pin 2.x, ghi lý do |
| Số migration chưa ấn định | Đã có: access V8, community V9, assessment V21, learning V41 (correlation); access V7 dành cho idempotency | Cột trace context: access V9, assessment V22, community V10, learning V42; kiểm số trống |

## Phases

| Phase | Tên | Người làm | Phụ thuộc | Status |
|---|---|---|---|---|
| 1 | [Hạ tầng Loki, Tempo, Alloy, Grafana](./phase-01-ha-tang-loki-tempo-grafana.md) | Claude | plan 2145 xong gói E | completed: đã kiểm trên stack (Tempo nhận trace qua Alloy, datasource Grafana OK) |
| 2 | [Tracing trong Java](./phase-02-tracing-trong-java.md) | Claude | 1 | partial: code và reactor verify xanh (3891 test); CHƯA kiểm waterfall gateway → learning → content (cần đủ 12 service) |
| 3 | [Trace xuyên outbox, RabbitMQ, DLQ](./phase-03-trace-qua-outbox-rabbit-dlq.md) | Claude (thực hiện code sau khi Codex dừng) | 2 | partial: code và test tập trung xanh ở 5 module; CHƯA có test đầy đủ xuyên HTTP → relay → consumer trên stack thật |

Lý do chia: phase 1–2 là hạ tầng và module dùng chung (thuộc Claude theo plan gốc); phase 3 sửa outbox/DLQ vốn nằm trong phạm vi Codex. Claude không commit lên `feat/reliability-patterns` khi Codex còn chạy; phase 1 trùng `docker-compose.yml` và config-repo với gói E nên phải đợi E xong.

## Việc cần làm ở plan 2145 sau khi Codex xong (chưa làm, tránh đụng nhánh đang chạy)

1. `plan.md`: dòng "Ngoài phạm vi: ... tracing phân tán ..." đổi thành "tracing phân tán local làm ở plan 261101-1211; HA và production ngoài phạm vi"; thêm quyết định chấp thuận hạ tầng Loki/Tempo/Alloy/Grafana.
2. `phase-02-observability.md`: thêm con trỏ tới plan này, ghi đây là mở rộng, không sửa phần đã code.
3. Phase 4, 7, 8: con trỏ tới phase 3 của plan này (trace xuyên outbox và DLQ replay).

## Tiêu chí nghiệm thu tổng

- [ ] `docker compose up` mặc định không đổi; profile `observability` chạy Prometheus + Grafana + Loki + Alloy + Tempo.
- [ ] Log JSON tìm được theo `service_name`, `container`, `correlationId`; `--scale learning-service=2` thấy cả hai replica; `traceId`/`spanId`/`correlationId` không là label.
- [ ] Gateway → learning → content cho waterfall trong Tempo; log ↔ trace bấm được hai chiều.
- [ ] Event qua outbox + relay + consumer giữ `traceId` sau mất thread gốc, retry, replay DLQ. Chưa xong thì ghi **PARTIAL**.
- [ ] Tắt Alloy/Tempo: request vẫn thành công, không chậm đáng kể.
- [ ] Loki, Tempo, Alloy không truy cập được từ gateway hay host (trừ Grafana `127.0.0.1:3000`).
- [ ] Đã đo RAM/CPU/dung lượng; `TRACING_ENABLED=false` khi không dùng profile.
- [ ] Test của plan độ tin cậy và `mvn -B -ntp verify` xanh.

## Red Team Review

Ngày 2026-11-01. Đối chiếu từng giả định với repo (Spring Boot 3.5.14, `logback-spring.xml` dùng `LogstashEncoder`, `docker-compose.yml` không có Grafana, không có cổng 3000, không có dependency tracing nào).

| # | Mức | Phát hiện | Bằng chứng | Xử lý |
|---|---|---|---|---|
| 1 | High | Bật profile `observability` không bật tracing ở service, nên tiêu chí "waterfall trong Tempo" không đạt mặc định | `TRACING_ENABLED` mặc định `false` trong phase 2 | **Mở:** chọn cách bật (xem dưới) |
| 2 | High | Mật khẩu Grafana chưa quy định; mặc định `admin` và anonymous là rủi ro | phase 1 không nêu | Đã sửa phase 1: `${GRAFANA_ADMIN_PASSWORD:?...}`, anonymous tắt |
| 3 | High | Test "span trong bộ nhớ" không có dependency nào để chạy | `shared/common-observability/pom.xml` không có tracing test | Đã sửa phase 2: thêm `micrometer-tracing-test` scope test |
| 4 | Medium | Tag image chưa pin, nên chưa thể chạy stack | phase 1 chỉ nói "pin" | Đã sửa phase 1: bước 0 chọn tag và ghi vào plan. **Mở:** cần người chạy kiểm Tempo 3.x có đòi Kafka không |
| 5 | Medium | Không có rollback | phase 1 | Đã sửa phase 1: mục Rollback |
| 6 | Medium | Cổng 3000 có thể trùng với frontend dev trên host | `docker-compose.yml` không publish 3000, frontend nằm ngoài compose | Đã ghi cách đổi cổng trong phase 1, không đổi mặc định |
| 7 | Low | `service_name` Alloy phải khớp `service.name` Tempo, không kiểm được trước khi chạy | phase 1 | Đã có bước kiểm trong phase 1; không đổi |
| 8 | Ghi nhận | Logback đã có `LogstashEncoder`, MDC sẽ ra JSON; `spring.reactor.context-propagation` hợp lệ ở Boot 3.5.14 | `logback-spring.xml`, `pom.xml:17` | Không đổi |
| 9 | Ghi nhận | Relay phase 3 đổi các bảng outbox vừa thêm `correlation_id` (plan 0930); cùng điểm ghi, cần một lượt sửa | phase 3 | Gộp vào gói F3 cho Codex, không tách riêng |

**Đã chốt (2026-11-01)**

1. Cách bật tracing (phát hiện 1): file `docker-compose.observability.yml` bật `TRACING_ENABLED=true` cho các service khi chạy kèm observability; chạy thường không đổi.
2. Tag image: Claude tra và đề xuất, người dùng duyệt trước khi ghi vào phase 1 bước 0.

**Tag image (kiểm 2026-11-01 qua Docker Hub API, không pull image)**

| Image | Tag | Trạng thái |
|---|---|---|
| `grafana/loki` | `3.7.6` | **Đã duyệt.** Tag tồn tại. Có `3.7.9` mới hơn cùng nhánh (chưa chọn) |
| `grafana/tempo` | `2.10.10` | **Đổi theo quyết định ngày 2026-11-02** (từ `3.0.5`): Tempo 2.10.10 nhận trace đúng qua Alloy (kiểm bằng telemetrygen; trace tìm được trong Tempo). HTTP 500 trước đó là do payload JSON viết tay của phép thử, không phải do Tempo. Tag `3.0.0` không tồn tại. Cấu hình local và OTLP trong plan khớp hướng dẫn monolithic 3.0 (không Kafka); khởi động thật vẫn là bước kiểm |
| `grafana/alloy` | `v1.21.3` | **Đã duyệt.** Mirror cộng đồng `1.15.0` đã cũ |
| `grafana/grafana` | `13.2.5` | **Đã duyệt.** Dùng `grafana/grafana`, không `grafana-oss` |

## Ngoài phạm vi

Kubernetes, HA Loki/Tempo, Kafka cho Tempo, ELK/OpenSearch, dashboard và alert production, giám sát VPS, Java Agent toàn hệ thống, span JDBC, gửi log trực tiếp từ code ứng dụng vào Loki.

## Nguồn

- Spring Boot 3.5 Tracing: https://docs.spring.io/spring-boot/3.5/reference/actuator/tracing.html
- Alloy, monitor Docker containers: https://grafana.com/docs/alloy/latest/monitor/monitor-docker-containers/
- Alloy `otelcol.receiver.otlp`: https://grafana.com/docs/alloy/latest/reference/components/otelcol/otelcol.receiver.otlp/
- Loki retention: https://grafana.com/docs/loki/latest/operations/storage/retention/
- Tempo triển khai: https://grafana.com/docs/tempo/latest/set-up-for-tracing/setup-tempo/
- Grafana trace-to-logs: https://grafana.com/docs/grafana/latest/datasources/tempo/configure-tempo-data-source/configure-trace-to-logs/
