---
title: "Cập nhật system-architecture theo cách chia service mới"
description: "Ghi kiến trúc đích (library-service mới, giải thể learning-support, activity/streak sang user-service, model bài học, hồ sơ người chấm, bảng notification) vào docs/system-architecture.md: mục đích ngay bây giờ, gộp vào các mục chính khi code đã tách xong."
status: pending
priority: P2
branch: "main"
tags: [docs, architecture, library-service, learning-support-service, notification-service]
blockedBy: [260929-1830-sync-database-and-service-split-docs]
blocks: []
created: "2026-09-28T13:19:00.000Z"
createdBy: "ck:plan"
source: skill
---

# Cập nhật system-architecture theo cách chia service mới

## Overview

`docs/system-architecture.md` (§1–§11) mô tả code tại commit `79f9fd6` và tự khẳng định "code là nguồn đúng khi tài liệu này lệch". Cách chia service đích (mục dưới) **chưa triển khai**, nên:

- **Pha 1 (làm ngay):** thêm **§12 "Kiến trúc đích (chưa triển khai)"** theo đặc tả dưới. §1–§11 giữ nguyên.
- **Pha 2 (khi code đã tách xong):** gộp §12 vào §1–§11 theo code thật, xóa §12.

Plan này là **nguồn duy nhất** ghi cách chia đích (plan triển khai trước đó đã được xóa theo chủ ý người dùng). Khi lập plan triển khai mới, lấy đặc tả từ đây. Luồng học chính (path, bài học, bài ôn, luật mở khóa): [main-learning-pipeline.md](./main-learning-pipeline.md).

**Đã chốt 2026-09-28** (người dùng chọn cách chia này, so với hai phương án: giữ learning-support nhận thêm catalog, hoặc không tách). Đã đối chiếu code tại `2079964` ([scout report](../reports/scout-260928-2133-codebase-deep-read-report.md)): cụm video/từ vựng chỉ có một FK ra ngoài (`learning_videos.topic_id → topics`), không bảng content nào tham chiếu ngược; content đã tách nhánh snapshot `VOCABULARY`/`GRAMMAR`; assessment và game chỉ giữ id logic (không FK, không gọi content); ai-learning không gọi video/từ vựng; không seed nào dùng `LESSON`; cổng 8081 và 5437 còn trống.

## Đặc tả kiến trúc đích

### Service, DB, route

| Service | Cổng | DB | Route Gateway | Thay đổi |
| --- | --- | --- | --- | --- |
| user-service | 8085 | `user_db` (local 5432) | `/auth/**`, `/api/users/**`, **`/api/learning-support/{activities,streak}/**`** | + `learning_activities`, `streaks`, `examiner_profiles` |
| access-service | 8084 | `access_db` (local 5432) | `/api/access/**` | — |
| content-service | 8082 | `content_db` (local 5432) | `/api/content/**` (trừ video, từ vựng) | − video, từ vựng; + bài học; `package_type` thêm `TOPIC_TEST` (giữ `LESSON`); gói đề gắn topic, nhiều mã đề; KP bỏ band |
| **library-service** (mới) | 8081 | `library_db` (compose 5437) | `/api/content/videos/**`, `/api/content/vocabulary/**`, `/api/content/admin/vocabulary/**`, `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` | Service mới |
| assessment-service | 8083 | `assessment_db` (local 5432) | `/api/assessments/**` | Tự chấm câu khách quan cho đề `TOPIC_TEST`; lấy đề từ content; tự quyết loại attempt theo loại gói; không gọi user-service (không đổi bảng) |
| game-service | 8087 | `game_db` (compose 5435) | `/api/games/**`, `/ws/games/**` | Snapshot `VOCABULARY` lấy từ library |
| community-service | 8089 | `community_db` (local 5432 / compose 5434) | `/api/community/**` | — |
| notification-service | 8088 | `notification_db` (local 5432) | `/api/notifications/**` | + 5 bảng (theo `DATABASE_V5.md` §10) |
| ai-learning-service | 8000 | `ai_learning_db` (compose 5436) | `/api/ai-learning/**` | + API học bài (mở bài, chấm bài tập nhúng, tiến độ, mở khóa, bài ôn và luyện thêm bằng gói câu mới, giao mã đề); + 6 bảng, − 1 bảng; path gắn học viên thay vì goal |
| ~~learning-support-service~~ | ~~8086~~ | ~~`learning_support_db` (compose 5433)~~ | — | **Xóa** |

