---
title: "Scout — đọc sâu toàn bộ codebase"
date: 2026-09-28 21:33
branch: main
commit: 2079964
method: graphify update (7.428 node first-party) + 6 Explore agent song song, không sửa code
supersedes: plans/reports/scout-260927-2037-graphify-codebase-map-report.md (bản đồ graph, nông hơn)
---

# Scout — đọc sâu toàn bộ codebase

## Tóm tắt

- Code không đổi kể từ `79f9fd6` (quota tutor). Các commit sau đó (`69285ec`, `ebda1b4`, `2079964`) chỉ đổi docs/plans → `docs/system-architecture.md` vẫn khớp code.
- Layering Java sạch ở cả 8 service: domain không import Spring/JPA, application không import api/infrastructure. Ngoại lệ duy nhất: user-service `application` import `user.config.AuthTokenProperties`.
- Rủi ro lớn nhất là **phân quyền**: gateway chỉ `anyExchange().authenticated()`, không có role rule; nhiều service dựa hoàn toàn vào `@PreAuthorize` và có chỗ thiếu.
- Outbox: chỉ assessment có relay. access, game ghi event nhưng không ai phát; content, learning-support, community có bảng nhưng không ghi.
- Nhiều lifecycle mới làm nửa đường (GradingJob, VideoPractice, question publish, room/match game).

Đánh dấu ✅ = main agent đã tự kiểm lại trong code; còn lại là kết quả agent, chưa kiểm độc lập.

## 1. Bản đồ service

| Service | Trạng thái | Aggregate chính | Guard | Test |
| --- | --- | --- | --- | --- |
| api-gateway | Đủ | — | JWT ngoài HS256; không role rule | InternalJwtServiceTest |
| common-security | Đủ | — | `@EnableMethodSecurity`, validator issuer/UUID/role | validators, provider |
| user-service | Đủ cho auth; thiếu verify email, OAuth login | User, LearningGoal, RefreshToken, AccountActionToken | Admin controller có `@PreAuthorize` class-level | Tốt (domain, use case, security, migration) |
| access-service | Phần lớn đủ | Subscription, PointWallet, ActivationKey | Admin OK; **Internal không guard** | Controller test standalone, không test security |
| content-service | Thiếu publish question | Topic, KP, Question(+version), ContentPackage, Vocabulary, LearningVideo, Asset | Chỉ Topic, Vocabulary, Reading có role | 62 test; controller standalone |
| assessment-service | Luồng thi + chấm tay + event đủ; GradingJob/VideoPractice dừng ở trạng thái đầu | AssessmentAttempt, AssessmentResult (versioned) | Chỉ GradingController (EXAMINER, ADMIN) | Use case, outbox Testcontainers |
| learning-support | CRUD đủ; streak do client ghi | Flashcard, Deck, Note, VideoProgress, SavedSegment, Activity, Streak | Không role; scope theo userId | 54 test |
| game-service | Solo + multiplayer chạy; lifecycle room/match thiếu | GameRoom, GameMatch, GameSession | Không role | Domain, evaluator, schema |
| community-service | Đủ | Post, Comment, Reaction | Admin check ở application | 27 test |
| notification-service | Khung rỗng | — | — | 1 test trivial |
| ai-learning (Python) | Đủ: path, tutor SSE, practice, consumer, quota | mastery path (DeepTutor port), tutor session/turn | JWT nội bộ, không role | Rộng; cần Postgres/RabbitMQ |

## 2. Phát hiện ưu tiên

### P0 — bảo mật

