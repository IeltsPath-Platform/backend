---
title: "Production deployment: VPS + Docker Compose, GitHub secrets, frontend on Vercel/Cloudflare Pages"
description: "CD lần đầu cho IELTSPath: image lên GHCR, deploy qua SSH, secret theo từng service, HTTPS, sao lưu DB, frontend tĩnh trên CDN"
status: pending
priority: P1
branch: "chore/production-deployment"
tags: [deploy, ci-cd, secrets, docker, vercel, security]
blockedBy: [261111-1620-platform-hardening]
blocks: []
created: "2026-11-11T09:40:00.000Z"
---

# Production deployment

Người dùng chốt ngày 2026-11-11: dự án **sẽ deploy thật**; backend trên **một VPS chạy Docker Compose**; secret ở
**GitHub Actions secrets**, ghi ra **file env riêng từng service** trên server; frontend trên **Vercel hoặc Cloudflare
Pages**. Chưa có code nào được viết.

## Hiện trạng (đã đọc, chưa chạy)

| Dữ kiện | Nơi | Hệ quả |
| --- | --- | --- |
| Có `docker-compose.yml` cho toàn stack, build tại chỗ (`Dockerfile.spring-service`, `MODULE_PATH`) | root | Dùng lại; production chỉ cần file override kéo image có sẵn |
| CI chỉ `mvn verify` và `docker compose config`; không build image, không deploy | `.github/workflows/ci.yml` | Thêm workflow build image và deploy |
| Config Server đọc config-repo từ **filesystem** (`native`, `CONFIG_NATIVE_REPO_URI`) | `infra/config-server` | Config-repo phải có trong image Config Server hoặc được mount; đổi cấu hình = deploy lại |
| Config-repo còn secret dự phòng | 7 file | **Chặn deploy:** làm phase 1 của plan `261111-1620-platform-hardening` trước |
| Cookie auth cấu hình qua biến: `APP_AUTH_COOKIE_SECURE` (mặc định `false`), `APP_AUTH_COOKIE_SAME_SITE` (`LAX`), domain tùy chọn; refresh cookie path `/auth` | `user-service.yaml`, `AuthCookieService` | Production phải `SECURE=true` |
| Frontend gọi API bằng `API_BASE_URL`, `credentials: include` + Bearer trong bộ nhớ | `frontend/src/lib/httpClient.ts`, `env.ts` | Frontend khác host với API: cần CORS và cookie cùng site |
| Gateway có CORS theo danh sách origin cấu hình được | `SecurityConfig.allowedOriginPatterns` | Thêm origin của frontend production |
| Stack ≈ 12 JVM + PostgreSQL + RabbitMQ + Redis | Compose | VPS tối thiểu 8 GB RAM, 4 vCPU; giới hạn heap từng JVM |

## Quyết định đã chốt
1. VPS + Docker Compose; image ở GitHub Container Registry (GHCR); deploy bằng GitHub Actions qua SSH.
2. Secret ở GitHub Actions secrets (environment `production`); deploy ghi ra `/opt/ieltspath/env/<service>.env` mode 600,
   mỗi service chỉ nhận biến nó cần.
3. Frontend tĩnh trên Vercel hoặc Cloudflare Pages.

## Cần chốt (chờ người dùng)

| Câu hỏi | Đề xuất | Lý do |
| --- | --- | --- |
| Tên miền | **Bắt buộc có tên miền riêng**, frontend `app.<domain>`, API `api.<domain>` | Cùng tên miền gốc thì cookie refresh `SameSite=Lax` vẫn gửi được. Dùng `*.vercel.app` hoặc `*.pages.dev` thì cookie thành bên thứ ba, trình duyệt chặn dần, học viên bị đăng xuất liên tục |
| Reverse proxy trước Gateway | Caddy (tự cấp và gia hạn HTTPS) | Một container, cấu hình vài dòng; Nginx + certbot cũng được nhưng nhiều bước hơn |
| Nơi lưu bản sao lưu DB | Bucket riêng trên Supabase Storage (đã dùng) hoặc S3-compatible khác, khác máy VPS | Sao lưu nằm trên chính VPS thì mất máy là mất hết |
| Nhà cung cấp VPS | Người dùng chọn (8 GB RAM, 4 vCPU, ổ SSD ≥ 80 GB) | Không ảnh hưởng thiết kế |

## Phases

| Phase | Việc | Phụ thuộc | Status |
| --- | --- | --- | --- |
| 1 | [Build image lên GHCR](./phase-01-build-images.md) | - | Pending |
| 2 | [Compose production và reverse proxy](./phase-02-production-compose.md) | 1 | Pending |
| 3 | [Secret theo từng service](./phase-03-secrets-per-service.md) | hardening phase 1 | Pending |
| 4 | [Workflow deploy, sao lưu, quay lui](./phase-04-deploy-backup-rollback.md) | 1, 2, 3 | Pending |
| 5 | [Frontend trên CDN và cookie/CORS](./phase-05-frontend-cdn.md) | 4, tên miền | Pending |

## Rủi ro

| Rủi ro | Giảm thiểu |
| --- | --- |
| Secret cũ trong lịch sử git | Sinh **mới toàn bộ** cho production (JWT, DB, RabbitMQ, Redis); không tái dùng giá trị local |
| Một VPS là một điểm hỏng | Sao lưu DB hằng ngày ra ngoài, có quy trình khôi phục đã thử; chấp nhận downtime khi máy hỏng |
| Hết RAM khi 12 JVM cùng chạy | Giới hạn heap (`-XX:MaxRAMPercentage`, `mem_limit`), đo sau lần deploy đầu |
| Lộ cổng nội bộ (PostgreSQL, RabbitMQ UI, Eureka, Config Server) | Chỉ mở 80/443 (và SSH); mọi cổng khác chỉ trong mạng Docker |
| Demo data lên production | `DEMO_DATA_ENABLED=false` trong env production; kiểm sau deploy |
| Migration lỗi giữa chừng | Sao lưu trước mỗi deploy; Flyway chạy khi service khởi động, lỗi thì service không lên và workflow báo đỏ |

## Quy ước
- Không ghi giá trị secret vào repo, plan, log CI. Workflow dùng `::add-mask::` cho mọi giá trị đọc ra.
- Tài liệu vận hành mới: `docs/deployment.md` (cài server, deploy, quay lui, xoay khóa, khôi phục DB).
