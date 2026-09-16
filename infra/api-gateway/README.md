# API Gateway

`api-gateway` là cửa vào chính của hệ thống. Client không gọi trực tiếp các service nghiệp vụ mà đi qua gateway để được xác thực, định tuyến và gắn token nội bộ.

## Vai trò

- Nhận request từ frontend/client tại port `8080`.
- Verify external JWT do `user-service` phát hành sau khi login.
- Tạo internal JWT ngắn hạn do gateway ký để gửi xuống downstream service.
- Định tuyến request tới service qua Eureka.
- Gắn và truyền `X-Correlation-Id` để trace log giữa các service.

## Luồng bảo mật

```text
Client
  -> Authorization: Bearer <external-jwt>
  -> API Gateway verify external JWT bằng EXTERNAL_JWT_SECRET
  -> Gateway tạo internal JWT bằng GATEWAY_INTERNAL_JWT_SECRET
  -> Forward xuống service với Authorization: Bearer <internal-jwt>
  -> Service verify internal JWT
```

Gateway không truyền identity bằng `X-User-*`. Downstream chỉ tin JWT nội bộ do gateway ký.

## JWT hiện tại

External JWT từ `user-service`:

```json
{
  "iss": "urn:code-base:auth",
  "sub": "user-id",
  "exp": 1760003600,
  "roles": ["LEARNER"]
}
```

Internal JWT do gateway ký:

```json
{
  "iss": "urn:code-base:api-gateway",
  "sub": "user-id",
  "exp": 1760003600,
  "roles": ["LEARNER"]
}
```

## Các package chính

```text
com.group01.apigateway
├── filter
│   ├── CorrelationIdFilter.java
│   ├── InternalJwtGatewayFilter.java
│   └── LoggingFilter.java
├── security
│   ├── AuthProperties.java
│   ├── HmacKeyFactory.java
│   ├── PublicEndpointProperties.java
│   └── SecurityConfig.java
├── service
│   └── InternalJwtService.java
└── error
    ├── GatewayErrorAttributes.java
    └── GatewayErrorResponse.java
```

## Cấu hình quan trọng

Các biến chính được lấy qua Config Server hoặc Docker Compose:

```text
EXTERNAL_JWT_ISSUER=urn:code-base:auth
EXTERNAL_JWT_SECRET=<base64 32 bytes>
INTERNAL_JWT_ISSUER=urn:code-base:api-gateway
GATEWAY_INTERNAL_JWT_SECRET=<base64 32 bytes>
INTERNAL_TOKEN_MAX_AGE_SECONDS=60
FRONTEND_ORIGIN=http://localhost:5173
```

Secret phải là base64 của tối thiểu 32 bytes random.

## Chạy local

Gateway phụ thuộc vào Config Server và Eureka Server.

```powershell
cd infra/api-gateway
.\mvnw.cmd spring-boot:run
```

Hoặc chạy từ root:

```powershell
mvn -pl infra/api-gateway spring-boot:run
```

## Endpoint hữu ích

```text
Gateway: http://localhost:8080
Health:  http://localhost:8080/actuator/health
```
