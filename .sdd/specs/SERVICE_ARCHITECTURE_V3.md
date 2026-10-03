---
type: service-architecture
version: V3
status: current
updated: 2026-10-03
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

Đối chiếu với code ngày 2026-10-03. Các thay đổi đã triển khai so với thiết kế V2:

1. **Tách service (2026-10-01).** `library-service` nhận catalog từ vựng và video từ content, cùng thư viện học cá nhân
   (flashcard, note, tiến độ video). Activity và streak chuyển sang `user-service`. Service hỗ trợ học viên cũ đã gỡ.
2. **Learning Service Java (2026-10-01).** `learning-service` (`learning_db`) thay `ai-learning-service` Python; không có
   tutor runtime. Chỉ công thức mastery được port.
3. **Học theo topic → bài, theo skill (2026-10-03).** Content có bài học, topic mang một skill, đề Practice gắn bài;
   Learning giữ lộ trình theo skill, tiến độ, Practice, thang ôn tập và giao mã đề cuối; Assessment tự chấm đề cuối.
4. **Bỏ band ở knowledge point.** Listening (audio do team upload, Content ghép URL) và Writing trong bài đã chạy.

Nguồn: `plans/261002-1600-skill-tracks-practice-remediation/plan.md`, `plans/260929-1640-lesson-learning-pipeline-mvp/plan.md`,
`plans/260930-0851-listening-topic-audio-lessons/plan.md`; schema ở `DATABASE_V5.md`.

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
    ├── learning-service/         # Java + Spring Boot; lộ trình, bài học, Practice, bài ôn
    ├── library-service/          # Java + Spring Boot; catalog và thư viện cá nhân
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
ai-assistant-service, ai_assistant_db
ai-learning-service (Python), ai_learning_db
dịch vụ hỗ trợ học viên cũ (đã gỡ; dữ liệu mới thuộc user_db và library_db)
```

---

# 3. Service và database ownership

| Service | Technology | Database | Bảng nghiệp vụ | Trách nhiệm chính |
| :--- | :--- | :--- | ---: | :--- |
| `user-service` | Java + Spring Boot | `user_db` | 10 | Identity, role, profile, learning goal, activity, streak |
| `access-service` | Java + Spring Boot | `access_db` | 8 | Plan, subscription, Premium, activation key, point |
| `content-service` | Java + Spring Boot | `content_db` | 17 | Curriculum, topic theo skill, knowledge point, question bank, gói đề, bài học |
| `assessment-service` | Java + Spring Boot | `assessment_db` | 15 | Formal assessment, đề cuối topic tự chấm, result, error analysis, grading |
| `learning-service` | Java + Spring Boot | `learning_db` | 14 | Lộ trình theo skill, tiến độ bài, Practice, thang ôn tập, mastery, giao mã đề, chấm Writing trong bài |
| `library-service` | Java + Spring Boot | `library_db` | 11 | Catalog từ vựng, video; flashcard, note, tiến độ video, đoạn video đã lưu |
| `game-service` | Java + Spring Boot | `game_db` | 11 | Learning game, realtime room/match; quiz/leaderboard (bảng có, chưa có API) |
| `notification-service` | Java + Spring Boot | `notification_db` | 0 | Chưa triển khai (khung package); thiết kế 5 bảng |
| `community-service` | Java + Spring Boot | `community_db` | 3 | Post, comment, reaction |

Tổng theo migration: **89 bảng nghiệp vụ + 5 `outbox_events` = 94 bảng** (access, content, assessment, game, community
có outbox; user, learning, library, notification không có). Learning nhận `AssessmentCompleted.v2` qua RabbitMQ, chưa
phát event nào.

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
examiner_profiles        (thiết kế, chưa có migration)
learning_activities      (V3, từ learning-support)
streaks                  (V3, từ learning-support)
```