Mọi public path giữ nguyên; Gateway (`order: -1`) trỏ theo nhóm path. Reactor vẫn 12 module (thêm library, bỏ learning-support).

### Bảng theo service

Tên bảng lấy từ migration hiện tại (commit `2079964`). **Đậm** = bảng mới hoặc chuyển tới service đó. Cột, FK và ràng buộc của bảng mới ở mục "Chi tiết bảng mới" ngay dưới.

| Service | DB (số bảng) | Nhóm | Bảng |
| --- | --- | --- | --- |
| **user-service** | `user_db` (11) | Tài khoản, xác thực | `users`, `roles`, `user_roles`, `refresh_tokens`, `account_action_tokens`, `oauth_identities` |
| | | Hồ sơ học viên | `learner_profiles`, `learning_goals` |
| | | Hồ sơ người chấm (mới) | **`examiner_profiles`** |
| | | Từ learning-support | **`learning_activities`**, **`streaks`** |
| **access-service** | `access_db` (9), không đổi | Gói, subscription | `plans`, `plan_features`, `subscriptions` |
| | | Key kích hoạt | `key_products`, `activation_keys`, `key_activations` |
| | | Điểm | `point_wallets`, `point_ledger_entries` |
| | | Outbox | `outbox_events` |
| **content-service** | `content_db` (17) | Curriculum | `topics`, `knowledge_points` |
| | | Câu hỏi | `questions`, `question_versions`, `question_knowledge_points` |
| | | Đề, gói | `content_packages`, `content_package_versions`, `content_sections`, `section_questions` |
| | | Asset | `content_assets`, `content_asset_links` |
| | | Bài học (mới) | **`lessons`**, **`lesson_blocks`**, **`lesson_block_vocabulary`**, **`lesson_block_questions`**, **`lesson_knowledge_points`** |
| | | Outbox | `outbox_events` |
| | | Chuyển đi → library | `vocabulary_items`, `vocabulary_senses`, `learning_videos`, `video_segments`, `video_segment_lexical_entries` |
| **library-service** (mới) | `library_db` (11) | Catalog (từ content) | **`vocabulary_items`**, **`vocabulary_senses`**, **`learning_videos`**, **`video_segments`**, **`video_segment_lexical_entries`** |
| | | Thư viện học cá nhân (từ learning-support) | **`flashcard_decks`**, **`flashcards`**, **`flashcard_deck_items`**, **`notes`**, **`video_learning_progress`**, **`saved_video_segments`** |
| **assessment-service** | `assessment_db` (16), không đổi | Làm bài | `assessment_attempts`, `attempt_sections`, `attempt_items`, `attempt_item_knowledge_points`, `attempt_responses`, `learner_submissions`, `video_practice_attempts` |
| | | Kết quả, chấm | `assessment_results`, `skill_scores`, `item_results`, `item_result_knowledge_judgments`, `error_analysis_items`, `human_reviews` (chưa có code) |
| | | Chấm AI | `grading_jobs`, `grading_point_costs` |
| | | Outbox | `outbox_events` |
| **game-service** | `game_db` (12), không đổi | Phòng, trận | `game_rooms`, `game_room_members`, `game_matches`, `game_match_players`, `game_events` |
| | | Phiên chơi | `game_sessions`, `game_answers` |
| | | Quiz, leaderboard (chưa có code) | `quiz_events`, `quiz_participations`, `leaderboard_periods`, `leaderboard_entries` |
| | | Outbox | `outbox_events` |
| **community-service** | `community_db` (4), không đổi | Bài viết | `posts`, `comments`, `post_reactions` |
| | | Outbox | `outbox_events` |
| **notification-service** | `notification_db` (5, mới) | Cấu hình, thiết bị | **`notification_preferences`**, **`push_devices`** |
| | | Lịch nhắc, thông báo | **`reminder_schedules`**, **`notifications`** |
| | | Lần gửi | **`notification_deliveries`** |
| **ai-learning-service** | `ai_learning_db` (22) | Mastery path | `mastery_paths`, `mastery_path_knowledge_point_details`, `mastery_interactions`, `mastery_events`, `mastery_learning_evidence` |
| | | Kết quả thi | `formal_assessment_result_versions`, `pending_formal_assessment_results` |
| | | Tutor | `sessions`, `turns`, `messages`, `session_materials`, `learner_memory` |
| | | Practice | `notebook_entries`, `practice_review_state`, `practice_review_events` |
| | | Hạn mức LLM | `llm_daily_usage` |
| | | Tiến độ học bài (mới) | **`topic_progress`**, **`lesson_progress`**, **`lesson_exercise_submissions`**, **`path_review_items`**, **`path_review_sets`**, **`topic_test_assignments`** |
| ~~learning-support-service~~ | ~~`learning_support_db`~~ | Xóa | 2 bảng sang user, 6 bảng sang library; `outbox_events` bỏ |

