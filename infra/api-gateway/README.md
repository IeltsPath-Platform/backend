# API Gateway

API Gateway là điểm truy cập trung tâm cho toàn bộ hệ thống microservices. Service này được xây dựng dựa trên **Spring Cloud Gateway** và **Spring WebFlux** (reactive stack). Tất cả các request từ client (web, mobile, ứng dụng bên thứ ba) đều được định tuyến qua Gateway để đi đến các microservice backend phù hợp.

## 🚀 Tính năng chính

* **Định tuyến động (Dynamic Routing):** Tự động định tuyến các request đầu vào đến các microservice dựa trên đường dẫn (path) hoặc các thuộc tính khác của request.
* **Tích hợp Service Discovery:** Tích hợp với Eureka Server để tự động tìm kiếm các service backend. Không cần phải hardcode các URL.
* **Bảo mật tập trung:** Đóng vai trò là một OAuth2 Resource Server, xác thực các JWT token trước khi chuyển tiếp request đến các service nội bộ. Điều này giúp các service nội bộ không phải tự triển khai logic xác thực phức tạp.
* **Cấu hình tập trung:** Tự động lấy cấu hình (configuration) từ Config Server.
* **Giám sát & Health Checks:** Tích hợp Spring Boot Actuator để kiểm tra trạng thái (health checks) và thu thập metrics.

## ⚡ Tại sao API Gateway lại sử dụng Reactive Stack (WebFlux)?

Khác với các service backend thông thường (sử dụng Spring Web MVC với mô hình *Thread-per-request*), API Gateway là điểm chặn đầu tiên đón nhận toàn bộ lượng truy cập của hệ thống. Việc sử dụng Reactive Stack (dựa trên Project Reactor và Netty) mang lại các lợi thế vô cùng lớn:

* **Non-blocking I/O (I/O không chặn):** Gateway chủ yếu làm nhiệm vụ trung chuyển (nhận request từ client, forward cho backend, chờ phản hồi và trả lại cho client). Trong lúc chờ backend xử lý, luồng (thread) của Gateway không bị block (chặn) mà được giải phóng ngay lập tức để tiếp nhận các request mới.
* **Khả năng chịu tải cao (High Concurrency):** Nhờ cơ chế non-blocking, Gateway có thể xử lý hàng chục ngàn kết nối đồng thời (concurrent connections) chỉ với một số lượng Thread rất nhỏ (thường chỉ bằng số lượng core của CPU). Điều này giúp tiết kiệm tối đa bộ nhớ (RAM) và tránh lãng phí tài nguyên hệ thống.
* **Streaming & Backpressure:** Hỗ trợ tốt các luồng dữ liệu liên tục (streaming) và cơ chế *backpressure* (giúp hệ thống có khả năng báo hiệu cho client giảm tốc độ gửi request khi tải quá cao, tránh hiện tượng sập hệ thống).

## 🛠️ Công nghệ sử dụng

* **Java:** 21
* **Framework:** Spring Boot 3.x, Spring Cloud
* **Core Modules:**
  * `spring-cloud-starter-gateway-server-webflux`
  * `spring-cloud-starter-netflix-eureka-client`
  * `spring-boot-starter-oauth2-resource-server`
  * `spring-cloud-starter-config`

## 📂 Cấu trúc Source Code (Source Tree)

Dưới đây là cấu trúc thư mục chính của Gateway và ý nghĩa của từng package/class:

```text
src
├── main
│   ├── java
│   │   └── com.group01.apigateway
│   │       ├── ApiGatewayApplication.java       # Class khởi động chính (entry point) của Spring Boot.
│   │       ├── error
│   │       │   ├── GatewayErrorAttributes.java  # Tùy chỉnh các thuộc tính (attributes) lỗi trả về cho client ở dạng JSON.
│   │       │   └── GatewayErrorResponse.java    # Model định nghĩa cấu trúc dữ liệu chuẩn của một response khi có lỗi.
│   │       ├── filter
│   │       │   ├── CorrelationIdFilter.java     # Filter tạo/chuyển tiếp Correlation ID (tracking log xuyên suốt các microservice).
│   │       │   └── LoggingFilter.java           # Filter ghi log thông tin request/response tại Gateway.
│   │       ├── security
│   │       │   ├── AuthProperties.java          # @ConfigurationProperties map các thuộc tính bảo mật từ config-repo/api-gateway.yaml.
│   │       │   ├── HmacKeyFactory.java          # Lớp tiện ích sinh SecretKey (HMAC-SHA256) từ chuỗi base64; validate tối thiểu 32 bytes.
│   │       │   ├── PublicEndpointProperties.java# @ConfigurationProperties chứa danh sách endpoint không cần xác thực (public endpoints).
│   │       │   └── SecurityConfig.java          # Cấu hình Spring Security WebFlux: filter chain, JWT decoder, CORS, exception handler.
│   │       └── service
│   │           └── InternalJwtService.java      # Tạo internal JWT ngắn hạn để Gateway ký và forward xuống backend service.
│   └── resources
│       └── application.yml                      # Config bootstrap: khai báo tên service và URL của Config Server.
└── test
    └── java
        └── com.group01.apigateway
            ├── ApiGatewayApplicationTests.java          # Smoke test: kiểm tra Spring context load thành công.
            └── security
                └── InternalJwtServiceTest.java          # Unit test: kiểm tra logic tạo và ký internal JWT.
```

> [!NOTE]
> `SecurityConfig` sử dụng các constants tập trung từ `shared/common-security`:
> - `CanonicalRoles.ALL` — danh sách role hợp lệ (`ADMIN`, `LEARNER`)
> - `InternalJwtClaims.ROLES` — tên claim `"roles"` trong JWT
> - `InternalJwtAuthorities` — parse claim roles thành `GrantedAuthority`

## ⚙️ Các thông số cấu hình

Gateway phụ thuộc vào Config Server để lấy cấu hình. Các thuộc tính mặc định:

* **Port mặc định:** `8080` (có thể ghi đè qua `SERVER_PORT`)
* **Config Server URI:** `http://localhost:8888` (ghi đè qua `SPRING_CLOUD_CONFIG_URI`)
* **Eureka Server URI:** Thường được lấy từ Config Server hoặc mặc định là `http://localhost:8761/eureka/`

## 🏃 Hướng dẫn chạy ở Local

### Yêu cầu hệ thống

Trước khi chạy API Gateway, đảm bảo các service sau đã hoạt động:

1. **Config Server:** Đang chạy ở port `8888`.
2. **Eureka Server:** Đang chạy ở port `8761`.
3. **Identity Provider (Tùy chọn nhưng khuyến khích):** Nếu bạn đang xác thực JWT, Keycloak hoặc IdP bên ngoài của bạn phải có thể truy cập được.

### Khởi chạy ứng dụng

Di chuyển đến thư mục `infra/api-gateway` từ thư mục gốc của project:

```bash
cd infra/api-gateway
./mvnw spring-boot:run
```

Hoặc bạn có thể build file JAR và chạy:

```bash
./mvnw clean package -DskipTests
java -jar target/api-gateway-1.0-SNAPSHOT.jar
```

## 🔗 Các Endpoint hữu ích

* **Base URL:** `http://localhost:8080`
* **Actuator Health:** `http://localhost:8080/actuator/health`

## 🐳 Docker (Tùy chọn)

API Gateway được build qua `Dockerfile.spring-service` từ `docker-compose.yml`
với `MODULE_PATH=infra/api-gateway`.
