# Backend Service Architecture — IELTSPath

> Kiến trúc hiện tại gồm infrastructure applications, shared libraries và các business services tách database ownership rõ ràng.

---

# 1. Cấu trúc tổng thể

```text
backend/
│
├── infrastructure/
│   ├── eureka-server/
│   ├── api-gateway/
│   └── config-server/
│
├── shared/
│   ├── common-security/
│   └── common-event/
│
└── services/
    ├── user-service/
    ├── access-service/
    ├── content-service/
    ├── assessment-service/
    ├── learning-service/
    ├── game-service/
    ├── ai-assistant-service/
    ├── notification-service/
    └── community-service/
```

Tổng:

```text
3 infrastructure applications
2 shared libraries
9 business services
```

`common-security` và `common-event` là shared library, không deploy như microservice độc lập.

---

# 2. Infrastructure

## 2.1 `eureka-server`

Service Discovery cho toàn hệ thống.

Các service đăng ký với Eureka, ví dụ:

```text
USER-SERVICE
ACCESS-SERVICE
CONTENT-SERVICE
ASSESSMENT-SERVICE
LEARNING-SERVICE
GAME-SERVICE
...
```

Gateway/service khác có thể route bằng:

```text
lb://USER-SERVICE
lb://GAME-SERVICE
```

Không chứa business logic hoặc business database.

## 2.2 `api-gateway`

Điểm vào chính của Web/Mobile:

```text
Client
  ↓
API Gateway
  ↓
Business Services
```

Trách nhiệm chính:

- route request;
- validate external JWT;
- CORS;
- rate limit;
- correlation/tracing;
- internal JWT cho downstream service nếu dùng;
- route HTTP và WebSocket.

Ví dụ:

```text
/api/users/**  → USER-SERVICE
/api/games/**  → GAME-SERVICE
/ws/games/**   → GAME-SERVICE
```

Không đặt business logic vào Gateway.

## 2.3 `config-server`

Quản lý config tập trung:

```text
application.yaml
user-service.yaml
content-service.yaml
game-service.yaml
...
```

Dùng cho các cấu hình như Eureka URL, database, Redis, broker, actuator, timeout, JWT config.

Secret nhạy cảm nên lấy từ environment/secret manager thay vì commit trực tiếp vào config repository.

---

# 3. Shared libraries

## 3.1 `common-security`

Shared Spring library cho downstream service.

Trách nhiệm:

- `SecurityFilterChain`;
- validate internal JWT;
- JWT claims → Spring authorities;
- support `@PreAuthorize`;
- chuẩn hóa security exception/convention.

Role hiện tại:

```text
ADMIN
CUSTOMER
CONTENT_AUTHOR
EXAMINER
SALES_STAFF
```

Không đặt business rule như Premium, point hoặc Human Grading Credit vào `common-security`.

## 3.2 `common-event`

Shared library định nghĩa event contract giữa các service.

Ví dụ:

```text
SubscriptionActivated
SubscriptionExpired
AssessmentCompleted
GradingCompleted
DailyPlanCreated
GameMatchCompleted
```

Chỉ chứa:

- event DTO/record;
- event name;
- event version;
- common metadata.

Không chứa:

- JPA Entity;
- Repository;
- business service;
- database access.

Nên version event:

```text
AssessmentCompleted.v1
GameMatchCompleted.v1
```

---

# 4. Business services

## 4.1 `user-service`

Database:

```text
user_db
```

Sở hữu:

```text
users
roles
user_roles
refresh_tokens
learner_profiles
oauth_identities
account_action_tokens
learning_goals

outbox_events
```

Trách nhiệm:

- tài khoản;
- authentication data;
- role;
- learner profile;
- learning goal.

`user-service` là **source of truth cho identity và role**.

Premium, plan, entitlement và Human Grading Credit **không còn thuộc `user-service`**; các nghiệp vụ này được chuyển xuống `access-service`.

---

## 4.2 `access-service`

Database:

```text
access_db
```

Sở hữu:

```text
plans
plan_features
subscriptions

key_products
activation_keys
key_activations

point_wallets
point_ledger_entries

outbox_events
```

Trách nhiệm:

```text
Plan / Feature Entitlement
Premium Subscription
Human Grading Credit

POINT key
PREMIUM key
Point wallet
Point ledger
Activation history
```

### Plan và Premium

`access-service` là source of truth cho:

```text
FREE / PREMIUM plan
plan feature
Premium active/expired
Premium start/end time
Human Grading Credit
```

Ví dụ một subscription:

```text
plan = PREMIUM
status = ACTIVE
starts_at = ...
ends_at = ...
human_grading_credits_total = 4
human_grading_credits_used = 1
```

### Activation Key

Ví dụ:

```text
POINT_50
→ key_activations
→ point_wallet +50
→ point_ledger_entries +50
```

Premium key:

