---
phase: 2
title: "Production compose and reverse proxy"
status: done
priority: P1
dependencies: [1]
---

# Phase 2: Compose production và reverse proxy

## Requirements
- `docker-compose.prod.yml` dùng chồng lên `docker-compose.yml` (không sửa hành vi local):
  - Mọi service dùng `image: ghcr.io/...:${IMAGE_TAG}` thay vì `build`.
  - **Bỏ mọi `ports:` ra host** trừ reverse proxy; PostgreSQL, RabbitMQ (cả UI), Redis, Eureka, Config Server, các
    service chỉ trong mạng Docker.
  - `env_file: /opt/ieltspath/env/<service>.env` cho từng service (phase 3), thay cho biến nội suy từ `.env` gốc.
  - `restart: unless-stopped`, `mem_limit` theo bảng đo ở phase 4, healthcheck có sẵn giữ nguyên.
  - `DEMO_DATA_ENABLED=false`; `SPRING_PROFILES_ACTIVE` production nếu service có profile riêng.
  - Volume có tên cho dữ liệu PostgreSQL, RabbitMQ, Redis.
- Reverse proxy (Caddy theo đề xuất ở `plan.md`): container duy nhất mở 80/443, `api.<domain>` → `api-gateway:8080`, tự
  cấp HTTPS; header bảo mật cơ bản (HSTS); giới hạn kích thước body theo route upload hiện có.
- WebSocket của game (`/ws/games/**`) đi qua proxy được.
- Stack observability (`docker-compose.observability.yml`) tùy chọn, không mở Grafana ra ngoài nếu bật (truy cập qua SSH tunnel).

## Implementation Steps
1. Viết file override; `docker compose -f docker-compose.yml -f docker-compose.prod.yml config --quiet` trong CI.
2. Chạy thử trên một máy sạch (hoặc VM local) với image từ GHCR và env giả: đăng nhập, nộp một bài, game WebSocket.
3. `nmap`/`ss` trên máy thử: chỉ 22, 80, 443 mở.

## Success Criteria
- [ ] Compose production chạy toàn stack từ image GHCR, không build tại chỗ.
- [ ] Chỉ 80/443 (và SSH) mở ra ngoài; HTTPS hợp lệ.
- [ ] Không còn biến nào lấy từ `.env` gốc khi chạy production.