### Chi tiết bảng mới

**`user_db`**
- `learning_activities`, `streaks`: không FK tới `users`.
- `examiner_profiles` (cùng khuôn `learner_profiles`):

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `user_id` | uuid | PK, FK → `users` ON DELETE CASCADE | Người chấm |
| `display_name` | varchar(150) | NOT NULL | Tên hiển thị |
| `avatar_reference` | varchar(500) | NULL | Ảnh đại diện |
| `bio` | text | NULL | Giới thiệu |
| `timezone` | varchar(50) | NOT NULL, mặc định `Asia/Ho_Chi_Minh` | Múi giờ |
| `certification` | text | NULL | Chứng chỉ, kinh nghiệm chấm |
| `can_grade_writing` | boolean | NOT NULL, mặc định false | Chấm được Writing |
| `can_grade_speaking` | boolean | NOT NULL, mặc định false | Chấm được Speaking |
| `status` | varchar(20) | NOT NULL, CHECK `ACTIVE`/`PAUSED`, mặc định `ACTIVE` | Có đang nhận bài không |
| `created_at`, `updated_at` | timestamp | NOT NULL | |

- Chỉ user có role `EXAMINER` mới có hồ sơ (kiểm ở use case; DB không kiểm được vì role nằm ở `user_roles`).
- `EXAMINER` tự sửa `display_name`, `avatar_reference`, `bio`, `timezone`. `ADMIN` sửa `certification`, `can_grade_*`, `status`.
- Số bài tối đa và việc phân công vẫn là luật của assessment (`human_reviews`), không đặt ở đây.