```text
PREMIUM_30D
→ key_activations
→ create/extend subscriptions
→ +4 Human Grading Credits
```

Vì `activation_keys`, `key_activations` và `subscriptions` cùng thuộc `access-service`, việc activate Premium **không cần gọi sang `user-service` để tạo subscription**.

Trong cùng business flow, `access-service` có thể cập nhật subscription rồi publish integration event như:

```text
SubscriptionActivated
SubscriptionExtended
SubscriptionExpired
```

cho các service khác nếu cần phản ứng.

---

## 4.3 `content-service`

Database:

```text
content_db
```

Sở hữu curriculum/content chuẩn:

```text
topics
knowledge_points
topic_prerequisites
topic_gate_rules

vocabulary_items
vocabulary_senses
vocabulary_knowledge_points
package_lexical_entries

content_packages
content_package_versions
content_sections

questions
question_versions
question_options
section_questions
question_knowledge_points

content_assets
section_assets
question_assets

outbox_events
```

Role chính:

```text
CONTENT_AUTHOR
ADMIN
```

Không lưu learner mastery hoặc assessment result.

---

## 4.4 `assessment-service`

Database:

```text
assessment_db
```

Assessment:

```text
assessment_attempts
attempt_sections
attempt_items
attempt_responses
learner_submissions

assessment_results
skill_scores
item_results
error_analysis_items
```

Grading:

```text
grading_point_costs
grading_jobs
human_reviews

outbox_events
```

AI grading:

```text
Writing  → 3 point
Speaking → 5 point
```

Human grading:

```text
assessment-service
→ access-service
→ check Premium ACTIVE
→ reserve/consume Human Grading Credit
→ EXAMINER review
```

Event tiêu biểu:

```text
AssessmentCompleted
GradingCompleted
HumanReviewCompleted
```

---

## 4.5 `learning-service`

Database:

```text
learning_db
```

Sở hữu Adaptive Learning core và Personal Library của learner:

```text
mastery_records
review_events
topic_progress
topic_gate_attempts
daily_plans
daily_tasks
learning_activities
streaks
mistake_notebook_entries

notes
flashcard_decks
flashcards
flashcard_deck_items

outbox_events
```

Trong code nên tách rõ package/module:

```text
learning/
adaptive/
mastery/
progress/
planning/
activity/
mistake/
library/
  note/
  flashcard/
```

`notes` và `flashcards` là dữ liệu học cá nhân của learner nên dùng chung `learning_db`; chúng không cập nhật mastery trực tiếp.

Luồng chính:

```text
AssessmentCompleted
      ↓
update mastery
      ↓
update topic progress
      ↓
generate daily plan
```

MVP dùng hard-coded adaptive rule.

Personal Library được gộp vào `learning-service` vì Note/Flashcard là dữ liệu học cá nhân, workload chủ yếu là CRUD và chưa có nhu cầu scale/deploy độc lập. Game vẫn tách riêng vì có WebSocket/Redis và workload realtime.

Game không update mastery trực tiếp.

---

## 4.6 `game-service`

Database:

```text
game_db
```

Sở hữu:

```text
game_sessions
game_answers

game_rooms
game_room_members
game_matches
game_match_players
game_events

outbox_events
```

Trách nhiệm:

- single-player game;
- multiplayer room;
- WebSocket;
- ready/countdown;
- match lifecycle;
- player state;
- score;
- reconnect;
- persistent game event;
- final result.

### Realtime architecture

```text
Client
  ↓ WebSocket
API Gateway
  ↓
game-service
  ├── Redis
  │    ├── connection/presence
  │    ├── ready state
  │    ├── countdown
  │    ├── current question
  │    └── live score
  │
  └── PostgreSQL game_db
       ├── rooms
       ├── matches
       ├── players
       ├── answers
       └── persistent business events
```

Không ghi heartbeat hoặc timer tick mỗi giây xuống PostgreSQL.

Khi start match, nên snapshot content cần dùng để game không phải gọi `content-service` ở từng WebSocket message.

---

## 4.7 `ai-assistant-service`

Database:

```text
ai_assistant_db
```

Sở hữu:

```text
ai_conversations
ai_messages

outbox_events
```

Trách nhiệm AI Chat/RAG.

Nếu sau này AI sinh bài adaptive:

```text
learning-service
→ quyết định cần luyện gì

ai-assistant-service
→ gọi model để generate nội dung
```

Adaptive business rule vẫn thuộc `learning-service`.

---

## 4.8 `notification-service`

Database:

```text
notification_db
```

Sở hữu:

```text
notification_preferences
push_devices
reminder_schedules
notifications
notification_deliveries

outbox_events
```

`notifications`:

```text
nội dung user nhìn thấy
```

`notification_deliveries`:

```text
từng lần gửi PUSH/EMAIL
PENDING / SENT / FAILED
```

