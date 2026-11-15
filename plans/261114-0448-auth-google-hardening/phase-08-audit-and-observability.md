---
phase: 8
title: "Audit sự kiện nhạy cảm, metric và cảnh báo (A8)"
status: pending
priority: P2
dependencies: [2, 7]
---

# Phase 8: Audit và quan sát

## Vấn đề
`login_history` chỉ ghi đăng nhập thành công/thất bại. Các sự kiện nhạy cảm khác (đổi/đặt lại mật khẩu, liên kết hoặc gỡ
Google, thu hồi phiên, phát hiện dùng lại refresh token) không có dấu vết thống nhất; metric đăng nhập và refresh chưa có
nên không đặt được cảnh báo.

## Thiết kế

### Bảng audit: `V17__auth_audit_events.sql`
```sql
CREATE TABLE auth_audit_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID,
    event_type     VARCHAR(40) NOT NULL,
    ip_address     VARCHAR(45),
    user_agent     VARCHAR(255),
    correlation_id VARCHAR(64),
    detail         JSONB,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_auth_audit_user_created ON auth_audit_events (user_id, created_at DESC);
CREATE INDEX idx_auth_audit_type_created ON auth_audit_events (event_type, created_at DESC);
```
- Không FK tới `users` để giữ dấu vết khi xóa tài khoản (theo chính sách giữ dữ liệu); `user_id` có thể rỗng cho sự kiện
  chưa xác định người dùng.
- Entity map cột `jsonb` dùng `@JdbcTypeCode(SqlTypes.JSON)` (cạm bẫy đã ghi trong `CLAUDE.md`); có test Testcontainers.
- `detail` chỉ chứa mã và id (`sessionId`, `reason`, `method`), **không** chứa token, mật khẩu, ID token, nonce, email đầy đủ.

### Sự kiện (`event_type`)
`PASSWORD_CHANGED`, `PASSWORD_RESET`, `PASSWORD_SET`, `GOOGLE_LINKED`, `GOOGLE_UNLINKED`, `GOOGLE_PASSWORD_CLEARED`,
`SESSION_REVOKED`, `SESSIONS_REVOKED_ALL`, `REFRESH_REUSE_DETECTED`, `REAUTH_SUCCEEDED`, `REAUTH_FAILED`,
`ACCOUNT_STATUS_CHANGED`, `ROLE_CHANGED`.

### Ghi
Cổng `application/port/AuthAuditRecorder` (`record(AuthAuditEvent)`), adapter JPA ở `infrastructure`; gọi ngay trong các use
case tương ứng (cùng transaction với thay đổi, **lỗi ghi audit không làm hỏng thao tác chính**, theo mẫu
`LoginHistoryRecorder`). IP và user agent lấy từ `LoginContext` (phase 2). `X-Correlation-Id` lấy từ MDC (đã có
`common-observability`).

### Đọc
- `GET /api/users/me/security-events` (người dùng, 50 mục gần nhất): thời gian, loại, IP che octet cuối, thiết bị.
- `GET /api/users/{id}/auth-events` (ADMIN, phân trang, tối đa 100 mỗi trang, lọc theo `eventType`).
- Mọi truy vấn có `LIMIT`/phân trang, dùng chỉ mục ở trên; không `findAll()` (AGENTS §3.7).

### Giữ và dọn
`PurgeAuthAuditUseCase` theo mẫu `PurgeLoginHistoryUseCase`: xóa bản ghi quá `app.auth.audit.retention-days` (mặc định
365) theo lô `SKIP LOCKED`; ghi một dòng vào bảng "Scheduler và nhiều instance".

### Metric (Micrometer qua `common-observability`)

| Metric | Tag | Ghi ở |
| --- | --- | --- |
| `auth.login` | `result` ∈ `success, bad_credentials, locked, rate_limited, inactive` | `AuditedLoginUseCase` |
| `auth.refresh` | `result` ∈ `success, invalid, reuse, expired_session` | `RefreshTokenUseCase` |
| `auth.session.revoked` | `reason` (theo `revoked_reason`) | các use case thu hồi |
| `auth.password` | `event` ∈ `change, reset, set, rehash` | use case mật khẩu |
| `auth.reauth` | `result`, `method` | `ReauthUseCase` |
| `auth.google.signin` | `result` (phase 1, thêm `stale_token`, `replay`) | `GoogleSignInUseCase` |
| `auth.revocation.write.errors`, `gateway.auth.revocation.errors` | - | phase 3 |

Không dùng id người dùng, email hay IP làm tag (cardinality cao).

### Cảnh báo (stack quan sát tùy chọn, `docker-compose.observability.yml`)
Thêm tệp luật cho Prometheus trong `infra/prometheus` (kiểm cấu trúc hiện có và khai báo `rule_files`):
- `AuthLoginFailureSpike`: tỉ lệ `auth.login` khác `success` vượt ngưỡng trong 5 phút.
- `RefreshTokenReuseDetected`: `increase(auth_refresh_total{result="reuse"}[5m]) > 0`.
- `GoogleSignInErrors`: `invalid_token` hoặc `unavailable` tăng đột biến.
- `RevocationStoreErrors`: lỗi ghi hoặc đọc kho thu hồi khác 0 kéo dài.
Ngưỡng cụ thể chốt sau khi có số liệu thực; đây là stack tùy chọn nên không chặn triển khai.

## Files
Mới: `V17__auth_audit_events.sql`, `domain/aggregate/AuthAuditEvent.java`, `application/port/AuthAuditRecorder.java`,
`infrastructure/persistence/{AuthAuditEventJpaEntity,AuthAuditEventJpaRepository}.java`, adapter, `api/controller/
SecurityEventsController.java`, DTO, `PurgeAuthAuditUseCase`, `infra/prometheus/rules/auth.yml`,
`docs/contracts/auth-audit-v1.md`. Sửa: các use case nêu trên, `docs/system-architecture.md`.

## Tests
- Testcontainers: ghi và đọc `jsonb`, chỉ mục, phân trang, purge theo lô.
- Use case: mỗi sự kiện được ghi đúng loại; lỗi audit không làm thao tác chính thất bại; `detail` không chứa dữ liệu nhạy cảm
  (test kiểm các khóa cho phép).
- `@WebMvcTest`: `me/security-events` chỉ trả của chính user; `auth-events` chỉ ADMIN, 403 cho role khác.
- Metric: counter tăng đúng tag cho từng nhánh.

## Success Criteria
- [ ] Mọi sự kiện nhạy cảm trong danh sách để lại một dòng audit với correlation id; không có dữ liệu bí mật trong `detail`.
- [ ] Người dùng xem được lịch sử bảo mật của mình; ADMIN xem được của người khác.
- [ ] Prometheus hiển thị các metric `auth_*`; luật cảnh báo nạp được.
- [ ] `ArchitectureTest` xanh; contract `auth-audit-v1.md` trong cùng PR.
