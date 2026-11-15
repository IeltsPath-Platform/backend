---
phase: 9
title: "CAPTCHA có điều kiện (A7), tùy chọn, cần duyệt"
status: pending
priority: P3
dependencies: [8]
---

# Phase 9: CAPTCHA sau nhiều lần đăng nhập sai (tùy chọn)

**Chỉ làm khi được duyệt** vì thêm một dịch vụ ngoài mới (Cloudflare Turnstile hoặc tương đương). Mặc định tắt; hệ thống
không phụ thuộc vào nó.

## Vấn đề
Giới hạn hiện có theo IP (Gateway) và theo cặp email+IP (`RedisLoginAttemptLimiter`: 5 lần sai trong 15 phút). Tấn công từ
nhiều IP vào nhiều email vẫn lọt dưới từng ngưỡng.

## Thiết kế
- Cổng `application/port/CaptchaVerifier` (`verify(token, clientIp)`), adapter `TurnstileCaptchaVerifier` ở
  `infrastructure`, gọi `siteverify` qua `ResilientCall` (`target=captcha`, `CallKind.IDEMPOTENT_WRITE`), chạy ngoài
  transaction và ngoài khóa. Cấu hình `app.auth.captcha.provider` (`none` | `turnstile`, mặc định `none`); khóa bí mật chỉ
  ở `.env` (`CAPTCHA_SECRET_KEY`, không log, không commit).
- Kích hoạt: sau `app.auth.captcha.after-failures` (mặc định 3) lần sai của cặp email+IP, `POST /auth/login` trả
  `428 CAPTCHA_REQUIRED`. Client gửi lại với trường tùy chọn mới `captchaToken` (chỉ thêm vào `LoginRequest`).
  `RedisLoginAttemptLimiter` bổ sung `failureCount(email, ip)`.
- Đăng ký và quên mật khẩu: danh sách `app.auth.captcha.required-endpoints` (mặc định rỗng) để bật từng endpoint khi
  frontend sẵn sàng.
- CAPTCHA không truy cập được: **mở** (cho qua) kèm metric `auth.captcha{result=unavailable}` và log WARN, để người dùng
  thật không bị khóa khi dịch vụ ngoài lỗi.
- Production: thêm host của nhà cung cấp vào danh sách egress cho phép của môi trường; ghi vào bảng "Đồng bộ (HTTP)" của
  `docs/system-architecture.md` (đích ngoài, không phải `<TÊN>_SERVICE_URL`).

## Files
`application/port/CaptchaVerifier.java`, `infrastructure/client/TurnstileCaptchaVerifier.java`, `RedisLoginAttemptLimiter`,
`AuditedLoginUseCase`, `api/dto/request/LoginRequest.java`, `GlobalExceptionHandler` (mã `CAPTCHA_REQUIRED`),
`config-repo/user-service.yaml`, `.env.example`, `docs/contracts/` (cập nhật mô tả đăng nhập), `docs/system-architecture.md`.

## Tests
- `MockRestServiceServer` hoặc `CaptchaVerifier` giả; không gọi dịch vụ thật (AGENTS §5).
- Dưới ngưỡng không đòi CAPTCHA; đạt ngưỡng đòi; token đúng cho qua; token sai từ chối; dịch vụ lỗi cho qua và tăng metric;
  `provider=none` không đổi hành vi.

## Success Criteria
- [ ] Với `provider=none` hành vi và hiệu năng không đổi.
- [ ] Với `turnstile`, sau 3 lần sai đăng nhập đòi CAPTCHA; dịch vụ ngoài lỗi không khóa người dùng thật.
- [ ] Không có khóa bí mật trong repo, log hay tài liệu.
