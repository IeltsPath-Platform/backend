# Kiến trúc hệ thống IELTSPath (backend)

- Cập nhật lần cuối: 2026-10-07; dữ kiện đã kiểm với code sau khi có course theo band, placement recommendation và thi cuối course
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
| `user-service` | Tài khoản, role, đăng nhập/refresh/logout, hồ sơ học viên, learning goal, activity và streak | `user_db` (Compose PostgreSQL host 5440; V5 tạo `learning_activities`, `streaks`) | `/auth/**`, `/api/users/**`, `/api/learning-support/{activities,streak}/**` |
| `content-service` | Course theo band, topic, knowledge point, câu hỏi (có version), gói nội dung/bài đọc, asset; kiểm topic cho library và snapshot grammar cho game | `content_db` (Compose PostgreSQL host 5440; V7 xóa năm bảng catalog) | `/api/content/**` trừ nhóm từ vựng/video |
| `library-service` | Catalog từ vựng/video (V1, năm bảng), flashcard/deck, note, tiến độ video và đoạn đã lưu (V2, sáu bảng) | `library_db` (Compose PostgreSQL host 5440) | `/api/content/{videos,vocabulary}/**`, `/api/content/admin/vocabulary/**`, `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` |
| `assessment-service` | Lượt làm bài, chấm tự động và LLM cho thi topic/course, chấm EXAMINER, kết quả (có version), bài nộp, video practice; phát `AssessmentCompleted.v2` | `assessment_db` (Compose PostgreSQL host 5440) | `/api/assessments/**` |
| `access-service` | Gói, subscription, activation key, ví điểm và sổ điểm | `access_db` (Compose PostgreSQL host 5440) | `/api/access/**` |
| `game-service` | Phòng game, trận, phiên chơi, WebSocket realtime | `game_db` (Compose PostgreSQL host 5440) | `/api/games/**`, `/ws/games/**` |
| `community-service` | Bài viết, bình luận, reaction, kiểm duyệt | `community_db` (Compose PostgreSQL host 5440) | `/api/community/**` |
| `notification-service` | Chưa triển khai (chỉ khung package) | Chưa có database runtime | Chưa có route hoạt động |
| `learning-service` | Placement recommendation; topic path theo course (một chuỗi cho mọi skill), bài học, Practice theo bài, mastery theo KP, thang ôn tập, course/topic progress và giao mã đề cuối; nhận kết quả thi chính thức | `learning_db` (Compose PostgreSQL host 5440; `learner_placements`, `course_progress`, `course_test_assignments`) | `/api/learning/**` |

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
| learning | content | `/internal/learning-content/{topic-sequence,topics/{id}/lessons,lessons/{id},lessons/{id}/practice-sets,topics/{id}/practice-sets,topics/{id}/test-packages,practice-sets/search,practice-sets/availability,package-versions/{id}}` | `CONTENT_SERVICE_URL` (mặc định `http://localhost:8082`) |
| learning | access | `GET /api/access/me/points` (số dư), `POST /internal/access/points/debit` (trừ 3 point sau khi chấm bài luận), bearer của học viên | `ACCESS_SERVICE_URL` (mặc định `http://localhost:8084`) |
| learning | LLM (ngoài hệ thống) | `POST {base}/chat/completions` (OpenAI-compatible) khi chấm bài luận, ngoài mọi transaction | `LEARNING_LLM_BASE_URL`, `LEARNING_LLM_API_KEY`, `LEARNING_LLM_MODEL` |
| assessment | LLM (ngoài hệ thống) | `POST {base}/chat/completions` cho essay đã nộp trong `TOPIC_GATE`/`COURSE_GATE`, ngoài transaction | `ASSESSMENT_LLM_BASE_URL`, `ASSESSMENT_LLM_API_KEY`, `ASSESSMENT_LLM_MODEL`; quota ngày `ASSESSMENT_LLM_DAILY_LIMIT` |

Assessment, library, game và learning gọi thẳng service đích (không qua Gateway), kèm bearer của request và `X-Correlation-Id`.
Library dùng timeout kết nối 2 giây, đọc 5 giây; lỗi Content khi kiểm topic trả 503. Hai nơi cài snapshot game theo
cùng [contract](contracts/game-content-snapshot-v1.md). Learning gọi Content qua API nội bộ (Gateway chặn `/internal/**`,
contract [`learning-content-internal-v1`](contracts/learning-content-internal-v1.md)); Content là nơi duy nhất ghép URL media.

