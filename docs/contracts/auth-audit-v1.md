# Authentication audit API v1

## Learner security history

`GET /api/users/me/security-events?page=0` returns up to 50 newest events for the authenticated user only.

## Administrator event history

`GET /api/users/{userId}/auth-events?page=0&size=20&eventType=SESSION_REVOKED` requires `ADMIN`. `page` starts at zero,
`size` is 1–100 (default 20), and `eventType` is optional and must be a supported event type. Results are paged in the
database and ordered newest first with a stable ID tie-breaker.

Both endpoints return `{items,page,size,total}`. Each item contains `id`, `eventType`, masked `ipAddress`, `userAgent`,
`correlationId`, allowlisted `detail`, and `createdAt`. The final IPv4 octet is hidden; IPv6 addresses have their final
segment hidden. Audit details never contain passwords, refresh/access tokens, Google ID tokens, nonce values, or full email
addresses.

Supported events: `PASSWORD_CHANGED`, `PASSWORD_RESET`, `PASSWORD_SET`, `GOOGLE_LINKED`, `GOOGLE_UNLINKED`,
`GOOGLE_PASSWORD_CLEARED`, `SESSION_REVOKED`, `SESSIONS_REVOKED_ALL`, `REFRESH_REUSE_DETECTED`, `REAUTH_SUCCEEDED`,
`REAUTH_FAILED`, `ACCOUNT_STATUS_CHANGED`, and `ROLE_CHANGED`.

Events are retained for 365 days by default and removed in bounded batches. The retention period and purge schedule are
configurable in the user service.
