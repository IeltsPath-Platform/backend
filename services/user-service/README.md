# User Service

`user-service` quản lý người dùng, đăng nhập, refresh token, phát hành external JWT và lưu activity/streak của học viên.

## Vai trò

- Đăng ký tài khoản người dùng.
- Đăng nhập bằng email/username và password.
- Phát hành access token và refresh token.
- Lưu refresh token dạng hash trong database.
- Verify internal JWT do API Gateway ký khi nhận request protected.
- Cung cấp API quản lý user/role cho `ADMIN`.
- Ghi `learning_activities`, đọc streak theo cùng public path `/api/learning-support/{activities,streak}`; Gateway route hai nhóm này tới user-service.

## Luồng đăng nhập

```text
Client gọi /auth/login
  -> user-service kiểm tra username/password
  -> tạo external JWT bằng EXTERNAL_JWT_SECRET
  -> tạo refresh token random, lưu hash vào database
  -> trả accessToken + refreshToken cho client
```

External JWT có payload tối giản:

```json
{
  "iss": "urn:code-base:auth",
  "sub": "user-id",
  "exp": 1760003600,
  "roles": ["CUSTOMER"]
}
```

## Luồng request protected

Client không gọi trực tiếp user-service trong mô hình chuẩn. Client gọi qua gateway:

```text
Client -> Gateway với external JWT
Gateway verify external JWT
Gateway ký internal JWT
Gateway -> user-service với Authorization: Bearer <internal-jwt>
user-service verify internal JWT bằng GATEWAY_INTERNAL_JWT_SECRET
```

User-service không đọc `X-User-*` header làm identity. Identity lấy từ JWT đã verify:

```java
@AuthenticationPrincipal Jwt jwt
UUID userId = UUID.fromString(jwt.getSubject());
```

## Role hiện tại

```text
CUSTOMER
CONTENT_AUTHOR
EXAMINER
SALES_STAFF
ADMIN
```

Registration public mặc định tạo user với role `CUSTOMER`. Các API quản trị yêu cầu `ADMIN`.

`ADMIN` không thể kết hợp với `CONTENT_AUTHOR` hoặc `EXAMINER`, nhưng có thể kết hợp với `CUSTOMER` và `SALES_STAFF`.
Quy tắc áp dụng khi tạo user hoặc thay vai trò; tài khoản cũ vẫn đăng nhập được và không bị tự động sửa.
Administrator không thể tự gỡ `ADMIN`, tự khóa/tắt hoặc tự xóa tài khoản. Tài khoản `ADMIN` đang `ACTIVE` cuối cùng
không thể mất vai trò này, chuyển sang `LOCKED`/`INACTIVE`, hoặc bị xóa. `GET /api/users` trả toàn bộ danh sách, chưa
phân trang; giao diện có thể lọc danh sách phía client.

Để kiểm tra các tài khoản cũ có tổ hợp vai trò xung đột:

```sql
SELECT u.email, string_agg(r.name, ',')
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
GROUP BY u.email
HAVING bool_or(r.name = 'ADMIN')
   AND bool_or(r.name IN ('CONTENT_AUTHOR', 'EXAMINER'));
```

## Các package chính

```text
com.ieltspath.user
├── api
│   ├── controller
│   ├── dto
│   ├── exception
│   └── filter
├── application
│   ├── command
│   ├── result
│   └── usecase
├── config
├── domain
│   ├── aggregate
│   ├── exception
│   ├── repository
│   └── vo
└── infrastructure
    ├── adapter
    ├── persistence
    └── specification
```

## Database

Migration chính:

```text
src/main/resources/db/migration/V1__create_user_tables.sql
src/main/resources/db/migration/V5__create_learning_activity_and_streak_tables.sql
```

Các bảng chính:

```text
users
roles
user_roles
refresh_tokens
learning_activities
streaks
```

V5 tạo hai bảng activity/streak trong `user_db`; `user_id` là ID logic, không có FK tới `users`. `GlobalExceptionHandler`
phân nhánh theo path `/api/learning-support/` để giữ định dạng lỗi của các endpoint cũ.

## Cấu hình quan trọng

```text
EXTERNAL_JWT_ISSUER=urn:code-base:auth
EXTERNAL_JWT_SECRET=<base64 32 bytes>
INTERNAL_JWT_ISSUER=urn:code-base:api-gateway
GATEWAY_INTERNAL_JWT_SECRET=<base64 32 bytes>
ACCESS_TOKEN_MAX_AGE_SECONDS=3600
REFRESH_TOKEN_MAX_AGE_SECONDS=604800
```