**`content_db`** (mô hình học: [brainstorm bài học](../reports/brainstorm-260928-2215-lesson-learning-flow-report.md))
- `topics` + `required_feature_key` (NULL = miễn phí; premium theo chủ đề; chưa dùng trong MVP). Không có `test_package_id` (V5.2: một topic có nhiều mã đề, đề trỏ về topic).
- `content_packages.package_type` thêm `TOPIC_TEST`, giữ `LESSON`; + `topic_id` (FK `topics`, bắt buộc với `TOPIC_TEST`; một topic nhiều mã đề). `PRACTICE_SET` ≥ 3 câu là ngân hàng câu cho bài ôn và luyện thêm.
- `knowledge_points` bỏ `band_min`, `band_max` (V5.2); band của topic giữ để hiển thị.
- `lessons`: `id`, `topic_id` FK, `code` UNIQUE, `title`, `summary`, `sort_order` (UQ theo `topic_id`, thứ tự học tuần tự), `status` `DRAFT`/`PUBLISHED`/`ARCHIVED`, `created_at`, `updated_at`. Không versioning.
- `lesson_blocks`: `id`, `lesson_id` FK, `sort_order` (UQ theo `lesson_id`), `block_type` `TEXT`/`ASSET`/`VOCABULARY`/`EXERCISE`, `text_content` (markdown cho TEXT; hướng dẫn tùy chọn cho EXERCISE), `asset_id` FK `content_assets` NULL (cho ASSET: IMAGE/AUDIO/PASSAGE); CHECK theo `block_type`.
- `lesson_block_vocabulary`: PK(`block_id`, `vocabulary_sense_id`); `vocabulary_sense_id` là id logic → library; `sort_order`.
- `lesson_block_questions`: PK(`block_id`, `question_version_id`), INDEX(`question_version_id`); ghim phiên bản đã publish; `sort_order`. Chỉ câu tự chấm; câu thuộc đề cuối hoặc gói luyện tập của cùng chủ đề không được gắn vào đây và ngược lại (kiểm ở use case).
- `lesson_knowledge_points`: PK(`lesson_id`, `knowledge_point_id`), INDEX(`knowledge_point_id`) (path tìm bài dạy KP yếu). KP cùng topic với bài (kiểm ở use case).
- `question_knowledge_points` (đã có): mỗi câu chỉ gắn KP chính, `weight` 1.0 (engine không dùng `weight`).
- Đặc tả đầy đủ và `answer_spec` theo dạng câu: `.sdd/database/DATABASE_V5.md` §4.1, §4.2, §5.1, §5.5, §5.13–§5.17 (V5.1, V5.2).

**`library_db`**
- Catalog: chỉ `ADMIN`, `CONTENT_AUTHOR` ghi; `learning_videos.topic_id` là id logic → content.
- Thư viện học cá nhân: chủ sở hữu tự ghi; FK `flashcards` → `vocabulary_senses`, `video_learning_progress` → `learning_videos`, `saved_video_segments` → `learning_videos`, `video_segments`.
- Không có `outbox_events` (bảng của learning-support chưa từng được dùng).
- Xóa catalog: chỉ soft-delete (`status` INACTIVE). FK từ dữ liệu người học tới catalog dùng `ON DELETE RESTRICT`, không CASCADE (xóa catalog không được xóa thẻ/tiến độ/đoạn đã lưu) và không SET NULL (thẻ `VOCABULARY_SENSE` bắt buộc có sense). FK nội bộ catalog (item → sense, video → segment → lexical entry) giữ như content hiện tại.

**`notification_db`** (theo `DATABASE_V5.md` §10; mọi `user_id` là id logic → user)
- `notification_preferences`: `id` PK; `user_id`; `channel` (`IN_APP`/`PUSH`/`EMAIL`); `is_enabled` boolean; `preferred_local_time` time NULL; `timezone` varchar(50); `created_at`, `updated_at`. UQ(`user_id`, `channel`).
- `push_devices`: `id` PK; `user_id`; `installation_id`; `platform` (`ANDROID`/`IOS`/`WEB`); `endpoint_reference` (token gửi push); `status` (`ACTIVE`/`REVOKED`); `last_seen_at`; `revoked_at` NULL; `created_at`, `updated_at`. UQ(`user_id`, `installation_id`).
- `reminder_schedules`: `id` PK; `user_id`; `trigger_type` (`DAILY_STUDY`/`DUE_REVIEW`/`PLAN_REMINDER`); `channel`; `scheduled_at` timestamptz; `deduplication_key` varchar(255); `status` (`PENDING`/`PROCESSED`/`CANCELLED`); `created_at`, `updated_at`. UQ(`user_id`, `deduplication_key`).
- `notifications`: `id` PK; `user_id`; `schedule_id` NULL FK → `reminder_schedules`; `title`; `body`; `target_type` NULL; `target_id` uuid NULL; `deduplication_key` varchar(255); `read_at` NULL; `created_at`. UQ(`user_id`, `deduplication_key`), dùng event id hoặc schedule id làm khóa để nhận event trùng không sinh thông báo trùng (bổ sung so với V5).
- `notification_deliveries`: `id` PK; `notification_id` FK → `notifications`; `channel` (`PUSH`/`EMAIL`); `push_device_id` NULL FK → `push_devices`; `provider`; `attempt_number` integer; `status` (`PENDING`/`SENT`/`FAILED`); `provider_message_id` NULL; `idempotency_key` varchar(255) UNIQUE; `attempted_at`; `sent_at` NULL; `failure_reason` NULL; `created_at`.
- Không có `outbox_events` (V5 có, nhưng chưa ai tiêu thụ event của notification).

