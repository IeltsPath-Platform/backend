---
phase: 5
title: "Frontend on CDN, cookies and CORS"
status: done-pending-vps
priority: P1
dependencies: [4]
---

# Phase 5: Frontend trên Vercel/Cloudflare Pages, cookie và CORS

## Requirements
- **Tên miền** (chờ chốt ở `plan.md`): frontend `app.<domain>`, API `api.<domain>`. Không dùng `*.vercel.app` /
  `*.pages.dev` cho production vì cookie refresh sẽ thành cookie bên thứ ba.
- **Frontend:** project Vercel hoặc Cloudflare Pages trỏ repo `frontend`, build `npm ci && npm run build`, output `dist`;
  biến build `VITE_API_BASE_URL=https://api.<domain>` (kiểm tên biến thật trong `src/lib/env.ts`), `VITE_USE_MOCK_LEARNING=false`.
  Rewrite mọi đường dẫn về `index.html` cho router phía client. Biến `VITE_*` vào bundle nên không chứa secret.
- **Cookie (user-service, env production):** `APP_AUTH_COOKIE_SECURE=true`, `APP_AUTH_COOKIE_SAME_SITE=LAX` (đủ vì hai
  subdomain cùng site), `APP_AUTH_COOKIE_DOMAIN` để trống (cookie gắn với `api.<domain>`). Kiểm refresh qua path `/auth`.
- **CORS (gateway):** thêm `https://app.<domain>` vào danh sách origin cho phép, cho phép credentials; không dùng `*`.
  Preview deploy của Vercel (tên miền tạm) không được thêm vào production.
- **Mobile:** đặt base URL API production trong cấu hình build của app (repo `mobile`); không cần CORS.
- `frontend/README.md` và `docs/deployment.md`: cách deploy frontend, biến build.

## Implementation Steps
1. Trỏ DNS `app` và `api`; HTTPS hợp lệ cho cả hai.
2. Cấu hình project CDN, deploy bản đầu.
3. Thử trên Chrome và Safari: đăng nhập, để access token hết hạn, refresh tự chạy, đăng xuất xóa cookie.

## Success Criteria
- [ ] Đăng nhập và tự gia hạn phiên chạy trên Chrome và Safari ở tên miền production.
- [ ] Request từ origin lạ bị CORS chặn; từ `app.<domain>` thì qua.
- [ ] Bundle frontend không chứa secret (`grep` trong `dist`).
