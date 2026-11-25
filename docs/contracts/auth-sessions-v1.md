# Auth sessions API v1

All routes are authenticated. The caller identity comes from the verified access token; clients cannot provide a user ID.

## List sessions

`GET /api/users/me/sessions` returns at most 50 active, unexpired refresh-token families, newest first.

```json
[{"sessionId":"uuid","startedAt":"2026-11-25T10:00:00","lastUsedAt":"2026-11-25T10:10:00","ipAddress":"203.0.113.x","userAgent":"Browser","current":null}]
```

The last IPv4 octet is masked. `current` is null until access-token session claims are introduced.

## Revoke sessions

- `DELETE /api/users/me/sessions/{sessionId}` revokes one session family.
- `DELETE /api/users/me/sessions` revokes all active sessions. `?exceptCurrent=true` is accepted; it has no exception behavior until the current session claim is available.

A missing or another user's session returns HTTP 404 with top-level code `SESSION_NOT_FOUND` and message `Không tìm thấy phiên đăng nhập.`

## Refresh token rotation

Refresh tokens rotate within a family. Reuse of a rotated token after the configured 10-second grace period revokes the family. Reuse during grace, or a token revoked for another reason, returns the existing 401 response without family-wide revocation. The default absolute session age is unlimited (`0`).
