---
type: service-architecture
version: V2
status: baseline
updated: 2026-09-22
scope: IELTSPath MVP
---

# Backend Service Architecture — IELTSPath V2

## 1. Mục đích

Tài liệu này chỉ mô tả:

- hệ thống được chia thành những service nào;
- trách nhiệm chính của từng service;
- database nào thuộc service nào;
- nhóm bảng nào do từng service sở hữu;
- ranh giới giao tiếp giữa các service.

Chi tiết schema cột, index, constraint và thuật toán nghiệp vụ nằm trong tài liệu Database/Spec tương ứng, không mô tả sâu tại đây.

V2 có hai thay đổi kiến trúc chính so với V1: `ai-assistant-service` bị loại bỏ và năng lực AI/adaptive được gom vào `ai-learning-service` chạy Python/FastAPI + DeepTutor; đồng thời learner-owned utility data (activity, streak, video progress, note, flashcard) được tách thành `learning-support-service` để `ai-learning-service` chỉ sở hữu ADP/Tutor core. Nguyên tắc database-per-service vẫn giữ nguyên.

---

# 2. Cấu trúc backend V2

```text
backend/
├── infrastructure/
│   ├── eureka-server/
│   ├── api-gateway/
│   └── config-server/
│
├── shared/
│   ├── common-security/          # Java services
│   └── event-contracts/          # JSON Schema / AsyncAPI, language-neutral
│
└── services/
    ├── user-service/             # Java + Spring Boot
    ├── access-service/           # Java + Spring Boot
    ├── content-service/          # Java + Spring Boot
    ├── assessment-service/       # Java + Spring Boot
    ├── ai-learning-service/      # Python + FastAPI + DeepTutor
    ├── learning-support-service/ # Java + Spring Boot
    ├── game-service/             # Java + Spring Boot
    ├── notification-service/     # Java + Spring Boot
    └── community-service/        # Java + Spring Boot
```

Tổng cộng:

```text
3 infrastructure applications
2 shared areas
9 business services
9 business databases
```

Không còn:

```text
ai-assistant-service
ai_assistant_db
```

---

# 3. Service và database ownership

| Service | Technology | Database | Số business tables | Trách nhiệm chính |
| :--- | :--- | :--- | ---: | :--- |
| `user-service` | Java + Spring Boot | `user_db` | 8 | Identity, role, profile, learning goal |
| `access-service` | Java + Spring Boot | `access_db` | 8 | Plan, subscription, Premium, activation key, point |
| `content-service` | Java + Spring Boot | `content_db` | 22 | Curriculum, knowledge point, question bank, vocabulary, video metadata |
| `assessment-service` | Java + Spring Boot | `assessment_db` | 13 | Formal assessment, result, error analysis, Writing/Speaking grading |
| `ai-learning-service` | Python + FastAPI + DeepTutor | `ai_learning_db` | 13 | DeepTutor adaptive learning, mastery, tutor runtime, question-level review |
| `learning-support-service` | Java + Spring Boot | `learning_support_db` | 8 | Learning activity, streak, video progress, note, flashcard |
| `game-service` | Java + Spring Boot | `game_db` | 11 | Learning game, realtime room/match, quiz event, leaderboard |
| `notification-service` | Java + Spring Boot | `notification_db` | 5 | Notification, reminder, push delivery |
| `community-service` | Java + Spring Boot | `community_db` | 3 | Post, comment, reaction |

Tổng:

```text
91 business tables
+ 9 outbox_events
= 100 physical tables
```

Mỗi database có một `outbox_events` riêng.

---

# 4. `user-service`

## 4.1 Trách nhiệm

```text
Authentication identity
User account
Role / authorization identity data
OAuth identity
Learner profile
Learning goal
Account recovery / verification token
```

`user-service` là source of truth cho thông tin định danh người dùng và mục tiêu học dài hạn.

## 4.2 Database

```text
user_db
```

## 4.3 Bảng sở hữu

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

`user-service` không sở hữu subscription, point hoặc Premium entitlement.

---

# 5. `access-service`

## 5.1 Trách nhiệm

```text
Plan / feature entitlement
Subscription
Premium access
Activation Key
Point wallet
Point ledger
```

`access-service` là source of truth cho toàn bộ quyền sử dụng trả phí và point.

## 5.2 Database

```text
access_db
```

## 5.3 Bảng sở hữu

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

Các service khác chỉ giữ logical reference đến subscription/ledger khi cần audit; không query trực tiếp `access_db`.

---

# 6. `content-service`

## 6.1 Trách nhiệm

```text
IELTS curriculum
Topic
Knowledge Point
Prerequisite
Topic gate rule
Vocabulary
Content package / version
Question Bank
Question ↔ Knowledge Point mapping
Media asset
YouTube learning metadata / segment
```

`content-service` là canonical source of truth cho nội dung học và nội dung assessment được publish.

Boundary hiện tại:

