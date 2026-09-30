# Kiến trúc hệ thống IELTSPath (backend)

- Cập nhật lần cuối: 2026-10-01; dữ kiện đã kiểm với code tại commit `ab613b1`
- Đọc khi cần hiểu toàn hệ thống. Quy tắc bắt buộc nằm ở [`AGENTS.md`](../AGENTS.md); hướng dẫn chạy chi tiết ở
  [`README.md`](../README.md). Code là nguồn đúng khi tài liệu này lệch.

## 1. Tổng quan

IELTSPath là backend microservices cho nền tảng học IELTS: 3 service hạ tầng Spring Cloud, 1 thư viện bảo mật dùng
chung, 8 business service Java (Spring Boot, Maven reactor) và 1 business service Python (AI Learning, FastAPI, ngoài
Maven). Mỗi business service sở hữu database PostgreSQL riêng. Client chỉ gọi qua API Gateway.

```text
                        config-repo (native)
                              |
                         Config Server :8888
                          /           \
                   Eureka :8761      (mọi service Java đọc config khi khởi động)
                        ^
                        | đăng ký / tìm instance (lb://)
Client --> API Gateway :8080 --(internal JWT)--> user :8085, content :8082, library :8081,
               |                                 assessment :8083, access :8084, game :8087 (+ws), notification :8088,
               |                                 community :8089
               +--(URI cố định, không Eureka)--> ai-learning :8000 (Python)

assessment --outbox--> RabbitMQ exchange assessment.events --assessment.completed.v2--> ai-learning consumer
assessment --HTTP--> content, user        game --HTTP--> library, content
library --HTTP--> content                 ai-learning --HTTP--> content, user
```

`third_party/deeptutor` là bản clone DeepTutor dùng để đọc khi port; không service nào import nó.

## 2. Service và dữ liệu

| Service | Bounded context | Database (chạy ở) | Route Gateway |
| --- | --- | --- | --- |
| `infra/config-server` | Cấu hình tập trung từ `infra/config-server/config-repo/` | — | — |
| `infra/eureka-server` | Service registry | — | — |
| `infra/api-gateway` | Ingress WebFlux: xác thực external JWT, ký internal JWT, routing, CORS, correlation id | — | — |
| `shared/common-security` | Thư viện: security servlet cho downstream, internal JWT, `CanonicalRoles`, `CurrentUserProvider` | — | — |
| `user-service` | Tài khoản, role, đăng nhập/refresh/logout, hồ sơ học viên, learning goal, activity và streak | `user_db` (Postgres local 5432; V5 tạo `learning_activities`, `streaks`) | `/auth/**`, `/api/users/**`, `/api/learning-support/{activities,streak}/**` |
| `content-service` | Chủ đề, knowledge point, câu hỏi (có version), gói nội dung/bài đọc, asset; kiểm topic cho library và snapshot grammar cho game | `content_db` (local 5432; V7 xóa năm bảng catalog) | `/api/content/**` trừ nhóm từ vựng/video |
| `library-service` | Catalog từ vựng/video (V1, năm bảng), flashcard/deck, note, tiến độ video và đoạn đã lưu (V2, sáu bảng) | `library_db` (compose host 5437) | `/api/content/{videos,vocabulary}/**`, `/api/content/admin/vocabulary/**`, `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` |
| `assessment-service` | Lượt làm bài, chấm, kết quả (có version), bài nộp, video practice; phát `AssessmentCompleted.v2` | `assessment_db` (local 5432) | `/api/assessments/**` |
| `access-service` | Gói, subscription, activation key, ví điểm và sổ điểm | `access_db` (local 5432) | `/api/access/**` |
| `game-service` | Phòng game, trận, phiên chơi, WebSocket realtime | `game_db` (compose 5435) | `/api/games/**`, `/ws/games/**` |
| `community-service` | Bài viết, bình luận, reaction, kiểm duyệt | `community_db` (compose 5434; default code là local 5432, đổi bằng `COMMUNITY_DB_URL`) | `/api/community/**` |
| `notification-service` | Chưa triển khai (chỉ khung package) | `notification_db` (local 5432) | `/api/notifications/**` |
| `ai-learning-service` | Mastery path thích ứng, tutor study/review (SSE), practice notebook, learner memory, hạn mức LLM theo ngày; nhận kết quả thi chính thức | `ai_learning_db` (compose 5436) | `/api/ai-learning/**` |

