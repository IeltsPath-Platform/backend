---
phase: 3
title: "Secrets per service"
status: done
priority: P1
dependencies: []
---

# Phase 3: Secret theo từng service

## Requirements
- **Bảng phân quyền secret** trong `docs/deployment.md`: mỗi secret, service nào nhận. Nguyên tắc: chỉ service cần mới
  nhận. Ví dụ (kiểm lại với config-repo lúc làm):
  | Secret | Nhận |
  | --- | --- |
  | `EXTERNAL_JWT_SECRET` | api-gateway, user-service |
  | `GATEWAY_INTERNAL_JWT_SECRET` | api-gateway và mọi service nghiệp vụ |
  | `<SERVICE>_DB_PASSWORD` | đúng service đó và PostgreSQL init |
  | `RABBITMQ_*` | service dùng RabbitMQ và broker |
  | `REDIS_PASSWORD` | gateway, user, game và Redis |
  | `SUPABASE_SECRET_KEY` | chỉ service bật `app.storage.enabled` |
  | `LEARNING_LLM_API_KEY`, `ASSESSMENT_LLM_API_KEY`, `CONTENT_LLM_API_KEY`, Azure Speech… | đúng service đó |
- GitHub environment `production` có required reviewer (người dùng duyệt trước mỗi deploy). Secret lưu ở đó, đặt tên
  theo `<SERVICE>__<VAR>` hoặc tương đương để workflow biết ghi vào file nào.
- Bước deploy (phase 4) render `/opt/ieltspath/env/<service>.env` qua SSH: `umask 077`, chủ `root`, mode 600; không echo
  giá trị; ghi đè nguyên file (không nối).
- **Sinh secret mới toàn bộ** cho production: JWT Base64 ≥ 32 byte, mật khẩu DB, RabbitMQ, Redis. Giá trị local và giá trị
  dự phòng cũ trong lịch sử git coi như đã lộ.
- Quy trình xoay khóa trong `docs/deployment.md`: JWT internal (đổi đồng thời gateway và mọi service, khởi động lại), JWT
  external (người dùng phải đăng nhập lại), Supabase (theo `AGENTS.md` §3.4), khóa LLM, mật khẩu DB.
- Khóa SSH deploy: cặp khóa riêng cho GitHub Actions, user `deploy` trên VPS chỉ được chạy `docker compose` trong
  `/opt/ieltspath` (không root shell nếu làm được bằng sudoers giới hạn).

## Success Criteria
- [ ] Mỗi service trên server chỉ có biến trong bảng phân quyền (kiểm bằng `docker exec ... env | cut -d= -f1`).
- [ ] Không giá trị secret nào xuất hiện trong log workflow.
- [ ] Quy trình xoay JWT internal đã thử một lần trên máy thử.
