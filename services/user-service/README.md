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
GET  /api/users
GET  /api/users/{id}
PUT  /api/users/{id}
PUT  /api/users/{id}/roles
PUT  /api/users/{id}/status
DELETE /api/users/{id}
POST /api/learning-support/activities
GET  /api/learning-support/activities
GET  /api/learning-support/streak
```

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

## Recorded study time

Authenticated personal APIs use `CurrentUserProvider`; existing activity CRUD does not contribute to these totals.

- `POST /api/users/me/study-sessions`: `{clientSessionId, sourceType, sourceId, takeover?}`. UUID identifiers;
  source types: `LESSON`, `PRACTICE`, `REVIEW`, `ASSESSMENT`, `VOCABULARY_REVIEW`.
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
Week/month buckets sum only dates inside the requested range and use the calendar bucket start date.
Mixed timezone buckets say `MULTIPLE`; historical intervals retain their recorded date and timezone.

Migration V6 adds sessions, durable replay receipts, an interval ledger, daily totals, goal snapshots,
and the activation timestamp. Goal changes apply the next local day, never overwrite a recorded day,
and supersede pending future choices. Current active goals are snapshotted at migration; no historical
active time or targets are inferred. Raw data is not automatically purged.

Source ownership and course context are verified once before starting, outside the database transaction,
through each owner’s `/statistics/study-context` endpoint. Bearer and correlation ID are forwarded;
connection/read timeouts are 2/3 seconds. Owner failure returns 503 `SOURCE_UNAVAILABLE` and must not
block lesson submission. Config Server defaults source URLs to Compose hostnames; for services on the
host, set `LEARNING_SERVICE_URL=http://localhost:8086`, `ASSESSMENT_SERVICE_URL=http://localhost:8083`,
and `LIBRARY_SERVICE_URL=http://localhost:8081`.

Run `mvn -q -pl services/user-service -am test` from the repository root. Study-time PostgreSQL tests
require Docker and explicitly skip when it is unavailable.