1. ✅ **access-service: user bất kỳ đã đăng nhập tự cộng điểm được cho bất kỳ ai.** `InternalAccessController` (`/api/access/points/debit|refund`, `/users/{userId}/entitlement|consume-human-grading`) không có `@PreAuthorize`, gateway route `/api/access/**`. `RefundPointsUseCase` credit thẳng, không kiểm tra có debit gốc → mint điểm vô hạn. Chưa có caller first-party nào.
2. ✅ **content-service: CUSTOMER tạo và publish được nội dung.** 14 endpoint ghi trong Question, ContentPackage, LearningVideo, ContentAsset, KnowledgePoint controller không có role guard; use case cũng không kiểm role. Alias `/api/content/admin/*` cũng không guard.
3. user-service: forgot-password trả 404 với email không tồn tại (lộ email), raw token bị bỏ (`AuthController.java:50`, không gửi được), reset password không thu hồi refresh token. Endpoint admin đổi status goal truyền `userId=null` (`UserManagementController.java:175-178`) → không kiểm goal thuộc user.
4. Không service nào giới hạn route học viên theo role (game, learning-support, ai-learning): ADMIN/EXAMINER dùng được route học viên. Rủi ro thấp vì dữ liệu scope theo `userId`.
5. Config-repo có giá trị fallback cho `EXTERNAL_JWT_SECRET`, `GATEWAY_INTERNAL_JWT_SECRET`, `*_DB_PASSWORD`, `RABBITMQ_PASSWORD` (đã biết; không tự sửa).
6. Gateway chỉ đọc bearer ở header, user-service lại set cookie → chưa rõ frontend gửi token thế nào.

Ghi chú: `/internal/assessment-content/**`, `/internal/game-content/**` của content không nằm dưới `/api/content/**` nên gateway không route ra ngoài.

### P1 — sai hành vi / lifecycle dang dở

**assessment**
- ✅ Nộp bài trễ: `AssessmentAttempt.submit` gọi `expire()` rồi ném `InvalidAssessmentStateException` trước `save`, trong `@Transactional` → EXPIRED không bao giờ được lưu, attempt kẹt IN_PROGRESS (`AssessmentAttempt.java:73-75`). Không có scheduler expire.
- GradingJob chỉ tạo QUEUED; không worker. `pointCostSnapshot` lấy từ client, bảng `grading_point_costs` không được đọc. VideoPractice chỉ IN_PROGRESS. Result status PROCESSING/FAILED không bao giờ set.
- Outbox relay: gọi broker trong DB transaction (tối đa 50 × 5s confirm); row vượt 20 lần retry nằm im, không cảnh báo/DLQ. `StartAssessmentAttemptUseCase` gọi HTTP content/user trong `@Transactional`.
- Lệch contract `assessment-completed-v2`: `error_type` là free text (contract chỉ cho 4 giá trị); `overall_band` không kiểm bước 0,5; chỉ gửi lỗi đầu tiên mỗi (item, KP). Chưa kiểm ObjectMapper có `NON_NULL` không (nếu có sẽ rớt field null).

**content**
- Không endpoint nào publish Question (`publishVersion` không có caller) → chỉ câu hỏi seed V4 là PUBLISHED.
- `Skill.ALL` có trong enum nhưng CHECK V1 không cho → 500.
- Trùng code trả 400 thay vì 409. `publishedBy`, `createdBy` luôn null; feature key hardcode trong controller.
- Không service nào enforce `requiredFeatureKey` khi đọc.

**game**
- ✅ `AbandonGameSessionUseCase` không đụng tới match player → bỏ phiên trong match làm match không bao giờ COMPLETED.
- Room không bao giờ rời IN_MATCH; EXPIRED/CANCELLED/DISCONNECTED/FORFEITED không dùng. GameMatchCompleted dùng `matchPlayerId` làm aggregateId (`SubmitGameAnswerUseCase.java:75`).
- WebSocket handler không `SubProtocolCapable` → có thể không echo `game.v1`, browser từ chối handshake (cần test browser). Ticket store và connection map in-memory → không scale >1 instance.

**learning-support**
- Streak do client PUT trực tiếp; activity không verify; không `@Version` (last-write-wins). `UpdateFlashcardUseCase.java:35-37` bỏ qua `dropLinks`.

**ai-learning**
- Turn có thể kẹt `running`: `finish_turn` lỗi DB 2 lần, psycopg2 không `connect_timeout`, lifespan không drain task. `recover_interrupted_turns` fail **mọi** turn running khi khởi động → chỉ an toàn với 1 replica.
- Không đọc/forward/log `X-Correlation-Id` (đã biết). Handler `ValueError` → 502 biến bug nội bộ thành lỗi upstream. Quota memory summary không hoàn khi LLM lỗi. `sqlalchemy` không dùng.

