---
phase: 4
title: "Deploy workflow, backup, rollback"
status: done-pending-vps
priority: P1
dependencies: [1, 2, 3]
---

# Phase 4: Workflow deploy, sao lưu, quay lui

## Requirements
- `.github/workflows/deploy.yml`, chạy tay (`workflow_dispatch`) với tham số `image_tag` (mặc định SHA mới nhất của
  `main`), environment `production` (cần duyệt).
- Các bước qua SSH:
  1. Sao lưu DB (`pg_dump` từng database, nén, đẩy ra nơi lưu ngoài VPS theo `plan.md`); lỗi sao lưu thì dừng deploy.
  2. Render env từng service (phase 3); đồng bộ `docker-compose*.yml`, Caddyfile vào `/opt/ieltspath`.
  3. `docker login ghcr.io` bằng token chỉ đọc; `IMAGE_TAG=<tag> docker compose -f ... pull`.
  4. `docker compose up -d` theo thứ tự đã có trong Compose (infra → config → eureka → gateway → service), chờ healthcheck.
  5. Kiểm sau deploy: `/actuator/health` của gateway qua HTTPS, một request đăng nhập thử bằng tài khoản kiểm tra.
  6. Ghi tag đang chạy vào `/opt/ieltspath/RELEASE` và vào summary của workflow.
- **Quay lui:** chạy lại workflow với `image_tag` cũ. Migration Flyway chỉ đi tiến; bản cũ chạy trên schema mới phải được
  giữ tương thích (migration chỉ cộng thêm). Migration phá hủy (như xóa bảng outbox ở plan hardening) ghi rõ trong release
  là không quay lui qua nó được, phải khôi phục từ bản sao lưu.
- **Sao lưu định kỳ:** cron hằng ngày trên VPS (hoặc workflow theo lịch) chạy cùng script; giữ 7 bản ngày, 4 bản tuần.
- **Khôi phục:** script khôi phục một database từ bản sao lưu; thử khôi phục thật một lần vào máy thử.
- Đo RAM thực tế sau deploy đầu, điền `mem_limit` vào compose production.
- `docs/deployment.md`: chuẩn bị VPS (Docker, firewall, user `deploy`), deploy, quay lui, sao lưu, khôi phục, xoay khóa.
  Cập nhật `AGENTS.md` §2 (đã có CD), `CLAUDE.md`, `README.md`.

## Success Criteria
- [ ] Deploy một tag mới và quay lui về tag cũ đều thành công trên server thật.
- [ ] Bản sao lưu nằm ngoài VPS; khôi phục thử thành công.
- [ ] `docs/deployment.md` đủ để người khác deploy lại từ đầu.
