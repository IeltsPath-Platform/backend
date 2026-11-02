---
phase: 1
title: "Hạ tầng Loki, Tempo, Alloy, Grafana"
status: pending
priority: P2
dependencies: []
---

# Phase 1: hạ tầng Loki, Tempo, Alloy, Grafana (không đổi Java)

## Overview

Thêm bốn container vào profile `observability`: log Docker → Alloy → Loki → Grafana; Tempo sẵn sàng nhận OTLP. Người làm: Claude. Chưa cần code Java.

## Files

| Hành động | File | Ghi chú |
|---|---|---|
| Create | `infra/loki/loki-config.yml` | monolithic, filesystem, giữ 7 ngày |
| Create | `infra/tempo/tempo.yml` | monolithic, local storage |
| Create | `infra/alloy/config.alloy` | một Alloy: log Docker + OTLP traces |
| Create | `infra/grafana/provisioning/datasources/datasources.yml` | Prometheus, Loki, Tempo với UID cố định và liên kết hai chiều |
| Modify | `docker-compose.yml` | 4 service profile `observability`; volume `loki-data`, `tempo-data`, `alloy-data`, `grafana-data` |
| Modify | `docs/system-architecture.md` | topology, trường log, cách tra một request |

**Grafana:** mật khẩu admin lấy từ `${GRAFANA_ADMIN_PASSWORD:?Set GRAFANA_ADMIN_PASSWORD}` (cùng kiểu với `POSTGRES_PASSWORD` trong compose), **không** dùng mặc định `admin`; đặt `GF_AUTH_ANONYMOUS_ENABLED=false`; không đưa mật khẩu vào file nào của repo. Biến nằm trong `.env` local, không commit.

**Chọn tag (bước 0, trước khi viết compose):** chọn tag Loki, Tempo, Alloy, Grafana; ghi vào bảng "Tag đã pin" của plan kèm ngày và cách kiểm (ví dụ chạy container và đọc phiên bản). Không có tag đã pin thì không làm các bước sau.

Compose: Grafana publish `127.0.0.1:3000` (chỉ local; repo hiện không publish cổng 3000 nên không đụng; frontend chạy dev trên host có thể chiếm 3000 khi cùng lúc, khi đó đổi cổng publish); Loki, Tempo, Alloy **không** publish ra host (Grafana và app gọi theo tên service). Nếu chạy Java ngoài Docker, thêm `127.0.0.1:4318:4318` cho Alloy. Alloy mount `/var/run/docker.sock:ro`: `ro` không bỏ rủi ro của Docker API; chấp nhận cho local, ghi rõ trong tài liệu. Loki/Tempo chạy non-root: kiểm quyền ghi volume khi khởi động, không đặt `user: root`. Image ghi tag literal đã kiểm (như `prom/prometheus:v2.55.1` hiện có), không `latest`, không `${VAR:?}`.

### Loki (`infra/loki/loki-config.yml`)

```yaml
auth_enabled: false
server:
  http_listen_port: 3100
common:
  path_prefix: /loki
  replication_factor: 1
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: inmemory
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules
schema_config:
  configs:
    - from: "2024-01-01"
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h
limits_config:
  retention_period: 168h
compactor:
  working_directory: /loki/compactor
  retention_enabled: true
  delete_request_store: filesystem
```

`auth_enabled: false` chấp nhận được vì không publish cổng 3100.

### Tempo (`infra/tempo/tempo.yml`)

```yaml
stream_over_http_enabled: true
server:
  http_listen_port: 3200
distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: "0.0.0.0:4317"
        http:
          endpoint: "0.0.0.0:4318"
storage:
  trace:
    backend: local
    wal:
      path: /var/tempo/wal
    local:
      path: /var/tempo/blocks
usage_report:
  reporting_enabled: false
```

Thêm `compactor.compaction.block_retention: 168h` nếu bản đã pin hỗ trợ khóa này (kiểm khi khởi động), để trace không đầy ổ đĩa.

### Alloy (`infra/alloy/config.alloy`)