`user_db` chưa có `outbox_events`.

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
Topic có một skill (LISTENING/READING/WRITING/SPEAKING)
Lesson / lesson block, KP mà khối TEXT dạy
Content package / version
Topic test (TOPIC_TEST, nhiều mã đề)
Practice set gắn một bài (cũng là kho set ôn)
Question Bank
Question ↔ Knowledge Point mapping
Media asset (audio Listening: key file trên object storage + transcript)
```

V3: từ vựng và video (catalog) chuyển sang `library-service`.

`content-service` là canonical source of truth cho nội dung học và nội dung assessment được publish.

Boundary hiện tại:

```text
topics / knowledge_points
= curriculum taxonomy + canonical KP metadata

knowledge_points.learning_type
= MEMORY | CONCEPT | PROCEDURE | DESIGN (metadata, không điều khiển mastery)
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

topics.skill (Content V15)
= một skill mỗi topic, bài thừa hưởng; khóa đổi khi đã có bài PUBLISHED (409 TOPIC_SKILL_LOCKED)

lessons / lesson_blocks
= nội dung bài học; bài biết câu hỏi qua lesson_block_questions,
  dạy KP qua lesson_knowledge_points; khối TEXT dạy KP qua lesson_block_knowledge_points (V16)
= content không giữ tiến độ học viên; khóa/mở bài do learning-service quyết định

content_packages.lesson_id (V16)
= PRACTICE_SET thuộc Practice của một bài

questions.purpose (V17) + một câu một chủ
= LEARNING (bài, Practice, đề cuối topic) hoặc EXAM (thi thử, xếp lớp);
  publish chặn câu đã thuộc nơi khác (422 QUESTION_ALREADY_USED) hoặc sai purpose (422 QUESTION_PURPOSE_MISMATCH)

