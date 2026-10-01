# Kiến trúc hệ thống IELTSPath (backend)

- Cập nhật lần cuối: 2026-10-01; dữ kiện đã kiểm với code sau khi learning-service có consumer kết quả thi
- Đọc khi cần hiểu toàn hệ thống. Quy tắc bắt buộc nằm ở [`AGENTS.md`](../AGENTS.md); hướng dẫn chạy chi tiết ở
  [`README.md`](../README.md). Code là nguồn đúng khi tài liệu này lệch.

## 1. Tổng quan

IELTSPath là backend microservices cho nền tảng học IELTS: 3 service hạ tầng Spring Cloud, 1 thư viện bảo mật dùng
chung và 9 business service Java (Spring Boot, Maven reactor). Service học AI Learning viết bằng Python đã được thay bằng
`learning-service` Java ngày 2026-10-01. Mỗi business service sở hữu database PostgreSQL riêng. Client chỉ gọi qua API Gateway.

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
               |                                 community :8089, learning :8086

assessment --outbox--> RabbitMQ exchange assessment.events --assessment.completed.v2--> learning consumer
assessment --HTTP--> content, user        game --HTTP--> library, content
library --HTTP--> content                 learning --HTTP--> content (/internal/learning-content/*)
```

`third_party/deeptutor` là bản clone DeepTutor dùng để đọc; công thức mastery của learning-service được port từ đó, không service nào build hay import thư mục này.

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
| `learning-service` | Thứ tự topic theo user, bài học và nộp khối bài tập, cổng mở bài, mastery theo KP, bài ôn bằng gói luyện, giao mã đề cuối; nhận kết quả thi chính thức | `learning_db` (compose 5436) | `/api/learning/**` |

Chi tiết schema: [`.sdd/database/DATABASE_V5.md`](../.sdd/database/DATABASE_V5.md). Service Java tự chạy Flyway khi khởi
động (`src/main/resources/db/migration`).
Gateway giữ các path công khai nhưng trỏ từng nhóm tới chủ sở hữu mới; không còn route tổng quát
`/api/learning-support/**`. Library V2 có bốn FK `ON DELETE RESTRICT` tới catalog; không có bảng `outbox_events`.

## 3. Giao tiếp giữa service

### Đồng bộ (HTTP)

| Caller | Callee | Endpoint | Cấu hình base URL |
| --- | --- | --- | --- |
| assessment | content | `GET /internal/learning-content/package-versions/{id}` khi tạo attempt (ngoài transaction) | `CONTENT_SERVICE_URL` |
| library | content | `GET /api/content/topics/{id}` khi ghi video | `CONTENT_SERVICE_URL` (mặc định `http://localhost:8082`) |
| game (`VOCABULARY`) | library | `POST /internal/game-content/snapshots` | `LIBRARY_SERVICE_URL` (mặc định `http://localhost:8081`) |
| game (`GRAMMAR`) | content | `POST /internal/game-content/snapshots` | `CONTENT_SERVICE_URL` |
| learning | content | `/internal/learning-content/{topic-sequence,topics/{id}/lessons,lessons/{id},topics/{id}/test-packages,practice-sets/search,package-versions/{id}}` | `CONTENT_SERVICE_URL` (mặc định `http://localhost:8082`) |
| learning | access | `GET /api/access/me/points` (số dư), `POST /internal/access/points/debit` (trừ 3 point sau khi chấm bài luận), bearer của học viên | `ACCESS_SERVICE_URL` (mặc định `http://localhost:8084`) |
| learning | LLM (ngoài hệ thống) | `POST {base}/chat/completions` (OpenAI-compatible) khi chấm bài luận, ngoài mọi transaction | `LEARNING_LLM_BASE_URL`, `LEARNING_LLM_API_KEY`, `LEARNING_LLM_MODEL` |

Assessment, library, game và learning gọi thẳng service đích (không qua Gateway), kèm bearer của request và `X-Correlation-Id`.
Library dùng timeout kết nối 2 giây, đọc 5 giây; lỗi Content khi kiểm topic trả 503. Hai nơi cài snapshot game theo
cùng [contract](contracts/game-content-snapshot-v1.md). Learning gọi Content qua API nội bộ (Gateway chặn `/internal/**`,
contract [`learning-content-internal-v1`](contracts/learning-content-internal-v1.md)); Content là nơi duy nhất ghép URL media.

### Bất đồng bộ (RabbitMQ)

```text
assessment: SubmitAssessmentAttemptUseCase -> AutoGradeAttemptService (mọi câu tự chấm được)
            hoặc FinalizeAssessmentResultUseCase (người chấm)
  -> AssessmentResultCompleter -> AssessmentCompletedEventFactory -> outbox_events (cùng transaction)
  -> OutboxRelayScheduler / OutboxRelay -> RabbitOutboxEventPublisher (publisher confirm)
  -> exchange assessment.events, routing key assessment.completed.v2
learning: AssessmentCompletedListener (ack thủ công)
  -> queue learning.assessment-completed.v2 (retry qua ...v2.retry có TTL, vi phạm contract/hết lượt -> ...v2.dlq)
  -> AssessmentCompletedParser -> ApplyAssessmentResultUseCase (một transaction, khóa theo user):
     version theo (attempt, result_version) -> kp_evidence -> TOPIC_GATE dùng lần giao mã đề -> chèn bài ôn
```

Contract: [`docs/contracts/assessment-completed-v2.md`](contracts/assessment-completed-v2.md). Bằng chứng gắn với user nên áp
dụng được ngay, kể cả khi học viên chưa gọi API học. access, content và game có bảng outbox và ghi event nhưng chưa có
relay, nên các event đó chưa được phát.

## 4. Mô hình bảo mật

```text
Client --external JWT--> Gateway: kiểm issuer, subject UUID, role hợp lệ
       Gateway --ký internal JWT (HMAC, issuer riêng, ~60 s)--> service trong internal-jwt-paths
       Service Java: CommonSecurityAutoConfiguration -> CurrentUserProvider -> controller
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

## 6. Luồng chính

### Đăng nhập và token

```text
Client -> Gateway (public /auth/login) -> user-service LoginUseCase -> UserRepository
       -> AuthTokenIssuer -> external JWT + refresh token (hash trong user_db) [+ cookie nếu APP_AUTH_COOKIE_ENABLED]
```

`RefreshTokenUseCase` hash token nhận vào, chỉ consume token còn hiệu lực một lần, kiểm user active rồi phát cặp mới.

### Học topic và bài (Learning Service)

```text
GET /api/learning/topics -> một lần gọi Content topic-sequence -> knowledge_point_catalog + topic_progress.sequence_order
  -> trạng thái suy ra khi đọc: PASSED (passed_at) / IN_PROGRESS (topic đầu chưa đạt) / LOCKED
POST /api/learning/lessons/{id}/exercises/{blockId}/submissions -> khóa theo user -> cổng (REVIEW_REQUIRED, TOPIC_LOCKED,
  LESSON_LOCKED) -> chấm answer-spec-v1 -> bằng chứng lần nộp đầu -> bài xong thì ReviewRule chèn bài ôn khi KP yếu
GET/POST /api/learning/reviews/{id} -> lý thuyết + một gói PRACTICE_SET; trượt 3 set -> SKIPPED
POST /api/learning/topics/{id}/test-assignments -> một mã đề dùng một lần, xoay vòng theo package
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

Học viên lấy mã đề từ learning (`POST /api/learning/topics/{id}/test-assignments`), tạo attempt bằng
`packageVersionId` (assessment đọc đề từ content, tự suy loại attempt), nộp bài → assessment tự chấm câu khách quan
(câu không chấm được thì chờ người chấm) → `AssessmentCompleted.v2` (mục 3) → Learning Service ghi bằng chứng, đề cuối đạt
≥ 70% thì topic PASSED; `GET /api/learning/mastery` và `GET /api/learning/topics` phản ánh kết quả. Học viên xem điểm,
đúng/sai ở `GET /api/assessments/attempts/{id}/result`; lời giải chỉ hiện khi đạt ≥ 70%.

## 7. Chạy local (tóm tắt)

- Compose (`docker-compose.yml`): `library-db` (host 5437), `community-db`, `game-db`, `learning-db` (host 5436), `rabbitmq`,
  `game-service` (không dùng được, xem §11). Chỉ bật các service cần, không `docker compose up` toàn bộ.
- Trên host (IDE hoặc `java -jar`): config-server → eureka → api-gateway → các business service Java. Postgres local
  5432 cần `user_db`, `content_db`, `assessment_db`, `access_db`, `notification_db`.
- `.env` ở root (gitignored) được compose nội suy; Gateway và mọi service Java nghiệp vụ import nó
  (`optional:file:../../.env[.properties]`), config-server và eureka-server thì không. Compose yêu cầu `LIBRARY_DB_PASSWORD`
  dù không bật `library-db`. Bài Listening cần `CONTENT_MEDIA_BASE_URL`. Game trên host cần đặt `CONTENT_SERVICE_URL=http://localhost:8082`;
  `LIBRARY_SERVICE_URL` đã mặc định `http://localhost:8081`.

## 8. Quyết định kiến trúc đã quan sát

Không có ADR chính thức; các quyết định sau suy ra từ code, README và lịch sử git.

| Quyết định | Lý do | Trade-off |
| --- | --- | --- |
| Gateway ký internal JWT; downstream chỉ tin internal JWT | Không tin identity header; tập trung validation trong `common-security` | Gateway và service phải cùng contract claim/issuer/role |
| Config Server native + Eureka | Cấu hình tập trung, route `lb://` thay host cố định | Khởi động phụ thuộc thứ tự hạ tầng |
| Domain model tách JPA entity (mapper + adapter) | Giữ JPA ngoài domain | Thêm mapper, lặp model |
| `Dockerfile.spring-service` theo `MODULE_PATH`; Config Server có Dockerfile riêng | Config Server cần đóng gói `config-repo` | Hai kiểu Dockerfile |
| Thay AI Learning Python bằng `learning-service` Java, chỉ giữ tính năng MVP (2026-10-01) | Một stack, dùng chung `common-security`, Eureka, Spring AMQP | Bỏ tutor, practice notebook, learner memory, sắp path bằng LLM |
| Bằng chứng theo user (`kp_evidence`), mastery tính khi đọc bằng `compute_mastery` port từ DeepTutor | Không cần aggregate path; consumer áp dụng được khi chưa có trạng thái học | Thứ tự bằng chứng dựa vào cột `ordinal` |
| Outbox + RabbitMQ cho assessment → learning; consumer có retry/DLQ | Không mất kết quả thi khi learning tạm lỗi | Thêm broker; outbox các service khác chưa có relay |
| Tách catalog/thư viện cá nhân sang library, activity/streak sang user | Mỗi nhóm dữ liệu có service và DB sở hữu rõ ràng, giữ public path | Gateway cần route theo nhóm; game chọn hai nguồn snapshot |

## 9. Pattern đang dùng

| Pattern | Nơi dùng |
| --- | --- |
| API Gateway, Configuration Server, Service Discovery | `infra/*` |
| Repository Adapter, Aggregate/Value Object, Mapper | Service Java |
| Ports & adapters (`application/port`) | assessment, game, library, learning, content (đọc nội dung học) |
| Transactional outbox + relay có publisher confirm | assessment (access/content/game mới có phần ghi) |
| Consumer với retry queue TTL và dead-letter queue | learning |
| Shared auto-configuration | `common-security` |
| Global exception handler → `ErrorResponse` | Service Java |
| WebSocket với ticket ngắn hạn | game |

## 10. Bài học kỹ thuật

- **Gom security downstream vào `common-security`** (`a18a44e`): bỏ `SecurityConfig` riêng của user-service, thêm
  auto-configuration, `CurrentUserProvider`, validator và role chuẩn dùng chung.
- **Docker theo Maven module** (`dac8b03`): root Dockerfile nhận `MODULE_PATH`; Config Server có Dockerfile riêng.
- **Bỏ service Python** (2026-10-01): sau khi đã port engine mastery (`3b97191`), MVP chỉ cần công thức `compute_mastery`;
  viết lại bằng Java bỏ được pipeline test, container và quy tắc riêng của Python.
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
| `.sdd/global/system-architecture.md`, `.sdd/constraints/global.md` (baseline 2026-09-18) ghi không có broker/outbox/learning service | Lỗi thời; `.sdd/global/constitution.md` vẫn là invariant cao nhất. Dữ kiện: code, `AGENTS.md`, tài liệu này |
| Gateway CORS chỉ expose `Authorization`, `Content-Type` | Browser không đọc được header khác (ví dụ `Retry-After`) |
| community có bảng `outbox_events` (V1) nhưng không dùng | Library không tạo bảng này; outbox của access/content/game chưa có relay |
| Consumer học chưa có test với RabbitMQ thật | Logic ack/nack/DLQ có unit test với channel giả; áp kết quả có test Postgres thật |
| Giá chấm Writing nằm ở hai nơi | Learning `learning.writing.point-cost` (3) cho bài luận trong bài học; assessment `grading_point_costs.WRITING` cho chấm qua `grading_jobs`. Đổi giá phải sửa cả hai |
| Bài Writing lưu ở hai nơi | Bài luận trong bài học ở `learning_db.lesson_writing_submissions`; Writing trong đề (sau MVP) ở `assessment_db.learner_submissions` |
