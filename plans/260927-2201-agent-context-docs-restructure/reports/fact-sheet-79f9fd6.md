# Fact sheet — HEAD `79f9fd6` (2026-09-27)

Nguồn duy nhất cho phase 2–4. Mỗi bảng ghi file nguồn. Không chứa giá trị secret (chỉ tên biến).

## 1. Module

Maven reactor (`pom.xml` `<modules>`): 12 module. `services/ai-learning-service` (Python) **ngoài** reactor.
`third_party/deeptutor` = bản clone tham khảo khi port, không module nào import (cưỡng chế bởi
`services/ai-learning-service/tests/test_no_deeptutor_dependency.py`).

| Module | Loại | Ngôn ngữ | Cổng (default) | Nguồn cổng |
| --- | --- | --- | --- | --- |
| `infra/config-server` | Infra (native `config-repo/`) | Java | 8888 | `infra/config-server/src/main/resources/application.yaml` |
| `infra/eureka-server` | Infra | Java | 8761 | `config-repo/eureka-server.yaml` |
| `infra/api-gateway` | Infra (WebFlux) | Java | 8080 | `infra/api-gateway/src/main/resources/application.yml` |
| `shared/common-security` | Lib (auto-config servlet) | Java | — | — |
| `services/user-service` | Business | Java | 8085 | `application.yml` `SERVER_PORT` |
| `services/content-service` | Business | Java | 8082 | idem |
| `services/assessment-service` | Business | Java | 8083 | idem |
| `services/access-service` | Business | Java | 8084 | idem |
| `services/learning-support-service` | Business | Java | 8086 | idem |
| `services/game-service` | Business (+WebSocket) | Java | 8087 | idem |
| `services/notification-service` | **Skeleton** (chỉ `package-info.java`) | Java | 8088 | idem |
| `services/community-service` | Business | Java | 8089 | idem |
| `services/ai-learning-service` | Business | Python 3.11 / FastAPI | 8000 | `main.py`, `docker-compose.yml` |

## 2. Database

| Service | DB | Host:port mặc định | Chạy ở đâu | Nguồn |
| --- | --- | --- | --- | --- |
| user | `user_db` | localhost:5432 (có `USER_DB_URL` trong `.env`) | Postgres local | `config-repo/user-service.yaml` |
| content | `content_db` | localhost:5432 | Postgres local | `config-repo/content-service.yaml` |
| assessment | `assessment_db` | localhost:5432 | Postgres local | `config-repo/assessment-service.yaml` |
| access | `access_db` | localhost:5432 | Postgres local | `config-repo/access-service.yaml` |
| notification | `notification_db` | localhost:5432 | Postgres local | `config-repo/notification-service.yaml` |
| learning-support | `learning_support_db` | localhost:5433 | compose `learning-support-db` | config-repo + compose |
| community | `community_db` | config default localhost:5432; compose DB ở **5434** (`.env` đặt `COMMUNITY_DB_URL`) | compose `community-db` | config-repo + compose |
| game | `game_db` | localhost:5435 | compose `game-db` | config-repo + compose |
| ai-learning | `ai_learning_db` | 127.0.0.1:5436 | compose `ai-learning-db`, migrate bằng container `ai-learning-migrate` (Flyway 11) | compose |

Postgres local (kiểm 2026-09-27): có `user_db`, `content_db`, `assessment_db`; **chưa có** `access_db`, `notification_db`.
Flyway: Java service tự migrate khi khởi động (`src/main/resources/db/migration`, số file: access 2, assessment 4,
community 1, content 6, game 1, learning-support 3, user 4); ai-learning `migrations/V0_1..V9`.

## 3. Route Gateway (`config-repo/api-gateway.yaml`)