`EXTERNAL_JWT_SECRET` dùng để ký access token trả cho client. `GATEWAY_INTERNAL_JWT_SECRET` dùng để verify internal JWT từ gateway.

## Endpoint chính

```text
POST /auth/login
POST /auth/refresh
POST /auth/logout
GET  /auth/me
POST /api/users/register
GET  /api/users/me/profile
PUT  /api/users/me/profile
POST /api/users/me/avatar
DELETE /api/users/me/avatar
POST /api/users/me/password
GET  /api/users
GET  /api/users/{id}
PUT  /api/users/{id}
PUT  /api/users/{id}/roles
PUT  /api/users/{id}/status
DELETE /api/users/{id}
POST /api/learning-support/activities
GET  /api/learning-support/activities
GET  /api/learning-support/streak
GET  /internal/users/{userId}/learning-goal
POST /internal/users/display-names
```

The two `/internal/users/**` routes require a verified internal service JWT and are not exposed to clients through the
Gateway. The learning-goal response contains `targetBand` and `examDate`; both are `null` when the user has no active
goal, while an unknown user returns 404. The display-name request accepts 1–50 IDs in one batch. It uses a learner
profile's `displayName` when available, falls back to the account `fullName`, omits unknown IDs, and never returns email.
These reads use the existing `users`, `learning_goals`, and `learner_profiles` tables; no migration is needed.

## Chạy local

User-service phụ thuộc vào Config Server, Eureka Server và PostgreSQL.

```powershell
cd services/user-service
.\mvnw.cmd spring-boot:run
```

Hoặc chạy từ root:

```powershell
mvn -pl services/user-service spring-boot:run
```

## Kiểm tra nhanh

```text
Health: /actuator/health
```

Khi chạy qua docker-compose, user-service không nên expose trực tiếp cho frontend. Frontend đi qua API Gateway.

## Community public profiles

- `GET /api/users/{userId}/public-profile` requires authentication and returns
  `{id, displayName, avatarUrl, bio, targetBand, streak}`.
- `POST /internal/users/public-profiles` accepts `{userIds: UUID[]}` (at most 100 entries; empty is allowed)
  and returns `{profiles: [...]}`. It uses the forwarded internal JWT's viewer identity and is blocked at Gateway.
  IDs are deduplicated in input order. Missing or hidden profiles are omitted.
- `GET /internal/users/public-profiles/search?q=...&limit=8` is an authenticated internal lookup for community mention suggestions. It returns at most 10 active public/community-visible profiles; private profiles are omitted except the viewer's own profile.
- `PUBLIC` and `COMMUNITY` profiles are visible to authenticated members. A `PRIVATE` profile is visible only
  to its owner; other viewers receive 404 from the single-profile endpoint. Neither endpoint grants an admin bypass.
- Existing users without a learner profile follow the current default profile policy: account full name (or
  `Learner`) and public visibility, without creating a profile on read. No email, phone, roles, timezone,
  self-reported band, exam date, or schedule are returned.
- `targetBand` is the current active learning goal's target, otherwise null. `streak` is the stored current-day
  streak, otherwise zero. `avatarUrl` contains the existing avatar reference only if it is an absolute HTTP(S)
  URL with a host and no user credentials; opaque references return null. The bio is edited through
  `PUT /api/users/me/profile` and the avatar through `POST`/`DELETE /api/users/me/avatar`.
- The bounded batch uses a single query joining profile, active goal, and streak in `user_db`.

## Personal profile and account security

- `POST /api/users/me/avatar` accepts multipart field `file`; JPEG, PNG, or WebP up to 2 MB. The authenticated user ID
  comes from `CurrentUserProvider`. Objects are stored in the public-read `avatars` bucket under
  `images/<userId>/<uuid>.<ext>`; writes and deletes use the server-side Supabase key. Uploads are limited to 10 per
  user per hour per service instance (in memory; idle users are forgotten periodically). A replaced avatar object is
  deleted after the new one is saved.
- `DELETE /api/users/me/avatar` clears the profile reference and best-effort deletes an owned avatar object.
- `POST /api/users/me/password` accepts `{currentPassword, newPassword}` and returns 204. New passwords must contain
  at least 6 characters (the same rule as password reset). Five incorrect current-password attempts in a 15-minute
  window lock further attempts for that user for 15 minutes. The counter is stored in `password_change_attempts`
  (migration V12), so the limit holds across service instances and restarts; it is cleared by a successful change.
  Changing or resetting the password revokes all active refresh tokens, including the current session because the access-token
  contract does not identify its refresh-token session.
