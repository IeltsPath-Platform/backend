# Hạ tầng hệ thống

Thư mục `infra/` chứa các service nền tảng dùng chung cho toàn bộ hệ thống microservices. Các service này nên chạy trước service nghiệp vụ để config, discovery và routing hoạt động đúng.

## Thành phần

| Module | Port mặc định | Vai trò |
| --- | --- | --- |
| `config-server` | `8888` | Cung cấp cấu hình tập trung cho các service |
| `eureka-server` | `8761` | Service registry để các service đăng ký và tìm nhau |
| `api-gateway` | `8080` | Cửa vào chính cho client, xác thực JWT và định tuyến request |

## Thứ tự khởi động

```text
1. config-server
2. eureka-server
3. api-gateway
4. services/*
```

`config-server` cần chạy trước vì các service khác lấy cấu hình từ đây. `eureka-server` cần chạy trước gateway và service nghiệp vụ để các instance có nơi đăng ký.

## Luồng request chính

```text
Client
  -> API Gateway
  -> Gateway verify external JWT
  -> Gateway ký internal JWT ngắn hạn
  -> Eureka tìm downstream service
  -> Gateway forward request kèm internal JWT
  -> Service verify internal JWT rồi xử lý nghiệp vụ
```

Gateway không truyền identity bằng các header `X-User-*`. Service phía sau chỉ tin JWT nội bộ do gateway ký.

## Cấu hình

Cấu hình runtime nằm trong:

```text
infra/config-server/config-repo/
├── application.yaml
├── api-gateway.yaml
├── eureka-server.yaml
└── user-service.yaml
```

Các secret JWT phải truyền qua environment hoặc cấu hình local riêng, không commit secret thật vào Git:

```text
EXTERNAL_JWT_SECRET
GATEWAY_INTERNAL_JWT_SECRET
```

Hai secret này là base64 của tối thiểu 32 bytes random. `EXTERNAL_JWT_SECRET` dùng cho token phát cho client; `GATEWAY_INTERNAL_JWT_SECRET` dùng cho token nội bộ giữa gateway và downstream service. Downstream service dùng `common-security` để verify internal JWT này, không copy/paste `SecurityConfig` riêng.

Khi chạy Spring service từ IntelliJ hoặc Maven, mỗi module nạp `.env` ở thư mục gốc repository làm property source sau Config Server. Cấu hình hỗ trợ working directory là thư mục gốc hoặc thư mục module. Vì vậy Gateway, User Service và mọi downstream service dùng cùng một `GATEWAY_INTERNAL_JWT_SECRET`; không nhập secret riêng vào từng Run Configuration. Docker Compose cũng nội suy từ `.env` gốc.

## Chạy local

Chạy từng module từ thư mục gốc:

```powershell
mvn -pl infra/config-server spring-boot:run
mvn -pl infra/eureka-server spring-boot:run
mvn -pl infra/api-gateway spring-boot:run
```

Chạy PostgreSQL, RabbitMQ, infrastructure và các business service đã triển khai bằng một Compose file tại root:

```powershell
docker compose up -d --build
```

Compose dùng `Dockerfile.spring-service` cho tất cả Spring Boot service và mount `config-repo` chỉ đọc vào Config Server.
`infra/config-server/Dockerfile` vẫn dùng được khi build độc lập. PostgreSQL có một database riêng cho từng service;
init script tạo database, sau đó Flyway
chạy migration và seed của service. User/Access demo data chỉ được ghi khi `DEMO_DATA_ENABLED=true`.

## Tài liệu chi tiết

- [API Gateway](api-gateway/README.md)
- [Config Server](config-server/README.md)
- [Eureka Server](eureka-server/README.md)
