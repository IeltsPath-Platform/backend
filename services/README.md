# Service nghiệp vụ

Thư mục `services/` chứa các microservice xử lý nghiệp vụ của hệ thống. Mỗi service nên có database/config riêng, tự đăng ký lên Eureka và chỉ nhận request từ API Gateway trong luồng chuẩn.

## Service hiện có

| Module | Vai trò |
| --- | --- |
| `user-service` | Quản lý người dùng, đăng nhập, refresh token và phát hành external JWT cho client |

## Nguyên tắc bảo mật

- Client gọi service thông qua `api-gateway`, không gọi trực tiếp service nghiệp vụ.
- Client gửi `Authorization: Bearer <external-jwt>` lên gateway.
- Gateway verify external JWT, sau đó thay bằng `Authorization: Bearer <internal-jwt>` trước khi forward xuống service.
- Service nghiệp vụ verify internal JWT bằng `GATEWAY_INTERNAL_JWT_SECRET`.
- Không dùng `X-User-*` header làm identity. Identity phải lấy từ JWT đã được Spring Security verify.

Ví dụ lấy user hiện tại trong controller:

```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@GetMapping("/me")
public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    // roles nằm trong claim "roles" và đã được SecurityConfig validate.
}
```

## JWT tối giản

External JWT do `user-service` phát cho client:

```json
{
  "iss": "urn:code-base:auth",
  "sub": "user-id",
  "exp": 1760003600,
  "roles": ["LEARNER"]
}
```

Internal JWT do `api-gateway` ký để gửi xuống service:

```json
{
  "iss": "urn:code-base:api-gateway",
  "sub": "user-id",
  "exp": 1760003600,
  "roles": ["LEARNER"]
}
```

## Thêm service mới

Khi thêm service mới trong `services/`, nên làm các bước tối thiểu sau:

1. Tạo thư mục mới, ví dụ `services/appointment-service`.
2. Khai báo module mới trong root `pom.xml`.
3. Thêm Eureka Client để service đăng ký lên Eureka.
4. Nếu có protected API, thêm Spring Security OAuth2 Resource Server để verify internal JWT.
5. Thêm file cấu hình tương ứng trong `infra/config-server/config-repo`.
6. Thêm route mới ở `infra/config-server/config-repo/api-gateway.yaml`.
7. Viết `README.md` riêng cho service đó.

## Chạy local

Các service nghiệp vụ phụ thuộc vào hạ tầng trong `infra/`, đặc biệt là Config Server, Eureka Server và database.

Ví dụ chạy `user-service` từ thư mục gốc:

```powershell
mvn -pl services/user-service spring-boot:run
```

## Tài liệu chi tiết

- [User Service](user-service/README.md)