Chi tiết schema: [`.sdd/database/DATABASE_V5.md`](../.sdd/database/DATABASE_V5.md). Service Java tự chạy Flyway khi khởi
động (`src/main/resources/db/migration`); AI Learning dùng container `ai-learning-migrate` (Flyway) trên `migrations/`.
Gateway giữ các path công khai nhưng trỏ từng nhóm tới chủ sở hữu mới; không còn route tổng quát
`/api/learning-support/**`. Library V2 có bốn FK `ON DELETE RESTRICT` tới catalog; không có bảng `outbox_events`.

## 3. Giao tiếp giữa service

### Đồng bộ (HTTP)

| Caller | Callee | Endpoint | Cấu hình base URL |
| --- | --- | --- | --- |
| assessment | content | `/internal/assessment-content/knowledge-point-mappings` | `CONTENT_SERVICE_URL` |
| assessment | user | `/api/users/me/learning-goals/active` | `USER_SERVICE_URL` |
| library | content | `GET /api/content/topics/{id}` khi ghi video | `CONTENT_SERVICE_URL` (mặc định `http://localhost:8082`) |
| game (`VOCABULARY`) | library | `POST /internal/game-content/snapshots` | `LIBRARY_SERVICE_URL` (mặc định `http://localhost:8081`) |
| game (`GRAMMAR`) | content | `POST /internal/game-content/snapshots` | `CONTENT_SERVICE_URL` |
| ai-learning | content | `/api/content/topics`, `/api/content/knowledge-points`, `/api/content/reading/sections/{id}` | `AI_LEARNING_CONTENT_SERVICE_BASE_URL` |
| ai-learning | user | `/api/users/me/learning-goals/active` | `AI_LEARNING_USER_SERVICE_BASE_URL` |

Assessment, library và game gọi thẳng service đích (không qua Gateway), kèm bearer của request và `X-Correlation-Id`.
Library dùng timeout kết nối 2 giây, đọc 5 giây; lỗi Content khi kiểm topic trả 503. Hai nơi cài snapshot game theo
cùng [contract](contracts/game-content-snapshot-v1.md). AI Learning
chuyển tiếp nguyên internal JWT của learner tới User/Content (chưa gửi `X-Correlation-Id`); token này sống ngắn (mặc định
60 giây), nên tutor copy bài đọc khi tạo session thay vì gọi Content trong từng lượt.

### Bất đồng bộ (RabbitMQ)

```text
assessment: FinalizeAssessmentResultUseCase
  -> AssessmentCompletedEventFactory -> outbox_events (cùng transaction)
  -> OutboxRelayScheduler / OutboxRelay -> RabbitOutboxEventPublisher (publisher confirm)
  -> exchange assessment.events, routing key assessment.completed.v2
ai-learning: app/messaging/assessment_consumer.py
  -> queue ai-learning.assessment-completed.v2 (retry qua ...v2.retry có TTL, hết lượt -> ...v2.dlq)
  -> FormalEvidenceAdapter -> FormalAssessmentIngestionService -> PathService (mastery path của goal)
```

Contract: [`docs/contracts/assessment-completed-v2.md`](contracts/assessment-completed-v2.md). Kết quả cho một goal chưa
có path được giữ lại và áp dụng khi path được tạo. access, content và game có bảng outbox và ghi event nhưng chưa có
relay, nên các event đó chưa được phát.

## 4. Mô hình bảo mật

```text
Client --external JWT--> Gateway: kiểm issuer, subject UUID, role hợp lệ
       Gateway --ký internal JWT (HMAC, issuer riêng, ~60 s)--> service trong internal-jwt-paths
       Service Java: CommonSecurityAutoConfiguration -> CurrentUserProvider -> controller
       AI Learning: app/security/internal_jwt.py kiểm cùng issuer/claim/role
```

- `user-service` ký external HMAC JWT và chỉ lưu refresh token dạng hash; refresh token dùng một lần.
- Gateway (WebFlux) có `SecurityWebFilterChain` riêng; `InternalJwtGatewayFilter` thay header Authorization cho các path
  trong `internal-jwt-paths`. Public: `/auth/login|refresh|logout|forgot-password|reset-password`, `/api/users/register`,
  `/ws/games/**`.