**`ai_learning_db`** (tiến độ theo học viên, không theo goal)
- `topic_progress`: PK(`user_id`, `topic_id`); `sequence_order` (thứ tự học: topic có bài và có đề, theo `sort_order`); `status` `LOCKED`/`IN_PROGRESS`/`PASSED` (mọi topic trong thứ tự học có dòng; PASSED một chiều); `passed_at`; `updated_at`.
- `lesson_progress`: PK(`user_id`, `lesson_id`); `topic_id`; `lesson_sort_order`; `knowledge_point_ids` uuid[] (chép từ content khi mở hoặc nộp bài, để tra bài dạy KP mà không gọi content); `passed_block_ids` text[]; `completed_at` NULL; `updated_at`.
- `lesson_exercise_submissions`: `id`; `user_id`; `lesson_id`; `block_id`; `request_id` UNIQUE (chống nộp trùng); `answers` JSONB; `block_passed`; `response` JSONB (trả lại khi nộp trùng); `submitted_at`. Chưa đạt chỉ trả đúng/sai; khối đã từng đạt nộp lại không ghi mastery.
- `path_review_items`: `id`; `user_id`; `knowledge_point_id`; `lesson_id` (bài có lý thuyết để xem lại); `status` `PENDING`/`DONE`; `created_at`; `done_at` NULL. UQ(`user_id`, `knowledge_point_id`) với `status = PENDING`. Chèn cả cho bài vừa học (luyện thêm theo mastery); `DONE` khi đạt một gói câu mới ≥ 70%.
- `path_review_sets` (V5.2): `id`; `review_item_id` FK; `user_id`; `package_id`; `package_version_id`; `assigned_at`; `submitted_at` NULL; `passed` NULL; `request_id` UNIQUE NULL; `response` JSONB NULL. Một set mở mỗi bài ôn; trượt thì giao gói khác.
- `topic_test_assignments` (V5.2): `id`; `user_id`; `topic_id`; `package_id`; `package_version_id`; `assigned_at`; `consumed_attempt_id` NULL; `consumed_at` NULL; `percent` NULL. Mỗi lần giao dùng một lần; consumer tra topic tại đây, không gọi content.
- `mastery_paths`, `pending_formal_assessment_results`: đổi từ theo (`user_id`, `learning_goal_id`) sang theo `user_id` (migration mới); một path mỗi học viên. Xóa `mastery_path_knowledge_point_bands` (KP không còn band).
- Luật tạo path, mở khóa, bài ôn và luyện thêm: [main-learning-pipeline.md](./main-learning-pipeline.md) §4. Đặc tả bảng: `DATABASE_V5.md` §7.21–§7.26 (V5.2). Plan triển khai: `plans/260929-1640-lesson-learning-pipeline-mvp`.

### Giao tiếp mới