| Path | Đích |
| --- | --- |
| `/auth/**`, `/api/users/**` | `lb://USER-SERVICE` |
| `/api/access/**` | `lb://ACCESS-SERVICE` |
| `/api/content/**` | `lb://CONTENT-SERVICE` |
| `/api/assessments/**` | `lb://ASSESSMENT-SERVICE` |
| `/api/learning-support/**` | `lb://LEARNING-SUPPORT-SERVICE` |
| `/api/games/**`; `/ws/games/**` (WebSocket) | `lb://GAME-SERVICE`; `lb:ws://GAME-SERVICE` |
| `/api/notifications/**` | `lb://NOTIFICATION-SERVICE` |
| `/api/community/**` | `lb://COMMUNITY-SERVICE` |
| `/api/ai-learning/**` | `${AI_LEARNING_SERVICE_URI:http://localhost:8000}` (không qua Eureka) |

`internal-jwt-paths`: `/api/users`, `/auth/me`, `/api/access`, `/api/content`, `/api/assessments`, `/api/learning-support`,
`/api/games`, `/api/notifications`, `/api/community`, `/api/ai-learning`. Public: `/auth/login|refresh|logout|forgot-password|reset-password`,
`/api/users/register`, `/ws/games/**`. CORS: `FRONTEND_ORIGIN` (default `http://localhost:5173`) + `http://localhost:*`,
`http://127.0.0.1:*`; exposed headers `Authorization`, `Content-Type` (`SecurityConfig.java:153`).

## 4. Giao tiếp giữa service

HTTP đồng bộ:

| Caller | Callee | Endpoint | Base URL (biến, default) | Nguồn |
| --- | --- | --- | --- | --- |
| assessment | content | `/internal/assessment-content/knowledge-point-mappings` | `CONTENT_SERVICE_URL`, `http://localhost:8082` | `ContentKnowledgeMappingClient` |
| assessment | user | `/api/users/me/learning-goals/active` | `USER_SERVICE_URL`, `http://localhost:8085` | `UserLearningGoalClient` |
| game | content | `/internal/game-content/snapshots` | `CONTENT_SERVICE_URL`, config-repo default `http://content-service:8082` | `ContentSnapshotClient` |
| ai-learning | content | `/api/content/topics`, `/api/content/knowledge-points`, `/api/content/reading/sections/{id}` | `AI_LEARNING_CONTENT_SERVICE_BASE_URL` | `app/clients/content_service.py` |
| ai-learning | user | `/api/users/me/learning-goals/active` | `AI_LEARNING_USER_SERVICE_BASE_URL` | `app/clients/user_service.py` |

ai-learning chuyển tiếp internal JWT của learner thẳng tới User/Content (không qua Gateway).

Async (RabbitMQ 3.13, compose `rabbitmq` 5672/15672):
- assessment: `FinalizeAssessmentResultUseCase` → `AssessmentCompletedEventFactory` → bảng outbox → `OutboxRelayScheduler`/`OutboxRelay`
  → `RabbitOutboxEventPublisher` → exchange `assessment.events` (`ASSESSMENT_EVENTS_EXCHANGE`), routing key
  `assessment.completed.v2`; bật/tắt relay `ASSESSMENT_OUTBOX_RELAY_ENABLED`. Contract: `docs/contracts/assessment-completed-v2.md`.
- ai-learning consumer (`app/messaging/assessment_consumer.py`, compose `ai-learning-consumer`): queue
  `ai-learning.assessment-completed.v2`, retry `…v2.retry` (TTL), DLQ `…v2.dlq`; tự khai báo topology.
- access, content, game: có entity/bảng `OutboxEvent` + adapter ghi, **không có relay/publisher** → event không được phát.

## 5. Biến môi trường

Tên biến (giá trị không ghi). `.env` root (gitignored) được compose nội suy và mọi Spring module import
(`spring.config.import: optional:file:./.env[.properties], optional:file:../../.env[.properties]`).