- Downstream không bao giờ tin header `X-User-*`; test MVC của user-service chứng minh token external và header giả bị từ chối.
- Role chuẩn (`CanonicalRoles.ALL`): `ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, `SALES_STAFF`. Đăng ký công khai
  chỉ tạo `CUSTOMER`.
- Hai secret tách biệt: `EXTERNAL_JWT_SECRET`, `GATEWAY_INTERNAL_JWT_SECRET` (Base64, ≥ 32 byte). Giá trị không ghi trong
  tài liệu. Game WebSocket xác thực bằng ticket ngắn hạn cấp qua REST (`/ws/games/**` là public ở Gateway).

## 5. Kiến trúc bên trong service

### Service Java (DDD + Clean Architecture)

```text
api ----------> application ----------> domain
                     |                    ^
                     v                    |
               (application/port)   infrastructure (JPA entity/repository, mapper, adapter, client, messaging)
```

- `domain`: aggregate, entity, value object, domain exception, repository contract. Không phụ thuộc Spring/JPA/HTTP.
- `application`: `*UseCase`, command/result (và query ở community, library); gọi repository contract. Assessment
  và game có thêm `application/port` cho client, outbox, WebSocket ticket; user-service dùng `domain/repository`.
- `infrastructure`: `*JpaEntity`, `*JpaRepository`, MapStruct mapper, `*RepositoryAdapter`, HTTP client, messaging.
- `api`: controller, DTO, `GlobalExceptionHandler` → `ErrorResponse`, filter log có `X-Correlation-Id`.
- Thực tế còn thực dụng: use case dùng `@Service`/`@Transactional` của Spring.

### AI Learning (Python)

```text
app/api (FastAPI routes, DTO camelCase, SSE) -> app/application (path service, ingestion, ordering)
   -> app/mastery (engine port từ DeepTutor v1.6.9), app/tutor (engine, tools, memory, session store),
      app/practice, app/usage (hạn mức ngày), app/learning (ordering LLM, provenance)
   -> app/persistence, stores (psycopg2, mở kết nối mỗi lần gọi), app/clients (httpx), app/messaging (pika)
```

`main.py` khai báo app FastAPI; lúc khởi động đánh dấu các lượt tutor bị gián đoạn là failed và kiểm múi giờ/bảng hạn
mức. Cấu hình qua pydantic-settings, tiền tố `AI_LEARNING_`.

## 6. Luồng chính

### Đăng nhập và token

```text
Client -> Gateway (public /auth/login) -> user-service LoginUseCase -> UserRepository
       -> AuthTokenIssuer -> external JWT + refresh token (hash trong user_db) [+ cookie nếu APP_AUTH_COOKIE_ENABLED]
```

`RefreshTokenUseCase` hash token nhận vào, chỉ consume token còn hiệu lực một lần, kiểm user active rồi phát cặp mới.

### Tạo lộ trình học (AI Learning)

```text
POST /api/ai-learning/paths -> PathService: goal đang active (User) -> curriculum trong band (Content)
  -> CurriculumAdapter -> mastery path (một path mỗi learning goal) -> PathOrderer (LLM, tùy chọn; lỗi thì giữ thứ tự Content)
```

### Catalog, thư viện cá nhân và game

```text
Client -> Gateway -> library: từ vựng/video, flashcard/deck, note, tiến độ và đoạn video đã lưu
                              -> content: GET /api/content/topics/{id} khi ghi video
Client -> Gateway -> user: POST/GET activity, GET streak (giữ prefix /api/learning-support)
Client -> Gateway -> game: tạo phòng/phiên chơi -> chọn snapshot theo learningDomain
                              VOCABULARY -> library; GRAMMAR -> content
```

Năm bảng catalog và sáu bảng thư viện cá nhân thuộc `library_db`; `learning_activities` và `streaks` thuộc `user_db`.
Game lưu snapshot để chơi; hai service cung cấp cùng request/response nội bộ.

### Kết quả thi → mastery

Học viên làm bài ở assessment → chấm → `FinalizeAssessmentResultUseCase` → `AssessmentCompleted.v2` (mục 3) → AI Learning
ghi bằng chứng chính thức vào path của goal; `GET /api/ai-learning/status` phản ánh kết quả.

### Lượt tutor (SSE)

```text
POST /api/ai-learning/tutor/sessions/{id}/turns
  -> open_turn (404 session lạ, 409 lượt đang chạy)
  -> tiêu hạn mức ngày (hết: 429 + resetsAt, không gọi LLM)
  -> TutorEngine.run: vòng tool-calling tối đa 6 vòng (mastery_*, path_*, practice/reading, save_note)
  -> SSE turn.started ... turn.completed | turn.failed; lượt luôn được đóng
  -> tóm tắt learner memory chạy nền (có hạn mức riêng)
```

Contract: [`tutor-sse-v1.md`](contracts/tutor-sse-v1.md), [`practice-v1.md`](contracts/practice-v1.md).

## 7. Chạy local (tóm tắt)

- Compose (`docker-compose.yml`): `library-db` (host 5437), `community-db`, `game-db`, `rabbitmq`, `ai-learning-db`,
  `ai-learning-migrate`, `ai-learning-api`, `ai-learning-consumer`, `game-service` (không dùng được, xem §11); `llm-stub`
  nằm sau profile `llm-stub`. Chỉ bật các service cần, không `docker compose up` toàn bộ.
- Trên host (IDE hoặc `java -jar`): config-server → eureka → api-gateway → các business service Java. Postgres local
  5432 cần `user_db`, `content_db`, `assessment_db`, `access_db`, `notification_db`.
- `.env` ở root (gitignored) được compose nội suy; Gateway và mọi service Java nghiệp vụ import nó
  (`optional:file:../../.env[.properties]`), config-server và eureka-server thì không. AI Learning chạy trên host đọc
  `.env` riêng của service. Compose yêu cầu `LIBRARY_DB_PASSWORD` dù không bật `library-db`. Container AI Learning gọi
  User/Content trên host qua `host.docker.internal`. Game trên host cần đặt `CONTENT_SERVICE_URL=http://localhost:8082`;
  `LIBRARY_SERVICE_URL` đã mặc định `http://localhost:8081`.

## 8. Quyết định kiến trúc đã quan sát

Không có ADR chính thức; các quyết định sau suy ra từ code, README và lịch sử git.

| Quyết định | Lý do | Trade-off |
| --- | --- | --- |
| Gateway ký internal JWT; downstream chỉ tin internal JWT | Không tin identity header; tập trung validation trong `common-security` | Gateway và service phải cùng contract claim/issuer/role (kể cả bản Python) |
| Config Server native + Eureka | Cấu hình tập trung, route `lb://` thay host cố định | Khởi động phụ thuộc thứ tự hạ tầng |
| Domain model tách JPA entity (mapper + adapter) | Giữ JPA ngoài domain | Thêm mapper, lặp model |
| `Dockerfile.spring-service` theo `MODULE_PATH`; Config Server có Dockerfile riêng | Config Server cần đóng gói `config-repo` | Hai kiểu Dockerfile |
| AI Learning viết bằng Python, ngoài Maven và Eureka, route URI cố định | Engine mastery/tutor gốc là Python | Không discovery/load-balancing; contract internal JWT phải giữ đồng bộ bằng tay |
| Port engine mastery từ DeepTutor v1.6.9 vào `app/mastery`, bỏ phụ thuộc runtime (`3b97191`) | Kiểm soát code, image nhỏ, không kéo cả DeepTutor | Tự bảo trì bản port; test giữ giá trị gốc để phát hiện lệch |
| Outbox + RabbitMQ cho assessment → AI Learning; consumer có retry/DLQ | Không mất kết quả thi khi AI Learning tạm lỗi | Thêm broker; outbox các service khác chưa có relay |
| Hạn mức lượt tutor/tóm tắt memory theo ngày trong AI Learning | Chặn chi phí LLM theo học viên | Chưa trừ AI Points (làm sau) |
| Tách catalog/thư viện cá nhân sang library, activity/streak sang user | Mỗi nhóm dữ liệu có service và DB sở hữu rõ ràng, giữ public path | Gateway cần route theo nhóm; game chọn hai nguồn snapshot |

## 9. Pattern đang dùng

| Pattern | Nơi dùng |
| --- | --- |
| API Gateway, Configuration Server, Service Discovery | `infra/*` |
| Repository Adapter, Aggregate/Value Object, Mapper | Service Java |
| Ports & adapters (`application/port`) | assessment, game, library |
| Transactional outbox + relay có publisher confirm | assessment (access/content/game mới có phần ghi) |
| Consumer với retry queue TTL và dead-letter queue | ai-learning |
| Shared auto-configuration | `common-security` |
| Global exception handler → `ErrorResponse` | Service Java |
| Server-sent events, tool-calling loop | AI Learning tutor |
| WebSocket với ticket ngắn hạn | game |

## 10. Bài học kỹ thuật

- **Gom security downstream vào `common-security`** (`a18a44e`): bỏ `SecurityConfig` riêng của user-service, thêm
  auto-configuration, `CurrentUserProvider`, validator và role chuẩn dùng chung.
- **Docker theo Maven module** (`dac8b03`): root Dockerfile nhận `MODULE_PATH`; Config Server có Dockerfile riêng.
- **Bỏ phụ thuộc DeepTutor** (`3b97191`): port engine mastery, thêm test chặn mọi import/tham chiếu `deeptutor`.
- **Không track bytecode Python** (`ad0a19b`): `.pyc` từng được commit và bị IDE ghi đè liên tục.
- **Graph chỉ chứa code first-party** (`9a1f5a8`): `third_party/` từng chiếm ~80% node graphify.

## 11. Điểm chưa nhất quán đã biết

| Điểm | Ghi chú |
| --- | --- |
| Application layer user-service import Spring | Khác Clean Architecture thuần; là baseline hiện hữu |
| `GetAllUsersUseCase` dùng `findAll()` không phân trang | Giới hạn hiện tại |
| Template service mới dùng `application/port`; user-service dùng `domain/repository` | Khác biệt template/hiện hữu |
| `CONTENT_SERVICE_URL` của game trong config-repo mặc định `http://content-service:8082` | Sai khi game chạy trên host; đặt biến này thành `http://localhost:8082` |
| `GlobalExceptionHandler` của user-service phân nhánh theo path `/api/learning-support/` | Giữ định dạng lỗi cũ sau khi activity/streak chuyển sang user; còn nợ kỹ thuật path-specific trong handler |
| Content V7 xóa vĩnh viễn năm bảng catalog | Chỉ chạy trên Testcontainers cho tới khi duyệt chạy trên `content_db` dùng chung |
| Outbox của access/content/game không có relay | Event chưa được phát |
| notification-service chỉ là khung | Route Gateway đã có |
| Compose `game-service` trỏ `http://config-server:8888` | Compose không có config-server |
| Secret có giá trị fallback trong config-repo được track | Thiếu biến env thì service dùng secret công khai trong repo |
| README root chỉ liệt kê 3 DB local | access, notification cần `access_db`, `notification_db` |
| community-service mặc định `localhost:5432/community_db`, DB compose ở 5434 | Đặt `COMMUNITY_DB_URL` khi dùng DB compose |
| `.sdd/global/system-architecture.md`, `.sdd/constraints/global.md` (baseline 2026-09-18) ghi không có broker/outbox/AI Learning | Lỗi thời; `.sdd/global/constitution.md` vẫn là invariant cao nhất. Dữ kiện: code, `AGENTS.md`, tài liệu này |
| AI Learning không gửi `X-Correlation-Id` khi gọi Content/User | Service Java có gửi |
| Gateway CORS chỉ expose `Authorization`, `Content-Type` | Browser không đọc được `Retry-After` của 429 tutor; dùng `resetsAt` (việc tùy chọn trong plan hạn mức) |
| community có bảng `outbox_events` (V1) nhưng không dùng | Library không tạo bảng này; outbox của access/content/game chưa có relay |
| `requirements.txt` của AI Learning có `sqlalchemy` nhưng code không import | Dependency thừa |
| `tests/e2e/tutor_e2e.py` ghi chạy từ root repo; README root ghi chạy từ `services/ai-learning-service` | Chưa thống nhất |
| `services/ai-learning-service/README.md` dòng ~500 nói event chưa có path "retry rồi vào DLQ" | Code (và dòng ~472 cùng README) lưu vào `pending_formal_assessment_results` và ACK; code đúng |
