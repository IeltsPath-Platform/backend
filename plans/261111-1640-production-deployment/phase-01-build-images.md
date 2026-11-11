---
phase: 1
title: "Build images to GHCR"
status: done
priority: P1
dependencies: []
---

# Phase 1: Build image lên GHCR

## Requirements
- Workflow mới `.github/workflows/images.yml`: chạy khi push vào `main` (sau khi CI xanh) và khi chạy tay.
- Matrix theo module: 3 infra (config-server có Dockerfile riêng, eureka, gateway) và 9 service nghiệp vụ, dùng
  `Dockerfile.spring-service` với `MODULE_PATH`.
- Tag: SHA commit (bất biến) và `main` (trỏ bản mới nhất). Tên `ghcr.io/<owner>/ieltspath-<module>`.
- Cache layer Maven bằng `docker/build-push-action` cache GHA để build không tải lại dependency mỗi lần.
- Image Config Server chứa `infra/config-server/config-repo` (kiểm Dockerfile hiện tại; nếu chưa có thì `COPY`), đặt
  `CONFIG_NATIVE_REPO_URI` trỏ tới đường dẫn trong image.
- Không có `.env` hay secret trong image (`.dockerignore` đã loại `.env`, `secrets`).
- Runtime image chạy user không phải root; thêm `JAVA_TOOL_OPTIONS` mặc định `-XX:MaxRAMPercentage=75`.

## Implementation Steps
1. Kiểm Dockerfile của Config Server và `Dockerfile.spring-service` (user, JVM options).
2. Viết workflow; chạy tay một lần; xác nhận 12 image có trên GHCR với tag SHA.
3. `docker run` thử một image với `.env` giả để chắc image tự chứa đủ.

## Success Criteria
- [ ] 12 image trên GHCR, tag theo SHA; build lại không đổi code dùng cache.
- [ ] `docker history` không có secret; container chạy user không phải root.