| Nhóm | Biến | Bắt buộc? |
| --- | --- | --- |
| Compose | `POSTGRES_PASSWORD`, `GAME_DB_PASSWORD`, `AI_LEARNING_DB_PASSWORD`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `GATEWAY_INTERNAL_JWT_SECRET` | Có (`:?`) |
| JWT | `EXTERNAL_JWT_SECRET`, `GATEWAY_INTERNAL_JWT_SECRET`, `EXTERNAL_JWT_ISSUER`, `INTERNAL_JWT_ISSUER`, `INTERNAL_TOKEN_MAX_AGE_SECONDS` | Secret có **fallback default trong config-repo** (rủi ro) |
| DB Java | `<SVC>_DB_URL/USERNAME/PASSWORD` (USER, ACCESS, CONTENT, ASSESSMENT, COMMUNITY, NOTIFICATION, LEARNING_SUPPORT, GAME) | `LEARNING_SUPPORT_DB_PASSWORD`, `GAME_DB_PASSWORD` không default; còn lại có default |
| Service URL | `CONTENT_SERVICE_URL`, `USER_SERVICE_URL`, `AI_LEARNING_SERVICE_URI`, `EUREKA_URL`, `SPRING_CLOUD_CONFIG_URI`, `FRONTEND_ORIGIN`, `SERVER_PORT` | Default |
| RabbitMQ (assessment) | `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `ASSESSMENT_EVENTS_EXCHANGE`, `ASSESSMENT_OUTBOX_RELAY_ENABLED` | Default `guest` sẽ fail với broker compose |
| Auth cookie (user) | `APP_AUTH_COOKIE_*`, `ACCESS_TOKEN_MAX_AGE_SECONDS`, `REFRESH_TOKEN_MAX_AGE_SECONDS` | Default |
| ai-learning `Settings` (`AI_LEARNING_`) | `INTERNAL_JWT_SECRET`, `DATABASE_URL`, `USER_SERVICE_BASE_URL`, `CONTENT_SERVICE_BASE_URL` (bắt buộc); `INTERNAL_JWT_ISSUER`, `TUTOR_TURNS_PER_DAY`=50, `MEMORY_SUMMARIES_PER_DAY`=10, `QUOTA_TIMEZONE`=`Asia/Ho_Chi_Minh` | `app/config.py` |
| ai-learning LLM (`AI_LEARNING_LLM_`) | `BASE_URL`, `MODEL`, `API_KEY`, `REASONING_EFFORT`, `TIMEOUT_SECONDS` | Không key → không gọi LLM |
| ai-learning consumer | `AI_LEARNING_DATABASE_URL`, `AI_LEARNING_AMQP_URL`, `AI_LEARNING_ASSESSMENT_EXCHANGE`, `AI_LEARNING_RETRY_DELAY_MS`, `AI_LEARNING_MAX_DELIVERY_ATTEMPTS` | 2 đầu bắt buộc |
| Test ai-learning | `AI_LEARNING_TEST_DATABASE_URL`, `AI_LEARNING_TEST_AMQP_URL` | Thiếu → test tương ứng skip |
| Stub | `LLM_STUB_MODE`, `LLM_STUB_SCRIPT` | compose `llm-stub` |

## 6. Chạy local (`docker-compose.yml`, `README.md` §4–6)

- Compose chứa: `learning-support-db`, `community-db`, `game-db`, `rabbitmq`, `ai-learning-db`, `ai-learning-migrate`,
  `ai-learning-api`, `ai-learning-consumer`, `llm-stub`, `game-service`. **Không** có config-server, eureka, gateway, user,
  content, assessment, access, community, learning-support, notification.
- Service Java chạy trên host (IDE / `java -jar`). Thứ tự README: Postgres local → `docker compose up -d --build rabbitmq
  ai-learning-db ai-learning-migrate ai-learning-api ai-learning-consumer` → config-server → eureka → api-gateway → user →
  content → assessment (các service khác sau eureka khi cần).
- Container AI Learning gọi User (8085) và Content (8082) trên host qua `host.docker.internal`.

## 7. Lệnh đã chạy thử (2026-09-27)

| Lệnh | Kết quả |
| --- | --- |
| `mvn -q -pl shared/common-security test` | exit 0, 14 s |
| `mvn -q -pl services/community-service -am test` (Testcontainers, cần Docker) | exit 0, 42 s |
| `mvn -q compile -DskipTests` (toàn reactor) | exit 0, 45 s — dùng thay `clean compile` để không xóa `target/` của service đang chạy trong IDE |
| `docker compose config --quiet` | exit 0 |
| ai-learning: venv Python 3.11, `python -m pip install pytest -r requirements-test.txt`, env `AI_LEARNING_TEST_DATABASE_URL`, `AI_LEARNING_TEST_AMQP_URL`, `PYTHONDONTWRITEBYTECODE=1`, `python -m pytest tests -q` (trong `services/ai-learning-service`) | 432 passed, 0 skip, ~2.5 phút |
| `graphify update . --force` (sau khi loại `third_party/`) | 7.429 node, 24.517 edge, 0 node `third_party/` |

Lưu ý: `.venv` ở root (Python 3.14, uv) không có pytest; Python 3.13 hệ thống không có dependency.

## 8. Bất biến có cưỡng chế

| Bất biến | Cưỡng chế |
| --- | --- |
| Không import/tham chiếu `deeptutor` trong ai-learning (code, image, requirements, compose) | `tests/test_no_deeptutor_dependency.py` |
| Test không gọi LLM thật | Test dùng `ScriptedChat` / stub OpenAI-compatible cục bộ (README "Run tests") |
| Không log nội dung hội thoại/prompt/key | Test assert log (`test_unconfigured_llm_fails_the_turn_without_leaking_text`, refund log test) |
| Downstream không tin `X-User-*`, chỉ internal JWT | `common-security`, test MVC security của user-service |
| `.env`, `__pycache__/`, `*.py[cod]`, `graphify-out/`, `.claude/` không commit | `.gitignore` |
| Migration đã áp dụng không sửa; version tăng dần | Quy ước README; test ai-learning dựng schema từ cùng chuỗi `migrations/V*` |
| Một lượt tutor luôn được đóng (không kẹt slot running) | `TutorEngine.run` finally + `run_turn`; test engine/API |

## 9. Điểm chưa nhất quán còn hiệu lực

| # | Điểm | Trạng thái |
| --- | --- | --- |
| 1 | Application layer user-service import Spring (`@Service`, `@Transactional`…): 28 file | Còn |
| 2 | `services/user-service/README.md` ghi `PATCH /api/users/{id}/status`; controller là `@PutMapping` | Còn |
| 3 | `GetAllUsersUseCase` dùng `findAll()` không phân trang | Còn |
| 4 | graph tham chiếu `SecurityConfig.java` đã xóa | **Hết** (0 node trỏ file không tồn tại) |
| 5 | Template service mới dùng `application/port`; user-service dùng `domain/repository` (assessment, game dùng port) | Còn |
| 6 | game `CONTENT_SERVICE_URL` config-repo default `http://content-service:8082` ghi đè default local → sai khi chạy host | Mới |
| 7 | Outbox access/content/game không có relay | Mới |
| 8 | notification-service chỉ là skeleton | Mới |
| 9 | Compose `game-service` trỏ `http://config-server:8888` nhưng compose không có config-server | Mới |
| 10 | Secret có fallback default trong config-repo tracked (`GATEWAY_INTERNAL_JWT_SECRET`, `EXTERNAL_JWT_SECRET`, DB password) | Mới |
| 11 | README root chỉ liệt kê 3 DB local; access/notification cần thêm `access_db`, `notification_db` | Mới |
| 12 | Gateway CORS không expose `Retry-After` (429 của AI Learning) | Mới (Low, plan `260927-1453` mục "Làm sau") |
