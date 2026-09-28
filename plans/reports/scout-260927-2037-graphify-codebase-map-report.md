# Scout Report — Codebase map qua graphify

- Ngày: 2026-09-27 · Branch: `feat/ai-learning-service` · Graph build từ commit `2665e1d` (= HEAD, graph mới)
- Nguồn: `graphify-out/graph.json` (46.496 node, 126.635 edge), `GRAPH_REPORT.md`, `graphify explain`, bổ sung config/compose cho phần runtime graph không bắt được.

## 1. Thành phần graph

| Phạm vi | Node | Ghi chú |
| --- | --- | --- |
| `third_party/deeptutor` | 37.095 (~80%) | Python + Next.js vendored; chiếm hết god nodes (`SQLiteSessionStore`, `apiFetch`, `UnifiedContext`...) và community hubs |
| First-party (`services/`, `infra/`, `shared/`) | ~7.600 | Phần cần đọc thực sự |
| External lib / no source | ~2.700 | Spring, Lombok, npm packages |

Hệ quả: `graphify query "<free text>"` thường bị kéo về deeptutor. Nên dùng `graphify explain "<Class>"` hoặc `path::Symbol` khi tên trùng (vd `PathService`).

## 2. Bản đồ module first-party

| Module | Loại | File main | Aggregate / core | Controller |
| --- | --- | --- | --- | --- |
| `infra/api-gateway` | WebFlux gateway | 12 | `SecurityConfig`, `InternalJwtGatewayFilter`, `InternalJwtService`, `CorrelationIdFilter` | — |
| `infra/config-server`, `infra/eureka-server` | Infra | 2 mỗi cái | — | — |
| `shared/common-security` | Lib | 14 | `CommonSecurityAutoConfiguration`, `CurrentUserProvider`, `InternalJwtValidators`, `CanonicalRoles` | — |
| `services/user-service` | Java | 142 | User, Role, RefreshToken, LearnerProfile, LearningGoal, OAuthIdentity, AccountActionToken | Auth, User, UserManagement, LearnerProfile, LearningGoal, OAuthIdentity |
| `services/access-service` | Java | 125 | ActivationKey, KeyProduct, Plan, PointWallet, Subscription | AccessCatalog, AdminAccess, LearnerAccess, InternalAccess |
| `services/content-service` | Java | 227 (lớn nhất) | ContentPackage, Question, KnowledgePoint, Topic, LearningVideo, VocabularyItem, ContentAsset | 10 controller gồm `InternalAssessmentContentController`, `InternalGameContentController` |
| `services/assessment-service` | Java | 162 | AssessmentAttempt (+13 entity: AssessmentResult, GradingJob, ItemResult...) | Attempt, Result, Grading, GradingJob, LearnerSubmission, VideoPractice |
| `services/learning-support-service` | Java | 141 | Flashcard, FlashcardDeck, Note, Streak, LearningActivity, SavedVideoSegment, VideoLearningProgress | 7 controller |
| `services/game-service` | Java + WebSocket | 100 | GameRoom, GameMatch, GameSession, GameAnswer | GameRoom, GameMatch, GameSession + `GameWebSocketHandler` |
| `services/community-service` | Java | 65 | Post, Comment | CommunityController |
| `services/notification-service` | Java | 6 | **Skeleton** — chỉ `package-info.java` | — |
| `services/ai-learning-service` | Python (FastAPI) | 63 | mastery engine, tutor, path ordering LLM, practice | `app/api/tutor.py`, `practice.py`, `tutor_sse.py` |

Layer Java nhất quán: `api/{controller,dto,exception}` → `application/{command,result,usecase}` → `domain/{aggregate,entity,vo,repository,exception}` ← `infrastructure/persistence` (JPA entity + mapper + adapter).

Khác biệt giữa service:
- `application/port`: chỉ assessment (`KnowledgeMappingProvider`, `LearningGoalProvider`) và game (`GameContentProvider`, `OutboxWriter`, `WebSocketTicketStore`...).
- Adapter đặt ở `infrastructure/adapter` (user, community, learning-support) vs trong `infrastructure/persistence` (access, assessment, content, game).
- `application/query/PageQuery` chỉ có ở community và learning-support.

Migration: access 2, assessment 4, community 1, content 6, game 1, learning-support 3, user 4; ai-learning dùng Flyway riêng `migrations/V0_1..V8`.

## 3. Giao tiếp giữa service

Gateway routes (`infra/config-server/config-repo/api-gateway.yaml`):
- `lb://` qua Eureka: `/auth/**,/api/users/**`→USER, `/api/access/**`, `/api/content/**`, `/api/assessments/**`, `/api/learning-support/**`, `/api/games/**` + `ws /ws/games/**`, `/api/notifications/**`, `/api/community/**`.
- `/api/ai-learning/**` → URI cố định `${AI_LEARNING_SERVICE_URI:http://localhost:8000}` (Python, không đăng ký Eureka).

