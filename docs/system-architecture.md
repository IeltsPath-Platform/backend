# Kiến trúc hệ thống IELTSPath (backend)

- Cập nhật lần cuối: 2026-10-25; thêm KP con theo mã lỗi, bằng chứng `error_trap`, chọn bộ theo bẫy, phản hồi `fixHint` và `ERROR_PATTERN` cho Learning
- Đọc khi cần hiểu toàn hệ thống. Quy tắc bắt buộc nằm ở [`AGENTS.md`](../AGENTS.md); hướng dẫn chạy chi tiết ở
  [`local-development.md`](local-development.md); tổng quan và bảng pattern ở [`README.md`](../README.md).
  Code là nguồn đúng khi tài liệu này lệch.

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
community --HTTP--> user, learning (streak)
content --HTTP--> library                 learning --HTTP--> content (/internal/learning-content/*)
```

`third_party/deeptutor` là bản clone DeepTutor dùng để đọc; công thức mastery của learning-service được port từ đó, không service nào build hay import thư mục này.

## 2. Service và dữ liệu

| Service | Bounded context | Database (chạy ở) | Route Gateway |
| --- | --- | --- | --- |
| `infra/config-server` | Cấu hình tập trung từ `infra/config-server/config-repo/` | — | — |
| `infra/eureka-server` | Service registry | — | — |
| `infra/api-gateway` | Ingress WebFlux: xác thực external JWT, ký internal JWT, routing, CORS, correlation id | — | — |
| `shared/common-security` | Thư viện: security servlet cho downstream, internal JWT, `CanonicalRoles`, `CurrentUserProvider` | — | — |
| `user-service` | Tài khoản, role, đăng nhập/refresh/logout, hồ sơ học viên (gồm avatar Supabase và múi giờ), đổi mật khẩu | `user_db` (Compose PostgreSQL host 5440; `learning_goals`, `learning_activities`, `study_*` và `streaks` còn lại sau khi dữ liệu chuyển sang learning, không còn được ghi hay đọc) | `/auth/**`, `/api/users/**` (trừ goal và study-time) |
| `content-service` | Course theo band, topic, knowledge point, câu hỏi (có version), gói nội dung/bài đọc, asset; snapshot grammar cho game | `content_db` (Compose PostgreSQL host 5440; V7 xóa năm bảng catalog) | `/api/content/**` trừ nhóm từ vựng/video |
| `library-service` | Catalog từ vựng/video (V1, năm bảng; V19 mở rộng CEFR/chủ đề/bộ từ), flashcard/deck, note, tiến độ video và đoạn đã lưu (V2, sáu bảng) | `library_db` (Compose PostgreSQL host 5440) | `/api/content/{videos,vocabulary}/**`, `/api/content/admin/vocabulary/**`, `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` |
| `assessment-service` | Lượt làm bài, chấm tự động và LLM cho thi topic/course, chấm EXAMINER, kết quả (có version), bài nộp, video practice; phát `AssessmentCompleted.v2` | `assessment_db` (Compose PostgreSQL host 5440) | `/api/assessments/**` |
| `access-service` | Gói, subscription, activation key, ví điểm và sổ điểm | `access_db` (Compose PostgreSQL host 5440) | `/api/access/**` |
| `game-service` | Phòng game, trận, phiên chơi, WebSocket realtime | `game_db` (Compose PostgreSQL host 5440) | `/api/games/**`, `/ws/games/**` |
| `community-service` | Bài viết, bình luận, reaction, kiểm duyệt | `community_db` (Compose PostgreSQL host 5440) | `/api/community/**` |
| `notification-service` | Thông báo bền vững từ Community events, REST unread/read và STOMP push | `notification_db` (Compose PostgreSQL host 5440) | `/api/notifications/**`, `/ws/notifications` |
| `learning-service` | Placement recommendation; topic path theo course (một chuỗi cho mọi skill), bài học, Practice theo bài, mastery theo KP và KP con mã lỗi, phản hồi fix hint, thang ôn tập, course/topic progress và giao mã đề cuối; mục tiêu học, activity, thời gian học và streak; nhận kết quả thi chính thức | `learning_db` (Compose PostgreSQL host 5440; `learner_placements`, `course_progress`, `course_test_assignments`, `learning_goals`, `learning_activities`, `study_*`; V40 tạo các bảng goal/activity/study) | `/api/learning/**`; Gateway viết lại `/api/users/me/learning-goals/**`, `/api/users/{id}/learning-goals/**`, `/api/users/me/study-sessions/**`, `/api/users/me/statistics/study-time`, `/api/learning-support/activities/**` sang đây |

Library owns the standalone vocabulary dictionary taxonomy in `vocabulary_topics` and vocabulary sets. These are separate from Content's curriculum `topics`; catalog reads do not call Content or synchronize curriculum topic IDs. Each sense's CEFR level lives in Library, and its IELTS band range is derived by Library domain rules rather than stored as a second level.

Content creates a child trap KP for each active error type with a parent KP. Its internal topic sequence includes children
only when Learning requests `includeErrorTraps=true`; normal curriculum reads exclude them. Learning maps tagged wrong
answers to those child IDs using its local catalog, stores weighted `error_trap` evidence without a Content request, and
uses the weakest eligible child to select parent practice sets. Result feedback resolves active error names and `fixHint`
after grading as a best-effort read. Report and tree responses nest children with learner evidence under their parent.

Chi tiết schema: [`.sdd/database/DATABASE_V5.md`](../.sdd/database/DATABASE_V5.md). Service Java tự chạy Flyway khi khởi
động (`src/main/resources/db/migration`).
Gateway giữ các path công khai nhưng trỏ từng nhóm tới chủ sở hữu mới; không còn route tổng quát
`/api/learning-support/**`. Library V2 có bốn FK `ON DELETE RESTRICT` tới catalog; không có bảng `outbox_events`.

## 3. Giao tiếp giữa service

### Đồng bộ (HTTP)

| Caller | Callee | Endpoint | Cấu hình base URL |
| --- | --- | --- | --- |
| assessment | content | `GET /internal/learning-content/package-versions/{id}` khi tạo attempt (ngoài transaction) | `CONTENT_SERVICE_URL` |
| content | library | `GET /api/content/vocabulary/senses`, `POST /api/content/admin/vocabulary/bulk` khi duyệt course draft và soạn lesson, bearer của tác giả | `LIBRARY_SERVICE_URL` (khóa `content.library.base-url`, mặc định `http://localhost:8081`) |
| community | user | `POST /internal/users/public-profiles`, `GET /internal/users/public-profiles/search`, bearer của người xem | `USER_SERVICE_URL` (khóa `community.user-service.base-url`) |
| community | learning | `POST /internal/learning/streaks` (streak của tối đa 100 học viên một lần, thêm cạnh hồ sơ công khai; lỗi thì ẩn streak, hồ sơ vẫn trả) | `LEARNING_SERVICE_URL` (khóa `community.learning-service.base-url`, mặc định `http://localhost:8086`) |
| game (`VOCABULARY`) | library | `POST /internal/game-content/snapshots` | `LIBRARY_SERVICE_URL` (mặc định `http://localhost:8081`) |
| game (`GRAMMAR`) | content | `POST /internal/game-content/snapshots` | `CONTENT_SERVICE_URL` |
| learning | content | `/internal/learning-content/{topic-sequence,topics/{id}/lessons,lessons/{id},lessons/{id}/practice-sets,topics/{id}/practice-sets,topics/{id}/test-packages,practice-sets/search,practice-sets/availability,package-versions/{id}}` | `CONTENT_SERVICE_URL` (mặc định `http://localhost:8082`) |
| learning | user | `GET /internal/users/{id}/study-timezone` khi mở phiên học (lỗi thì dùng múi giờ đã lưu rồi `Asia/Ho_Chi_Minh`), `POST /internal/users/display-names` cho bảng league | `USER_SERVICE_URL` (mặc định `http://localhost:8085`) |
| learning | assessment | `GET /api/assessments/me/statistics/study-context` xác minh bài thi của người học khi mở phiên học | `ASSESSMENT_SERVICE_URL` (mặc định `http://localhost:8083`) |
| learning | access | `GET /api/access/me/points` (số dư), `POST /internal/access/points/debit` (AI grading and durable course-skill unlock purchases) | `ACCESS_SERVICE_URL` (mặc định `http://localhost:8084`) |
| learning | LLM (ngoài hệ thống) | `POST {base}/chat/completions` (OpenAI-compatible) khi chấm bài luận, ngoài mọi transaction | `LEARNING_LLM_BASE_URL`, `LEARNING_LLM_API_KEY`, `LEARNING_LLM_MODEL` |
| learning | Azure Speech (ngoài hệ thống) | Speech SDK (`client-sdk` 1.53.0), một lượt nhận dạng liên tục cho cả ghi âm, đẩy audio qua stream (không ghi file); chạy ngoài transaction. Không qua `ResilientCall` (SDK tự mở kết nối); số phân tích chạy cùng lúc bị giới hạn bằng `learning.speech.max-concurrent` | `LEARNING_SPEECH_KEY`, `LEARNING_SPEECH_REGION`, `LEARNING_SPEECH_LOCALE` |
| assessment | LLM (ngoài hệ thống) | `POST {base}/chat/completions` cho essay đã nộp trong `TOPIC_GATE`/`COURSE_GATE`, ngoài transaction | `ASSESSMENT_LLM_BASE_URL`, `ASSESSMENT_LLM_API_KEY`, `ASSESSMENT_LLM_MODEL`; quota ngày `ASSESSMENT_LLM_DAILY_LIMIT` |

Các lời gọi HTTP đồng bộ dùng `shared/common-resilience`: retry tối đa một lần sau 200 ms chỉ cho lỗi kết nối/timeout và HTTP 5xx; chỉ `READ` và `IDEMPOTENT_WRITE` được retry. Circuit breaker dùng chung theo dịch vụ đích (content, access, user, assessment, library, learning, llm), mở 30 giây sau ngưỡng lỗi cấu hình; LLM có semaphore tối đa bốn lời gọi đồng thời. Lỗi breaker/bulkhead được client ánh xạ về contract lỗi của service gọi.

Gateway đặt connect timeout 2 giây và response timeout mặc định 15 giây. Route upload và AI của assessment dùng 120 giây; ba endpoint chấm essay của Learning dùng 60 giây và tier rate limit `ai`; route tạo AI draft của Content dùng 210 giây theo timeout LLM mặc định 180 giây cộng dự phòng retry và gateway.

Assessment, content, game và learning gọi thẳng service đích (không qua Gateway), kèm bearer của request và `X-Correlation-Id`.
Content dùng timeout kết nối 2 giây, đọc 5 giây khi gọi Library; Library không đáp được thì Content trả 503 mã `VOCABULARY_SERVICE_UNAVAILABLE`. Library không gọi Content. Hai nơi cài snapshot game theo
cùng [contract](contracts/game-content-snapshot-v1.md). Learning gọi Content qua API nội bộ (Gateway chặn `/internal/**`,
contract [`learning-content-internal-v1`](contracts/learning-content-internal-v1.md)); Content là nơi duy nhất ghép URL media.

Content resolve asset `PUBLIC` bằng `CONTENT_MEDIA_BASE_URL` của bucket `media`; asset `PREMIUM` được cấp URL ký từ
bucket riêng tư `media-premium`. Learning chỉ xin URL ký sau khi lesson gate cho phép đọc. URL `https://` đầy đủ vẫn
được giữ nguyên; Learning và Assessment chuyển tiếp URL đã resolve, không ghép lại. Transcript nằm trong
`content_assets.text_content`: Learning trả khi bài
hoàn thành, hoặc sau khi nộp practice attempt hay set ôn; Assessment giữ trong snapshot server và trả `sectionSolutions`
khi kết quả ≥ 70%.

### Lưu trữ file (Supabase Storage)

Các service dùng chung quy ước bucket và khóa object dưới đây. Cùng chủ và cùng chính sách thì dùng chung bucket, phân
biệt bằng tiền tố khóa; khác chính sách thì dùng bucket mới; khác service thì không dùng chung bucket.

| Bucket | Công khai | Dùng để | Giới hạn | Loại file (MIME) | Chủ | Khóa thường gặp | Trạng thái |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `avatars` | có | Ảnh đại diện học viên | 2 MB | png, jpeg, webp | user-service | `images/{userId}/{uuid}.{ext}` | đã có |
| `flashcards` | có | Ảnh thẻ, bìa bộ thẻ, bìa catalog từ vựng | 2 MB | png, jpeg, webp | library-service | `images/{userId}/{uuid}.{ext}`; bìa catalog `catalog-cover/{userId}/{uuid}.{ext}` | đã có; bìa catalog là kế hoạch |
| `community` | có | Ảnh bài đăng cộng đồng | 5 MB | png, jpeg, webp, gif | community-service | `community/post/{userId}/{uuid}.{ext}` | đã có |
| `media` | có | Audio và ảnh nội dung bài học | 25 MB | mpeg, mp4, wav, ogg, png, jpeg, webp | content-service | seed: `listening/demo/ls1.mp3`; upload API: `content-audio/{userId}/{uuid}.{ext}` | đã có |
| `speaking-recordings` | không | Ghi âm Speaking của học viên | 10 MB (nâng từ 5 MB; learning-service cần 10 MB, assessment-service vẫn giới hạn 5 MB trong code) | webm, mp4, ogg, mpeg, wav | assessment-service (tiền tố `speaking/`), learning-service (tiền tố `practice/`) | `speaking/{userId}/{uuid}.{ext}`; luyện tập chấm bằng AI: `practice/{userId}/{yyyy-MM-dd}_{submissionId}/part{n}_q{nn}[_r{k}].{ext}` | assessment đang dùng; learning: upload đã có, chấm chưa |
| `media-premium` | không | Audio PREMIUM, chỉ phát bằng URL ký | 25 MB | mpeg, mp4, wav, ogg | content-service | như `media` | đã có |

Content lưu `access_tier` cùng asset. `PUBLIC` dùng URL công khai của `media`; `PREMIUM` dùng URL ký của
`media-premium` với TTL 900 giây. Learning kiểm tra lesson gate trước khi Content tạo URL ký. URL ký chỉ giữ ở response,
không lưu vào DB hoặc log; frontend có thể xin URL mới khi URL hiện tại hết hạn.

Ví dụ URL công khai và URL ký, cùng các quy tắc tạo, kiểm tra khóa và giới hạn TTL, được ghi trong
[`shared/common-storage/README.md`](../shared/common-storage/README.md).

### Bất đồng bộ (RabbitMQ)

Tên exchange, routing key, header và hình dạng payload của event dùng chung nằm ở `shared/common-events`
(`PointsGrantMessaging`, `AssessmentMessaging`, `CommunityMessaging`, cùng record payload và helper `MessageRetry`,
`RetryTopology` cho hai queue grant). Đổi chúng phải đổi module và file trong `docs/contracts/` cùng lúc.
Service consumer sở hữu queue, retry queue và DLQ của mình, kể cả tên của chúng: topology retry của Learning
(assessment), Access (grant) và Notification (community, dùng header `community-retry-count`) giữ ở service.

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
dụng được ngay, kể cả khi học viên chưa gọi API học.

Luồng `SubscriptionGranted` của Access thông báo cho người dùng khi admin cấp Premium:

| Producer | Consumer | Queue / retry / DLQ | Contract |
| --- | --- | --- | --- |
| Access `GrantSubscriptionUseCase` ghi `SubscriptionGranted` vào `outbox_events` cùng transaction; `AccessOutboxRelay` publish với confirm tới exchange `access.events`, routing key `access.subscription-granted.v1` | Notification `SubscriptionGrantedListener` gọi `RecordSubscriptionGrantedUseCase`; `eventId` chống ghi thông báo trùng | `notification.subscription-granted.v1`; retry `notification.subscription-granted.v1.retry`; DLQ `notification.subscription-granted.v1.dlq` | [`subscription-granted-v1.md`](contracts/subscription-granted-v1.md) |

Lỗi xử lý tạm thời được chuyển qua retry queue; lỗi payload hoặc vi phạm contract được dead-letter. Retry queue trả message về đúng exchange nguồn với routing key ban đầu, nên consumer nhận lại đúng contract (`RetryTopologyTest`). Khi Community park message vào DLQ, consumer ghi routing key gốc vào header `x-original-routing-key`, vì routing key của dead-letter là tên DLQ; replay đọc header này để gửi lại đúng đường. Content và Game hiện vẫn ghi outbox cho event riêng nhưng chưa có relay.

### Correlation ID và metric

`common-observability` đặt `correlationId` trong MDC cho mỗi request và chuyển tiếp header `X-Correlation-Id` qua HTTP
client. Với event đi qua outbox, adapter ghi giá trị MDC vào cột nullable `correlation_id` ngay lúc lưu: `outbox_events`
ở Assessment, Community, Access và `reward_grants` ở Learning. Relay có thể chạy ở thread khác; publisher đọc id đã lưu
và gửi header RabbitMQ `x-correlation-id`, không đưa id vào payload. Event cũ hoặc do scheduler tạo không có id thì bỏ
header; listener tạo context mới và xóa MDC sau khi xử lý.

### Tracing và log tập trung (tùy chọn)

Tracing dùng Micrometer Tracing với OpenTelemetry và gửi OTLP. Mặc định tắt (`TRACING_ENABLED=false`), nên chạy thường không đổi hành vi. Khi bật, Gateway và các service gửi span tới Alloy (`OTLP_TRACES_ENDPOINT`, mặc định `http://alloy:4318/v1/traces`); Alloy chuyển trace tới Tempo. Alloy cũng đọc log container và đẩy vào Loki. Grafana đọc cả Tempo và Loki, nhảy hai chiều giữa log và trace.

- `traceId` nằm trong MDC và log JSON cùng `correlationId`. Hai id này khác nhau, và không id nào là label Loki.
- Trace context của event qua outbox được lưu trong cột `trace_context` cạnh `correlation_id` (Access V9, Assessment V22, Community V10, Learning V42), rồi đi qua RabbitMQ bằng header W3C `traceparent`.
- Hạ tầng nằm trong `docker-compose.observability.yml`, chạy cùng `docker-compose.yml` với profile `observability`. Cần `GRAFANA_ADMIN_PASSWORD` trong `.env`. Chỉ Grafana publish cổng, và chỉ trên `127.0.0.1:3000`; Loki, Tempo và Alloy không mở ra host.

Các service Java mở `/actuator/prometheus` trong mạng nội bộ; Gateway không route actuator. Metric
`dead_letter_messages{queue}` là gauge độ sâu DLQ hiện tại ở Notification, Access và Learning. Nếu broker không trả được
số lượng, gauge trả `NaN` để biểu thị trạng thái chưa biết.

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
GET /api/learning/placement-test -> Content internal placement-packages -> {packageId, packageVersionId};
  409 PLACEMENT_ALREADY_DONE khi learner_placements đã có dòng của user
(placement không bắt buộc: không route học nào đòi learner_placements)
GET /api/learning/courses -> refresh từ Content topic-sequence + course_progress + learner_placements
  -> danh sách course theo band, recommended lấy từ placement (chỉ gợi ý; mọi course luôn mở)
GET /api/learning/topics -> một lần gọi Content topic-sequence -> knowledge_point_catalog + topic_progress (course_id,
  skill, has_topic_test, sequence_order) -> trạng thái theo từng course: PASSED / IN_PROGRESS (topic đầu chưa đạt
  trong course) / LOCKED; mọi course đều mở và chuỗi không tách skill
GET /api/learning/lessons/{id} -> Content trả block VOCABULARY cùng danh sách vocabularySenseIds theo thứ tự;
  client có thể lấy chi tiết qua Library GET /api/content/vocabulary/senses?ids=...; block không chặn hoàn thành bài
POST /api/learning/lessons/{id}/exercises/{blockId}/submissions -> khóa theo user -> cổng (REVIEW_REQUIRED: bài ôn chờ
  của lesson trước hoặc cùng skill, TOPIC_LOCKED, LESSON_LOCKED, PRACTICE_REQUIRED: Practice lesson trước phải đạt)
  -> chấm answer-spec-v1 -> bằng chứng lần nộp đầu; xong bài không tạo bài ôn;
  topic không có thi cuối thì PASSED khi xong mọi bài
POST /api/learning/lessons/{id}/practice-attempts -> nộp -> lời giải; lần nộp đầu của package chưa lộ ghi bằng chứng
  practice_set; < 70% tạo bài ôn cho KP < 70% (còn package chưa lộ); lưu lesson_practice_passes khi bài qua Practice
GET /api/learning/topics/{id}/lessons -> topic LOCKED vẫn trả danh sách bài, mọi bài LOCKED (đọc trước lộ trình);
  mở bài của topic đó vẫn 403 TOPIC_LOCKED
GET /api/learning/reviews/{id} -> lý thuyết của KP; PRACTICE: một set chưa giao/chưa lộ, có hint (hết -> SKIPPED);
  THEORY: quick-check <= 3 câu của bài
POST /api/learning/reviews/{id}/submissions -> mỗi set một lần, luôn có lời giải; >=70% -> DONE; trượt lần đầu -> THEORY;
  các lần sau set mới đến khi đạt; SKIPPED chỉ khi hết package chưa lộ (bài ôn tùy chọn trượt -> SKIPPED)
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
Client -> Gateway -> user: POST/GET activity (giữ prefix /api/learning-support)
Client -> Gateway -> game: tạo phòng/phiên chơi -> chọn snapshot theo learningDomain
                              VOCABULARY -> library; GRAMMAR -> content
```

Năm bảng catalog và sáu bảng thư viện cá nhân thuộc `library_db`; mục tiêu học, `learning_activities` và dữ liệu thời gian học thuộc `learning_db` (các bảng cũ trong `user_db` còn đó, không dùng); streak do `learner_streaks` của learning giữ.
Game lưu snapshot để chơi; hai service cung cấp cùng request/response nội bộ.

### Kết quả thi → mastery

Học viên lấy mã đề từ learning (`POST /api/learning/topics/{id}/test-assignments`), tạo attempt bằng
`packageVersionId` (assessment đọc đề từ content, tự suy loại attempt), nộp bài → assessment tự chấm câu khách quan
(câu không chấm được thì chờ người chấm) → `AssessmentCompleted.v2` (mục 3) → Learning Service ghi bằng chứng, đề cuối đạt
≥ 70% thì topic PASSED; `GET /api/learning/mastery` và `GET /api/learning/topics` phản ánh kết quả. Học viên xem điểm,
đúng/sai ở `GET /api/assessments/attempts/{id}/result`; lời giải chỉ hiện khi đạt ≥ 70%.

Assessment không gọi User lấy goal; attempt mới và event có `learning_goal_id` null. Consumer Learning không gọi
HTTP và không cần path hoặc trạng thái học có sẵn. Trong một transaction khóa theo user, version bằng/cũ bị bỏ qua;
version cao hơn thay evidence của attempt. `PLACEMENT` lưu `overall_band` mới nhất (Assessment tự chấm đề placement 4 kỹ năng và tính band: Listening/Reading theo
% đúng, Writing theo band LLM hoặc band mặc định khi LLM không chấm được, Speaking theo band cố định cấu hình; band tổng là
trung bình làm tròn 0.5), không ghi mastery hay đổi trạng thái topic;
band chỉ dùng để đánh dấu course được gợi ý. `TOPIC_GATE` tìm assignment cùng user/package version, còn mở và đã giao
trước khi attempt hoàn tất; lần giao được consume cả khi trượt; đạt ≥70% ghi `passed_at` một chiều. `COURSE_GATE` dùng
quy tắc tương tự cho `course_test_assignments`; đạt ≥70% ghi `course_progress.passed_at`. ACK sau commit; topic kế trong
cùng course mở theo trạng thái suy ra khi đọc. Course test không chặn topic hay course khác.

## Scheduler và nhiều instance

Backend hiện có 21 phương thức `@Scheduled` trong 20 job. Cơ chế chống xử lý trùng theo mã đang chạy:

| Service | Job / lịch | Cơ chế cạnh tranh | HTTP/LLM khi giữ khóa hoặc transaction |
|---|---|---|---|
| game | `RocketRoundScheduler` | Khóa hàng trận (`FOR UPDATE`) và kiểm trạng thái vòng khi settle; hai lịch trùng không đóng vòng hai lần | Không |
| game | `SoloTimeoutScheduler` | Khóa trận và người chơi (`FOR UPDATE OF m,p`), kiểm deadline; hai lịch trùng chỉ trừ một mạng | Không |
| game | `OutboxRetentionJob` | Xóa theo ngưỡng thời gian; chạy lặp an toàn | Không |
| game | `RoomEventChannelSupervisor` | Mỗi instance tự đăng ký lại Redis pub/sub của nó | Không |
| notification | `NoticePushChannelConfig` | Mỗi instance tự đăng ký lại Redis pub/sub của nó | Không |
| assessment | `OutboxRelayScheduler` | `FOR UPDATE SKIP LOCKED`; publish broker trong transaction ngắn để giữ claim tới khi gửi xong | Broker trong transaction ngắn |
| access | `AccessOutboxRelayScheduler` | `FOR UPDATE SKIP LOCKED`; publish broker trong transaction ngắn | Broker trong transaction ngắn |
| community | `CommunityOutboxRelayScheduler` | `FOR UPDATE SKIP LOCKED`; publish broker trong transaction ngắn | Broker trong transaction ngắn |
| assessment | `GateEssayGradingScheduler` | Claim batch bằng `FOR UPDATE SKIP LOCKED` trong transaction; cập nhật job hoàn tất trong transaction riêng | LLM ngoài transaction claim và transaction ghi kết quả |
| assessment | `GradingDeadlineScheduler` | Store chọn các job bằng `FOR UPDATE SKIP LOCKED`; ghi thông báo/outbox trong transaction | Không |
| assessment | `SpeakingRecordingRetentionScheduler` | Xóa theo ngưỡng thời gian; chạy lặp an toàn | Không |
| content | `ScheduledPublishJob` | Advisory lock tránh hai instance publish cùng đợt | Không |
| content | `AiDraftScheduler` | Claim bản nháp bằng `FOR UPDATE SKIP LOCKED`, commit claim trước khi gọi generator; lưu kết quả riêng | Generator/LLM ngoài transaction claim |
| learning | `LeagueCloseJob` | Advisory transaction lock quanh cập nhật tuần | Không |
| learning | `InactivityFlagJob` | Advisory transaction lock quanh tính và ghi cờ | Không |
| learning | `SnapshotJob` (weekly) | Advisory transaction lock theo kỳ snapshot | Không |
| learning | `SnapshotJob` (monthly) | Advisory transaction lock theo kỳ snapshot | Không |
| learning | `RewardGrantRelayJob` | Advisory transaction lock và `SKIP LOCKED` cho batch | Broker trong transaction ngắn |
| learning | `MasteryDecayJob` | Session advisory lock; các batch ghi theo transaction riêng | Không |
| learning | `WorkItemSyncJob` | Đọc nguồn và lấy metadata Content trước; transaction ngắn lấy advisory lock rồi ghi snapshot; unique key loại mục trùng | Content HTTP ngoài transaction/khóa |
| user | `LoginHistoryPurgeScheduler` | Xóa theo cutoff thời gian; chạy lặp an toàn | Không |

Game không cần thêm claim riêng: hàng trận được khóa trước khi cập nhật và trạng thái được kiểm lại. Relay dùng row claim để instance khác có thể xử lý các hàng còn lại. Việc đọc nguồn của `WorkItemSyncJob` có thể chạy đồng thời; phần ghi được tuần tự hóa và repository bỏ qua work item đã tồn tại.

Redis pub/sub của `game.room-events` và `notification.push` là kênh đẩy không bền; API/DB vẫn là nguồn dữ liệu. Vé WebSocket game lưu với TTL dưới `game:ws-ticket:*`. Compose cho phép scale service ứng dụng bằng `--scale` vì không gán `container_name` và không bind các cổng nội bộ ra host; gateway vẫn chạy một instance.

## Dừng một instance

Các service Spring dùng graceful shutdown 30 giây; scheduler đợi công việc đang chạy tối đa 20 giây. Compose đặt
`stop_grace_period: 40s`, đủ cho khoảng drain của Spring và thời gian kết thúc JVM. Eureka server cập nhật cache mỗi
3 giây, quét instance hết hạn mỗi 5 giây; client fetch registry mỗi 5 giây với heartbeat 10 giây và lease 30 giây.
Gateway cache danh sách instance 5 giây. JVM của service dùng DNS TTL 5 giây cho các hostname trong Compose.

Các giá trị này rút ngắn thời gian gửi request tới replica đang dừng nhưng Compose không bảo đảm zero-downtime. Khi
deploy VPS, reverse proxy cần health check chủ động, loại instance khỏi pool trước khi dừng và lần lượt thay replica;
cần đặt thời gian drain cho proxy và xử lý riêng kết nối WebSocket. Chưa có phép đo stack thật cho cửa sổ định tuyến;
Claude sẽ ghi số request lỗi và thời gian dừng tới lỗi cuối sau khi chạy phép đo nhiều replica.

## Kiểm chứng reliability

| Kịch bản | Kiểm chứng tự động hiện có hoặc được thêm | Ranh giới |
|---|---|---|
| Server và scheduler chờ công việc trước khi dừng | `GracefulShutdownTest`, `SchedulerShutdownTest` | Kiểm tra config repo và scheduler đang chạy |
| Subscription grant nhận lại cùng khóa | `GrantSubscriptionIdempotencyTest` | PostgreSQL Testcontainers, gồm cạnh tranh và outbox |
| Mở breaker và hồi phục downstream | `RestLearningContentClientResilienceTest`, `ResilientCallTest` | Unit/integration test; phép dừng service qua gateway vẫn cần kiểm tay |
| Correlation HTTP → downstream content/assessment → outbox → relay → consumer | `CorrelationAcrossServicesReliabilityTest.sameCorrelationIdInHttpAndConsumerLogs`; `CommunityOutboxRelayIntegrationTest.correlationSurvivesHttpOutboxRelayAndBrokerConsumerLogs` | Learning kiểm hai HTTP client, reward outbox production, relay/publisher production và log consumer với PostgreSQL/RabbitMQ Testcontainers; Community kiểm luồng outbox riêng |
| Auth endpoint rate-limit mapping | `AuthRouteConsistencyTest` | `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`, `/api/users/register` thuộc tier `auth`; `/auth/logout` thuộc tier `default` |
| Message được retry và đưa lại từ DLQ | `SubscriptionGrantedBrokerIntegrationTest`, `ReplayedMessageIsProcessedTest` | RabbitMQ Testcontainers |
| Dừng một replica còn replica khác | Phép đo in-flight và tải Compose trong report E; trạng thái idempotent được đối chiếu bằng `OutboxRelayConcurrencyTest`, `CommunityOutboxRelayConcurrencyTest`, `GrantSubscriptionIdempotencyTest.concurrentSameKeyGrantsOnce`, `UnlockCourseFailureWindowTest.retryWithSameKeyWhilePendingDoesNotCreateIntent` | Request đang xử lý hoàn tất 200 và replica còn lại tiếp tục phục vụ; kiểm tra idempotency chạy riêng, không gộp vào cùng phép dừng replica |

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
| Block từ vựng trong bài học (2026-10-21) chỉ cung cấp sense IDs; flashcard SRS dùng `VOCABULARY_SENSE`/`vocabularySenseId` riêng | Nội dung từ vựng bổ trợ, không chặn hoàn thành; SRS không ghi `kp_evidence`, mastery `VOCABULARY` đến từ bài tập được chấm | Tạo card/deck UX và chính sách hoàn thành từ vựng chưa thuộc phạm vi |
| Outbox + RabbitMQ cho assessment → learning; consumer có retry/DLQ | Không mất kết quả thi khi learning tạm lỗi | Thêm broker; outbox các service khác chưa có relay |
| Tách catalog/thư viện cá nhân sang library, activity sang user (streak sau đó về learning) | Mỗi nhóm dữ liệu có service và DB sở hữu rõ ràng, giữ public path | Gateway cần route theo nhóm; game chọn hai nguồn snapshot |

## 9. Pattern đang dùng

Bảng tổng hợp đầy đủ theo nhóm (resilience, idempotency, claim `SKIP LOCKED`, tracing…) nằm ở [`README.md`](../README.md); dưới đây là các pattern có chi tiết cài đặt riêng.

| Pattern | Nơi dùng |
| --- | --- |
| API Gateway, Configuration Server, Service Discovery | `infra/*` |
| Repository Adapter, Aggregate/Value Object, Mapper | Service Java |
| Ports & adapters (`application/port`) | assessment, game, library, learning, content (đọc nội dung học) |
| Transactional outbox + relay có publisher confirm | assessment, access, community, learning (`reward_grants`); content và game mới có phần ghi, chưa có relay |
| Consumer với retry queue TTL và dead-letter queue | learning, access (grant), notification |
| Shared auto-configuration | `common-security` |
| Global exception handler → `ErrorResponse` | Service Java |
| WebSocket với ticket ngắn hạn | game |
| Rate limit dùng chung qua Redis | API Gateway, user-service, community-service |

Rate limit được cấu hình trong `infra/config-server/config-repo/api-gateway.yaml` theo các tier `auth`,
`upload`, `ai` và `default`; mỗi tier có `replenishRate` (token/giây) và `burstCapacity`. Gateway ưu tiên
khóa theo subject của JWT; request ẩn danh dùng IP, chỉ tin `X-Forwarded-For` từ proxy thuộc
`gateway.rate-limit.trusted-proxies`. Với header được tin, gateway duyệt các hop từ phải sang trái, bỏ qua proxy
được tin cậy và chọn IP đầu tiên không thuộc danh sách; nếu không còn IP phù hợp thì dùng remote address.
Tier `auth` áp dụng cho `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password` và
`/api/users/register`; `/auth/logout` thuộc tier `default`. Redis dùng chung trạng thái giữa các gateway instance. Khi Redis lỗi,
gateway cho request thường đi tiếp và ghi metric cảnh báo; login bị fail-closed với HTTP 503 để tránh bỏ qua
giới hạn đăng nhập. Upload avatar, media và ảnh community dùng Redis limiter riêng theo user, với mức lần
lượt 10, 30 và 30 upload mỗi giờ; lỗi Redis ở các limiter upload được fail-open để lỗi hạ tầng không chặn
hoạt động upload.

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
| Content V7 xóa vĩnh viễn năm bảng catalog | Chỉ chạy trên Testcontainers cho tới khi duyệt chạy trên `content_db` dùng chung |
| Outbox của access/content/game không có relay | Event chưa được phát |
| Notification nhận Community event qua RabbitMQ và lưu idempotent theo event id | WebSocket chỉ push sau commit; REST là nguồn đồng bộ lại khi reconnect |
| Secret có giá trị fallback trong config-repo được track | Thiếu biến env thì service dùng secret công khai trong repo |
| `.sdd/global/system-architecture.md`, `.sdd/constraints/global.md` (baseline 2026-09-18) ghi không có broker/outbox/learning service | Lỗi thời; `.sdd/global/constitution.md` vẫn là invariant cao nhất. Dữ kiện: code, `AGENTS.md`, tài liệu này |
| Gateway CORS chỉ expose `Authorization`, `Content-Type` | Browser không đọc được header khác (ví dụ `Retry-After`) |
| community có bảng `outbox_events` (V1) nhưng không dùng | Library không tạo bảng này; outbox của access/content/game chưa có relay |
| Consumer học chưa có automated integration test với RabbitMQ thật | Logic ack/nack/DLQ có unit test với channel giả; áp kết quả có test Postgres thật. [Live E2E 2026-10-02](../plans/260929-1640-lesson-learning-pipeline-mvp/reports/e2e-261002-foundation-learning-pipeline.md) đã kiểm outbox → broker → consumer và replay DLQ với broker thật |
| Giá chấm Writing nằm ở hai nơi | Learning `learning.writing.point-cost` (3) cho bài luận trong bài học; assessment `grading_point_costs.WRITING` cho chấm qua `grading_jobs`. Đổi giá phải sửa cả hai |
| Bài Writing lưu ở hai nơi | Bài luận trong bài học ở `learning_db.lesson_writing_submissions`; Writing trong đề (sau MVP) ở `assessment_db.learner_submissions` |
| Ảnh biểu đồ Task 1 mới dùng tham chiếu media | Content nhận URL `https://` hoặc data URI ảnh Base64 và trả `mediaUrl`; chưa có luồng upload hay tích hợp object storage cho ảnh. Seed dùng SVG data URI; grader đọc `chartFacts`, không đọc ảnh |
| Bản public cũ của audio `listening/demo/ls1.mp3` còn trong `media` | Lesson dùng chung audio đã chuyển sang `PREMIUM` và phát qua URL ký sau lesson gate; Placement dùng khóa public riêng. Xóa object public cũ còn chờ phê duyệt |
| Writing trong Practice và gate dùng chính sách phí khác nhau | Practice trừ điểm; essay trong `TOPIC_GATE`/`COURSE_GATE` chấm miễn phí và có quota riêng ở Assessment |
| Một số ghi Writing chưa theo quy tắc khóa mọi lượt ghi theo user | Bắt đầu/kết thúc chấm có transaction và advisory lock; quota, lưu grade và lỗi trung gian dùng SQL nguyên tử có điều kiện ngoài các transaction này. Dữ kiện hiện tại, không thay quy tắc AGENTS.md §3.8 |
| Cổng bài học là authority cho luồng học | `LessonAccessGate` giữ tiến trình review/topic/bài; `PaywallGate` mở 3 bài đầu mỗi skill, unlock skill/combo, tiến độ grandfathered, rồi Premium. Access outage chỉ chặn nội dung cần kiểm tra Premium. |
| Sơ đồ §1 còn cạnh assessment → user từ baseline cũ | Tạo attempt hiện chỉ đọc Content; không gọi User để lấy goal (xem §3 và §6) |
