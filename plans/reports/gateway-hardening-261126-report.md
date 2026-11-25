# Báo cáo hardening Gateway — 2026-11-26

## Phạm vi đã thực hiện

Chỉ thực hiện P1 trên nhánh `fix/gateway-hardening`. Không SSH vào server và không thay đổi P2–P7.

| Hạng mục | Thay đổi | Commit |
| --- | --- | --- |
| Trusted proxies | Gateway nhận danh sách CIDR/IP từ biến môi trường, chỉ tin `X-Forwarded-For` khi peer trực tiếp là proxy tin cậy, chọn hop không tin cậy ngoài cùng bên phải; Compose production cấp IP cố định cho Caddy và tài liệu vận hành được cập nhật. | `e27004dc` |
| Nén Caddy | Thêm `encode zstd gzip`; WebSocket upgrade và request upload không bị nén/thay đổi request body. | `d6fc169c` |
| DB pool | Hikari tối đa 5 cho bảy service, 8 cho Assessment/Learning; tối thiểu 1. PostgreSQL production đặt `max_connections=80`, `shared_buffers=256MB`; tổng pool tối đa là 56. | `b3030050` |
| Rate limit auth | Tách `/auth/refresh` sang tier `auth-refresh` (1 rps, burst 20). Gateway loại header client-IP do client gửi và truyền `X-Gateway-Client-IP` đã tính từ trusted proxy chain; user-service dùng header này để giữ khóa email+IP thật, đồng thời thêm giới hạn rộng 20 lần thất bại/phút theo IP, với test cho 30 đăng nhập hợp lệ cùng IP. | `3a2c490c` |
| CORS | Thêm `Date` vào các response header browser được phép đọc. | `e254fa04` |

## Kiểm chứng

- `docker compose config --quiet`: đạt.
- `py -3 scripts/production/compose.py config --quiet` với env-file trống tạm và image sentinel: đạt.
- Unit test mới bao phủ IP trực tiếp, proxy CIDR, XFF giả/từ nhiều hop và XFF malformed; test Redis bao phủ nhiều tài khoản hợp lệ một IP và thất bại phân tán theo IP.
- `mvn -q -pl infra/api-gateway test`: chưa chạy được vì lỗi biên dịch có sẵn ngoài P1: `InternalJwtClaims.SESSION_ID` không tồn tại trong `common-security` nhưng bị `InternalJwtService` dùng.
- `mvn -q -pl services/user-service -am test`: chưa hoàn tất trong môi trường này; lần chạy module trực tiếp xác nhận lỗi biên dịch ngoài P1: thiếu `com.ieltspath.events.user.GoogleAccountUnlinkedV1`, được dùng bởi `UnlinkOAuthIdentityUseCase` và `RabbitUserEventPublisher`.
- Không thêm metric mới: `management.endpoints.web.exposure.include` đã có `prometheus`, nên Hikari tự xuất `hikaricp_connections_*` qua Actuator.
- Review bổ sung đã sửa nguy cơ production đếm IP Caddy thay vì IP người dùng: Gateway vouch client IP bằng header nội bộ sau khi xóa header giả từ client; ngưỡng IP dùng `>= 20` để request tiếp theo bị chặn đúng.

## Việc cần làm trên server

- Khi deploy, giữ `GATEWAY_BEHIND_PROXY=true` và `GATEWAY_RATE_LIMIT_TRUSTED_PROXIES=172.28.0.2/32` cùng mạng Compose `172.28.0.0/24`; hướng dẫn chi tiết nằm trong `docs/deployment.md`.
- Không đặt CIDR mạng công cộng vào danh sách trusted proxy.

Status: DONE_WITH_CONCERNS
Summary: Đã hoàn thành năm hạng mục P1 bằng năm commit độc lập và xác thực Compose; kiểm thử Maven bị chặn bởi hai lỗi biên dịch có sẵn ngoài phạm vi P1.
Concerns/Blockers: Cần khôi phục các contract thiếu `InternalJwtClaims.SESSION_ID` và `GoogleAccountUnlinkedV1` trước khi CI có thể chạy lại test Gateway/User đầy đủ.
