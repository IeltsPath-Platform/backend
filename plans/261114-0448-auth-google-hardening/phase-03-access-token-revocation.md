---
phase: 3
title: "Thu hồi access token, đăng xuất thật sự (A2)"
status: pending
priority: P1
dependencies: [2]
---

# Phase 3: Thu hồi access token tại Gateway

## Vấn đề
Access token là stateless, sống 3600 giây và không thu hồi được. Sau khi đăng xuất, đổi mật khẩu, thu hồi phiên hay khóa
tài khoản, token cũ vẫn dùng được đến một giờ. Token hiện không có `jti`, `iat`, nên cũng không có gì để thu hồi theo.

## Thiết kế

### Claim mới (cần duyệt, chỉ thêm)
`JwtTokenService.createAccessToken(user, sessionId)` thêm: `jti` (UUID mới mỗi token), `iat` (giữ lại thay vì xóa như
hiện nay), `sid` (= `family_id` của phiên, phase 2). Không đổi claim cũ (`iss`, `sub`, `exp`, `roles`).
`AuthTokenIssuer` tạo `family_id` trước, dùng cho cả access token (`sid`) và refresh token.
Gateway chép thêm `sid` vào internal JWT (claim mới, chỉ thêm) để service biết phiên hiện tại; sửa
`shared/common-security` thêm `CurrentUserProvider.currentSessionId()` (Optional) — thay đổi additive.

### Kho thu hồi trong Redis (contract `docs/contracts/auth-token-revocation-v1.md`)

| Khóa | Giá trị | TTL | Dùng khi |
| --- | --- | --- | --- |
| `auth:revoked:sid:{sid}` | `1` | access TTL + 60 giây | đăng xuất, thu hồi phiên, phát hiện dùng lại refresh |
| `auth:revoked:jti:{jti}` | `1` | thời gian còn lại của token + 60 giây | thu hồi riêng một token (dự phòng) |
| `auth:revoked:user:{userId}` | epoch giây | access TTL + 60 giây | đổi/đặt lại mật khẩu, Google xóa mật khẩu, admin khóa/đổi role |

Token bị từ chối nếu `sid` hoặc `jti` có khóa, hoặc `iat` **nhỏ hơn** giá trị `auth:revoked:user:{userId}` (token cấp cùng
giây sau lúc thu hồi vẫn hợp lệ; chấp nhận cửa sổ tối đa 1 giây cho token cấp ngay trước lúc thu hồi, ghi trong contract).
Token cũ không có `jti/sid/iat` (cấp trước khi triển khai) **bỏ qua** kiểm tra, tự hết hạn trong tối đa một giờ.

### Ghi (user-service, cổng `application/port/AccessTokenRevocationStore`, adapter Redis ở `infrastructure`)
- `LogoutUseCase`: lấy `family_id` từ chính refresh token (không cần access token) → `revokeSession(sid)`.
- Thu hồi phiên (API phase 2), phát hiện dùng lại (phase 2): `revokeSession`.
- `ChangePasswordUseCase`, `ResetPasswordUseCase`, `LinkGoogleAccountUseCase` (khi xóa mật khẩu), đổi trạng thái hoặc role
  trong `UserManagementController`: `revokeUser(userId, now)`.
- Ghi sau commit (`TransactionSynchronization.afterCommit`); lỗi Redis chỉ log và tăng metric
  `auth.revocation.write.errors`, không làm hỏng thao tác chính. Phần bền vững là việc thu hồi refresh token trong DB.

### Kiểm tra (api-gateway)
- `TokenRevocationWebFilter` (WebFilter đặt sau bước xác thực JWT): đọc `Jwt` trong `ReactiveSecurityContextHolder`, một
  lần `multiGet` ba khóa bằng `ReactiveStringRedisTemplate` (đã có phụ thuộc Redis reactive), timeout 100 ms.
- Bị thu hồi: trả 401 theo định dạng lỗi của Gateway (`GatewayErrorAttributes`); không gọi downstream.
- Lỗi hoặc timeout Redis: **mở** (cho qua) kèm log WARN và metric `gateway.auth.revocation.errors`; cờ
  `gateway.auth.revocation.fail-closed` (mặc định `false`) đổi sang chặn (503).
- `WebSocketProtocolAuthFilter` (`/ws/notifications`) dùng chung lớp `RevocationChecker` để không bỏ sót kênh WebSocket.
- Không phải route công khai mới; không đổi tier rate limit.

### Thời hạn access token
Sau khi frontend xác nhận tự refresh khi gặp 401, đặt `ACCESS_TOKEN_MAX_AGE_SECONDS=900` (chỉ là biến môi trường, quay lui
không cần deploy). Phase này không đổi giá trị mặc định trong code.

### API phiên
Bật `exceptCurrent=true` và trường `current` của phase 2 bằng `currentSessionId()`.

## Files
- user-service: `JwtTokenService`, `AuthTokenIssuer`, `LogoutUseCase`, các use case đã nêu, `application/port/
  AccessTokenRevocationStore.java`, `infrastructure/adapter/RedisAccessTokenRevocationStore.java`, `SessionController`.
- api-gateway: `security/TokenRevocationWebFilter.java`, `security/RevocationChecker.java`, `SecurityConfig` (đăng ký
  filter), `filter/InternalJwtGatewayFilter.java` (chép `sid`), `filter/WebSocketProtocolAuthFilter.java`,
  `config-repo/api-gateway.yaml` (`gateway.auth.revocation.*`).
- common-security: `CurrentUserProvider`, `InternalJwtClaims` (thêm `SESSION_ID`).
- Tài liệu: `docs/contracts/auth-token-revocation-v1.md`, `docs/system-architecture.md` (luồng và khóa Redis).

## Tests
- `JwtTokenServiceTest`: có `jti` khác nhau mỗi lần, có `iat`, `sid` đúng; các claim cũ không đổi.
- Gateway (`TokenRevocationWebFilterTest`, mock Redis): sid bị thu hồi → 401; jti bị thu hồi → 401; `iat` trước mốc user →
  401, sau mốc → qua; token cũ không có claim → qua; Redis lỗi → qua và tăng metric; `fail-closed=true` → chặn.
- user-service: `LogoutUseCase` ghi `revokeSession` đúng `family_id`; đổi mật khẩu ghi `revokeUser`; Redis lỗi không làm
  đổi mật khẩu thất bại.
- Chạy lại trên stack local: đăng nhập → gọi API (200) → đăng xuất → dùng lại access token cũ (401 trong 1 giây);
  đổi mật khẩu ở một thiết bị làm token của thiết bị khác mất hiệu lực.

## Success Criteria
- [ ] Sau đăng xuất, thu hồi phiên, đổi/đặt lại mật khẩu, khóa tài khoản: access token cũ bị 401 ở Gateway ngay lập tức.
- [ ] Redis chết: hệ thống vẫn phục vụ (mở) và có metric/cảnh báo; có cờ chặn cho môi trường cần chặt.
- [ ] Token cấp trước triển khai vẫn dùng được đến khi hết hạn, không đăng xuất hàng loạt.
- [ ] `docs/contracts/auth-token-revocation-v1.md` và phê duyệt thêm claim có trước khi merge.
- [ ] `mvn -q -pl infra/api-gateway test`, `mvn -q -pl services/user-service -am test` xanh.
