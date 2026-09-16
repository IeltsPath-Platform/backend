# User Service

`user-service` quản lý người dùng, đăng nhập, refresh token và phát hành external JWT cho client.

## Vai trò

- Đăng ký tài khoản người dùng.
- Đăng nhập bằng email/username và password.
- Phát hành access token và refresh token.
- Lưu refresh token dạng hash trong database.
- Verify internal JWT do API Gateway ký khi nhận request protected.
- Cung cấp API quản lý user/role cho `ADMIN`.

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
  "roles": ["LEARNER"]
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
LEARNER
ADMIN
```

Registration public mặc định tạo user với role `LEARNER`. Các API quản trị yêu cầu `ADMIN`.

## Các package chính

```text
com.group01.user
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
```

Các bảng chính:

```text
users
roles
user_roles
refresh_tokens
```

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
PATCH /api/users/{id}/status
DELETE /api/users/{id}
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