```alloy
discovery.docker "local" {
  host = "unix:///var/run/docker.sock"
}

discovery.relabel "ieltspath" {
  targets = discovery.docker.local.targets
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_project"]
    regex         = "ieltspath"
    action        = "keep"
  }
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    target_label  = "service_name"
  }
  rule {
    source_labels = ["__meta_docker_container_name"]
    regex         = "/(.*)"
    target_label  = "container"
  }
}

loki.source.docker "ieltspath" {
  host       = "unix:///var/run/docker.sock"
  targets    = discovery.relabel.ieltspath.output
  forward_to = [loki.process.container.receiver]
}

loki.process "container" {
  stage.docker {}
  forward_to = [loki.write.local.receiver]
}

loki.write "local" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}

otelcol.receiver.otlp "apps" {
  http {
    endpoint = "0.0.0.0:4318"
  }
  output {
    traces = [otelcol.processor.batch.traces.input]
  }
}

otelcol.processor.batch "traces" {
  output {
    traces = [otelcol.exporter.otlphttp.tempo.input]
  }
}

otelcol.exporter.otlphttp "tempo" {
  client {
    endpoint = "http://tempo:4318"
  }
}
```

Kiểm `service_name` (Loki) khớp `service.name` (Tempo): compose service phải bằng `spring.application.name`; lệch thì sửa ở provisioning, không đổi tên service.

### Grafana provisioning (`infra/grafana/provisioning/datasources/datasources.yml`)

- Prometheus: URL service `prometheus` hiện có, `uid: prometheus`.
- Loki: `http://loki:3100`, `uid: loki`, **derived field** `TraceID`: regex `"traceId":"([0-9a-fA-F]{32})"`, link nội bộ tới `tempo`, giá trị `${__value.raw}`.
- Tempo: `http://tempo:3200`, `uid: tempo`, **tracesToLogs** → `loki`, ánh xạ `service.name` → label `service_name`, lọc theo trace ID, cửa sổ ±2 giây.
- Không tạo hai datasource trùng UID; không cấu hình tay trong UI (bị ghi đè).

## Lệnh kiểm tra

```bash
docker compose config --quiet                          # mặc định: phải sạch
docker compose --profile observability config --quiet
docker compose --profile observability up -d loki tempo alloy grafana
docker compose ps loki tempo alloy grafana
docker compose logs --tail=100 loki tempo alloy
```

Image Alloy/Loki/Tempo có thể không có `wget`/shell: kiểm `ready` bằng "Save & test" của Grafana hoặc container tạm cùng network; thiếu `wget` không có nghĩa service chết.

## Success Criteria

- [ ] `docker compose config --quiet` (mặc định) sạch; `docker compose up` mặc định không đổi.
- [ ] Profile `observability` chạy Prometheus + Grafana + Loki + Alloy + Tempo, cả bốn container mới ở trạng thái chạy.
- [ ] Grafana Explore/Loki tìm được log JSON theo `service_name`, `container`, `correlationId`; `--scale learning-service=2` thấy log cả hai replica.
- [ ] `docker compose logs` vẫn xem được log khi Alloy chạy.
- [ ] Loki, Tempo, Alloy không truy cập được từ gateway hay host (trừ Grafana `127.0.0.1:3000`).
- [ ] RAM/CPU/dung lượng của bốn container ghi vào báo cáo.

## Rủi ro

| Rủi ro | Biện pháp |
|---|---|
| Docker socket quyền cao | Chỉ local, `:ro`, không publish Alloy; ghi trong tài liệu |
| Đổi version làm hỏng cấu hình | Pin tag literal, smoke test; không `latest` |
| Tempo 3.x đổi kiến trúc (có thể đòi Kafka) | Kiểm khởi động bản đã pin với cấu hình trên; lùi bản 2.x gần nhất nếu cần, ghi lý do |
| Tăng RAM/CPU laptop | Profile opt-in, đo và ghi |
| Grafana mở ra với mật khẩu mặc định | Mật khẩu bắt buộc qua biến môi trường, anonymous tắt, publish `127.0.0.1` |
| Rollback | Không bật profile thì không có container nào mới; volume mới có thể xóa bằng `docker compose down -v` **chỉ** khi chấp nhận mất dữ liệu log/trace local. Không sửa gì ở service nghiệp vụ nên không cần rollback code |