```text
topics / knowledge_points
= curriculum taxonomy + canonical KP metadata

knowledge_points.learning_type
= MEMORY | CONCEPT | PROCEDURE | DESIGN
= map 1:1 sang DeepTutor KnowledgeType

required_feature_key
= requirement key tham chiếu logic tới access-service
= content-service không hard-code FREE/PREMIUM entitlement

content_package_versions.rules
= delivery / assessment configuration only
= không chứa mastery, unlock, prerequisite hay adaptive policy
```

## 6.2 Database

```text
content_db
```

## 6.3 Bảng sở hữu

```text
topics
knowledge_points
vocabulary_items
vocabulary_senses

content_packages
content_package_versions
content_sections
questions
question_versions
section_questions
question_knowledge_points
content_assets
content_asset_links
learning_videos
video_segments
video_segment_lexical_entries

outbox_events
```

`ai-learning-service` có thể đọc curriculum/content qua API/tool adapter nhưng không sở hữu bản canonical của các bảng này.

---

# 7. `assessment-service`

## 7.1 Trách nhiệm

```text
Placement Test
Official Practice
Mock Test
Topic Gate assessment
Attempt / response lifecycle
Formal scoring
Skill score
Item-level result
Error analysis
Video practice assessment
Writing/Speaking submission
AI grading job
Human grading workflow
```

`assessment-service` là source of truth cho kết quả assessment chính thức.

## 7.2 Database

```text
assessment_db
```

## 7.3 Bảng sở hữu

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
video_practice_attempts

grading_point_costs
grading_jobs
human_reviews

outbox_events
```

Point vẫn thuộc `access-service`; grading chỉ giữ logical reference tới point ledger/subscription khi cần.

Kết quả assessment được publish qua integration event cho `ai-learning-service`; Assessment không ghi trực tiếp vào `ai_learning_db`.

---

# 8. `ai-learning-service`

## 8.1 Technology

```text
Python
FastAPI
DeepTutor
PostgreSQL
```

## 8.2 Trách nhiệm

`ai-learning-service` chỉ sở hữu **Adaptive Learning / AI Tutor core**. Service này không quản lý note, flashcard, streak hay video progress.

```text
DeepTutor Mastery Path
Learner adaptive state
Mastery / evidence / retention state
Next learning objective
Tutor session runtime
Tutor interaction / deterministic grading
Question Notebook history used by Tutor
Question-level mistake/review practice
RAG / tutor memory integration ở runtime
Formal assessment evidence ingestion
```

DeepTutor là adaptive learning authority duy nhất; không tồn tại Java Adaptive Engine hoặc planner thứ hai chạy song song.

## 8.3 Internal boundary

```text
ai-learning-service/
├── mastery/
├── tutor-runtime/
├── practice/
├── integrations/
│   ├── content/
│   ├── assessment/
│   └── user/
├── rag/
└── memory/
```

## 8.4 Database

```text
ai_learning_db
```

## 8.5 Bảng sở hữu

### DeepTutor Mastery Core

```text
mastery_paths
mastery_path_sessions
mastery_interactions
mastery_events
mastery_learning_evidence
mastery_path_leases
```

### Tutor/session runtime

```text
sessions
messages
turns
turn_events
```

### Question-level mistake/review practice

```text
notebook_entries
practice_review_state
practice_review_events
```

### Integration outbox

```text
outbox_events
```

Không còn các bảng Adaptive Engine V1:

```text
mastery_records
review_events
topic_progress
topic_gate_attempts
daily_plans
daily_tasks
mistake_notebook_entries
```

Không còn:

```text
ai_conversations
ai_messages
ai_assistant_db
```

---

# 9. `learning-support-service`

## 9.1 Technology

```text
Java
Spring Boot
PostgreSQL
```

## 9.2 Trách nhiệm

Service này sở hữu **learner-owned learning utility state**, tách khỏi ADP core:

```text
Learning activity history
Study streak
Video learning progress
Saved video segments
Personal notes
Flashcard decks
Flashcards
Deck membership
```

`learning-support-service` không tính mastery, không chọn `next_objective()` và không chạy scheduler/policy của DeepTutor.

## 9.3 Database

```text
learning_support_db
```

## 9.4 Bảng sở hữu

### Learning Tracking

```text
learning_activities
streaks
video_learning_progress
saved_video_segments
```

### Personal Library

```text
notes
flashcard_decks
flashcards
flashcard_deck_items
```

### Integration outbox

```text
outbox_events
```

Video metadata/canonical segments vẫn thuộc `content-service`; service này chỉ sở hữu **progress/bookmark của learner**. Streak là projection từ activity và không phải adaptive mastery state.

---

# 10. `game-service`

## 10.1 Trách nhiệm

```text
Vocabulary / Grammar game
Single-player game session
Realtime multiplayer room
Realtime match
Game event stream / durable game state
Quiz event
Quiz participation
Leaderboard period / entry
```

Game không trực tiếp thay đổi DeepTutor mastery state. Nếu sau này game result được dùng làm learning evidence thì phải đi qua integration contract với `ai-learning-service`.

## 10.2 Database

```text
game_db
```

## 10.3 Bảng sở hữu

```text
game_sessions
game_answers
game_rooms
game_room_members
game_matches
game_match_players
game_events

