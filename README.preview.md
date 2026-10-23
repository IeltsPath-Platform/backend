# IELTSPath Backend

Backend microservices cho nền tảng học IELTS: **Java 21, Spring Boot 3.5, Spring Cloud 2025.0**, Maven multi-module.
Mỗi service sở hữu một database PostgreSQL riêng; service giao tiếp qua HTTP và RabbitMQ (transactional outbox).

## Kiến trúc nhanh

```text
Client ──► API Gateway :8080 ──► service (qua Eureka, lb://)
            │ xác thực external JWT, ký internal JWT ngắn hạn
            └─ Config Server :8888 · Eureka :8761

Assessment ──outbox──► RabbitMQ ──► Learning (consumer có retry + DLQ)
```

| Nhóm | Module | Cổng | Vai trò |
| --- | --- | --- | --- |
| Infra | `infra/api-gateway` | 8080 | Routing, xác thực JWT, CORS (WebFlux) |
| | `infra/config-server` | 8888 | Cấu hình tập trung (`config-repo/`) |
| | `infra/eureka-server` | 8761 | Service registry |
| Shared | `shared/common-security` | — | Internal JWT, `CanonicalRoles`, `CurrentUserProvider` |
| | `shared/common-events` | — | Tên/hình dạng event, `MessageRetry`, `RetryTopology` |
| | `shared/common-storage` | — | `ObjectStorage` (Supabase), `ObjectKeys`, `UploadPolicy` |
| Business | `services/user-service` | 8085 | Tài khoản, auth, hồ sơ, activity, streak |
| | `services/library-service` | 8081 | Từ vựng, video, flashcard, ghi chú |
| | `services/content-service` | 8082 | Topic, knowledge point, câu hỏi, asset |
| | `services/assessment-service` | 8083 | Làm bài, chấm bài, phát `AssessmentCompleted.v2` |
| | `services/access-service` | 8084 | Gói, subscription, activation key, điểm |
| | `services/learning-service` | 8086 | Lộ trình, bài học, mastery, bài ôn, gamification |
| | `services/game-service` | 8087 | Phòng game, WebSocket |
| | `services/notification-service` | 8088 | Thông báo (trạng thái: xem `AGENTS.md`) |
| | `services/community-service` | 8089 | Bài viết, bình luận, kiểm duyệt |

Namespace Java: `com.ieltspath.<module>`. Mỗi service theo layer `api → application → domain`, `infrastructure` triển khai
contract hướng vào trong. Chi tiết: [`docs/system-architecture.md`](docs/system-architecture.md).

**Bảo mật:** Gateway thay token của client bằng internal JWT; service chỉ tin danh tính đã xác thực, không tin header
`X-User-*`. `EXTERNAL_JWT_SECRET` và `GATEWAY_INTERNAL_JWT_SECRET` tách biệt.

## Bắt đầu

**Yêu cầu:** JDK 21, Maven 3.9+, Docker.

```bash
# 1. Tạo .env ở root (đã gitignore), xem bảng biến bên dưới
# 2. Kiểm cấu hình rồi chạy toàn stack
docker compose config --quiet
docker compose up -d --build

# Dừng (thêm -v để xóa dữ liệu và nạp lại seed)
docker compose down
```

Sau khi tạo lại container Gateway, chờ 1–2 phút để Eureka cập nhật route. Swagger: <http://localhost:8080/swagger-ui.html>
(đăng nhập `POST /auth/login`, bấm **Authorize**, dán `accessToken` không kèm `Bearer`).

| Dịch vụ | Cổng host | Đổi bằng |
| --- | --- | --- |
| Gateway | 8080 | `MVP_GATEWAY_PORT` |
| Eureka | 8761 | `MVP_EUREKA_PORT` |
| PostgreSQL | 5440 | `MVP_POSTGRES_PORT` |
| RabbitMQ / UI | 5672 / 15673 | `RABBITMQ_HOST_PORT` / `MVP_RABBITMQ_UI_PORT` |

### Biến môi trường (`.env`)

Không commit giá trị. **Bắt buộc:** `POSTGRES_PASSWORD`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `EXTERNAL_JWT_SECRET`,
`GATEWAY_INTERNAL_JWT_SECRET` (phải giống nhau ở Gateway và mọi service, lệch là 401).

