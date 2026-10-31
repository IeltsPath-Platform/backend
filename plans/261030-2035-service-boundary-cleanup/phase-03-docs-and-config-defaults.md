---
phase: 3
title: "Tài liệu và cổng DB mặc định"
status: pending
priority: P2
effort: "0.5 ngày"
dependencies: []
---

# Phase 03: Tài liệu và cổng DB mặc định

## Overview

Hai chỗ lệch giữa tài liệu/cấu hình và thực tế: notification-service bị ghi là khung trong khi đã có chức năng; cổng DB mặc định trong config-repo không thống nhất (5432, 5435, 5436, 5437) trong khi Compose chạy PostgreSQL ở 5440.

## Requirements

- `AGENTS.md` (dòng 31) và `CLAUDE.md` (dòng 36, bảng module) mô tả đúng notification-service: nhận event RabbitMQ (4 listener chấm bài, consumer Community), inbox, tùy chọn thông báo, 4 migration (V1-V4), `notification_db`, cổng 8088. Tìm thêm mọi chỗ khác ghi "khung" hay "chưa có DB runtime" (`grep -n -i "khung\|chưa có DB" AGENTS.md CLAUDE.md README.md docs`), kể cả mục "Cạm bẫy đã biết" nếu có.
- Mặc định `*_DB_URL` trong mọi `config-repo/*-service.yaml` đồng nhất `jdbc:postgresql://localhost:5440/<tên>_db` (D8). Compose vẫn ghi đè bằng env nên không đổi hành vi trong container.
- Không đổi tên database, user hay mật khẩu mặc định.

## Tests first (red)

- Test cấu hình (cùng kiểu các test đọc YAML của Gateway): mọi `config-repo/*-service.yaml` có DB URL mặc định trỏ `localhost:5440`; đỏ trước khi sửa.
- Không có test cho tài liệu; kiểm bằng `grep` ở Success Criteria.

## Related Code Files

- Sửa: `AGENTS.md`, `CLAUDE.md` (cập nhật cả dòng "Cập nhật lần cuối"), `docs/system-architecture.md` nếu bảng service nhắc notification.
- Sửa (cấu hình tập trung, commit riêng): `infra/config-server/config-repo/{access,assessment,community,content,game,learning,library,notification,user}-service.yaml`.

## Implementation Steps

1. Viết test đỏ cho cổng DB.
2. Đổi mặc định; chạy test của Config Server/Gateway nếu có test đọc config-repo.
3. Sửa tài liệu; đọc lại để chắc dòng ghi chú ngày và nội dung khớp thực tế.

## Success Criteria

- [ ] `grep -rn "localhost:543[0-9]\|localhost:5432" infra/config-server/config-repo` chỉ trả về 5440 hoặc rỗng.
- [ ] `grep -n -i "chưa triển khai\|khung" AGENTS.md CLAUDE.md` không còn nhắc notification-service sai.
- [ ] Test và `docker compose config --quiet` xanh.

## Risk Assessment

- Ai đó đang chạy service trên host với PostgreSQL riêng ở 5432 sẽ phải đặt `*_DB_URL`; điều này đã nằm trong hướng dẫn `AGENTS.md` §3.6. Ghi vào mô tả PR.
- Rollback: revert commit.