quiz_events
quiz_participations
leaderboard_periods
leaderboard_entries

outbox_events
```

---

# 11. `notification-service`

## 11.1 Trách nhiệm

```text
Notification preferences
Push device registration
Reminder schedule
Notification creation
Delivery tracking
```

## 11.2 Database

```text
notification_db
```

## 11.3 Bảng sở hữu

```text
notification_preferences
push_devices
reminder_schedules
notifications
notification_deliveries
outbox_events
```

Notification Service phản ứng với integration event từ các service khác nhưng không sở hữu business state nguồn của event đó.

---

# 12. `community-service`

## 12.1 Trách nhiệm

```text
Community post
Comment
Reaction
Feed/community interaction
```

## 12.2 Database

```text
community_db
```

## 12.3 Bảng sở hữu

```text
posts
comments
post_reactions
outbox_events
```

---

# 13. Infrastructure

## 13.1 `api-gateway`

Trách nhiệm:

```text
External API entry point
Routing
Cross-cutting authentication checks
Rate limit / security policy nếu cấu hình
Forward identity/security context
```

Gateway không sở hữu business database.

## 13.2 `eureka-server`

Trách nhiệm:

```text
Service discovery
Service registration
Health/discovery metadata
```

Không sở hữu business database.

## 13.3 `config-server`

Trách nhiệm:

```text
Centralized application configuration
Environment-specific config distribution
```

Không sở hữu business database.

---

# 14. Shared areas

## 14.1 `common-security`

Dùng cho các Java/Spring service:

```text
JWT/security helpers
Spring Security conventions
Shared authorization utilities
```

`ai-learning-service` là Python nên không import Java library này. Python service tuân theo cùng JWT/JWKS/security contract bằng implementation riêng.

## 14.2 `event-contracts`

Integration event contract phải độc lập ngôn ngữ:

```text
JSON Schema / AsyncAPI
versioned event names
common envelope conventions
```

Ví dụ:

```text
AssessmentCompleted.v2
SubscriptionChanged.v1
LearningProgressUpdated.v1
NotificationRequested.v1
```

Java và Python cùng serialize/validate theo contract; không chia sẻ Java DTO trực tiếp.

---

# 15. Quy tắc database ownership

Áp dụng cho toàn bộ V2:

```text
1 service = owner duy nhất của database đó
```

Không service nào được:

```text
query trực tiếp database của service khác
JOIN cross-service database
đặt physical FK sang bảng của database khác
update bảng do service khác sở hữu
```

Cross-service reference chỉ là logical ID.

Ví dụ:

```text
assessment_db.grading_jobs.user_id
→ logical reference tới user_db.users.id
→ KHÔNG physical FK
```

hoặc:

```text
ai_learning_db.mastery_learning_evidence.knowledge_point_id
→ logical reference tới content_db.knowledge_points.id
→ KHÔNG physical FK
```

---

# 16. Giao tiếp giữa các service

## 16.1 Synchronous REST

Dùng khi caller cần kết quả ngay.

Ví dụ:

```text
ai-learning-service → user-service
get learner profile / learning goal

ai-learning-service → content-service
get topic / KP / learning material / question metadata

learning-support-service → content-service
get video/vocabulary metadata cho progress, saved segment và flashcard

assessment-service → access-service
validate/debit point hoặc validate Premium khi workflow yêu cầu
```

## 16.2 Asynchronous event

Dùng khi service khác cần phản ứng sau khi business transaction đã commit.

Ví dụ:

```text
assessment-service
→ AssessmentCompleted
→ ai-learning-service

ai-learning-service
→ LearningActivityRecorded / TutorSessionCompleted
→ learning-support-service

access-service
→ SubscriptionChanged
→ interested consumers

ai-learning-service
→ LearningProgressUpdated
→ notification-service

learning-support-service
→ StreakUpdated
→ notification-service / game-service khi cần
```

Tất cả event phát sinh từ business mutation phải đi qua `outbox_events` của chính database owner.

---

# 17. Database map cuối cùng

```text
user-service
└── user_db

access-service
└── access_db

content-service
└── content_db

assessment-service
└── assessment_db

ai-learning-service
└── ai_learning_db

learning-support-service
└── learning_support_db

game-service
└── game_db

notification-service
└── notification_db

community-service
└── community_db
```

Không có shared business database.

Không có:

```text
ai_assistant_db
```

---

# 18. Baseline V2

```text
Business services:     9
Business databases:   9
Business tables:      91
Outbox tables:         9
Physical tables:     100
```

Source of truth theo domain:

```text
user_db        → identity/profile/learning goal
access_db      → entitlement/subscription/point
content_db     → canonical IELTS content
assessment_db  → formal assessment/grading
ai_learning_db        → DeepTutor adaptive learning / tutor state
learning_support_db  → activity/streak/video progress/personal library
game_db        → game/quiz/leaderboard
notification_db→ notification/reminder
community_db   → community content
```

Đây là boundary chính của Backend Service Architecture V2. Chi tiết cấu trúc từng bảng nằm trong `DATABASE_V5.md`.
