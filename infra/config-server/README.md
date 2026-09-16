# Config Server

`config-server` là service cấu hình tập trung của hệ thống. Các service như gateway, eureka và user-service sẽ lấy cấu hình từ đây khi khởi động.

## Vai trò

- Cung cấp cấu hình theo tên ứng dụng và profile.
- Giữ cấu hình runtime trong `infra/config-server/config-repo`.
- Giúp các service không phải hardcode issuer, secret, database URL, Eureka URL.

## Cấu trúc config-repo

```text
infra/config-server/config-repo
├── application.yaml
├── api-gateway.yaml
├── eureka-server.yaml
└── user-service.yaml
```

Ví dụ khi `api-gateway` khởi động, nó gọi Config Server để lấy:

```text
http://localhost:8888/api-gateway/default
```

## Cấu hình quan trọng

Config Server chạy profile `native`, nghĩa là đọc file cấu hình từ thư mục local thay vì Git remote.

```yaml
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations:
            - file:./infra/config-server/config-repo
            - file:./config-repo
            - file:../config-repo
```

## Thứ tự khởi động

Config Server nên chạy đầu tiên:

```text
1. config-server
2. eureka-server
3. api-gateway
4. user-service
```

## Chạy local

```powershell
cd infra/config-server
.\mvnw.cmd spring-boot:run
```

Hoặc chạy từ root:

```powershell
mvn -pl infra/config-server spring-boot:run
```

## Endpoint hữu ích

```text
Config Server: http://localhost:8888
Health:        http://localhost:8888/actuator/health
Gateway config: http://localhost:8888/api-gateway/default
User config:    http://localhost:8888/user-service/default
```

## Lưu ý bảo mật

Không commit secret thật vào Git. JWT secret nên truyền qua environment:

```text
EXTERNAL_JWT_SECRET
GATEWAY_INTERNAL_JWT_SECRET
```