Listening dùng mp3 do team tự upload lên bucket cloud public-read. Content resolve key với `CONTENT_MEDIA_BASE_URL`
(prefix `https://`) thành `mediaUrl`; URL `https://` đầy đủ được giữ nguyên, không cần base. Learning và Assessment
chuyển tiếp URL đã resolve, không ghép lại. Transcript nằm trong `content_assets.text_content`: Learning trả khi bài
hoàn thành, hoặc sau khi nộp practice attempt hay set ôn; Assessment giữ trong snapshot server và trả `sectionSolutions`
khi kết quả ≥ 70%.

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
     (mastery < 0.6; stage ban đầu xét lúc học viên mở bài ôn, vì consumer không gọi Content)
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
GET /api/learning/courses -> refresh từ Content topic-sequence + course_progress + learner_placements
  -> danh sách course theo band, recommended lấy từ placement (chỉ gợi ý)
GET /api/learning/topics -> một lần gọi Content topic-sequence -> knowledge_point_catalog + topic_progress (course_id,
  skill, has_topic_test, sequence_order) -> trạng thái theo từng course: PASSED / IN_PROGRESS (topic đầu chưa đạt
  trong course) / LOCKED; mọi course đều mở và chuỗi không tách skill
POST /api/learning/lessons/{id}/exercises/{blockId}/submissions -> khóa theo user -> cổng (REVIEW_REQUIRED cùng skill,
  TOPIC_LOCKED, LESSON_LOCKED) -> chấm answer-spec-v1 -> bằng chứng lần nộp đầu; xong bài không tạo bài ôn;
  topic không có thi cuối thì PASSED khi xong mọi bài
POST /api/learning/lessons/{id}/practice-attempts -> nộp -> lời giải; lần nộp đầu của package chưa lộ ghi bằng chứng
  practice_set; < 70% tạo bài ôn cho KP < 70% (còn package chưa lộ); lưu lesson_practice_passes khi bài qua Practice
GET /api/learning/reviews/{id} -> lý thuyết của KP; PRACTICE: một set chưa giao/chưa lộ, có hint (hết -> SKIPPED);
  THEORY: quick-check <= 3 câu của bài
POST /api/learning/reviews/{id}/submissions -> mỗi set một lần, luôn có lời giải; >=70% -> DONE; trượt -> THEORY;
  trượt set thứ hai -> SKIPPED
POST /api/learning/reviews/{id}/theory-check -> không ghi bằng chứng -> PRACTICE
POST /api/learning/topics/{id}/test-assignments -> REVIEW_REQUIRED, PRACTICE_REQUIRED (mọi bài phải qua Practice),
  TEST_LOCKED -> một mã đề dùng một lần, xoay vòng theo package
POST /api/learning/courses/{id}/test-assignments -> cần mọi topic trong course PASSED -> một COURSE_TEST assignment
  -> Assessment attempt COURSE_GATE -> đạt >= 70% thì course_progress PASSED; không chặn topic khác
