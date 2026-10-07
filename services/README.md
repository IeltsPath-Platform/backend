# Service nghiệp vụ

Thư mục `services/` chứa các microservice xử lý nghiệp vụ của hệ thống. Mỗi service nên có database/config riêng, tự đăng ký lên Eureka và chỉ nhận request từ API Gateway trong luồng chuẩn.

## Service hiện có

| Module | Vai trò |
| --- | --- |
| `user-service` | Tài khoản, đăng nhập, refresh token, learning goal, activity và streak |
| `content-service` | Topic, knowledge point, câu hỏi, gói nội dung và asset |
| `library-service` | Catalog từ vựng/video và thư viện cá nhân (flashcard, deck, note, tiến độ video, đoạn đã lưu) |
| `assessment-service` | Bài làm, chấm điểm và kết quả thi |
| `access-service` | Gói, subscription, activation key và điểm |
| `game-service` | Phòng game, phiên chơi và WebSocket |
| `community-service` | Bài viết, bình luận và kiểm duyệt |
| `notification-service` | Khung service thông báo |
| `learning-service` | Java, cổng 8086: khung service học với schema tiến độ, bài ôn và mastery |

Gateway giữ các path `/api/learning-support/**`: `activities` và `streak` tới user-service; `flashcards`, `decks`,
`notes`, `video-progress` và `saved-segments` tới library-service. Không có route tổng quát cho prefix này.

## Nguyên tắc bảo mật

- Client gọi service thông qua `api-gateway`, không gọi trực tiếp service nghiệp vụ.
- Client gửi `Authorization: Bearer <external-jwt>` lên gateway.
- Gateway verify external JWT, sau đó thay bằng `Authorization: Bearer <internal-jwt>` trước khi forward xuống service.
- Service nghiệp vụ verify internal JWT bằng `GATEWAY_INTERNAL_JWT_SECRET`.
- Không dùng `X-User-*` header làm identity. Identity phải lấy từ JWT đã được Spring Security verify.

Ví dụ lấy user hiện tại trong controller:

```java
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;

private final CurrentUserProvider currentUserProvider;

@GetMapping("/me")
public UserResponse me() {
    UUID userId = currentUserProvider.requireUserId();
    // roles nằm trong claim "roles" và đã được common-security validate.
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
4. Nếu có protected API, thêm dependency `common-security`; module này auto-config Spring Security OAuth2 Resource Server để verify internal JWT.
5. Thêm file cấu hình tương ứng trong `infra/config-server/config-repo`.
6. Thêm route mới ở `infra/config-server/config-repo/api-gateway.yaml`.
7. Viết `README.md` riêng cho service đó.

Service có endpoint public thì khai báo qua config, không copy `SecurityConfig`:

```yaml
app:
  security:
    public-endpoints:
      - method: POST
        patterns:
          - /api/example/register
```

Service cần lấy user hiện tại thì inject `CurrentUserProvider` từ `common-security`
và gọi `requireUserId()` hoặc `requireCurrentUser()`.

## Chạy local

Các service nghiệp vụ phụ thuộc vào hạ tầng trong `infra/`, đặc biệt là Config Server, Eureka Server và database.

Ví dụ chạy `user-service` từ thư mục gốc:

```powershell
mvn -pl services/user-service spring-boot:run
```

## Tài liệu chi tiết

- [User Service](user-service/README.md)
- [Content Service](content-service/README.md)
- [Library Service](library-service/README.md)
- [Learning Service](learning-service/README.md)
- [Game Service](game-service/README.md)