| Tùy chọn | Dùng cho | Nếu thiếu |
| --- | --- | --- |
| `LEARNING_LLM_BASE_URL`, `_API_KEY`, `_MODEL` (+ `_REASONING_EFFORT`) | Learning chấm essay | Nộp essay trả 503 |
| `ASSESSMENT_LLM_BASE_URL`, `_API_KEY`, `_MODEL`, `_DAILY_LIMIT` (mặc định 20) | Assessment chấm essay thi topic/course | Chuyển hàng chờ EXAMINER |
| `CONTENT_MEDIA_BASE_URL` | Bucket mp3 Listening | Bài Listening trả 500 `INVALID_MEDIA_REFERENCE` |
| `SUPABASE_URL`, `SUPABASE_SECRET_KEY` | Service bật `app.storage.enabled` | Upload ảnh/file không dùng được |
| `DEMO_DATA_ENABLED` | Tài khoản/điểm demo (Compose local mặc định bật) | Không có tài khoản demo |

Mật khẩu có ký tự đặc biệt cần percent-encode vì nằm trong URL DB/AMQP.

### Chạy service từ IDE

```bash
docker compose up -d postgres rabbitmq
```

Đặt `*_DB_URL` thành `jdbc:postgresql://localhost:5440/<service>_db`, password là `POSTGRES_PASSWORD`. Thứ tự khởi động:
Config Server → Eureka → Gateway → user → content → library → assessment → các service còn lại. Khi chạy game trên host,
đặt `CONTENT_SERVICE_URL=http://localhost:8082`.

### Tài khoản demo (chỉ dev, `DEMO_DATA_ENABLED=true`)

| Tài khoản | Mật khẩu | Ghi chú |
| --- | --- | --- |
| `learner@ielts.demo` | `Demo@123` | CUSTOMER, 30 point |
| `admin@ielts.demo` | `Demo@123` | ADMIN |

Không bật cờ demo trên database dùng chung hoặc production. Hệ thống không seed ADMIN thật: gán role bằng SQL trên
`user_db` local rồi đăng nhập lại để token mang role mới.

## Build và test

```bash
mvn -q compile -DskipTests                    # cả reactor
mvn -q -pl services/<name>-service -am test   # một service (Testcontainers cần Docker)
mvn -q -pl shared/common-events test          # shared
mvn clean package                             # build jar; chỉ khi không có service chạy từ IDE
```

CI (`.github/workflows/ci.yml`) chạy `mvn -B -ntp verify` và `docker compose config --quiet` cho mọi PR vào `main`.
Test Testcontainers tự bỏ qua khi máy không có Docker, nên xanh ở local chưa chắc xanh trên CI.

### Migration (Flyway)

Thêm file `V<version>__<mo_ta>.sql` vào `services/<service>/src/main/resources/db/migration/`, version tăng dần. Không sửa
migration đã áp dụng. Init script database chỉ chạy khi volume mới được tạo; `docker compose down -v` để khởi tạo lại.

## Tài liệu

| Cần gì | Đọc |
| --- | --- |
| Quy tắc cho AI agent và contributor | [`AGENTS.md`](AGENTS.md), [`CLAUDE.md`](CLAUDE.md) |
| Kiến trúc, flow, điểm chưa nhất quán | [`docs/system-architecture.md`](docs/system-architecture.md) |
| Contract HTTP/event | [`docs/contracts/`](docs/contracts/) |
| Thứ tự gọi API cho frontend | [`docs/fe-main-flow-guide.md`](docs/fe-main-flow-guide.md) |
| Chi tiết từng service | `services/<name>-service/README.md`, `infra/README.md` |
| Kế hoạch đang làm | `plans/` |
| Graphify (graph code, tùy chọn) | `AGENTS.md` §6 |

## Quy ước nhánh

Chữ thường, `kebab-case`, dạng `<type>/<module>-<mo-ta-ngan>` hoặc `<type>/<issue-id>-<mo-ta-ngan>`.
Prefix: `feature/`, `fix/`, `hotfix/`, `refactor/`, `chore/`, `docs/`, `test/`, `perf/`, `release/`.
Commit theo conventional commit.