**access**
- Idempotency activate key theo key, không theo user; cùng cột idempotency unique ở ledger → activate và debit có thể va nhau. ConsumeHumanGrading không idempotent. GrantSubscription bỏ qua `planId` khi đã có subscription.

### P2 — hiệu năng và quy tắc AGENTS.md

- N+1: `ContentAssetRepositoryAdapter.java:54-55,64-65` (findById trong vòng lặp); mapper list package/video/question duyệt lazy collection không có entity graph; `StartGameMatchUseCase.java:44-53`, `GameRoomRepositoryAdapter.java:41-46` save từng member; `GameWebSocketHandler.java:131-135` query theo từng member.
- List không phân trang: toàn bộ content (question `findAll`, topic tree, KP, package, video; vocabulary search `query=""` trả hết); user `GET /api/users`, goals, oauth, action tokens; access plans, key products.
- Logic trong controller/infrastructure: `FlashcardController.java:37-47`, `NoteController.java:131-148`, `GameWebSocketHandler.java:106-127` (authorization + orchestration).
- ai-learning: mỗi call mở một kết nối psycopg2 (~10+ mỗi turn); `path_service.py:187-195` membership trên list (O(P·M), nhỏ).

## 3. Messaging và outbox

| Service | Ghi outbox | Relay | Consumer |
| --- | --- | --- | --- |
| assessment | `AssessmentCompleted.v2` (unique theo result) | Có, SKIP LOCKED, 2s, batch 50 | ai-learning (retry TTL 30s + DLQ, idempotent theo version) |
| access | PointCredited/Debited, Subscription*, HumanGradingCreditConsumed | Không | — |
| game | 11 loại (room, member, match, session) | Không | — |
| content, learning-support, community | Không (bảng chết) | Không | — |
| user | Không có bảng | — | — |

## 4. Luồng chính

- **Auth:** login (email + BCrypt, ACTIVE) → access JWT HS256 (`EXTERNAL_JWT_SECRET`, 3600s) + refresh 48 byte (lưu SHA-256, rotate atomic, 7 ngày) → gateway kiểm JWT ngoài, bỏ header client, ký internal JWT (60s, tối đa 300s) cho path trong `internal-jwt-paths`.
- **Thi → mastery:** start attempt (snapshot goal + KP mapping qua HTTP) → lưu response (optimistic revision) → submit → examiner tạo result, lưu chi tiết, finalize → outbox → Rabbit `assessment.events`/`assessment.completed.v2` → ai-learning áp evidence vào path (chưa có path thì park vào `pending_formal_assessment_results`).
- **Tutor:** mở turn (1 running/session) → consume quota (429 nếu hết) → engine tối đa 6 vòng tool → SSE (11 loại event, khớp `tutor-sse-v1`) → finish turn → memory summary chạy nền.
- **Game:** REST tạo room/session → ticket WS 30s dùng 1 lần → `/ws/games` subprotocol `ticket.<token>` → START_MATCH/ANSWER qua WS; snapshot nội dung lấy từ content lúc tạo.

## 5. Liên quan plan tách service (`plans/260928-2019-architecture-doc-service-split`)

- Cụm Flashcard + Deck + DeckItem gắn FK và join (`FlashcardDeckItemJpaRepository.java:19-40`) → phải chuyển cùng nhau sang library.
- Note, SavedVideoSegment, VideoLearningProgress độc lập; Activity và Streak không liên kết gì → chuyển sang user-service rẻ.
- Helper phải nhân bản ở cả hai bên: `DomainChecks`, `PageQuery`, `PageResult`, `OwnedPage`, `ApplicationSupport`, `PersistenceExceptions`, `JpaSupport`, `GlobalExceptionHandler`.
- Hiện không kiểm id nội dung (video, segment, sense, question) khi lưu. Game snapshot VOCABULARY (`GetGameContentSnapshotUseCase`) sẽ phải lấy từ library.