/internal/learning-content/*
= endpoint nội bộ cho learning-service và assessment, trả cả answer_spec;
  Gateway chặn /internal/**

content_assets AUDIO (V3, bổ sung 2026-09-30)
= media_reference lưu key file mp3; content ghép CONTENT_MEDIA_BASE_URL khi trả ra
  (content là nơi duy nhất biết bucket)
= text_content là transcript = đáp án; chỉ đi qua /internal/learning-content/*;
  GET /api/content/assets/{id} chỉ ADMIN, CONTENT_AUTHOR

question_versions.hint (Content V13; learner flow triển khai 2026-10-02)
= Content lưu gợi ý không chứa đáp án, tối đa 500 ký tự qua API thêm version;
  trả qua /internal/learning-content/lessons/{id}, không trả trong package/game
= Learning Service Java quyết định hiển thị: FILL hoặc CHOICE hợp lệ có ≥3 lựa chọn
  (TFNG hỗ trợ options thiếu/rỗng), câu từng sai trong cùng user/bài/khối chưa đạt;
  giữ cả khi câu đúng ở lần sau nhưng khối vẫn trượt, khối đạt thì hint = null
= lịch sử từ mọi lesson_exercise_submissions.response.results; không thêm schema
  Learning, không ghi hints_used và không đổi luật evidence/mastery lần nộp đầu

question_versions.difficulty
= chọn gói luyện theo độ khó hoãn ngoài MVP (2026-10-01); gói luyện không gắn độ khó
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
lesson_block_knowledge_points  (V16)

outbox_events
```

V3 chuyển sang `library-service`: `vocabulary_items`, `vocabulary_senses`, `learning_videos`, `video_segments`, `video_segment_lexical_entries`. `lesson_block_vocabulary.vocabulary_sense_id` là logical reference tới library.

Listening đã triển khai, không thêm bảng: dùng `content_assets` (AUDIO), khối `ASSET` của `lesson_blocks` và section
`skill = LISTENING` có sẵn. Content trả `mediaUrl` từ key + `CONTENT_MEDIA_BASE_URL` hoặc giữ nguyên URL `https://`;
transcript nằm ở `text_content`, chỉ đi qua API nội bộ hoặc API asset dành cho `ADMIN`/`CONTENT_AUTHOR`.
Seed có một gói luyện mỗi KP Listening (chưa có mp3 mới); Reading có ít nhất hai đề Practice mỗi bài (V18). Chọn gói theo độ khó (`DATABASE_V5.md` §5.18) hoãn ngoài MVP.

`learning-service` đọc curriculum/content qua `/internal/learning-content/*` nhưng không sở hữu bản canonical của các bảng này.

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

Kết quả assessment được publish qua `AssessmentCompleted.v2` cho `learning-service`; Assessment không ghi trực tiếp vào `learning_db`.

V3 (không đổi bảng):

- **Tạo attempt:** app chỉ gửi `packageVersionId`. Assessment lấy đề và đáp án từ content (`/internal/learning-content/package-versions/{id}`) và tự suy `attempt_type` từ loại gói. `PRACTICE_SET` bị từ chối, vì Practice và set ôn chấm ở learning-service.
- **Đáp án:** nằm trong `attempt_items.answer_snapshot`, không bao giờ trả cho học viên.
- **Nộp bài:** câu khách quan được tự chấm; kết quả `COMPLETED` và outbox cùng transaction. Lời giải chỉ trả khi ≥ 70%.
- **Không gọi user-service:** `AssessmentCompleted.v2` có `package_version_id`, `learning_goal_id` null.
- **Đề Listening (đã triển khai):** `attempt_sections.section_snapshot` có `audio {url, durationSeconds}` và
  `solution.transcript`. Học viên nhận `snapshot` dạng object theo danh sách trường cho phép, không thấy `solution` hay
  transcript trong cấu trúc attempt. Kết quả ≥ 70% trả riêng `sectionSolutions[{attemptSectionId, transcript}]`;
  dưới 70% không có trường này. Lời giải từng câu và event `AssessmentCompleted.v2` giữ nguyên; event không có transcript.
  Không migration schema.

---

# 8. `learning-service`

## 8.1 Technology

```text
Java 21 + Spring Boot 3.5, JDBC (NamedParameterJdbcTemplate), Flyway, Spring AMQP (consumer)
```

## 8.2 Trách nhiệm

```text
Lộ trình topic theo skill (mỗi skill một topic IN_PROGRESS)
Cổng bài học, nộp bài tập, gợi ý câu sai
Practice theo bài: catalog, attempt, lời giải sau khi nộp
Bài qua Practice (điều kiện thi cuối)
Thang ôn tập: set có hint → lý thuyết của KP + quick-check → set mới chưa lộ → SKIPPED
Mastery theo KP (công thức port từ DeepTutor v1.6.9, tính khi đọc)
Giao mã đề cuối topic; topic không có đề cuối đạt khi xong bài
Chấm Writing trong bài bằng LLM (hạn mức ngày, trừ point qua access)
Consumer AssessmentCompleted.v2 (không gọi HTTP)
```

Không có tutor runtime, RAG, learner memory hay planner. Mọi lượt ghi của học viên mở đầu bằng `pg_advisory_xact_lock`.

## 8.3 Database

```text
learning_db
```

## 8.4 Bảng sở hữu

```text
knowledge_point_catalog
topic_progress
lesson_progress
lesson_exercise_submissions
review_items
review_sets
topic_test_assignments
kp_evidence
assessment_result_versions
lesson_writing_submissions     (V2)
llm_daily_usage                (V2)
practice_attempts              (V4)
lesson_practice_passes         (V4)
review_theory_checks           (V5)
```

V3 thêm `skill` cho `topic_progress`, `knowledge_point_catalog`, `review_items` và `has_topic_test`; V4 thêm
`trigger_kind`, `source_attempt_id` và source evidence `practice_set`; V5 thêm stage của bài ôn. Chi tiết ở
`DATABASE_V5.md` §7 và `.sdd/database/mvp-database.md`. Không có `outbox_events`.

---

# 9. `library-service` (đã triển khai trong V3)

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

`library-service` không tính mastery và không quyết định thứ tự học hay bài ôn. Catalog chỉ `ADMIN`, `CONTENT_AUTHOR` ghi; thư viện cá nhân do chủ sở hữu ghi.

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
- **Library V1** tạo 5 bảng catalog; **library V2** tạo 6 bảng thư viện cá nhân, giữ index và `uq_flashcards_user_practice_question`.
- **Bốn FK `ON DELETE RESTRICT` tới catalog**: `flashcards.vocabulary_sense_id` → `vocabulary_senses`;
  `video_learning_progress.video_id`, `saved_video_segments.video_id` → `learning_videos`;
  `saved_video_segments.segment_id` → `video_segments`.
- **`learning_videos.topic_id`** là logical reference tới content.
- **Route công khai giữ nguyên:** Gateway trỏ `/api/content/videos/**`, `/api/content/vocabulary/**`, `/api/content/admin/vocabulary/**` và `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` về `library-service`.

Service và DB hỗ trợ học viên cũ đã gỡ: `learning_activities` và `streaks` được tạo trong `user_db` bằng user V5 (§4); 6 bảng thư viện cá nhân được tạo mới trong `library_db` bằng library V2. Không chép dữ liệu cũ. Content V7 xóa 5 bảng catalog khỏi `content_db`; không chạy migration nào trên DB cũ. Streak vẫn là projection từ activity và không phải adaptive mastery state.

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

Game không ghi bằng chứng mastery. Nếu sau này kết quả game được dùng làm bằng chứng học thì phải đi qua integration contract với `learning-service`.

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

Mọi business service hiện là Java và dùng chung thư viện này.

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

Producer và consumer cùng serialize/validate theo contract; không chia sẻ Java DTO trực tiếp.

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
learning_db.kp_evidence.kp_id
→ logical reference tới content_db.knowledge_points.id
→ KHÔNG physical FK
```

---

# 16. Giao tiếp giữa các service

## 16.1 Synchronous REST

Dùng khi caller cần kết quả ngay.

Ví dụ:

```text
learning-service → content-service
/internal/learning-content/*: topic-sequence, topics/{id}/lessons, lessons/{id},
lessons/{id}/practice-sets, topics/{id}/practice-sets, topics/{id}/test-packages,
practice-sets/search, practice-sets/availability, package-versions/{id}

learning-service → access-service
số dư point, trừ point sau khi chấm Writing

assessment-service → content-service
/internal/learning-content/package-versions/{id}: đề + đáp án khi tạo attempt   (V3)

library-service → content-service
GET /api/content/topics/{id} khi ghi video (learning_videos.topic_id); forward bearer và X-Correlation-Id

game-service → library-service / content-service
/internal/game-content/snapshots (VOCABULARY → library, GRAMMAR → content)    (V3)

assessment-service → access-service
validate/debit point hoặc validate Premium khi workflow yêu cầu
```

Không có `learning-service → user-service` (lộ trình không dùng goal) hay `assessment-service → user-service`. Kiểm premium theo topic khi học hoãn khỏi MVP. Gọi nội bộ forward bearer của request; Gateway chặn `/internal/**`.

## 16.2 Asynchronous event

Dùng khi service khác cần phản ứng sau khi business transaction đã commit.

Ví dụ:

```text
assessment-service
→ AssessmentCompleted.v2 (V3: có package_version_id, learning_goal_id null)
→ learning-service
   (topic lấy từ topic_test_assignments; không gọi HTTP khi xử lý event)

learning-service
→ LearningActivityRecorded (dự kiến)
→ user-service

access-service
→ SubscriptionChanged
→ interested consumers

learning-service
→ LearningProgressUpdated (dự kiến)
→ notification-service

user-service
→ StreakUpdated (V3; service hỗ trợ học viên cũ đã gỡ)
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

learning-service
└── learning_db

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

Không có (DB hỗ trợ học viên cũ đã gỡ):

```text
ai_assistant_db, ai_learning_db
```

---

# 18. Kiểm kê hiện tại

```text
Business services:     9 (notification chỉ là khung)
Business databases:    8 có migration (notification chưa có)
Business tables:      89
Outbox tables:         5
Physical tables:      94
```

Source of truth theo domain:

```text
user_db        → identity/profile/learning goal/activity/streak
access_db      → entitlement/subscription/point
content_db     → canonical IELTS content, bài học, đề cuối topic, gói luyện tập
assessment_db  → formal assessment/grading
learning_db    → lộ trình theo skill, tiến độ bài, Practice, bài ôn, bằng chứng mastery, giao mã đề
library_db     → catalog từ vựng, video / thư viện học cá nhân
game_db        → game/quiz/leaderboard
notification_db→ notification/reminder
community_db   → community content
```

Đây là boundary chính của backend. Chi tiết cấu trúc từng bảng nằm trong `DATABASE_V5.md`.