Hỗ trợ retry/debug FCM hoặc email.

---

## 4.9 `community-service`

Database:

```text
community_db
```

Sở hữu:

```text
posts
comments
post_reactions

outbox_events
```

Không còn study group/challenge.

---

# 5. Database ownership

| Service                  | Database            |
| :----------------------- | :------------------ |
| `user-service`         | `user_db`         |
| `access-service`       | `access_db`       |
| `content-service`      | `content_db`      |
| `assessment-service`   | `assessment_db`   |
| `learning-service`     | `learning_db`     |
| `game-service`         | `game_db`         |
| `ai-assistant-service` | `ai_assistant_db` |
| `notification-service` | `notification_db` |
| `community-service`    | `community_db`    |

Ownership quan trọng:

```text
user_db
→ identity / role / profile / learning goal

access_db
→ plans / plan_features / subscriptions
→ activation keys
→ point wallet / ledger
```

`subscriptions.user_id`, `point_wallets.user_id` và các bảng access khác chỉ lưu **logical reference** tới `user-service`; không tạo FK xuyên database.

Mỗi business database có một:

```text
outbox_events
```

riêng.

Tổng:

```text
9 business databases
→ 9 outbox_events vật lý
```

---

# 6. Giao tiếp giữa service

## Synchronous REST

Dùng khi caller cần kết quả ngay.

Ví dụ:

```text
assessment-service
→ access-service
→ debit AI grading points
```

Human Grading cũng gọi `access-service`:

```text
assessment-service
→ access-service
→ kiểm tra Premium ACTIVE
→ kiểm tra/reserve Human Grading Credit
```

`user-service` chỉ cần được gọi khi cần dữ liệu identity/profile/role của user.

Không query database của service khác.

Sai:

```text
assessment-service
→ SELECT FROM access_db.point_wallets
```

Đúng:

```text
assessment-service
→ access-service API
```

## Asynchronous Event

Dùng khi service khác chỉ cần phản ứng sau event.

Ví dụ:

```text
assessment-service
  ↓ AssessmentCompleted
message broker
  ↓
learning-service
  ↓
update mastery
```

Ví dụ khác:

```text
access-service
  ↓ SubscriptionActivated
message broker
  ↓
notification-service
  ↓
thông báo Premium đã được kích hoạt
```

`subscriptions` nằm cùng `access-service`, nên không cần event sang `user-service` để tạo/update subscription.

Event contract đặt trong:

```text
common-event
```

---

# 7. Transactional Outbox

Mỗi service tự ghi business state và outbox event trong cùng transaction.

Ví dụ:

```text
assessment-service transaction
├── update assessment_results
└── insert outbox_events(AssessmentCompleted)
```

Sau commit:

```text
Outbox Publisher
   ↓
Message Broker
   ↓
learning-service
```

Giúp tránh trường hợp database đã commit nhưng publish event thất bại.

---

# 8. Dependency rule

Business service có thể import:

```text
common-security
common-event
```

Nhưng không import Java module của business service khác.

Ví dụ không nên:

```text
assessment-service
→ dependency learning-service
```

Mà phải:

```text
assessment-service
→ REST / Event
→ learning-service
```

---

# 9. Tổng quan dependency

```text
                    eureka-server
                         ▲
                         │
                    api-gateway
                         │
        ┌────────────────┼─────────────────┐
        │                │                 │
        ▼                ▼                 ▼
   user-service     content-service   access-service
                         │                 │
                         └──────┬──────────┘
                                ▼
                         assessment-service
                     │
                     │ AssessmentCompleted
                     ▼
                learning-service
                     │
          ┌──────────┼────────────┐
          ▼          ▼            ▼
 notification   ai-assistant   game-service

community-service
```

`game-service` có WebSocket/Redis workload riêng và scale độc lập.

---

# 10. Nguyên tắc bắt buộc

1. Mỗi business service sở hữu database của chính nó.
2. Không tạo FK xuyên service/database.
3. Không query trực tiếp database của service khác.
4. Sync communication dùng REST khi cần kết quả ngay.
5. Async communication dùng event khi phù hợp.
6. Event contract chung đặt ở `common-event`.
7. Security infrastructure chung đặt ở `common-security`.
8. Không đặt business logic trong `common-*`.
9. Mỗi service có `outbox_events` riêng.
10. API Gateway không chứa business logic.
11. Game realtime state ưu tiên Redis; PostgreSQL chỉ giữ dữ liệu cần persist.
12. Game scale độc lập với `learning-service`; Note/Flashcard dùng workload CRUD thông thường trong `learning-service`.
13. `plans`, `plan_features`, `subscriptions`, Activation Key và Point cùng thuộc `access-service` vì đều là **access/entitlement domain**.
14. `user-service` không quyết định user có Premium hay còn Human Grading Credit; service cần entitlement phải gọi `access-service`.