## 6. Khoảng trống test

- Controller test của access và content dùng `standaloneSetup` → không chạy `@PreAuthorize`; đó là lý do lỗ hổng P0 #1, #2 không bị bắt.
- assessment: không test HTTP client, submit/expire, grading job, submission, video practice.
- content: không test video, asset, game snapshot, adapter.

## Relevant Files

- `infra/config-server/config-repo/api-gateway.yaml` — route, public endpoint, internal-jwt-paths
- `infra/api-gateway/src/main/java/com/group01/apigateway/security/SecurityConfig.java` — JWT ngoài, CORS, rule `authenticated()`
- `infra/api-gateway/src/main/java/com/group01/apigateway/filter/InternalJwtGatewayFilter.java` — đổi sang internal JWT
- `shared/common-security/src/main/java/com/group01/commonsecurity/config/CommonSecurityAutoConfiguration.java` — security downstream
- `services/access-service/src/main/java/com/group01/access/api/controller/InternalAccessController.java` — endpoint không guard
- `services/access-service/src/main/java/com/group01/access/application/usecase/RefundPointsUseCase.java` — credit không kiểm debit
- `services/content-service/src/main/java/com/group01/content/api/controller/` — 10 controller, 5 cái thiếu role guard
- `services/assessment-service/src/main/java/com/group01/assessment/domain/aggregate/AssessmentAttempt.java` — state machine, lỗi rollback expire
- `services/assessment-service/src/main/java/com/group01/assessment/infrastructure/messaging/OutboxRelay.java` — relay
- `services/game-service/src/main/java/com/group01/game/application/usecase/AbandonGameSessionUseCase.java` — không cập nhật match player
- `services/game-service/src/main/java/com/group01/game/infrastructure/websocket/` — WS handler, ticket store
- `services/learning-support-service/src/main/java/com/group01/learningsupport/infrastructure/persistence/repository/FlashcardDeckItemJpaRepository.java` — join làm ràng buộc khi tách
- `services/user-service/src/main/java/com/group01/user/api/controller/AuthController.java` — auth, cookie, forgot-password
- `services/ai-learning-service/app/api/tutor.py`, `app/tutor/engine.py`, `app/tutor/session_store.py` — vòng đời turn
- `services/ai-learning-service/app/messaging/assessment_consumer.py` — consumer, retry, DLQ
- `docker-compose.yml` — chỉ DB phụ, RabbitMQ, stack ai-learning, game-service (container này không chạy được)

## Khuyến nghị

1. Sửa P0 #1 và #2 trước (thêm guard + test `@WebMvcTest` có security thật). Endpoint internal của access cần quyết định: chặn ở gateway hay yêu cầu role/scope service.
2. Sửa lỗi rollback expire của assessment và abandon của game — nhỏ, rõ ràng, có test.
3. Chốt hướng outbox cho access/game (relay hay bỏ) trước khi tách service.
4. Thêm phân trang cho list content trước khi dữ liệu thật tăng.

## Unresolved Questions

- Endpoint internal của access: chặn ở gateway hay cần role service? Ai sẽ gọi chúng?
- `requiredFeatureKey` được enforce ở đâu (gateway, access, hay frontend)?
- Publish question bị thiếu có chủ đích không?
- Outbox access/game/content: có kế hoạch relay không, hay bỏ bảng khi tách service?
- Học viên có được tự tạo result version (`POST /attempts/{id}/result` với band tự đặt) và đọc result DRAFT không?
- Ai đẩy GradingJob/VideoPractice đi tiếp? Giá điểm có lấy từ `grading_point_costs` không?
- ai-learning có đảm bảo chạy 1 replica không (recovery lúc khởi động phụ thuộc điều này)?
- Frontend gửi token bằng header hay cookie?
- `.sdd/database/DATABASE_V5.md` ghi subscription thuộc user-service, code để ở access-service — bên nào đúng?
- WS subprotocol `game.v1` có được echo không (cần test browser)?
