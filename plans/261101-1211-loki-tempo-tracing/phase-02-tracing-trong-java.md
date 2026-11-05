---
phase: 2
title: "Tracing trong Java"
status: pending
priority: P2
dependencies: [1]
---

# Phase 2: tracing trong Java

## Overview

Gateway và 9 service phát trace W3C qua Micrometer Tracing + OTLP; log JSON có `traceId`/`spanId`. Người làm: Claude. Chưa gồm outbox/RabbitMQ (phase 3).

## Files

| Hành động | File | Ghi chú |
|---|---|---|
| Modify | `infra/api-gateway/pom.xml`, `services/*/pom.xml` (9) | `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`; version do Spring Boot BOM quản lý, không tự ghi version |
| Modify | `pom.xml` gốc (quản lý version) và `services/*/pom.xml`, `infra/api-gateway/pom.xml` | thêm `micrometer-tracing-test` scope test để test span dùng `TestSpanHandler` hoặc tương đương; version lấy từ Spring Boot BOM |
| Modify | `infra/config-server/config-repo/application.yaml` | ghép vào khối `management:` đang có |
| Modify | `infra/config-server/config-repo/api-gateway.yaml` | `spring.reactor.context-propagation: auto` |
| Modify | `docker-compose.yml` (`x-spring-env`) | `TRACING_ENABLED`, `TRACING_SAMPLING_PROBABILITY`, `OTLP_TRACES_ENDPOINT` |
| Modify | `shared/common-observability/` | chỉ nếu `traceId`/`spanId` chưa ra trong log JSON; không viết lại filter/customizer đang có |

```yaml
management:
  tracing:
    enabled: ${TRACING_ENABLED:false}
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:1.0}
  otlp:
    tracing:
      endpoint: ${OTLP_TRACES_ENDPOINT:http://alloy:4318/v1/traces}
```

- Mặc định `false`: chạy backend không có profile `observability` thì exporter không gọi host chưa tồn tại. Sampling 1.0 chỉ cho local.
- **Quyết định mở (xem plan):** profile `observability` **không** tự bật tracing ở service, nên chỉ bật profile sẽ có log mà không có trace. Đề xuất: file `docker-compose.observability.yml` đặt `TRACING_ENABLED=true` và `OTLP_TRACES_ENDPOINT` cho các service, chạy bằng `docker compose -f docker-compose.yml -f docker-compose.observability.yml --profile observability up`.
- Mọi client HTTP phải dựng từ `RestClient.Builder`/`WebClient.Builder` auto-configured (đã đúng nhờ customizer phase 2 gốc); `RestClient.create(...)` làm mất tracing. Rà 22 client + gateway.
- `LogstashEncoder` mặc định ghi MDC ra field JSON, nên `traceId`, `spanId` (Micrometer đặt vào MDC) tự ra khi tracing bật; test khẳng định, không giả định. Không đổi `correlationId` thành `traceId`. Không log JWT, API key, body, bài viết, prompt, phản hồi LLM.
- `@Async`, executor, scheduler: context không tự truyền sang thread khác; dùng `ContextPropagatingTaskDecorator` hoặc bọc rõ ràng ở chỗ cần.
- Gateway là WebFlux: không dựa vào MDC `ThreadLocal` trong pipeline reactive; kiểm propagation bằng test.

## Test viết trước

1. HTTP gateway → learning → content bằng exporter span trong bộ nhớ: span server/client cùng `traceId`, `traceparent` được gửi đi.
2. Log JSON của request chứa `correlationId`, `traceId`, `spanId`; không chứa token hay body.
3. `TRACING_ENABLED=false`: không span, không lỗi, `correlationId` vẫn hoạt động.
4. Kiểm tay: tắt Alloy/Tempo, request vẫn thành công và không chậm đáng kể.

## Success Criteria

- [ ] Waterfall gateway → learning → content hiện trong Tempo từ một request thật.
- [ ] Log → trace và trace → log đều bấm được trong Grafana.
- [ ] Log JSON không rò `traceId`/`spanId`/`correlationId` thành label Loki.
- [ ] Test của plan độ tin cậy và `mvn -B -ntp verify` vẫn xanh.

## Rủi ro

| Rủi ro | Biện pháp |
|---|---|
| Hai client span do vừa Java Agent vừa Micrometer | Không dùng Java Agent |
| Trace mất ở `@Scheduled`/`@Async` | Bọc context rõ ràng; phase 3 lo outbox |
| Exporter chặn request khi Alloy chết | Test và kiểm tay tắt Alloy/Tempo |