- `PUT /api/users/me/profile` edits the display name, bio, self-reported band, timezone and visibility (`PUBLIC` or
  `PRIVATE`). `avatarReference` in the body is ignored: the stored avatar is never changed or cleared by this call, so a
  form that only edits text cannot lose the photo or orphan the stored object. Only upload and delete change it.
  `COMMUNITY` exists in the database and enum but is not accepted until it has behaviour of its own distinct from
  `PUBLIC`.
- Storage is enabled with an empty `SUPABASE_URL` by default: the service still starts, and avatar upload answers
  503 until the variables are set.
- Runtime configuration: `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, and optional `USER_AVATAR_BUCKET` (defaults to
  `avatars`). Keep the secret key only in the ignored root `.env`. The bucket must allow public reads for returned
  avatar URLs; uploads/deletes remain server-side.

## Recorded study time

Authenticated personal APIs use `CurrentUserProvider`; existing activity CRUD does not contribute to these totals.

- `POST /api/users/me/study-sessions`: `{clientSessionId, sourceType, sourceId, takeover?}`. UUID identifiers;
  source types: `LESSON`, `PRACTICE`, `REVIEW`, `ASSESSMENT`.
- `POST /api/users/me/study-sessions/{id}/heartbeats` and `/close`:
  `{epoch, sequence, intervals: [{startOffsetMs, endOffsetMs}]}`. At most 16 ordered, non-overlapping
  intervals per request; offsets use the server session anchor. Close includes the final flush.
- `GET /api/users/me/statistics/study-time?from=YYYY-MM-DD&to=YYYY-MM-DD&grain=DAY&courseId=UUID`:
  inclusive range, at most 366 days; grain is `DAY` (default), `WEEK`, or `MONTH`; course is optional.

Start returns `sessionId`, `epoch`, `serverStartedAt`, `serverNow`, `leaseExpiresAt`, `nextSequence`,
`heartbeatSeconds=30`, `idleSeconds=120`, `leaseSeconds=90`, `status`, and `totalAcceptedMs`.
Heartbeat/close return `sessionId`, `epoch`, `sequence`, `acceptedMs`, `totalAcceptedMs`, `leaseExpiresAt`,
and `status`. Retry the same payload/sequence after a lost response. A conflicting payload returns
409 `PAYLOAD_CONFLICT`. A closed/expired session is never revived: use a new `clientSessionId`.
Only one session holds a user lease; explicit takeover closes the previous session without filling gaps.
The client excludes hidden/idle time; the server limits submitted intervals to elapsed server time and
the latest 90 seconds. This measures recorded activity, not verified human attention.

Statistics return `availability`, `generatedAt`, `dataFrom`, `timezonePolicy=RECORDED_LOCAL_DATE`,
`scope=ALL|courseId`, and `days`. Each bucket contains `date`, `timezone`, `actualSeconds`,
`plannedSeconds`, and `status` (`NOT_TRACKED`, `FUTURE`, `PARTIAL_DAY`, `COMPLETE`). Actual is null
before measurement or for future dates; a measured inactive date is zero. Course targets are null.
Week/month buckets sum raw milliseconds for dates inside the requested range before converting to whole
seconds, and use the calendar bucket start date.
Mixed timezone buckets say `MULTIPLE`; historical intervals retain their recorded date and timezone.

Migration V6 adds sessions, durable replay receipts, an interval ledger, daily totals, goal snapshots,
and the activation timestamp. Goal changes apply the next local day, never overwrite a recorded day,
and supersede pending future choices. Current active goals are snapshotted at migration; no historical
active time or targets are inferred. Changing the profile timezone reschedules an active goal using the
saved timezone under the same per-user lock as goal updates; unchanged timezones and users without an
active goal do not create another snapshot. Recorded days remain unchanged. Raw data is not automatically purged.

Source ownership and course context are verified once before starting, outside the database transaction,
through each owner’s `/statistics/study-context` endpoint. Bearer and correlation ID are forwarded;
connection/read timeouts are 2/3 seconds. Owner failure returns 503 `SOURCE_UNAVAILABLE` and must not
block lesson submission. Config Server defaults source URLs to Compose hostnames; for services on the
host, set `LEARNING_SERVICE_URL=http://localhost:8086` and `ASSESSMENT_SERVICE_URL=http://localhost:8083`.

Run `mvn -q -pl services/user-service -am test` from the repository root. Study-time PostgreSQL tests
require Docker and explicitly skip when it is unavailable.
