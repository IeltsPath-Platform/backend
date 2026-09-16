# Eureka Server

`eureka-server` là service registry của hệ thống. Các service đăng ký vào Eureka để gateway có thể tìm và định tuyến request mà không cần hardcode host/port.

## Vai trò

- Nhận đăng ký từ `api-gateway`, `user-service` và các service khác.
- Cung cấp danh sách instance đang sống cho gateway.
- Hỗ trợ load balancing phía client qua Spring Cloud LoadBalancer.
- Có dashboard để xem service nào đang đăng ký.

## Luồng hoạt động

```text
Service khởi động
  -> lấy config từ Config Server
  -> đăng ký instance vào Eureka
  -> Gateway hỏi Eureka vị trí service
  -> Gateway forward request tới service phù hợp
```

## Cấu hình mặc định

```text
Port: 8761
Dashboard: http://localhost:8761
Config Server: http://localhost:8888
```

Ở môi trường local, Eureka thường chạy standalone:

```yaml
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

## Chạy local

Config Server phải chạy trước.

```powershell
cd infra/eureka-server
.\mvnw.cmd spring-boot:run
```

Hoặc chạy từ root:

```powershell
mvn -pl infra/eureka-server spring-boot:run
```

## Endpoint hữu ích

```text
Dashboard: http://localhost:8761
Registry:  http://localhost:8761/eureka/apps
Health:    http://localhost:8761/actuator/health
```

## Lưu ý

Khi chạy local ít service, Eureka có thể log cảnh báo về self-preservation hoặc renewal threshold. Đây là hành vi bình thường trong môi trường dev.
