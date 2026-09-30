---
type: service-architecture
version: V3
status: target-design
updated: 2026-09-29
scope: IELTSPath MVP
---
# Backend Service Architecture — IELTSPath V3

## 1. Mục đích

Tài liệu này chỉ mô tả:

- hệ thống được chia thành những service nào;
- trách nhiệm chính của từng service;
- database nào thuộc service nào;
- nhóm bảng nào do từng service sở hữu;
- ranh giới giao tiếp giữa các service.

Chi tiết schema cột, index, constraint và thuật toán nghiệp vụ nằm trong tài liệu Database/Spec tương ứng, không mô tả sâu tại đây.

V2 có hai thay đổi kiến trúc chính so với V1: `ai-assistant-service` bị loại bỏ và năng lực AI/adaptive được gom vào `ai-learning-service` chạy Python/FastAPI + DeepTutor; đồng thời learner-owned utility data (activity, streak, video progress, note, flashcard) được tách thành `learning-support-service` để `ai-learning-service` chỉ sở hữu ADP/Tutor core. Nguyên tắc database-per-service vẫn giữ nguyên.

**V3 (2026-09-29, thiết kế đích, chưa triển khai)** có ba thay đổi so với V2:

1. **Tách service.** Thêm `library-service`: nhận catalog từ vựng và video từ content, cùng thư viện học cá nhân (flashcard, note, tiến độ video) từ learning-support. `learning-support-service` giải thể; activity và streak chuyển sang `user-service`.
2. **Học theo topic → bài.** Content có bài học; ai-learning giữ thứ tự học, tiến độ, luyện thêm và ôn bằng gói câu mới, giao mã đề cuối; assessment tự chấm đề cuối topic.
3. **Bỏ band ở knowledge point.**

Nguồn: `plans/260928-2019-architecture-doc-service-split/plan.md`, `plans/260929-1640-lesson-learning-pipeline-mvp/plan.md`; schema ở `DATABASE_V5.md` V5.1, V5.2. Vẫn 9 business service và 9 database.

---

# 2. Cấu trúc backend V3

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
    ├── library-service/          # Java + Spring Boot (V3, thay learning-support-service)
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
learning-support-service   (V3: giải thể)
learning_support_db        (V3: dữ liệu chia sang user_db và library_db)
```

---

# 3. Service và database ownership

| Service                  | Technology                   | Database            | Số business tables | Trách nhiệm chính                                                                                                                   |
| :----------------------- | :--------------------------- | :------------------ | ------------------: | :------------------------------------------------------------------------------------------------------------------------------------- |
| `user-service`         | Java + Spring Boot           | `user_db`         |                  11 | Identity, role, profile, learning goal, hồ sơ người chấm, activity, streak                                                        |
| `access-service`       | Java + Spring Boot           | `access_db`       |                   8 | Plan, subscription, Premium, activation key, point                                                                                     |
| `content-service`      | Java + Spring Boot           | `content_db`      |                  16 | Curriculum, knowledge point, question bank, gói đề, bài học                                                                       |
| `assessment-service`   | Java + Spring Boot           | `assessment_db`   |                  13 | Formal assessment, đề cuối topic tự chấm, result, error analysis, Writing/Speaking grading                                        |
| `ai-learning-service`  | Python + FastAPI + DeepTutor | `ai_learning_db`  |                  22 | DeepTutor mastery, tutor runtime, question-level review, luồng học bài (thứ tự học, tiến độ, luyện thêm/ôn, giao mã đề) |
| `library-service`      | Java + Spring Boot           | `library_db`      |                  11 | Catalog từ vựng, video; flashcard, note, tiến độ video, đoạn video đã lưu                                                    |
| `game-service`         | Java + Spring Boot           | `game_db`         |                  11 | Learning game, realtime room/match, quiz event, leaderboard                                                                            |
| `notification-service` | Java + Spring Boot           | `notification_db` |                   5 | Notification, reminder, push delivery                                                                                                  |
| `community-service`    | Java + Spring Boot           | `community_db`    |                   3 | Post, comment, reaction                                                                                                                |

Tổng (V3, thiết kế đích):

```text
100 business tables
+ 7 outbox_events
= 107 physical tables
```

Service có phát event giữ một `outbox_events` riêng. V3: `library_db` và `notification_db` không có outbox (chưa có consumer cho event của hai service này). Số bảng AI Learning tính theo `DATABASE_V5.md` §7.1–§7.13, §7.18–§7.26; các bảng snapshot và ingest kết quả thi (§7.15, §7.17) không tính vào con số này.

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
Examiner profile             (V3)
Learning activity / streak   (V3, từ learning-support)
```