HTTP đồng bộ (RestClient / httpx):
- assessment → content `/internal/assessment-content/knowledge-point-mappings` (`ContentKnowledgeMappingClient`)
- assessment → user `/api/users/me/learning-goals/active` (`UserLearningGoalClient`)
- game → content `/internal/game-content/snapshots` (`ContentSnapshotClient` implements `GameContentProvider`)
- ai-learning → content `/api/content/topics`, `/knowledge-points`, `/reading/sections/{id}` và → user (`app/clients/user_service.py`)

Async (RabbitMQ):
- assessment: `FinalizeAssessmentResultUseCase` → `AssessmentCompletedEventFactory` → outbox → `OutboxRelayScheduler`/`OutboxRelay` → `RabbitOutboxEventPublisher` → `AssessmentCompleted.v2`
- ai-learning: `app/messaging/assessment_consumer.py` (`AssessmentCompletedConsumer`, topology có retry + dead-letter exchange) → `FormalEvidenceAdapter` → `FormalAssessmentIngestionService` → `PathService` / `PostgresLearningStore`
- **access, content, game có bảng/entity `OutboxEvent` + adapter ghi nhưng không có relay/publisher Rabbit** → event ghi vào DB nhưng chưa được phát đi.

## 4. Security

- Mọi Java downstream dùng `common-security` (`CurrentUserProvider.requireUserId()` trong controller); gateway import `SecurityHeaders`, `InternalJwtClaims`, `InternalJwtAuthorities` từ lib chung.
- ai-learning tự implement lại contract internal JWT trong `app/security/internal_jwt.py` → phải giữ đồng bộ tay issuer/claim/role với `InternalJwtValidators`.
- `api-gateway.yaml` (tracked) có giá trị fallback mặc định cho `GATEWAY_INTERNAL_JWT_SECRET` → nếu env thiếu, gateway ký bằng secret công khai trong repo.

## 5. ai-learning-service chi tiết

- `app/mastery/*` (models, grading, policy, scheduler, service, store, pending) ghi rõ "Derived from DeepTutor v1.6.9 (Apache-2.0)". Không có `import deeptutor` runtime — `third_party/deeptutor` chỉ là nguồn tham chiếu/port.
- `app/learning/*`: path ordering bằng LLM (`ordering_llm.py`, `path_ordering.py`), provenance formal/override, placement test-out.
- `app/tutor/*`: engine, memory, prompts, reading, session store, tools; SSE qua `tutor_sse.py`.
- Compose: `ai-learning-db`, `ai-learning-migrate` (Flyway), `ai-learning-api` (:8000), `ai-learning-consumer`, `llm-stub`.

## 6. Độ lệch giữa tài liệu và code

- `CLAUDE.md` mục 1, 3, 15 ghi "chỉ có User", "5 module", "không có messaging" → sai: hiện 13 module first-party, 9 service, RabbitMQ đang dùng.
- `docker-compose.yml` hiện chỉ có learning-support-db, community-db, game-db, rabbitmq, stack ai-learning, game-service; không có config/eureka/gateway/user/content như CLAUDE.md mô tả.

## 7. Nhiễu trong graph (không phải dependency thật)

- Test của content/user/access/learning-support/assessment "import `community/Post.java`" → thực ra là static `MockMvcRequestBuilders.post`, graphify resolve nhầm.
- `user/User.java -> content/AccessLevel.java`: nghi resolve trùng tên, cần kiểm tra trước khi tin.
- Surprising connections `ai-learning → deeptutor` (INFERRED `indirect_call`) là trùng tên hàm (`item()`, `fallback()`), không phải call thật.

## Relevant Files

- `infra/config-server/config-repo/api-gateway.yaml` — routes, internal-jwt paths, public endpoints
- `infra/api-gateway/src/main/java/com/group01/apigateway/filter/InternalJwtGatewayFilter.java` — đổi external → internal JWT
- `shared/common-security/src/main/java/com/group01/commonsecurity/config/CommonSecurityAutoConfiguration.java` — security downstream
- `services/assessment-service/src/main/java/com/group01/assessment/infrastructure/messaging/` — outbox relay + Rabbit publisher
- `services/assessment-service/src/main/java/com/group01/assessment/application/event/AssessmentCompletedV2.java` — event contract
- `services/ai-learning-service/app/messaging/assessment_consumer.py` — consumer phía Python
- `services/ai-learning-service/app/application/path_service.py` — áp dụng evidence vào mastery path
- `services/ai-learning-service/app/security/internal_jwt.py` — bản Python của internal JWT contract
- `services/content-service/src/main/java/com/group01/content/api/controller/Internal*ContentController.java` — API nội bộ cho assessment/game
- `services/game-service/src/main/java/com/group01/game/infrastructure/websocket/` — realtime game
- `docker-compose.yml` — stack local hiện tại

## Unresolved Questions

- Outbox ở access/content/game: cố ý để sau hay thiếu relay?
- `InternalAccessController` (access) — chưa thấy client nào gọi trong code; ai là consumer?
- notification-service chỉ là skeleton — có kế hoạch triển khai?
- Có nên loại `third_party/` khỏi graphify (`.graphifyignore`) để query/community phản ánh first-party?
- ai-learning không qua Eureka/`lb://` — chấp nhận cho deploy thật hay chỉ local?
- Fallback secret trong `api-gateway.yaml`: có muốn bỏ default để fail-fast khi thiếu env?