| Caller | Callee | Endpoint | Base URL |
| --- | --- | --- | --- |
| library | content | `GET /api/content/topics/{id}` (mới; kiểm topic khi ghi video) | `CONTENT_SERVICE_URL` |
| game | library | `/internal/game-content/snapshots` (nhánh `VOCABULARY`) | `LIBRARY_SERVICE_URL` |
| game | content | `/internal/game-content/snapshots` (chỉ `GRAMMAR`) | `CONTENT_SERVICE_URL` |
| ai-learning | content | `/internal/learning-content/`: `topic-sequence`, `topics/{id}/lessons`, `lessons/{id}`, `topics/{id}/test-packages`, `practice-sets/search`, `package-versions/{id}` (kèm `answer_spec`, KP của câu hỏi) | `AI_LEARNING_CONTENT_SERVICE_BASE_URL` |
| assessment | content | `GET /internal/learning-content/package-versions/{id}` (đề + đáp án; thay `/internal/assessment-content/knowledge-point-mappings`). Assessment không còn gọi user-service lấy goal | base URL content có sẵn |
| assessment | ai-learning | `AssessmentCompleted.v2`: thêm `package_version_id` (producer luôn gửi); `learning_goal_id` luôn null; bỏ luật "không goal thì không phát"; cần duyệt đổi contract | RabbitMQ có sẵn |

API học viên qua ai-learning (`/api/ai-learning/topics`, `/topics/{id}/lessons`, `/lessons/{id}`, nộp khối, hoàn thành bài không có bài tập, bài ôn và luyện thêm `/reviews/{id}`, giao mã đề `/topics/{id}/test-assignments`); chưa đạt chỉ trả đúng/sai, không bao giờ trả `answer_spec`. Premium (ai-learning → access) hoãn khỏi MVP. Contract `answer_spec` + bộ test vector ghi vào `docs/contracts/` (game, assessment, ai-learning cùng chấm theo nó).

Id logic đổi đích: `video_id` (assessment `video_practice_attempts`), `vocabulary_sense_id` (game `game_answers`) → library.

`/internal/game-content/snapshots` tồn tại ở cả content và library với cùng request/response; ghi contract vào `docs/contracts/` (game chọn đích theo `learningDomain`).

### Quyết định chính

| Quyết định | Lý do | Trade-off |
| --- | --- | --- |
| Tách video + từ vựng khỏi content | Cụm duy nhất không bám KP; chỉ mất FK `learning_videos.topic_id` | Thêm một service Java |
| Gom dữ liệu người học có tham chiếu video/từ vựng vào library | FK thật tới sense/video/segment; join từ vựng khi đọc thẻ | Library có hai loại quyền (catalog vs chủ sở hữu) |
| Activity, streak sang user-service; giải thể learning-support | library chỉ gồm catalog + thư viện học cá nhân; user đã giữ learner profile, learning goal nên activity/streak hợp ở đó | Streak vẫn do client tự khai (`PUT /streak`); nếu sau này activity suy ra từ event của assessment/game/library thì user-service (lõi auth) thành consumer của nhiều service |
| Catalog chỉ soft-delete, FK người học → catalog `RESTRICT` | Không mất dữ liệu người học khi sửa catalog; giữ invariant thẻ `VOCABULARY_SENSE` | Mục catalog đã dùng không xóa cứng được |
| Bài học có model riêng gắn KP | Bài học không còn bị ép vào package/section | Không versioning bài học |
| Học theo chủ đề → bài, tuần tự, khóa bài sau; một path mỗi học viên, tạo khi bắt đầu học, ban đầu theo `sort_order`; thứ tự học = chủ đề có bài và có đề; KP yếu kèm câu sai thì giao gói câu mới cùng KP (tính cả bài vừa học = luyện thêm theo mastery); giấu đáp án tới khi đạt; mã đề dùng một lần; xếp lại chủ đề chưa học hoãn (V5.2) | Học như web luyện thi; path khác dần theo kết quả; goal, band mục tiêu, ngày thi, số phút, placement không dùng để tạo path | Đổi hướng so với `FEATURE_TREE_V2.md` (tutor là nơi học chính) → spec cần cập nhật |
| ai-learning chấm bài tập nhúng và gói luyện tập, giữ tiến độ, là cổng mở bài; assessment chấm đề cuối chủ đề, từ chối gói `PRACTICE_SET` | Khớp spec V2 §7 (micro question không cần attempt formal); khóa và premium chặn ở server | Ba nơi chấm theo `answer_spec` (game, assessment, ai-learning); `grade_answer` (port DeepTutor) chấm gần đúng nên không dùng được |
| Hồ sơ người chấm `examiner_profiles` ở user-service, gồm năng lực chấm | Hồ sơ người dùng thuộc context danh tính, cùng chỗ `learner_profiles` | Khi phân công, assessment phải gọi user-service để lấy người chấm đủ điều kiện (endpoint mới) |
| Notification theo 5 bảng của V5, thêm khóa chống trùng, bỏ outbox | Đã thiết kế trong `DATABASE_V5.md`; consumer event phải idempotent | `push_devices`, `notification_deliveries` chỉ dùng được khi có nhà cung cấp gửi (FCM, email) |
| Giữ mọi public path qua Gateway | AGENTS §3.5 giữ path tương thích | Tên path `/api/learning-support/**` không còn khớp service |