```

Khi nộp `TOPIC_GATE`/`COURSE_GATE`, essay đã gửi qua `/api/assessments/submissions` được xếp thành grading job miễn phí.
Scheduler claim job trong transaction ngắn, gọi Assessment LLM ngoài transaction rồi ghi band và hoàn tất result/outbox
trong transaction mới. Thiếu cấu hình, lỗi LLM hoặc hết quota ngày chuyển essay sang hàng chờ EXAMINER; essay không nộp
được tính 0 và không tạo job. `MOCK` tiếp tục do EXAMINER chấm.

Gợi ý Reading (`question_versions.hint`, Content V13) do Content lưu và trả qua
`/internal/learning-content/lessons/{id}`; Learning Service Java quyết định hiển thị cho câu `FILL` hoặc `CHOICE`
hợp lệ có ≥3 lựa chọn (TFNG hỗ trợ options thiếu/rỗng). Câu từng sai trong cùng user/bài/khối mở gợi ý khi khối chưa đạt,
giữ cả khi câu đúng ở lần sau nhưng khối vẫn trượt; khối đạt thì `hint = null`. Lịch sử lấy từ mọi
`lesson_exercise_submissions.response.results`; replay `requestId` trả response đã lưu, không đổi evidence/mastery lần nộp đầu.

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

Assessment không gọi User lấy goal; attempt mới và event có `learning_goal_id` null. Consumer Learning không gọi
HTTP và không cần path hoặc trạng thái học có sẵn. Trong một transaction khóa theo user, version bằng/cũ bị bỏ qua;
version cao hơn thay evidence của attempt. `PLACEMENT` lưu `overall_band` mới nhất, không ghi mastery hay đổi trạng thái topic;
band chỉ dùng để đánh dấu course được gợi ý. `TOPIC_GATE` tìm assignment cùng user/package version, còn mở và đã giao
trước khi attempt hoàn tất; lần giao được consume cả khi trượt; đạt ≥70% ghi `passed_at` một chiều. `COURSE_GATE` dùng
quy tắc tương tự cho `course_test_assignments`; đạt ≥70% ghi `course_progress.passed_at`. ACK sau commit; topic kế trong
cùng course mở theo trạng thái suy ra khi đọc. Course test không chặn topic hay course khác.

## 7. Chạy local (tóm tắt)

- Root `docker-compose.yml` khởi chạy PostgreSQL, RabbitMQ, Config Server, Eureka, Gateway và tất cả service đã triển khai.
  PostgreSQL ở host port 5440, với database riêng mỗi bounded context; `infra/postgres/init-databases.sql` tạo database,
  sau đó Flyway của từng service tạo schema và seed. User/Access nạp tài khoản/điểm demo mặc định; đặt
  `DEMO_DATA_ENABLED=false` để tắt.
- Dùng `docker compose up -d --build` cho toàn stack. Khi chạy Java trên host, dùng `docker compose up -d postgres rabbitmq`
  rồi trỏ `*_DB_URL` tới `localhost:5440/<service>_db`. `.env` ở root được Compose và các service Java nghiệp vụ nạp;
  Config Server và Eureka không nạp file này. Bài Listening cần `CONTENT_MEDIA_BASE_URL`. Game chạy trên host cần đặt
  `CONTENT_SERVICE_URL=http://localhost:8082`; `LIBRARY_SERVICE_URL` mặc định `http://localhost:8081`.

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
| Secret có giá trị fallback trong config-repo được track | Thiếu biến env thì service dùng secret công khai trong repo |
| `.sdd/global/system-architecture.md`, `.sdd/constraints/global.md` (baseline 2026-09-18) ghi không có broker/outbox/learning service | Lỗi thời; `.sdd/global/constitution.md` vẫn là invariant cao nhất. Dữ kiện: code, `AGENTS.md`, tài liệu này |
| Gateway CORS chỉ expose `Authorization`, `Content-Type` | Browser không đọc được header khác (ví dụ `Retry-After`) |
| community có bảng `outbox_events` (V1) nhưng không dùng | Library không tạo bảng này; outbox của access/content/game chưa có relay |
| Consumer học chưa có automated integration test với RabbitMQ thật | Logic ack/nack/DLQ có unit test với channel giả; áp kết quả có test Postgres thật. [Live E2E 2026-10-02](../plans/260929-1640-lesson-learning-pipeline-mvp/reports/e2e-261002-foundation-learning-pipeline.md) đã kiểm outbox → broker → consumer và replay DLQ với broker thật |
| Giá chấm Writing nằm ở hai nơi | Learning `learning.writing.point-cost` (3) cho bài luận trong bài học; assessment `grading_point_costs.WRITING` cho chấm qua `grading_jobs`. Đổi giá phải sửa cả hai |
| Bài Writing lưu ở hai nơi | Bài luận trong bài học ở `learning_db.lesson_writing_submissions`; Writing trong đề (sau MVP) ở `assessment_db.learner_submissions` |
| Ảnh biểu đồ Task 1 mới dùng tham chiếu media | Content nhận URL `https://` hoặc data URI ảnh Base64 và trả `mediaUrl`; chưa có luồng upload hay tích hợp object storage cho ảnh. Seed dùng SVG data URI; grader đọc `chartFacts`, không đọc ảnh |
| Media Listening dùng bucket public-read | Team tự upload 8 mp3 của seed; backend chưa có upload API, signed URL hay giới hạn lượt nghe. Thiếu `CONTENT_MEDIA_BASE_URL` thì các reference dạng key không resolve được (`INVALID_MEDIA_REFERENCE`); URL `https://` đầy đủ không cần base |
| Writing trong Practice và gate dùng chính sách phí khác nhau | Practice trừ điểm; essay trong `TOPIC_GATE`/`COURSE_GATE` chấm miễn phí và có quota riêng ở Assessment |
| Một số ghi Writing chưa theo quy tắc khóa mọi lượt ghi theo user | Bắt đầu/kết thúc chấm có transaction và advisory lock; quota, lưu grade và lỗi trung gian dùng SQL nguyên tử có điều kiện ngoài các transaction này. Dữ kiện hiện tại, không thay quy tắc AGENTS.md §3.8 |
| Cổng bài học là authority cho luồng học | `LessonAccessGate` kiểm review/topic/bài trước; runtime Java MVP không có tutor hay `next_objective` |
| Sơ đồ §1 còn cạnh assessment → user từ baseline cũ | Tạo attempt hiện chỉ đọc Content; không gọi User để lấy goal (xem §3 và §6) |