`user-service` là source of truth cho thông tin định danh người dùng và mục tiêu học dài hạn.

Mỗi learner có tối đa một learning goal ở trạng thái `ACTIVE` (partial unique index trên `learning_goals(user_id)`); tạo hoặc kích hoạt goal mới sẽ pause goal active hiện tại trong cùng transaction. Active-goal endpoint chỉ trả goal duy nhất; nếu dữ liệu vi phạm invariant, service fail closed thay vì chọn một goal tùy ý.

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
examiner_profiles        (V3)
learning_activities      (V3, từ learning-support)
streaks                  (V3, từ learning-support)
outbox_events
```

`user-service` không sở hữu subscription, point hoặc Premium entitlement. Learning goal từ V3 chỉ để hiển thị và nhắc học; không dùng để tạo path. Route công khai `/api/learning-support/{activities,streak}/**` giữ nguyên, Gateway trỏ về `user-service`.

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
Lesson / lesson block                    (V3)
Content package / version
Topic test (TOPIC_TEST, nhiều mã đề)     (V3)
Practice set làm ngân hàng câu ôn        (V3)
Question Bank
Question ↔ Knowledge Point mapping
Media asset
```

V3: từ vựng và video (catalog) chuyển sang `library-service`.

`content-service` là canonical source of truth cho nội dung học và nội dung assessment được publish.

Boundary hiện tại:

```text
topics / knowledge_points
= curriculum taxonomy + canonical KP metadata

knowledge_points.learning_type
= MEMORY | CONCEPT | PROCEDURE | DESIGN
= map 1:1 sang DeepTutor KnowledgeType
ACTIVE Knowledge Point bắt buộc có learning_type; `kind` là phân loại content riêng

required_feature_key
= requirement key tham chiếu logic tới access-service
= content-service không hard-code FREE/PREMIUM entitlement

content_package_versions.rules
= delivery / assessment configuration only
= không chứa mastery, unlock, prerequisite hay adaptive policy

knowledge_points (V3)
= không còn band; band chỉ còn ở topics để hiển thị

content_packages.topic_id (V3)
= mã đề TOPIC_TEST gắn topic; một topic nhiều mã đề

lessons / lesson_blocks (V3)
= nội dung bài học; bài biết câu hỏi qua lesson_block_questions,
  dạy KP qua lesson_knowledge_points
= content không giữ tiến độ học viên; khóa/mở bài do ai-learning quyết định

/internal/learning-content/* (V3)
= endpoint nội bộ cho ai-learning và assessment, trả cả answer_spec;
  Gateway chặn /internal/**
```

## 6.2 Database

```text
content_db
```

## 6.3 Bảng sở hữu

```text
topics
knowledge_points

content_packages
content_package_versions
content_sections
questions
question_versions
section_questions
question_knowledge_points
content_assets
content_asset_links

lessons                        (V3)
lesson_blocks                  (V3)
lesson_block_vocabulary        (V3)
lesson_block_questions         (V3)
lesson_knowledge_points        (V3)

outbox_events
```

V3 chuyển sang `library-service`: `vocabulary_items`, `vocabulary_senses`, `learning_videos`, `video_segments`, `video_segment_lexical_entries`. `lesson_block_vocabulary.vocabulary_sense_id` là logical reference tới library.

`ai-learning-service` có thể đọc curriculum/content qua API/tool adapter nhưng không sở hữu bản canonical của các bảng này.

---

# 7. `assessment-service`

## 7.1 Trách nhiệm

```text
Placement Test
Official Practice
Mock Test
Đề cuối topic (TOPIC_GATE) — tự chấm câu khách quan (V3)
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

V3 (không đổi bảng):

- **Tạo attempt:** app chỉ gửi `packageVersionId`. Assessment lấy đề và đáp án từ content (`/internal/learning-content/package-versions/{id}`) và tự suy `attempt_type` từ loại gói. `PRACTICE_SET` bị từ chối, vì gói ôn chấm ở ai-learning.
- **Đáp án:** nằm trong `attempt_items.answer_snapshot`, không bao giờ trả cho học viên.
- **Nộp bài:** câu khách quan được tự chấm; kết quả `COMPLETED` và outbox cùng transaction. Lời giải chỉ trả khi ≥ 70%.
- **Không gọi user-service:** `AssessmentCompleted.v2` có `package_version_id`, `learning_goal_id` null.

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

Luồng học bài (V3):
Thứ tự học theo topic (topic có bài và có đề, theo sort_order)
Cổng mở bài (khóa tuần tự, bài ôn đang chờ)
Chấm bài tập theo answer_spec, giấu đáp án tới khi đạt
Luyện thêm / ôn bắt buộc bằng gói PRACTICE_SET câu mới
Giao mã đề cuối (mỗi lần giao dùng một lần), nhận kết quả đề qua event
```

DeepTutor là adaptive learning authority duy nhất; không tồn tại Java Adaptive Engine hoặc planner thứ hai chạy song song. V3: con số mastery do DeepTutor core tính. Luật luồng học (khóa bài, chèn luyện thêm/ôn khi KP dưới ngưỡng và có câu sai) là code riêng của IELTSPath, đặt ngoài `app/mastery`. Path một mỗi học viên, tạo theo `sort_order`, không goal, không LLM.

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

### Tutor memory / material / hạn mức LLM

```text
learner_memory
session_materials
llm_daily_usage
```

### Luồng học bài (V5.1, sửa V5.2)

```text
topic_progress
lesson_progress
lesson_exercise_submissions
path_review_items
path_review_sets          (V5.2)
topic_test_assignments    (V5.2)
```

### Integration outbox

```text
outbox_events
```

Không còn các bảng Adaptive Engine V1:

```text
mastery_records
review_events
topic_gate_attempts
daily_plans
daily_tasks
mistake_notebook_entries
```

`topic_progress` quay lại từ V5.1 nhưng chỉ lưu học viên đã học tới topic nào; mastery vẫn do DeepTutor core giữ. V5.2 xóa `mastery_path_knowledge_point_bands`.

Không còn:

```text
ai_conversations
ai_messages
ai_assistant_db
```

---

# 9. `library-service` (V3, thay `learning-support-service`)

## 9.1 Technology

```text
Java
Spring Boot
PostgreSQL
```

## 9.2 Trách nhiệm

Service này gồm hai phần: **catalog từ vựng và video** (chuyển từ content) và **thư viện học cá nhân của learner** (chuyển từ learning-support).

```text
Vocabulary catalog (item, sense)
Learning video catalog (video, segment, lexical entry)
Video learning progress
Saved video segments
Personal notes
Flashcard decks
Flashcards
Deck membership
```

`library-service` không tính mastery, không chọn `next_objective()` và không chạy scheduler/policy của DeepTutor. Catalog chỉ `ADMIN`, `CONTENT_AUTHOR` ghi; thư viện cá nhân do chủ sở hữu ghi.

## 9.3 Database

```text
library_db
```

## 9.4 Bảng sở hữu

### Catalog (từ content)

```text
vocabulary_items
vocabulary_senses
learning_videos
video_segments
video_segment_lexical_entries
```

### Thư viện học cá nhân (từ learning-support)

```text
flashcard_decks
flashcards
flashcard_deck_items
notes
video_learning_progress
saved_video_segments
```

- **Không có `outbox_events`**: bảng của learning-support chưa từng được dùng.
- **Chỉ soft-delete catalog** (`status` INACTIVE).
- **FK từ dữ liệu người học tới catalog dùng `ON DELETE RESTRICT`**, để không mất thẻ hay tiến độ khi sửa catalog.
- **`learning_videos.topic_id`** là logical reference tới content.
- **Route công khai giữ nguyên:** Gateway trỏ `/api/content/videos/**`, `/api/content/vocabulary/**` và `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` về `library-service`.

`learning-support-service` và `learning_support_db` bị giải thể: `learning_activities` và `streaks` sang `user-service` (§4); 6 bảng còn lại sang đây. Streak vẫn là projection từ activity và không phải adaptive mastery state.

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

V3:

- Snapshot câu hỏi `VOCABULARY` lấy từ `library-service`, `GRAMMAR` lấy từ `content-service`, cùng contract `/internal/game-content/snapshots`.
- Content từ chối đưa câu thuộc đề cuối, gói luyện tập hoặc bài học vào snapshot game, để game không thành nơi dò đáp án.
- `game_answers.vocabulary_sense_id` là logical reference tới library.

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
```

Notification Service phản ứng với integration event từ các service khác nhưng không sở hữu business state nguồn của event đó. V3: không có `outbox_events` (chưa có consumer cho event của notification); `notifications` có khóa chống trùng để nhận lại event không sinh thông báo trùng.

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

Áp dụng cho toàn bộ V3:

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
ai-learning-service → content-service
get topic / KP / reading material (API công khai)
/internal/learning-content/*: topic-sequence, topics/{id}/lessons, lessons/{id},
topics/{id}/test-packages, practice-sets/search, package-versions/{id}      (V3)

assessment-service → content-service
/internal/learning-content/package-versions/{id}: đề + đáp án khi tạo attempt   (V3)

library-service → content-service
get topic khi ghi video (learning_videos.topic_id)                            (V3)

game-service → library-service / content-service
/internal/game-content/snapshots (VOCABULARY → library, GRAMMAR → content)    (V3)

assessment-service → access-service
validate/debit point hoặc validate Premium khi workflow yêu cầu
```

V3 bỏ: `ai-learning-service → user-service` (path không còn dùng goal), `assessment-service → user-service` (không tra goal), `learning-support-service → content-service` (service giải thể). `ai-learning-service → access-service` (kiểm premium theo topic) hoãn khỏi MVP. Gọi nội bộ forward bearer của request; Gateway chặn `/internal/**`.

## 16.2 Asynchronous event

Dùng khi service khác cần phản ứng sau khi business transaction đã commit.

Ví dụ:

```text
assessment-service
→ AssessmentCompleted.v2 (V3: có package_version_id, learning_goal_id null)
→ ai-learning-service
   (topic lấy từ topic_test_assignments; không gọi HTTP khi xử lý event)

ai-learning-service
→ LearningActivityRecorded / TutorSessionCompleted
→ user-service (V3, thay learning-support-service)

access-service
→ SubscriptionChanged
→ interested consumers

ai-learning-service
→ LearningProgressUpdated
→ notification-service

user-service
→ StreakUpdated (V3, thay learning-support-service)
→ notification-service / game-service khi cần
```

Các event ngoài `AssessmentCompleted.v2` là dự kiến, chưa có producer.

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

library-service
└── library_db

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
learning_support_db
```

---

# 18. Baseline V3 (thiết kế đích)

```text
Business services:     9
Business databases:    9
Business tables:     100
Outbox tables:         7
Physical tables:     107
```

V2 (baseline 2026-09-22): 9 service, 85 business table, 9 outbox, 94 bảng vật lý. Phần chênh lệch đến từ bảng bài học (V5.1/V5.2), hồ sơ người chấm, bảng tutor memory/hạn mức, và việc bỏ outbox của library/notification.

Source of truth theo domain:

```text
user_db        → identity/profile/learning goal/examiner profile/activity/streak
access_db      → entitlement/subscription/point
content_db     → canonical IELTS content, bài học, đề cuối topic, gói luyện tập
assessment_db  → formal assessment/grading
ai_learning_db → DeepTutor mastery / tutor state / tiến độ học bài
library_db     → catalog từ vựng, video / thư viện học cá nhân
game_db        → game/quiz/leaderboard
notification_db→ notification/reminder
community_db   → community content
```

Đây là boundary chính của Backend Service Architecture V3. Chi tiết cấu trúc từng bảng nằm trong `DATABASE_V5.md`.