### Điểm mở

- Hồ sơ người chấm được tạo khi `ADMIN` gán role `EXAMINER`, hay người chấm tự tạo lần đầu đăng nhập?
- Nguồn tạo notification: event nào (ví dụ `AssessmentCompleted.v2` → "bài đã được chấm", cần queue riêng cho notification trên exchange `assessment.events`) và reminder nào (học hằng ngày, thẻ đến hạn).
- Nhà cung cấp gửi push/email (FCM, SMTP) là hạ tầng mới, cần duyệt riêng (AGENTS §2). Gửi token quên mật khẩu của user-service cũng phụ thuộc điểm này.
- Bài học: đã chốt ngày 2026-09-29 (bài mới thêm vào chủ đề đã đạt, dạng câu hỏi của `answer_spec`, luật chấm điền từ, luật bài ôn; xem [main-learning-pipeline.md](./main-learning-pipeline.md) §8). Còn mở: vai trò tutor trong bài. Tutor đọc bài qua `/reading/sections/{id}` vẫn chạy với package `PRACTICE_SET`; giữ `LESSON` (V5.2, `GetReadingPassageUseCase` còn dùng). Còn mở: giới hạn số lần trượt gói ôn.
- Premium hoãn khỏi MVP; lỗ entitlement và các endpoint điểm của access chuyển sang plan bảo mật riêng.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Thêm mục kiến trúc đích](./phase-01-them-muc-kien-truc-dich.md) | Pending |
| 2 | [Gộp kiến trúc đích vào tài liệu sau khi tách xong](./phase-02-gop-kien-truc-dich-sau-khi-tach-xong.md) | Pending |

## Dependencies

- Pha 1: không phụ thuộc.
- Pha 2: chờ code đã tách thật (library-service tồn tại, learning-support đã xóa). Hiện **chưa có plan triển khai** — cần lập lại từ đặc tả trên.
- `examiner_profiles` và bảng notification triển khai được độc lập với việc tách service; pha 2 chỉ gộp vào §1–§11 phần đã có code.
- Không sửa `.sdd/global/system-architecture.md` (baseline 2026-09-18, AGENTS đánh dấu lỗi thời).

## Acceptance criteria

- [ ] Sau pha 1: §12 có đủ nội dung đặc tả, ghi "chưa triển khai"; §1–§11 không đổi.
- [ ] Sau pha 2: §1–§11 khớp code sau khi tách; không còn §12; không còn `learning-support-service`/`learning_support_db` ngoài ghi chú lịch sử có chủ đích.
